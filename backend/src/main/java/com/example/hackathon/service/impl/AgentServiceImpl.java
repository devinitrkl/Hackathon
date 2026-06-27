package com.example.hackathon.service.impl;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.dto.Dtos.AgentResponse;
import com.example.hackathon.event.AgentOfflineEvent;
import com.example.hackathon.exception.ResourceNotFoundException;
import com.example.hackathon.repository.AgentRepository;
import com.example.hackathon.service.AgentService;
import com.example.hackathon.web.DtoMapper;

@Service
public class AgentServiceImpl implements AgentService {

	private final AgentRepository agentRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final DtoMapper mapper;

	public AgentServiceImpl(
			AgentRepository agentRepository,
			ApplicationEventPublisher eventPublisher,
			DtoMapper mapper) {
		this.agentRepository = agentRepository;
		this.eventPublisher = eventPublisher;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AgentResponse> listAgents() {
		return agentRepository.findAll().stream().map(mapper::toAgentResponse).toList();
	}

	@Override
	@Transactional
	public AgentResponse updateStatus(String agentId, AgentStatus newStatus) {
		Agent agent = agentRepository.findById(agentId)
				.orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));

		AgentStatus previousStatus = agent.getStatus();
		agent.setStatus(newStatus);
		agentRepository.save(agent);

		if (newStatus == AgentStatus.OFFLINE && previousStatus != AgentStatus.OFFLINE) {
			eventPublisher.publishEvent(new AgentOfflineEvent(agentId));
		}

		return mapper.toAgentResponse(agent);
	}

	@Override
	@Transactional(readOnly = true)
	public Agent getAgentOrThrow(String agentId) {
		return agentRepository.findById(agentId)
				.orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));
	}
}
