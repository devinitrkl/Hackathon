package com.example.hackathon.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.routing.RoutingContext;

@Component
public class PromptBuilder {

	public String buildInitialPrompt(RoutingContext context) {
		Order order = context.order();
		return """
				You are a delivery dispatch advisor for ZipRun. Recommend the best available agent for this order.

				ORDER:
				- id: %s
				- description: %s
				- currently assigned to: %s (%s)

				AVAILABLE AGENTS:
				%s

				Respond with JSON only, no markdown:
				{"agentId":"AGT-xxx","confidence":0.85,"reasoning":"plain English explanation for ops"}
				""".formatted(
				order.getId(),
				order.getDescription(),
				agentLabel(order.getAssignedAgent()),
				order.getAssignedAgent() != null ? order.getAssignedAgent().getId() : "none",
				formatRoster(context.availableAgents(), context));
	}

	public String buildReplanPrompt(RoutingContext context) {
		Order order = context.order();
		String failedAgentInfo = context.failedAgent()
				.map(agent -> agent.getName() + " (" + agent.getId() + ")")
				.orElse("unknown agent");
		return """
				SITUATION REPORT — AGENT OFFLINE RECOVERY

				An agent has gone offline mid-shift. Previous assignments to that agent are void.
				You must recommend a replacement agent to recover this stranded order.

				FAILURE CONTEXT:
				- offline agent: %s
				- total stranded orders from this agent: %d
				- recovery mode: AGENT_OFFLINE

				STRANDED ORDER:
				- id: %s
				- description: %s
				- was assigned to: %s (now unavailable)

				REMAINING AVAILABLE AGENTS:
				%s

				Recovery priorities:
				1. Minimize disruption — pick an agent who can take this order now
				2. Prefer agents with lower current load
				3. Do not recommend the offline agent

				Respond with JSON only, no markdown:
				{"agentId":"AGT-xxx","confidence":0.85,"reasoning":"plain English recovery explanation for ops"}
				""".formatted(
				failedAgentInfo,
				context.strandedOrderCount(),
				order.getId(),
				order.getDescription(),
				failedAgentInfo,
				formatRoster(context.availableAgents(), context));
	}

	public String buildPrompt(RoutingContext context) {
		if (context.triggerReason() == TriggerReason.AGENT_OFFLINE) {
			return buildReplanPrompt(context);
		}
		return buildInitialPrompt(context);
	}

	private String formatRoster(List<Agent> agents, RoutingContext context) {
		if (agents.isEmpty()) {
			return "(none)";
		}
		return agents.stream()
				.map(agent -> {
					int effective = context.effectiveLoad(agent);
					int pending = effective - agent.getActiveOrderCount();
					String pendingNote = pending > 0 ? " (+%d pending reassignment)".formatted(pending) : "";
					return "- %s (%s): status=%s, activeOrders=%d%s, effectiveLoad=%d%s".formatted(
							agent.getName(),
							agent.getId(),
							agent.getStatus(),
							agent.getActiveOrderCount(),
							pendingNote,
							effective,
							agent.getMaxCapacity() != null ? ", maxCapacity=" + agent.getMaxCapacity() : "");
				})
				.collect(Collectors.joining("\n"));
	}

	private String agentLabel(Agent agent) {
		if (agent == null) {
			return "unassigned";
		}
		return agent.getName();
	}
}
