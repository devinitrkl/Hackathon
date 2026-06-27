package com.example.hackathon.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.support.TestData;

class RuleBasedRoutingStrategyTest {

	private final RuleBasedRoutingStrategy strategy = new RuleBasedRoutingStrategy();

	private RoutingContext context(Map<String, Integer> pending, Agent... agents) {
		Order order = TestData.order("ORD-1", null, OrderStatus.ASSIGNED);
		return new RoutingContext(order, List.of(agents), TriggerReason.AGENT_OFFLINE,
				Optional.empty(), agents.length, pending);
	}

	@Test
	void hasStableName() {
		assertThat(strategy.getName()).isEqualTo("rule-based");
	}

	@Test
	void ranksAgentsByLowestLoadFirst() {
		Agent heavy = TestData.agent("AGT-HEAVY", "Heavy", 4, AgentStatus.BUSY);
		Agent light = TestData.agent("AGT-LIGHT", "Light", 1, AgentStatus.AVAILABLE);
		Agent idle = TestData.agent("AGT-IDLE", "Idle", 0, AgentStatus.AVAILABLE);

		List<RoutingRecommendation> result = strategy.recommend(context(Map.of(), heavy, light, idle));

		assertThat(result).extracting(RoutingRecommendation::agentId)
				.containsExactly("AGT-IDLE", "AGT-LIGHT", "AGT-HEAVY");
	}

	@Test
	void topRecommendationHasFullConfidenceAndDecreasesDownTheList() {
		Agent a = TestData.agent("AGT-1", "A", 0, AgentStatus.AVAILABLE);
		Agent b = TestData.agent("AGT-2", "B", 1, AgentStatus.AVAILABLE);

		List<RoutingRecommendation> result = strategy.recommend(context(Map.of(), a, b));

		assertThat(result.get(0).confidence()).isEqualTo(1.0);
		assertThat(result.get(1).confidence()).isLessThan(1.0);
	}

	@Test
	void excludesOfflineAgents() {
		Agent online = TestData.agent("AGT-ON", "On", 2, AgentStatus.AVAILABLE);
		Agent offline = TestData.agent("AGT-OFF", "Off", 0, AgentStatus.OFFLINE);

		List<RoutingRecommendation> result = strategy.recommend(context(Map.of(), online, offline));

		assertThat(result).extracting(RoutingRecommendation::agentId).containsExactly("AGT-ON");
	}

	@Test
	void excludesAgentsAtOrAboveCapacityUsingEffectiveLoad() {
		Agent full = TestData.agent("AGT-FULL", "Full", 5, AgentStatus.BUSY); // maxCapacity 5
		Agent room = TestData.agent("AGT-ROOM", "Room", 1, AgentStatus.AVAILABLE);

		List<RoutingRecommendation> result = strategy.recommend(context(Map.of(), full, room));

		assertThat(result).extracting(RoutingRecommendation::agentId).containsExactly("AGT-ROOM");
	}

	@Test
	void pendingAllocationsPushAnAgentDownTheRanking() {
		Agent a = TestData.agent("AGT-1", "A", 0, AgentStatus.AVAILABLE);
		Agent b = TestData.agent("AGT-2", "B", 0, AgentStatus.AVAILABLE);

		// AGT-1 already virtually holds 2 orders this batch, so AGT-2 should win.
		List<RoutingRecommendation> result = strategy.recommend(context(Map.of("AGT-1", 2), a, b));

		assertThat(result.get(0).agentId()).isEqualTo("AGT-2");
	}

	@Test
	void pendingAllocationCanPushAgentOverCapacityAndExcludeIt() {
		Agent a = TestData.agent("AGT-1", "A", 4, AgentStatus.BUSY); // cap 5
		Agent b = TestData.agent("AGT-2", "B", 2, AgentStatus.BUSY);

		// AGT-1 effective load = 4 + 1 = 5 == capacity -> excluded.
		List<RoutingRecommendation> result = strategy.recommend(context(Map.of("AGT-1", 1), a, b));

		assertThat(result).extracting(RoutingRecommendation::agentId).containsExactly("AGT-2");
	}

	@Test
	void throwsWhenNoEligibleAgents() {
		Agent offline = TestData.agent("AGT-OFF", "Off", 0, AgentStatus.OFFLINE);

		assertThatThrownBy(() -> strategy.recommend(context(Map.of(), offline)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No available agents");
	}
}
