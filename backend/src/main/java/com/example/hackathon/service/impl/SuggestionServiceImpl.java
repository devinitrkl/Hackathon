package com.example.hackathon.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.dto.Dtos.SuggestionResponse;
import com.example.hackathon.dto.Dtos.UpdateSuggestionRequest;
import com.example.hackathon.exception.ConflictException;
import com.example.hackathon.exception.ResourceNotFoundException;
import com.example.hackathon.repository.AgentRepository;
import com.example.hackathon.repository.OrderRepository;
import com.example.hackathon.repository.ReassignmentSuggestionRepository;
import com.example.hackathon.service.SuggestionService;
import com.example.hackathon.web.DtoMapper;

@Service
public class SuggestionServiceImpl implements SuggestionService {

	private final ReassignmentSuggestionRepository suggestionRepository;
	private final OrderRepository orderRepository;
	private final AgentRepository agentRepository;
	private final DtoMapper mapper;

	public SuggestionServiceImpl(
			ReassignmentSuggestionRepository suggestionRepository,
			OrderRepository orderRepository,
			AgentRepository agentRepository,
			DtoMapper mapper) {
		this.suggestionRepository = suggestionRepository;
		this.orderRepository = orderRepository;
		this.agentRepository = agentRepository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SuggestionResponse> listSuggestions(SuggestionStatus status) {
		List<ReassignmentSuggestion> suggestions = status == null
				? suggestionRepository.findAll()
				: suggestionRepository.findByStatus(status);
		return suggestions.stream().map(mapper::toSuggestionResponse).toList();
	}

	@Override
	@Transactional
	public SuggestionResponse updateSuggestion(String suggestionId, UpdateSuggestionRequest request) {
		ReassignmentSuggestion suggestion = suggestionRepository.findById(suggestionId)
				.orElseThrow(() -> new ResourceNotFoundException("Suggestion not found: " + suggestionId));

		if (suggestion.getStatus() != SuggestionStatus.PENDING) {
			throw new ConflictException("Suggestion is already " + suggestion.getStatus());
		}

		if (request.status() == SuggestionStatus.PENDING) {
			throw new ConflictException("Cannot set suggestion back to PENDING");
		}

		if (request.status() == SuggestionStatus.ACCEPTED) {
			if (suggestion.getRecommendedAgent().getStatus() == AgentStatus.OFFLINE) {
				throw new ConflictException(
						"Recommended agent " + suggestion.getRecommendedAgent().getId()
								+ " is OFFLINE; cannot accept this suggestion. Wait for a re-plan.");
			}
			suggestion.setStatus(request.status());
			applyAcceptance(suggestion);
		}
		else if (request.status() == SuggestionStatus.REJECTED) {
			suggestion.setStatus(SuggestionStatus.REJECTED);
			Order order = suggestion.getOrder();
			if (order.getStatus() == OrderStatus.REASSIGNMENT_PENDING) {
				order.setStatus(OrderStatus.ASSIGNED);
				orderRepository.save(order);
			}
		}

		return mapper.toSuggestionResponse(suggestionRepository.save(suggestion));
	}

	private void applyAcceptance(ReassignmentSuggestion suggestion) {
		Order order = suggestion.getOrder();
		Agent previousAgent = order.getAssignedAgent();
		Agent newAgent = suggestion.getRecommendedAgent();

		if (previousAgent != null) {
			previousAgent.setActiveOrderCount(Math.max(0, previousAgent.getActiveOrderCount() - 1));
			if (previousAgent.getActiveOrderCount() == 0 && previousAgent.getStatus() != AgentStatus.OFFLINE) {
				previousAgent.setStatus(AgentStatus.AVAILABLE);
			}
			agentRepository.save(previousAgent);
		}

		order.setAssignedAgent(newAgent);
		order.setStatus(OrderStatus.REASSIGNED);
		orderRepository.save(order);

		newAgent.setActiveOrderCount(newAgent.getActiveOrderCount() + 1);
		if (newAgent.getStatus() == AgentStatus.AVAILABLE) {
			newAgent.setStatus(AgentStatus.BUSY);
		}
		agentRepository.save(newAgent);
	}
}
