package com.example.hackathon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.ReassignmentSuggestion;
import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;

public interface ReassignmentSuggestionRepository extends JpaRepository<ReassignmentSuggestion, String> {

	List<ReassignmentSuggestion> findByStatus(SuggestionStatus status);

	Optional<ReassignmentSuggestion> findFirstByOrderAndStatusOrderByCreatedAtDesc(Order order, SuggestionStatus status);

	boolean existsByOrderAndStatusAndTriggerReason(Order order, SuggestionStatus status, TriggerReason triggerReason);

	List<ReassignmentSuggestion> findByStatusAndRecommendedAgentId(SuggestionStatus status, String recommendedAgentId);
}
