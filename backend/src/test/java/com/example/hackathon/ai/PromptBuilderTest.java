package com.example.hackathon.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.routing.RoutingContext;
import com.example.hackathon.support.TestData;

class PromptBuilderTest {

	private final PromptBuilder builder = new PromptBuilder();

	private RoutingContext context(TriggerReason reason, Map<String, Integer> pending, Agent... agents) {
		Agent assigned = TestData.agent("AGT-3", "Ananya", 4, AgentStatus.OFFLINE);
		Order order = TestData.order("ORD-1", assigned, OrderStatus.ASSIGNED);
		Optional<Agent> failed = reason == TriggerReason.AGENT_OFFLINE ? Optional.of(assigned) : Optional.empty();
		return new RoutingContext(order, List.of(agents), reason, failed, agents.length, pending);
	}

	@Test
	void initialPromptDescribesDispatchAdvisorAndOrder() {
		Agent a = TestData.agent("AGT-1", "Priya", 0, AgentStatus.AVAILABLE);

		String prompt = builder.buildPrompt(context(TriggerReason.INITIAL, Map.of(), a));

		assertThat(prompt).contains("dispatch advisor");
		assertThat(prompt).contains("ORD-1");
		assertThat(prompt).contains("AGT-1");
		assertThat(prompt).contains("JSON only");
	}

	@Test
	void replanPromptDescribesOfflineRecovery() {
		Agent a = TestData.agent("AGT-1", "Priya", 0, AgentStatus.AVAILABLE);

		String prompt = builder.buildPrompt(context(TriggerReason.AGENT_OFFLINE, Map.of(), a));

		assertThat(prompt).contains("AGENT OFFLINE RECOVERY");
		assertThat(prompt).contains("Ananya");
		assertThat(prompt).contains("Recovery priorities");
	}

	@Test
	void rosterExposesEffectiveLoadIncludingPendingAllocations() {
		Agent a = TestData.agent("AGT-1", "Priya", 1, AgentStatus.AVAILABLE);

		String prompt = builder.buildPrompt(context(TriggerReason.AGENT_OFFLINE, Map.of("AGT-1", 2), a));

		assertThat(prompt).contains("effectiveLoad=3");
		assertThat(prompt).contains("pending reassignment");
	}
}
