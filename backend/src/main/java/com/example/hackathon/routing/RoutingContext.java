package com.example.hackathon.routing;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.TriggerReason;

/**
 * pendingAllocations tracks "virtual" extra load for agents that have already been
 * recommended in this batch but whose orders haven't been accepted yet.
 * Used so that batch re-planning distributes orders across agents instead of
 * piling them all on the single least-loaded agent.
 */
public record RoutingContext(
		Order order,
		List<Agent> availableAgents,
		TriggerReason triggerReason,
		Optional<Agent> failedAgent,
		int strandedOrderCount,
		Map<String, Integer> pendingAllocations) {

	public int effectiveLoad(Agent agent) {
		return agent.getActiveOrderCount() + pendingAllocations.getOrDefault(agent.getId(), 0);
	}
}
