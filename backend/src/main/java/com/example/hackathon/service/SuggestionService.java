package com.example.hackathon.service;

import java.util.List;

import com.example.hackathon.domain.SuggestionStatus;
import com.example.hackathon.dto.Dtos.SuggestionResponse;
import com.example.hackathon.dto.Dtos.UpdateSuggestionRequest;

public interface SuggestionService {

	List<SuggestionResponse> listSuggestions(SuggestionStatus status);

	SuggestionResponse updateSuggestion(String suggestionId, UpdateSuggestionRequest request);
}
