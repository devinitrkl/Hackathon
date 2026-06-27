package com.example.hackathon.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.dto.Dtos.SuggestionResponse;
import com.example.hackathon.dto.Dtos.UpdateSuggestionRequest;
import com.example.hackathon.service.SuggestionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/suggestions")
public class SuggestionController {

	private final SuggestionService suggestionService;

	public SuggestionController(SuggestionService suggestionService) {
		this.suggestionService = suggestionService;
	}

	@GetMapping
	public List<SuggestionResponse> listSuggestions(@RequestParam(required = false) SuggestionStatus status) {
		return suggestionService.listSuggestions(status);
	}

	@PatchMapping("/{id}")
	public SuggestionResponse updateSuggestion(
			@PathVariable String id,
			@Valid @RequestBody UpdateSuggestionRequest request) {
		return suggestionService.updateSuggestion(id, request);
	}
}
