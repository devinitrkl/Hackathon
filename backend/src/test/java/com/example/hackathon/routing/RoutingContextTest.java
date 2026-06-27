package com.example.hackathon.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.support.TestData;

class RoutingContextTest {

	private RoutingContext contextWith(Map<String, Integer> pending, Agent... agents) {
		return new RoutingContext(
				TestData.order("ORD-1", null, com.example.hackathon.domain.OrderStatus.ASSIGNED),
				List.of(agents),
				TriggerReason.INITIAL,
				Optional.empty(),
				1,
				pending);
	}

	@Test
	void effectiveLoadIsActualWhenNoPendingAllocations() {
		Agent agent = TestData.agent("AGT-1", "A", 2, AgentStatus.BUSY);
		RoutingContext context = contextWith(Map.of(), agent);

		assertThat(context.effectiveLoad(agent)).isEqualTo(2);
	}

	@Test
	void effectiveLoadAddsPendingAllocations() {
		Agent agent = TestData.agent("AGT-1", "A", 2, AgentStatus.BUSY);
		RoutingContext context = contextWith(Map.of("AGT-1", 3), agent);

		assertThat(context.effectiveLoad(agent)).isEqualTo(5);
	}

	@Test
	void effectiveLoadDefaultsToZeroPendingForUnknownAgent() {
		Agent agent = TestData.agent("AGT-1", "A", 1, AgentStatus.BUSY);
		RoutingContext context = contextWith(Map.of("AGT-OTHER", 4), agent);

		assertThat(context.effectiveLoad(agent)).isEqualTo(1);
	}
}
