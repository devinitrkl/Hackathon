package com.example.hackathon.service;

import java.util.List;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.dto.Dtos.AgentResponse;

public interface AgentService {

	List<AgentResponse> listAgents();

	AgentResponse updateStatus(String agentId, AgentStatus newStatus);

	Agent getAgentOrThrow(String agentId);
}
