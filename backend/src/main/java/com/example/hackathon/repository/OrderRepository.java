package com.example.hackathon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, String> {

	List<Order> findByStatus(OrderStatus status);

	List<Order> findByAssignedAgentIdAndStatus(String agentId, OrderStatus status);
}
