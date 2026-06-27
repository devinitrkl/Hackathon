package com.example.hackathon.web;

import org.springframework.stereotype.Component;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.dto.Dtos.AgentResponse;
import com.example.hackathon.dto.Dtos.OrderResponse;
import com.example.hackathon.dto.Dtos.SuggestionResponse;

import java.util.Optional;

@Component
public class DtoMapper {

	public AgentResponse toAgentResponse(Agent agent) {
		return new AgentResponse(
				agent.getId(),
				agent.getName(),
				agent.getActiveOrderCount(),
				agent.getStatus(),
				agent.getCurrentZone(),
				agent.getMaxCapacity());
	}

	public SuggestionResponse toSuggestionResponse(ReassignmentSuggestion suggestion) {
		return new SuggestionResponse(
				suggestion.getId(),
				suggestion.getOrder().getId(),
				suggestion.getOrder().getDescription(),
				suggestion.getRecommendedAgent().getId(),
				suggestion.getRecommendedAgent().getName(),
				suggestion.getConfidence(),
				suggestion.getReasoning(),
				suggestion.getStatus(),
				suggestion.getTriggerReason(),
				suggestion.getCreatedAt().toString());
	}

	public OrderResponse toOrderResponse(Order order, Optional<ReassignmentSuggestion> pendingSuggestion) {
		Agent assigned = order.getAssignedAgent();
		return new OrderResponse(
				order.getId(),
				order.getDescription(),
				assigned != null ? assigned.getId() : null,
				assigned != null ? assigned.getName() : null,
				order.getStatus(),
				order.getCreatedAt().toString(),
				pendingSuggestion.map(this::toSuggestionResponse).orElse(null));
	}
}
