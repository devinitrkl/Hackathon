package com.example.hackathon.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import com.example.hackathon.dto.Dtos.CreateOrderRequest;
import com.example.hackathon.exception.ConflictException;
import com.example.hackathon.exception.ResourceNotFoundException;
import com.example.hackathon.repository.AgentRepository;
import com.example.hackathon.repository.OrderRepository;
import com.example.hackathon.repository.ReassignmentSuggestionRepository;
import com.example.hackathon.routing.RoutingContext;
import com.example.hackathon.routing.RoutingEngine;
import com.example.hackathon.routing.RoutingRecommendation;
import com.example.hackathon.support.TestData;
import com.example.hackathon.web.DtoMapper;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private AgentRepository agentRepository;
	@Mock
	private ReassignmentSuggestionRepository suggestionRepository;
	@Mock
	private RoutingEngine routingEngine;
	@Mock
	private DtoMapper mapper;

	@InjectMocks
	private OrderServiceImpl service;

	@Captor
	private ArgumentCaptor<RoutingContext> contextCaptor;
	@Captor
	private ArgumentCaptor<ReassignmentSuggestion> suggestionCaptor;

	private Agent online;

	@BeforeEach
	void setUp() {
		online = TestData.agent("AGT-1", "Priya", 1, AgentStatus.AVAILABLE);
	}

	@Test
	void createOrderThrowsWhenDuplicateId() {
		when(orderRepository.existsById("ORD-1")).thenReturn(true);

		assertThatThrownBy(() -> service.createOrder(new CreateOrderRequest("ORD-1", "x", "AGT-1")))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("already exists");
	}

	@Test
	void createOrderThrowsWhenAgentMissing() {
		when(orderRepository.existsById("ORD-1")).thenReturn(false);
		when(agentRepository.findById("AGT-1")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createOrder(new CreateOrderRequest("ORD-1", "x", "AGT-1")))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void createOrderThrowsWhenAgentOffline() {
		Agent offline = TestData.agent("AGT-1", "Priya", 0, AgentStatus.OFFLINE);
		when(orderRepository.existsById("ORD-1")).thenReturn(false);
		when(agentRepository.findById("AGT-1")).thenReturn(Optional.of(offline));

		assertThatThrownBy(() -> service.createOrder(new CreateOrderRequest("ORD-1", "x", "AGT-1")))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("offline");
	}

	@Test
	void createOrderIncrementsLoadAndMarksAgentBusy() {
		Agent available = TestData.agent("AGT-1", "Priya", 0, AgentStatus.AVAILABLE);
		when(orderRepository.existsById("ORD-1")).thenReturn(false);
		when(agentRepository.findById("AGT-1")).thenReturn(Optional.of(available));

		service.createOrder(new CreateOrderRequest("ORD-1", "x", "AGT-1"));

		assertThat(available.getActiveOrderCount()).isEqualTo(1);
		assertThat(available.getStatus()).isEqualTo(AgentStatus.BUSY);
		verify(orderRepository).save(any(Order.class));
		verify(agentRepository).save(available);
	}

	@Test
	void listOrdersUsesFindAllWhenStatusNull() {
		when(orderRepository.findAll()).thenReturn(List.of());

		service.listOrders(null);

		verify(orderRepository).findAll();
		verify(orderRepository, never()).findByStatus(any());
	}

	@Test
	void listOrdersFiltersByStatusWhenProvided() {
		when(orderRepository.findByStatus(OrderStatus.ASSIGNED)).thenReturn(List.of());

		service.listOrders(OrderStatus.ASSIGNED);

		verify(orderRepository).findByStatus(OrderStatus.ASSIGNED);
	}

	@Test
	void createSuggestionThrowsWhenOrderMissing() {
		when(orderRepository.findById("ORD-X")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createSuggestion("ORD-X", TriggerReason.INITIAL))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void createSuggestionBuildsPendingSuggestionAndMarksOrderPending() {
		Order order = TestData.order("ORD-1", online, OrderStatus.ASSIGNED);
		Agent recommended = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);

		when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(order));
		when(agentRepository.findAll()).thenReturn(List.of(online, recommended));
		when(routingEngine.recommend(any())).thenReturn(
				List.of(new RoutingRecommendation("AGT-2", 0.9, "go with Rahul")));
		when(agentRepository.findById("AGT-2")).thenReturn(Optional.of(recommended));
		when(suggestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.createSuggestion("ORD-1", TriggerReason.INITIAL);

		verify(suggestionRepository).save(suggestionCaptor.capture());
		ReassignmentSuggestion saved = suggestionCaptor.getValue();
		assertThat(saved.getRecommendedAgent()).isEqualTo(recommended);
		assertThat(saved.getStatus()).isEqualTo(SuggestionStatus.PENDING);
		assertThat(saved.getConfidence()).isEqualTo(0.9);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.REASSIGNMENT_PENDING);
	}

	@Test
	void createSuggestionThrowsWhenNoRecommendationProduced() {
		Order order = TestData.order("ORD-1", online, OrderStatus.ASSIGNED);
		when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(order));
		when(agentRepository.findAll()).thenReturn(List.of(online));
		when(routingEngine.recommend(any())).thenReturn(List.of());

		assertThatThrownBy(() -> service.createSuggestion("ORD-1", TriggerReason.INITIAL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No routing recommendation");
	}

	@Test
	void createSuggestionThrowsWhenRecommendedAgentNotFound() {
		Order order = TestData.order("ORD-1", online, OrderStatus.ASSIGNED);
		when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(order));
		when(agentRepository.findAll()).thenReturn(List.of(online));
		when(routingEngine.recommend(any())).thenReturn(
				List.of(new RoutingRecommendation("AGT-GONE", 0.9, "x")));
		when(agentRepository.findById("AGT-GONE")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createSuggestion("ORD-1", TriggerReason.INITIAL))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void agentOfflineReplanSkipsWhenExistingSuggestionStillValid() {
		Order order = TestData.order("ORD-1", online, OrderStatus.ASSIGNED);
		Agent stillUsable = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);
		ReassignmentSuggestion existing = TestData.suggestion(
				"S1", order, stillUsable, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

		when(suggestionRepository.findFirstByOrderAndStatusOrderByCreatedAtDesc(order, SuggestionStatus.PENDING))
				.thenReturn(Optional.of(existing));

		ReassignmentSuggestion result = service.createSuggestionForOrder(
				order, TriggerReason.AGENT_OFFLINE, Optional.empty(), 1, Map.of());

		assertThat(result).isSameAs(existing);
		verify(routingEngine, never()).recommend(any());
	}

	@Test
	void agentOfflineReplanSupersedesStaleSuggestionWhenRecommendedAgentOffline() {
		Order order = TestData.order("ORD-1", online, OrderStatus.ASSIGNED);
		Agent nowOffline = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.OFFLINE);
		ReassignmentSuggestion stale = TestData.suggestion(
				"S1", order, nowOffline, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);
		Agent fresh = TestData.agent("AGT-3", "Kiran", 0, AgentStatus.AVAILABLE);

		when(suggestionRepository.findFirstByOrderAndStatusOrderByCreatedAtDesc(order, SuggestionStatus.PENDING))
				.thenReturn(Optional.of(stale));
		when(agentRepository.findAll()).thenReturn(List.of(fresh));
		when(routingEngine.recommend(any())).thenReturn(
				List.of(new RoutingRecommendation("AGT-3", 1.0, "kiran")));
		when(agentRepository.findById("AGT-3")).thenReturn(Optional.of(fresh));
		when(suggestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.createSuggestionForOrder(order, TriggerReason.AGENT_OFFLINE, Optional.empty(), 1, Map.of());

		assertThat(stale.getStatus()).isEqualTo(SuggestionStatus.REJECTED);
		verify(routingEngine).recommend(any());
	}

	@Test
	void pendingAllocationsArePropagatedIntoRoutingContext() {
		Order order = TestData.order("ORD-1", online, OrderStatus.ASSIGNED);
		Agent recommended = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.AVAILABLE);
		Map<String, Integer> pending = Map.of("AGT-2", 2);

		when(agentRepository.findAll()).thenReturn(List.of(online, recommended));
		when(routingEngine.recommend(contextCaptor.capture())).thenReturn(
				List.of(new RoutingRecommendation("AGT-2", 0.9, "x")));
		when(agentRepository.findById("AGT-2")).thenReturn(Optional.of(recommended));
		when(suggestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.createSuggestionForOrder(order, TriggerReason.INITIAL, Optional.empty(), 1, pending);

		assertThat(contextCaptor.getValue().pendingAllocations()).containsEntry("AGT-2", 2);
	}
}
