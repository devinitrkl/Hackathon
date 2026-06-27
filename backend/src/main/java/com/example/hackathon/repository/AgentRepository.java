package com.example.hackathon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;

public interface AgentRepository extends JpaRepository<Agent, String> {

	List<Agent> findByStatus(AgentStatus status);
}
