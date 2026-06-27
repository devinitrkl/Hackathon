package com.example.hackathon.dto;

import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class Dtos {

	private Dtos() {
	}

	public record CreateOrderRequest(
			@NotBlank String id,
			@NotBlank String description,
			@NotBlank String assignedAgentId) {
	}

	public record UpdateAgentStatusRequest(@NotNull AgentStatus status) {
	}

	public record UpdateSuggestionRequest(@NotNull SuggestionStatus status) {
	}

	public record AgentResponse(
			String id,
			String name,
			int activeOrderCount,
			AgentStatus status,
			String currentZone,
			Integer maxCapacity) {
	}

	public record SuggestionResponse(
			String id,
			String orderId,
			String orderDescription,
			String recommendedAgentId,
			String recommendedAgentName,
			double confidence,
			String reasoning,
			SuggestionStatus status,
			TriggerReason triggerReason,
			String createdAt) {
	}

	public record OrderResponse(
			String id,
			String description,
			String assignedAgentId,
			String assignedAgentName,
			OrderStatus status,
			String createdAt,
			SuggestionResponse pendingSuggestion) {
	}

	public record ErrorResponse(String error) {
	}
}
