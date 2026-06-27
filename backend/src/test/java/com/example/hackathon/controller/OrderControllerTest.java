package com.example.hackathon.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.dto.Dtos.OrderResponse;
import com.example.hackathon.dto.Dtos.SuggestionResponse;
import com.example.hackathon.exception.ResourceNotFoundException;
import com.example.hackathon.service.OrderService;
import com.example.hackathon.web.DtoMapper;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	@MockitoBean
	private DtoMapper mapper;

	@Test
	void createOrderReturns201() throws Exception {
		OrderResponse response = new OrderResponse(
				"ORD-1", "desc", "AGT-1", "Priya", OrderStatus.ASSIGNED, "now", null);
		when(orderService.createOrder(any())).thenReturn(response);

		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"ORD-1\",\"description\":\"desc\",\"assignedAgentId\":\"AGT-1\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value("ORD-1"))
				.andExpect(jsonPath("$.assignedAgentName").value("Priya"));
	}

	@Test
	void createOrderReturns400WhenBodyInvalid() throws Exception {
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"\",\"description\":\"\",\"assignedAgentId\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listOrdersReturnsArray() throws Exception {
		when(orderService.listOrders(null)).thenReturn(List.of(
				new OrderResponse("ORD-1", "d", "AGT-1", "Priya", OrderStatus.ASSIGNED, "now", null)));

		mockMvc.perform(get("/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("ORD-1"));
	}

	@Test
	void suggestReturns201WithSuggestionBody() throws Exception {
		var suggestion = mockSuggestionEntity();
		when(orderService.createSuggestion(eq("ORD-1"), eq(TriggerReason.INITIAL))).thenReturn(suggestion);
		when(mapper.toSuggestionResponse(any())).thenReturn(new SuggestionResponse(
				"S1", "ORD-1", "d", "AGT-2", "Rahul", 0.9, "reason",
				SuggestionStatus.PENDING, TriggerReason.INITIAL, "now"));

		mockMvc.perform(post("/orders/ORD-1/suggest"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.recommendedAgentId").value("AGT-2"));
	}

	@Test
	void notFoundFromServiceMapsTo404() throws Exception {
		when(orderService.createSuggestion(any(), any()))
				.thenThrow(new ResourceNotFoundException("Order not found: ORD-X"));

		mockMvc.perform(post("/orders/ORD-X/suggest"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Order not found: ORD-X"));
	}

	private com.example.hackathon.domain.ReassignmentSuggestion mockSuggestionEntity() {
		return org.mockito.Mockito.mock(com.example.hackathon.domain.ReassignmentSuggestion.class);
	}
}
