package com.example.hackathon.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	private String id;

	@Column(nullable = false)
	private String description;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "assigned_agent_id")
	private Agent assignedAgent;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(nullable = false)
	private Instant createdAt;

	@Column
	private String weightClass;

	@Column
	private String pickupZone;

	@Column
	private String dropoffZone;

	public Order() {
	}

	public Order(String id, String description, Agent assignedAgent, OrderStatus status, Instant createdAt) {
		this.id = id;
		this.description = description;
		this.assignedAgent = assignedAgent;
		this.status = status;
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Agent getAssignedAgent() {
		return assignedAgent;
	}

	public void setAssignedAgent(Agent assignedAgent) {
		this.assignedAgent = assignedAgent;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getWeightClass() {
		return weightClass;
	}

	public void setWeightClass(String weightClass) {
		this.weightClass = weightClass;
	}

	public String getPickupZone() {
		return pickupZone;
	}

	public void setPickupZone(String pickupZone) {
		this.pickupZone = pickupZone;
	}

	public String getDropoffZone() {
		return dropoffZone;
	}

	public void setDropoffZone(String dropoffZone) {
		this.dropoffZone = dropoffZone;
	}
}
