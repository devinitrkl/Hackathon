package com.example.hackathon.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.repository.OrderRepository;
import com.example.hackathon.repository.ReassignmentSuggestionRepository;
import com.example.hackathon.service.AgentService;
import com.example.hackathon.service.OrderService;

@Component
public class ReplanEventListener {

	private static final Logger log = LoggerFactory.getLogger(ReplanEventListener.class);

	private final OrderRepository orderRepository;
	private final ReassignmentSuggestionRepository suggestionRepository;
	private final OrderService orderService;
	private final AgentService agentService;

	public ReplanEventListener(
			OrderRepository orderRepository,
			ReassignmentSuggestionRepository suggestionRepository,
			OrderService orderService,
			AgentService agentService) {
		this.orderRepository = orderRepository;
		this.suggestionRepository = suggestionRepository;
		this.orderService = orderService;
		this.agentService = agentService;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAgentOffline(AgentOfflineEvent event) {
		log.info("Starting async re-plan for offline agent {}", event.agentId());

		try {
			Agent failedAgent = agentService.getAgentOrThrow(event.agentId());

			// Case 1: orders still directly assigned to the agent who just went offline.
			List<Order> strandedOrders = orderRepository.findByAssignedAgentIdAndStatus(
					event.agentId(), OrderStatus.ASSIGNED);

			// Case 2: orders already pending whose current recommendation was THIS agent,
			// which is now invalid because the recommended agent just went offline.
			List<ReassignmentSuggestion> staleSuggestions = suggestionRepository
					.findByStatusAndRecommendedAgentId(SuggestionStatus.PENDING, event.agentId());

			int affected = strandedOrders.size() + staleSuggestions.size();
			log.info("Re-plan scope for agent {}: {} stranded order(s), {} stale suggestion(s)",
					event.agentId(), strandedOrders.size(), staleSuggestions.size());

			// Track virtual load so successive orders in this batch are spread across agents
			// rather than all being piled onto the single least-loaded agent.
			Map<String, Integer> pendingAllocations = new HashMap<>();

			for (Order order : strandedOrders) {
				replanOrder(order, failedAgent, affected, pendingAllocations);
			}

			for (ReassignmentSuggestion stale : staleSuggestions) {
				replanOrder(stale.getOrder(), failedAgent, affected, pendingAllocations);
			}
		}
		catch (Exception ex) {
			log.error("Async re-plan failed for agent {}: {}", event.agentId(), ex.getMessage(), ex);
		}
	}

	private void replanOrder(Order order, Agent failedAgent, int strandedCount,
			Map<String, Integer> pendingAllocations) {
		try {
			ReassignmentSuggestion suggestion = orderService.createSuggestionForOrder(
					order,
					TriggerReason.AGENT_OFFLINE,
					Optional.of(failedAgent),
					strandedCount,
					pendingAllocations);
			// Record this recommendation so the next order in the batch sees the virtual load
			String recommendedId = suggestion.getRecommendedAgent().getId();
			pendingAllocations.merge(recommendedId, 1, Integer::sum);
			log.info("Queued re-plan suggestion for order {} → agent {} (pending load now {})",
					order.getId(), recommendedId, pendingAllocations.get(recommendedId));
		}
		catch (Exception ex) {
			log.error("Failed to re-plan order {}: {}", order.getId(), ex.getMessage(), ex);
		}
	}
}
