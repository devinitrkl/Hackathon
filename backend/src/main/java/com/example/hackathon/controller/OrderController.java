package com.example.hackathon.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.dto.Dtos.CreateOrderRequest;
import com.example.hackathon.dto.Dtos.OrderResponse;
import com.example.hackathon.dto.Dtos.SuggestionResponse;
import com.example.hackathon.service.OrderService;
import com.example.hackathon.web.DtoMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final OrderService orderService;
	private final DtoMapper mapper;

	public OrderController(OrderService orderService, DtoMapper mapper) {
		this.orderService = orderService;
		this.mapper = mapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
		return orderService.createOrder(request);
	}

	@GetMapping
	public List<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status) {
		return orderService.listOrders(status);
	}

	@PostMapping("/{id}/suggest")
	@ResponseStatus(HttpStatus.CREATED)
	public SuggestionResponse suggest(@PathVariable String id) {
		return mapper.toSuggestionResponse(orderService.createSuggestion(id, TriggerReason.INITIAL));
	}
}
