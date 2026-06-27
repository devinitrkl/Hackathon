package com.example.hackathon.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.hackathon.ai.LLMGateway;
import com.example.hackathon.ai.PromptBuilder;
import com.example.hackathon.domain.Agent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component("ai")
public class AiRoutingStrategy implements RoutingStrategy {

	private static final Logger log = LoggerFactory.getLogger(AiRoutingStrategy.class);
	private static final Pattern JSON_BLOCK = Pattern.compile("\\{.*}", Pattern.DOTALL);

	private final LLMGateway llmGateway;
	private final PromptBuilder promptBuilder;
	private final RuleBasedRoutingStrategy ruleBasedFallback;
	private final ObjectMapper objectMapper;

	public AiRoutingStrategy(
			LLMGateway llmGateway,
			PromptBuilder promptBuilder,
			RuleBasedRoutingStrategy ruleBasedFallback,
			ObjectMapper objectMapper) {
		this.llmGateway = llmGateway;
		this.promptBuilder = promptBuilder;
		this.ruleBasedFallback = ruleBasedFallback;
		this.objectMapper = objectMapper;
	}

	@Override
	public String getName() {
		return "ai";
	}

	@Override
	public List<RoutingRecommendation> recommend(RoutingContext context) {
		try {
			String prompt = promptBuilder.buildPrompt(context);
			String raw = llmGateway.callLLM(prompt);
			RoutingRecommendation parsed = parseResponse(raw);

			Set<String> validAgentIds = context.availableAgents().stream()
					.map(Agent::getId)
					.collect(Collectors.toSet());

			if (!validAgentIds.contains(parsed.agentId())) {
				log.warn("LLM returned invalid agentId '{}', falling back to rule-based", parsed.agentId());
				return withFallbackNote(ruleBasedFallback.recommend(context),
						"AI returned invalid agent ID; used rule-based fallback.");
			}

			return List.of(parsed);
		}
		catch (Exception ex) {
			log.error("AI routing failed for order {}: {}", context.order().getId(), ex.getMessage());
			return withFallbackNote(ruleBasedFallback.recommend(context),
					"AI unavailable; used rule-based fallback. Cause: " + ex.getMessage());
		}
	}

	private List<RoutingRecommendation> withFallbackNote(List<RoutingRecommendation> fallback, String prefix) {
		if (fallback.isEmpty()) {
			return fallback;
		}
		List<RoutingRecommendation> annotated = new ArrayList<>(fallback);
		RoutingRecommendation first = annotated.getFirst();
		annotated.set(0, new RoutingRecommendation(
				first.agentId(), first.confidence(), prefix + " " + first.reasoning()));
		return annotated;
	}

	private RoutingRecommendation parseResponse(String raw) throws Exception {
		String json = extractJson(raw);
		JsonNode node = objectMapper.readTree(json);

		String agentId = node.path("agentId").asText(null);
		double confidence = node.path("confidence").asDouble(0.5);
		String reasoning = node.path("reasoning").asText("No reasoning provided");

		if (agentId == null || agentId.isBlank()) {
			throw new IllegalStateException("Missing agentId in LLM response");
		}

		confidence = Math.clamp(confidence, 0.0, 1.0);
		return new RoutingRecommendation(agentId, confidence, reasoning);
	}

	private String extractJson(String raw) {
		Matcher matcher = JSON_BLOCK.matcher(raw.trim());
		if (matcher.find()) {
			return matcher.group();
		}
		throw new IllegalStateException("No JSON object found in LLM response");
	}
}
