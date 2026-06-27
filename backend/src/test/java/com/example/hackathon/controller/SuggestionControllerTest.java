package com.example.hackathon.controller;

import static org.mockito.ArgumentMatchers.any;
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

import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.dto.Dtos.SuggestionResponse;
import com.example.hackathon.exception.ConflictException;
import com.example.hackathon.service.SuggestionService;

@WebMvcTest(SuggestionController.class)
class SuggestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SuggestionService suggestionService;

	@Test
	void listSuggestionsReturnsArray() throws Exception {
		when(suggestionService.listSuggestions(SuggestionStatus.PENDING)).thenReturn(List.of(
				new SuggestionResponse("S1", "ORD-1", "d", "AGT-2", "Rahul", 0.9, "r",
						SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE, "now")));

		mockMvc.perform(get("/suggestions").param("status", "PENDING"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("S1"));
	}

	@Test
	void updateSuggestionReturns200() throws Exception {
		when(suggestionService.updateSuggestion(eq("S1"), any())).thenReturn(
				new SuggestionResponse("S1", "ORD-1", "d", "AGT-2", "Rahul", 0.9, "r",
						SuggestionStatus.ACCEPTED, TriggerReason.AGENT_OFFLINE, "now"));

		mockMvc.perform(patch("/suggestions/S1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACCEPTED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"));
	}

	@Test
	void conflictFromServiceMapsTo409() throws Exception {
		when(suggestionService.updateSuggestion(any(), any()))
				.thenThrow(new ConflictException("Recommended agent AGT-2 is OFFLINE"));

		mockMvc.perform(patch("/suggestions/S1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACCEPTED\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Recommended agent AGT-2 is OFFLINE"));
	}

	@Test
	void updateSuggestionReturns400WhenStatusMissing() throws Exception {
		mockMvc.perform(patch("/suggestions/S1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}
}
