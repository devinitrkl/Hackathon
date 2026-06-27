package com.example.hackathon.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.domain.TriggerReason;
import com.example.hackathon.support.TestData;

@ExtendWith(MockitoExtension.class)
class RoutingEngineTest {

	@Mock
	private RoutingStrategy ruleBased;

	@Mock
	private RoutingStrategy ai;

	@Mock
	private Environment environment;

	private RoutingEngine engine;

	private final RoutingContext context = new RoutingContext(
			TestData.order("ORD-1", null, OrderStatus.ASSIGNED),
			List.of(),
			TriggerReason.INITIAL,
			Optional.empty(),
			1,
			Map.of());

	@BeforeEach
	void setUp() {
		lenient().when(ruleBased.getName()).thenReturn("rule-based");
		lenient().when(ai.getName()).thenReturn("ai");
		engine = new RoutingEngine(Map.of("rule-based", ruleBased, "ai", ai), environment);
	}

	@Test
	void defaultsToRuleBasedWhenPropertyAbsent() {
		when(environment.getProperty("routing.strategy", "rule-based")).thenReturn("rule-based");

		assertThat(engine.getActiveStrategyName()).isEqualTo("rule-based");
	}

	@Test
	void recommendDelegatesToActiveStrategy() {
		when(environment.getProperty("routing.strategy", "rule-based")).thenReturn("ai");
		when(ai.recommend(any())).thenReturn(List.of(new RoutingRecommendation("AGT-1", 1.0, "ai pick")));

		List<RoutingRecommendation> result = engine.recommend(context);

		assertThat(result.get(0).reasoning()).isEqualTo("ai pick");
	}

	@Test
	void recommendThrowsWhenActiveStrategyNotRegistered() {
		when(environment.getProperty("routing.strategy", "rule-based")).thenReturn("ghost");

		assertThatThrownBy(() -> engine.recommend(context))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ghost");
	}

	@Test
	void recommendWithStrategyUsesNamedStrategy() {
		when(ruleBased.recommend(any())).thenReturn(List.of(new RoutingRecommendation("AGT-2", 0.9, "rb")));

		List<RoutingRecommendation> result = engine.recommendWithStrategy(context, "rule-based");

		assertThat(result.get(0).agentId()).isEqualTo("AGT-2");
	}

	@Test
	void recommendWithStrategyThrowsForUnknownStrategy() {
		assertThatThrownBy(() -> engine.recommendWithStrategy(context, "nope"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("nope");
	}

	@Test
	void validateConfiguredStrategyPassesWhenStrategyExists() {
		when(environment.getProperty("routing.strategy", "rule-based")).thenReturn("ai");

		engine.validateConfiguredStrategy(); // should not throw
	}

	@Test
	void validateConfiguredStrategyThrowsWhenStrategyMissing() {
		when(environment.getProperty("routing.strategy", "rule-based")).thenReturn("missing");

		assertThatThrownBy(() -> engine.validateConfiguredStrategy())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("missing");
	}
}
