package com.example.hackathon.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;

@DataJpaTest
class RepositoryQueriesTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private AgentRepository agentRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private ReassignmentSuggestionRepository suggestionRepository;

	private Agent persistAgent(String id, AgentStatus status) {
		Agent agent = new Agent(id, "Agent " + id, 0, status);
		agent.setMaxCapacity(5);
		return em.persist(agent);
	}

	private Order persistOrder(String id, Agent agent, OrderStatus status) {
		Order order = new Order(id, "Order " + id, agent, status, Instant.now());
		return em.persist(order);
	}

	private ReassignmentSuggestion persistSuggestion(
			Order order, Agent recommended, SuggestionStatus status) {
		ReassignmentSuggestion s = new ReassignmentSuggestion();
		s.setOrder(order);
		s.setRecommendedAgent(recommended);
		s.setConfidence(0.9);
		s.setReasoning("r");
		s.setStatus(status);
		s.setTriggerReason(TriggerReason.AGENT_OFFLINE);
		s.setCreatedAt(Instant.now());
		return em.persist(s);
	}

	@Test
	void findByStatusReturnsOnlyMatchingAgents() {
		persistAgent("AGT-1", AgentStatus.AVAILABLE);
		persistAgent("AGT-2", AgentStatus.OFFLINE);
		em.flush();

		assertThat(agentRepository.findByStatus(AgentStatus.OFFLINE))
				.extracting(Agent::getId).containsExactly("AGT-2");
	}

	@Test
	void findByAssignedAgentIdAndStatusFiltersOrders() {
		Agent agent = persistAgent("AGT-1", AgentStatus.BUSY);
		persistOrder("ORD-1", agent, OrderStatus.ASSIGNED);
		persistOrder("ORD-2", agent, OrderStatus.REASSIGNED);
		em.flush();

		assertThat(orderRepository.findByAssignedAgentIdAndStatus("AGT-1", OrderStatus.ASSIGNED))
				.extracting(Order::getId).containsExactly("ORD-1");
	}

	@Test
	void findByStatusAndRecommendedAgentIdMatchesPendingSuggestionsForAgent() {
		Agent recommended = persistAgent("AGT-2", AgentStatus.AVAILABLE);
		Agent other = persistAgent("AGT-3", AgentStatus.AVAILABLE);
		Order order1 = persistOrder("ORD-1", recommended, OrderStatus.REASSIGNMENT_PENDING);
		Order order2 = persistOrder("ORD-2", other, OrderStatus.REASSIGNMENT_PENDING);
		persistSuggestion(order1, recommended, SuggestionStatus.PENDING);
		persistSuggestion(order2, other, SuggestionStatus.PENDING);
		em.flush();

		assertThat(suggestionRepository.findByStatusAndRecommendedAgentId(SuggestionStatus.PENDING, "AGT-2"))
				.hasSize(1)
				.allSatisfy(s -> assertThat(s.getRecommendedAgent().getId()).isEqualTo("AGT-2"));
	}

	@Test
	void findFirstByOrderAndStatusReturnsMostRecentPending() {
		Agent agent = persistAgent("AGT-2", AgentStatus.AVAILABLE);
		Order order = persistOrder("ORD-1", agent, OrderStatus.REASSIGNMENT_PENDING);
		persistSuggestion(order, agent, SuggestionStatus.REJECTED);
		ReassignmentSuggestion pending = persistSuggestion(order, agent, SuggestionStatus.PENDING);
		em.flush();

		assertThat(suggestionRepository
				.findFirstByOrderAndStatusOrderByCreatedAtDesc(order, SuggestionStatus.PENDING))
				.isPresent()
				.get()
				.extracting(ReassignmentSuggestion::getId)
				.isEqualTo(pending.getId());
	}
}
