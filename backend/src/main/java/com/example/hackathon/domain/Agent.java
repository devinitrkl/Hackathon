package com.example.hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agents")
public class Agent {

	@Id
	private String id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private int activeOrderCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AgentStatus status;

	@Column
	private String currentZone;

	@Column
	private Integer maxCapacity;

	public Agent() {
	}

	public Agent(String id, String name, int activeOrderCount, AgentStatus status) {
		this.id = id;
		this.name = name;
		this.activeOrderCount = activeOrderCount;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getActiveOrderCount() {
		return activeOrderCount;
	}

	public void setActiveOrderCount(int activeOrderCount) {
		this.activeOrderCount = activeOrderCount;
	}

	public AgentStatus getStatus() {
		return status;
	}

	public void setStatus(AgentStatus status) {
		this.status = status;
	}

	public String getCurrentZone() {
		return currentZone;
	}

	public void setCurrentZone(String currentZone) {
		this.currentZone = currentZone;
	}

	public Integer getMaxCapacity() {
		return maxCapacity;
	}

	public void setMaxCapacity(Integer maxCapacity) {
		this.maxCapacity = maxCapacity;
	}
}
