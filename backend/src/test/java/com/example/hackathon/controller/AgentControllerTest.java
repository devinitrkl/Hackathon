package com.example.hackathon.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.dto.Dtos.AgentResponse;
import com.example.hackathon.service.AgentService;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AgentService agentService;

	@Test
	void listAgentsReturnsArray() throws Exception {
		when(agentService.listAgents()).thenReturn(List.of(
				new AgentResponse("AGT-1", "Priya", 2, AgentStatus.BUSY, "NORTH", 5)));

		mockMvc.perform(get("/agents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("AGT-1"))
				.andExpect(jsonPath("$[0].activeOrderCount").value(2));
	}

	@Test
	void updateStatusReturns200() throws Exception {
		when(agentService.updateStatus(eq("AGT-1"), eq(AgentStatus.OFFLINE))).thenReturn(
				new AgentResponse("AGT-1", "Priya", 2, AgentStatus.OFFLINE, "NORTH", 5));

		mockMvc.perform(patch("/agents/AGT-1/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"OFFLINE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OFFLINE"));
	}

	@Test
	void updateStatusReturns400WhenStatusMissing() throws Exception {
		mockMvc.perform(patch("/agents/AGT-1/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}
}
