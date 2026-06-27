package com.example.hackathon.support;

import java.time.Instant;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;

/**
 * Small factory helpers so individual tests can build domain objects without
 * repeating boilerplate. Every method returns a fresh, fully-initialised object.
 */
public final class TestData {

	private TestData() {
	}

	public static Agent agent(String id, String name, int activeOrderCount, AgentStatus status) {
		Agent agent = new Agent(id, name, activeOrderCount, status);
		agent.setMaxCapacity(5);
		agent.setCurrentZone("NORTH");
		return agent;
	}

	public static Agent availableAgent(String id, int load) {
		return agent(id, "Agent " + id, load, AgentStatus.AVAILABLE);
	}

	public static Order order(String id, Agent assignedAgent, OrderStatus status) {
		Order order = new Order(id, "Order " + id, assignedAgent, status, Instant.now());
		order.setPickupZone("NORTH");
		order.setDropoffZone("SOUTH");
		order.setWeightClass("LIGHT");
		return order;
	}

	public static ReassignmentSuggestion suggestion(
			String id, Order order, Agent recommendedAgent,
			SuggestionStatus status, TriggerReason triggerReason) {
		ReassignmentSuggestion suggestion = new ReassignmentSuggestion();
		suggestion.setId(id);
		suggestion.setOrder(order);
		suggestion.setRecommendedAgent(recommendedAgent);
		suggestion.setConfidence(0.9);
		suggestion.setReasoning("test reasoning");
		suggestion.setStatus(status);
		suggestion.setTriggerReason(triggerReason);
		suggestion.setCreatedAt(Instant.now());
		return suggestion;
	}
}
