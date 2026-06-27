package com.example.hackathon.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.dto.Dtos.UpdateSuggestionRequest;
import com.example.hackathon.exception.ConflictException;
import com.example.hackathon.exception.ResourceNotFoundException;
import com.example.hackathon.repository.AgentRepository;
import com.example.hackathon.repository.OrderRepository;
import com.example.hackathon.repository.ReassignmentSuggestionRepository;
import com.example.hackathon.support.TestData;
import com.example.hackathon.web.DtoMapper;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceImplTest {

	@Mock
	private ReassignmentSuggestionRepository suggestionRepository;
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private AgentRepository agentRepository;
	@Mock
	private DtoMapper mapper;

	@InjectMocks
	private SuggestionServiceImpl service;

	private UpdateSuggestionRequest accept() {
		return new UpdateSuggestionRequest(SuggestionStatus.ACCEPTED);
	}

	private UpdateSuggestionRequest reject() {
		return new UpdateSuggestionRequest(SuggestionStatus.REJECTED);
	}

	@Test
	void updateThrowsWhenSuggestionMissing() {
		when(suggestionRepository.findById("S-X")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateSuggestion("S-X", accept()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateThrowsWhenSuggestionAlreadyProcessed() {
		Order order = TestData.order("ORD-1", null, OrderStatus.REASSIGNMENT_PENDING);
		Agent agent = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);
		ReassignmentSuggestion s = TestData.suggestion(
				"S1", order, agent, SuggestionStatus.ACCEPTED, TriggerReason.AGENT_OFFLINE);
		when(suggestionRepository.findById("S1")).thenReturn(Optional.of(s));

		assertThatThrownBy(() -> service.updateSuggestion("S1", accept()))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("already");
	}

	@Test
	void updateThrowsWhenSettingBackToPending() {
		Order order = TestData.order("ORD-1", null, OrderStatus.REASSIGNMENT_PENDING);
		Agent agent = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);
		ReassignmentSuggestion s = TestData.suggestion(
				"S1", order, agent, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);
		when(suggestionRepository.findById("S1")).thenReturn(Optional.of(s));

		assertThatThrownBy(() -> service.updateSuggestion("S1", new UpdateSuggestionRequest(SuggestionStatus.PENDING)))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("PENDING");
	}

	@Test
	void cannotAcceptWhenRecommendedAgentOffline() {
		Order order = TestData.order("ORD-1", null, OrderStatus.REASSIGNMENT_PENDING);
		Agent offline = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.OFFLINE);
		ReassignmentSuggestion s = TestData.suggestion(
				"S1", order, offline, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);
		when(suggestionRepository.findById("S1")).thenReturn(Optional.of(s));

		assertThatThrownBy(() -> service.updateSuggestion("S1", accept()))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("OFFLINE");
		verify(orderRepository, never()).save(any());
	}

	@Test
	void acceptReassignsOrderAndRebalancesAgentLoads() {
		Agent previous = TestData.agent("AGT-1", "Priya", 3, AgentStatus.BUSY);
		Agent recommended = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);
		Order order = TestData.order("ORD-1", previous, OrderStatus.REASSIGNMENT_PENDING);
		ReassignmentSuggestion s = TestData.suggestion(
				"S1", order, recommended, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

		when(suggestionRepository.findById("S1")).thenReturn(Optional.of(s));
		when(suggestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.updateSuggestion("S1", accept());

		assertThat(s.getStatus()).isEqualTo(SuggestionStatus.ACCEPTED);
		assertThat(previous.getActiveOrderCount()).isEqualTo(2);
		assertThat(recommended.getActiveOrderCount()).isEqualTo(1);
		assertThat(recommended.getStatus()).isEqualTo(AgentStatus.BUSY);
		assertThat(order.getAssignedAgent()).isEqualTo(recommended);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.REASSIGNED);
	}

	@Test
	void acceptMarksPreviousAgentAvailableWhenLoadHitsZero() {
		Agent previous = TestData.agent("AGT-1", "Priya", 1, AgentStatus.BUSY);
		Agent recommended = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);
		Order order = TestData.order("ORD-1", previous, OrderStatus.REASSIGNMENT_PENDING);
		ReassignmentSuggestion s = TestData.suggestion(
				"S1", order, recommended, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

		when(suggestionRepository.findById("S1")).thenReturn(Optional.of(s));
		when(suggestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.updateSuggestion("S1", accept());

		assertThat(previous.getActiveOrderCount()).isEqualTo(0);
		assertThat(previous.getStatus()).isEqualTo(AgentStatus.AVAILABLE);
	}

	@Test
	void rejectRevertsOrderBackToAssigned() {
		Agent agent = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);
		Order order = TestData.order("ORD-1", agent, OrderStatus.REASSIGNMENT_PENDING);
		ReassignmentSuggestion s = TestData.suggestion(
				"S1", order, agent, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

		when(suggestionRepository.findById("S1")).thenReturn(Optional.of(s));
		when(suggestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.updateSuggestion("S1", reject());

		assertThat(s.getStatus()).isEqualTo(SuggestionStatus.REJECTED);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
	}

	@Test
	void listSuggestionsFiltersByStatusWhenProvided() {
		when(suggestionRepository.findByStatus(SuggestionStatus.PENDING)).thenReturn(java.util.List.of());

		service.listSuggestions(SuggestionStatus.PENDING);

		verify(suggestionRepository).findByStatus(SuggestionStatus.PENDING);
		verify(suggestionRepository, never()).findAll();
	}

	@Test
	void listSuggestionsReturnsAllWhenStatusNull() {
		when(suggestionRepository.findAll()).thenReturn(java.util.List.of());

		service.listSuggestions(null);

		verify(suggestionRepository).findAll();
	}
}
