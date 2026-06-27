package com.example.hackathon.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.example.hackathon.repository.OrderRepository;
import com.example.hackathon.repository.ReassignmentSuggestionRepository;
import com.example.hackathon.service.AgentService;
import com.example.hackathon.service.OrderService;
import com.example.hackathon.support.TestData;

@ExtendWith(MockitoExtension.class)
class ReplanEventListenerTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private ReassignmentSuggestionRepository suggestionRepository;
	@Mock
	private OrderService orderService;
	@Mock
	private AgentService agentService;

	@InjectMocks
	private ReplanEventListener listener;

	@Test
	void replansEveryStrandedOrderAndAccumulatesPendingAllocations() {
		Agent failed = TestData.agent("AGT-3", "Ananya", 2, AgentStatus.OFFLINE);
		Agent priya = TestData.agent("AGT-1", "Priya", 0, AgentStatus.AVAILABLE);
		Agent sneha = TestData.agent("AGT-6", "Sneha", 0, AgentStatus.AVAILABLE);
		Order ord1 = TestData.order("ORD-1", failed, OrderStatus.ASSIGNED);
		Order ord2 = TestData.order("ORD-2", failed, OrderStatus.ASSIGNED);

		when(agentService.getAgentOrThrow("AGT-3")).thenReturn(failed);
		when(orderRepository.findByAssignedAgentIdAndStatus("AGT-3", OrderStatus.ASSIGNED))
				.thenReturn(List.of(ord1, ord2));
		when(suggestionRepository.findByStatusAndRecommendedAgentId(SuggestionStatus.PENDING, "AGT-3"))
				.thenReturn(List.of());

		// Snapshot the pendingAllocations map as the service sees it on each call, and
		// return a suggestion recommending a distinct agent per order.
		List<Map<String, Integer>> snapshots = new java.util.ArrayList<>();
		when(orderService.createSuggestionForOrder(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
				.thenAnswer(inv -> {
					Order order = inv.getArgument(0);
					@SuppressWarnings("unchecked")
					Map<String, Integer> pending = (Map<String, Integer>) inv.getArgument(4);
					snapshots.add(new HashMap<>(pending));
					Agent recommended = order.getId().equals("ORD-1") ? priya : sneha;
					return TestData.suggestion("S-" + order.getId(), order, recommended,
							SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);
				});

		listener.onAgentOffline(new AgentOfflineEvent("AGT-3"));

		verify(orderService, times(2)).createSuggestionForOrder(
				any(), eq(TriggerReason.AGENT_OFFLINE), eq(Optional.of(failed)), eq(2), any());

		// First order is routed against an empty pending map...
		assertThat(snapshots.get(0)).isEmpty();
		// ...by the time the second order is routed, the first pick (AGT-1) is recorded.
		assertThat(snapshots.get(1)).containsEntry("AGT-1", 1);
	}

	@Test
	void alsoReplansOrdersWhoseStalePendingSuggestionPointedAtNowOfflineAgent() {
		Agent failed = TestData.agent("AGT-2", "Rahul", 0, AgentStatus.OFFLINE);
		Order strandedOrder = TestData.order("ORD-9", failed, OrderStatus.REASSIGNMENT_PENDING);
		ReassignmentSuggestion stale = TestData.suggestion(
				"S9", strandedOrder, failed, SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);

		when(agentService.getAgentOrThrow("AGT-2")).thenReturn(failed);
		when(orderRepository.findByAssignedAgentIdAndStatus("AGT-2", OrderStatus.ASSIGNED))
				.thenReturn(List.of());
		when(suggestionRepository.findByStatusAndRecommendedAgentId(SuggestionStatus.PENDING, "AGT-2"))
				.thenReturn(List.of(stale));
		when(orderService.createSuggestionForOrder(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
				.thenReturn(stale);

		listener.onAgentOffline(new AgentOfflineEvent("AGT-2"));

		verify(orderService).createSuggestionForOrder(
				eq(strandedOrder), eq(TriggerReason.AGENT_OFFLINE), eq(Optional.of(failed)),
				org.mockito.ArgumentMatchers.anyInt(), any());
	}

	@Test
	void swallowsExceptionsFromIndividualOrdersWithoutFailingWholeBatch() {
		Agent failed = TestData.agent("AGT-3", "Ananya", 1, AgentStatus.OFFLINE);
		Order ord1 = TestData.order("ORD-1", failed, OrderStatus.ASSIGNED);

		when(agentService.getAgentOrThrow("AGT-3")).thenReturn(failed);
		when(orderRepository.findByAssignedAgentIdAndStatus("AGT-3", OrderStatus.ASSIGNED))
				.thenReturn(List.of(ord1));
		when(suggestionRepository.findByStatusAndRecommendedAgentId(SuggestionStatus.PENDING, "AGT-3"))
				.thenReturn(List.of());
		when(orderService.createSuggestionForOrder(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
				.thenThrow(new RuntimeException("routing blew up"));

		// Should not propagate.
		listener.onAgentOffline(new AgentOfflineEvent("AGT-3"));

		verify(orderService).createSuggestionForOrder(any(), any(), any(),
				org.mockito.ArgumentMatchers.anyInt(), any());
	}
}
