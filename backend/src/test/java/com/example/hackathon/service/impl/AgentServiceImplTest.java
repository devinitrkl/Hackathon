package com.example.hackathon.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.event.AgentOfflineEvent;
import com.example.hackathon.exception.ResourceNotFoundException;
import com.example.hackathon.repository.AgentRepository;
import com.example.hackathon.support.TestData;
import com.example.hackathon.web.DtoMapper;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

	@Mock
	private AgentRepository agentRepository;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Mock
	private DtoMapper mapper;

	@InjectMocks
	private AgentServiceImpl service;

	@Test
	void updateStatusPublishesEventWhenAgentGoesOffline() {
		Agent agent = TestData.agent("AGT-1", "Priya", 3, AgentStatus.BUSY);
		when(agentRepository.findById("AGT-1")).thenReturn(Optional.of(agent));

		service.updateStatus("AGT-1", AgentStatus.OFFLINE);

		assertThat(agent.getStatus()).isEqualTo(AgentStatus.OFFLINE);
		verify(eventPublisher).publishEvent(new AgentOfflineEvent("AGT-1"));
	}

	@Test
	void updateStatusDoesNotRepublishWhenAlreadyOffline() {
		Agent agent = TestData.agent("AGT-1", "Priya", 0, AgentStatus.OFFLINE);
		when(agentRepository.findById("AGT-1")).thenReturn(Optional.of(agent));

		service.updateStatus("AGT-1", AgentStatus.OFFLINE);

		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void updateStatusToAvailableDoesNotPublishOfflineEvent() {
		Agent agent = TestData.agent("AGT-1", "Priya", 0, AgentStatus.BUSY);
		when(agentRepository.findById("AGT-1")).thenReturn(Optional.of(agent));

		service.updateStatus("AGT-1", AgentStatus.AVAILABLE);

		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void updateStatusThrowsWhenAgentMissing() {
		when(agentRepository.findById("AGT-X")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateStatus("AGT-X", AgentStatus.OFFLINE))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void getAgentOrThrowReturnsAgent() {
		Agent agent = TestData.agent("AGT-1", "Priya", 0, AgentStatus.AVAILABLE);
		when(agentRepository.findById("AGT-1")).thenReturn(Optional.of(agent));

		assertThat(service.getAgentOrThrow("AGT-1")).isSameAs(agent);
	}

	@Test
	void getAgentOrThrowThrowsWhenMissing() {
		when(agentRepository.findById("AGT-X")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAgentOrThrow("AGT-X"))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
