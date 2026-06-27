package com.example.hackathon.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.hackathon.ai.LLMGateway;
import com.example.hackathon.ai.PromptBuilder;
import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.support.TestData;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AiRoutingStrategyTest {

	@Mock
	private LLMGateway llmGateway;

	@Mock
	private PromptBuilder promptBuilder;

	@Mock
	private RuleBasedRoutingStrategy ruleBasedFallback;

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	private AiRoutingStrategy strategy;

	private RoutingContext context;

	@BeforeEach
	void setUp() {
		strategy = new AiRoutingStrategy(llmGateway, promptBuilder, ruleBasedFallback, objectMapper);

		Agent agent1 = TestData.agent("AGT-1", "Priya", 0, AgentStatus.AVAILABLE);
		Agent agent2 = TestData.agent("AGT-2", "Rahul", 1, AgentStatus.AVAILABLE);
		context = new RoutingContext(
				TestData.order("ORD-1", null, OrderStatus.ASSIGNED),
				List.of(agent1, agent2),
				TriggerReason.AGENT_OFFLINE,
				Optional.empty(),
				2,
				Map.of());

		lenient().when(promptBuilder.buildPrompt(any())).thenReturn("prompt");
	}

	@Test
	void hasStableName() {
		assertThat(strategy.getName()).isEqualTo("ai");
	}

	@Test
	void returnsParsedRecommendationWhenLlmReturnsValidJson() {
		when(llmGateway.callLLM(any())).thenReturn(
				"{\"agentId\":\"AGT-2\",\"confidence\":0.88,\"reasoning\":\"Rahul is closest\"}");

		List<RoutingRecommendation> result = strategy.recommend(context);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).agentId()).isEqualTo("AGT-2");
		assertThat(result.get(0).confidence()).isEqualTo(0.88);
		assertThat(result.get(0).reasoning()).isEqualTo("Rahul is closest");
		verify(ruleBasedFallback, never()).recommend(any());
	}

	@Test
	void extractsJsonEmbeddedInMarkdownFence() {
		when(llmGateway.callLLM(any())).thenReturn(
				"Sure! Here is my answer:\n```json\n{\"agentId\":\"AGT-1\",\"confidence\":0.7,\"reasoning\":\"ok\"}\n```");

		List<RoutingRecommendation> result = strategy.recommend(context);

		assertThat(result.get(0).agentId()).isEqualTo("AGT-1");
	}

	@Test
	void clampsConfidenceIntoValidRange() {
		when(llmGateway.callLLM(any())).thenReturn(
				"{\"agentId\":\"AGT-1\",\"confidence\":4.2,\"reasoning\":\"overconfident\"}");

		List<RoutingRecommendation> result = strategy.recommend(context);

		assertThat(result.get(0).confidence()).isEqualTo(1.0);
	}

	@Test
	void fallsBackToRuleBasedWhenLlmReturnsUnknownAgentId() {
		when(llmGateway.callLLM(any())).thenReturn(
				"{\"agentId\":\"AGT-DOES-NOT-EXIST\",\"confidence\":0.9,\"reasoning\":\"hallucinated\"}");
		when(ruleBasedFallback.recommend(any())).thenReturn(
				List.of(new RoutingRecommendation("AGT-1", 1.0, "rule-based pick")));

		List<RoutingRecommendation> result = strategy.recommend(context);

		assertThat(result.get(0).agentId()).isEqualTo("AGT-1");
		assertThat(result.get(0).reasoning()).contains("invalid agent ID");
		verify(ruleBasedFallback).recommend(context);
	}

	@Test
	void fallsBackToRuleBasedWhenLlmThrows() {
		when(llmGateway.callLLM(any())).thenThrow(new RuntimeException("timeout"));
		when(ruleBasedFallback.recommend(any())).thenReturn(
				List.of(new RoutingRecommendation("AGT-2", 1.0, "rule-based pick")));

		List<RoutingRecommendation> result = strategy.recommend(context);

		assertThat(result.get(0).agentId()).isEqualTo("AGT-2");
		assertThat(result.get(0).reasoning()).contains("AI unavailable");
		assertThat(result.get(0).reasoning()).contains("timeout");
	}

	@Test
	void fallsBackWhenJsonMissingAgentId() {
		when(llmGateway.callLLM(any())).thenReturn(
				"{\"confidence\":0.9,\"reasoning\":\"no agent\"}");
		when(ruleBasedFallback.recommend(any())).thenReturn(
				List.of(new RoutingRecommendation("AGT-1", 1.0, "rule-based pick")));

		List<RoutingRecommendation> result = strategy.recommend(context);

		assertThat(result.get(0).agentId()).isEqualTo("AGT-1");
		verify(ruleBasedFallback).recommend(context);
	}

	@Test
	void fallsBackWhenNoJsonInResponse() {
		when(llmGateway.callLLM(any())).thenReturn("I cannot help with that.");
		when(ruleBasedFallback.recommend(any())).thenReturn(
				List.of(new RoutingRecommendation("AGT-1", 1.0, "rule-based pick")));

		List<RoutingRecommendation> result = strategy.recommend(context);

		assertThat(result.get(0).agentId()).isEqualTo("AGT-1");
	}
}
