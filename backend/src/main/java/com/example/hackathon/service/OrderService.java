package com.example.hackathon.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.dto.Dtos.CreateOrderRequest;
import com.example.hackathon.dto.Dtos.OrderResponse;

public interface OrderService {

	OrderResponse createOrder(CreateOrderRequest request);

	List<OrderResponse> listOrders(OrderStatus status);

	Order getOrderOrThrow(String orderId);

	ReassignmentSuggestion createSuggestion(String orderId, TriggerReason triggerReason);

	ReassignmentSuggestion createSuggestionForOrder(
			Order order,
			TriggerReason triggerReason,
			Optional<Agent> failedAgent,
			int strandedOrderCount,
			Map<String, Integer> pendingAllocations);
}
