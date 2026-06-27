package com.example.hackathon.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reassignment_suggestions")
public class ReassignmentSuggestion {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "recommended_agent_id", nullable = false)
	private Agent recommendedAgent;

	@Column(nullable = false)
	private double confidence;

	@Column(nullable = false, length = 2000)
	private String reasoning;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SuggestionStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TriggerReason triggerReason;

	@Column(nullable = false)
	private Instant createdAt;

	public ReassignmentSuggestion() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Agent getRecommendedAgent() {
		return recommendedAgent;
	}

	public void setRecommendedAgent(Agent recommendedAgent) {
		this.recommendedAgent = recommendedAgent;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public String getReasoning() {
		return reasoning;
	}

	public void setReasoning(String reasoning) {
		this.reasoning = reasoning;
	}

	public SuggestionStatus getStatus() {
		return status;
	}

	public void setStatus(SuggestionStatus status) {
		this.status = status;
	}

	public TriggerReason getTriggerReason() {
		return triggerReason;
	}

	public void setTriggerReason(TriggerReason triggerReason) {
		this.triggerReason = triggerReason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
