package com.example.hackathon.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.dto.Dtos.AgentResponse;
import com.example.hackathon.dto.Dtos.OrderResponse;
import com.example.hackathon.dto.Dtos.SuggestionResponse;
import com.example.hackathon.support.TestData;

class DtoMapperTest {

	private final DtoMapper mapper = new DtoMapper();

	@Test
	void mapsAgentFields() {
		Agent agent = TestData.agent("AGT-1", "Priya", 3, AgentStatus.BUSY);

		AgentResponse response = mapper.toAgentResponse(agent);

		assertThat(response.id()).isEqualTo("AGT-1");
		assertThat(response.name()).isEqualTo("Priya");
		assertThat(response.activeOrderCount()).isEqualTo(3);
		assertThat(response.status()).isEqualTo(AgentStatus.BUSY);
		assertThat(response.maxCapacity()).isEqualTo(5);
	}

	@Test
	void mapsSuggestionFields() {
		Agent agent = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);
		Order order = TestData.order("ORD-1", agent, OrderStatus.REASSIGNMENT_PENDING);
		ReassignmentSuggestion s = TestData.suggestion(
				"S1", order, agent, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

		SuggestionResponse response = mapper.toSuggestionResponse(s);

		assertThat(response.id()).isEqualTo("S1");
		assertThat(response.orderId()).isEqualTo("ORD-1");
		assertThat(response.recommendedAgentId()).isEqualTo("AGT-2");
		assertThat(response.recommendedAgentName()).isEqualTo("Rahul");
		assertThat(response.status()).isEqualTo(SuggestionStatus.PENDING);
		assertThat(response.triggerReason()).isEqualTo(TriggerReason.AGENT_OFFLINE);
	}

	@Test
	void mapsOrderWithPendingSuggestion() {
		Agent agent = TestData.agent("AGT-1", "Priya", 1, AgentStatus.BUSY);
		Order order = TestData.order("ORD-1", agent, OrderStatus.REASSIGNMENT_PENDING);
		ReassignmentSuggestion s = TestData.suggestion(
				"S1", order, agent, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

		OrderResponse response = mapper.toOrderResponse(order, Optional.of(s));

		assertThat(response.id()).isEqualTo("ORD-1");
		assertThat(response.assignedAgentId()).isEqualTo("AGT-1");
		assertThat(response.assignedAgentName()).isEqualTo("Priya");
		assertThat(response.pendingSuggestion()).isNotNull();
		assertThat(response.pendingSuggestion().id()).isEqualTo("S1");
	}

	@Test
	void mapsOrderWithoutAssignedAgentOrSuggestion() {
		Order order = TestData.order("ORD-1", null, OrderStatus.ASSIGNED);

		OrderResponse response = mapper.toOrderResponse(order, Optional.empty());

		assertThat(response.assignedAgentId()).isNull();
		assertThat(response.assignedAgentName()).isNull();
		assertThat(response.pendingSuggestion()).isNull();
	}
}
