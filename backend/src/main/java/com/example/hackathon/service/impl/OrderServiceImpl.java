package com.example.hackathon.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.dto.Dtos.CreateOrderRequest;
import com.example.hackathon.dto.Dtos.OrderResponse;
import com.example.hackathon.exception.ConflictException;
import com.example.hackathon.exception.ResourceNotFoundException;
import com.example.hackathon.repository.AgentRepository;
import com.example.hackathon.repository.OrderRepository;
import com.example.hackathon.repository.ReassignmentSuggestionRepository;
import com.example.hackathon.routing.RoutingContext;
import com.example.hackathon.routing.RoutingEngine;
import com.example.hackathon.routing.RoutingRecommendation;
import com.example.hackathon.service.OrderService;
import com.example.hackathon.web.DtoMapper;

@Service
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final AgentRepository agentRepository;
	private final ReassignmentSuggestionRepository suggestionRepository;
	private final RoutingEngine routingEngine;
	private final DtoMapper mapper;

	public OrderServiceImpl(
			OrderRepository orderRepository,
			AgentRepository agentRepository,
			ReassignmentSuggestionRepository suggestionRepository,
			RoutingEngine routingEngine,
			DtoMapper mapper) {
		this.orderRepository = orderRepository;
		this.agentRepository = agentRepository;
		this.suggestionRepository = suggestionRepository;
		this.routingEngine = routingEngine;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request) {
		if (orderRepository.existsById(request.id())) {
			throw new ConflictException("Order already exists: " + request.id());
		}

		Agent agent = agentRepository.findById(request.assignedAgentId())
				.orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + request.assignedAgentId()));

		if (agent.getStatus() == AgentStatus.OFFLINE) {
			throw new ConflictException("Cannot assign order to offline agent");
		}

		Order order = new Order(request.id(), request.description(), agent, OrderStatus.ASSIGNED, Instant.now());
		orderRepository.save(order);

		agent.setActiveOrderCount(agent.getActiveOrderCount() + 1);
		if (agent.getStatus() == AgentStatus.AVAILABLE && agent.getActiveOrderCount() > 0) {
			agent.setStatus(AgentStatus.BUSY);
		}
		agentRepository.save(agent);

		return mapper.toOrderResponse(order, Optional.empty());
	}

	@Override
	@Transactional(readOnly = true)
	public List<OrderResponse> listOrders(OrderStatus status) {
		List<Order> orders = status == null ? orderRepository.findAll() : orderRepository.findByStatus(status);
		return orders.stream()
				.map(order -> {
					Optional<ReassignmentSuggestion> pending = suggestionRepository
							.findFirstByOrderAndStatusOrderByCreatedAtDesc(order, SuggestionStatus.PENDING);
					return mapper.toOrderResponse(order, pending);
				})
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Order getOrderOrThrow(String orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
	}

	@Override
	@Transactional
	public ReassignmentSuggestion createSuggestion(String orderId, TriggerReason triggerReason) {
		Order order = getOrderOrThrow(orderId);
		return createSuggestionForOrder(order, triggerReason, Optional.empty(), 1, Map.of());
	}

	@Override
	@Transactional
	public ReassignmentSuggestion createSuggestionForOrder(
			Order order,
			TriggerReason triggerReason,
			Optional<Agent> failedAgent,
			int strandedOrderCount,
			Map<String, Integer> pendingAllocations) {

		if (triggerReason == TriggerReason.AGENT_OFFLINE) {
			Optional<ReassignmentSuggestion> existing = suggestionRepository
					.findFirstByOrderAndStatusOrderByCreatedAtDesc(order, SuggestionStatus.PENDING);
			if (existing.isPresent()) {
				ReassignmentSuggestion current = existing.get();
				// True duplicate: a re-plan suggestion already exists and its recommended
				// agent is still usable -> idempotent skip (same agent flipping offline twice).
				if (current.getTriggerReason() == TriggerReason.AGENT_OFFLINE
						&& current.getRecommendedAgent().getStatus() != AgentStatus.OFFLINE) {
					return current;
				}
				// Stale: the recommended agent has since gone offline (or it was a manual
				// suggestion) -> supersede it and produce a fresh recommendation.
				current.setStatus(SuggestionStatus.REJECTED);
				suggestionRepository.save(current);
			}
		}

		List<Agent> availableAgents = agentRepository.findAll().stream()
				.filter(agent -> agent.getStatus() != AgentStatus.OFFLINE)
				.toList();

		RoutingContext context = new RoutingContext(
				order, availableAgents, triggerReason, failedAgent, strandedOrderCount, pendingAllocations);

		List<RoutingRecommendation> recommendations = routingEngine.recommend(context);
		if (recommendations.isEmpty()) {
			throw new IllegalStateException("No routing recommendation produced for order " + order.getId());
		}
		RoutingRecommendation recommendation = recommendations.getFirst();
		Agent recommendedAgent = agentRepository.findById(recommendation.agentId())
				.orElseThrow(() -> new ResourceNotFoundException("Recommended agent not found: " + recommendation.agentId()));

		ReassignmentSuggestion suggestion = new ReassignmentSuggestion();
		suggestion.setOrder(order);
		suggestion.setRecommendedAgent(recommendedAgent);
		suggestion.setConfidence(recommendation.confidence());
		suggestion.setReasoning(recommendation.reasoning());
		suggestion.setStatus(SuggestionStatus.PENDING);
		suggestion.setTriggerReason(triggerReason);
		suggestion.setCreatedAt(Instant.now());

		order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
		orderRepository.save(order);
		return suggestionRepository.save(suggestion);
	}
}
