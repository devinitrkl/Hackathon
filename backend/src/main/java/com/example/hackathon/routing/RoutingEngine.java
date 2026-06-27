package com.example.hackathon.routing;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class RoutingEngine {

	private static final Logger log = LoggerFactory.getLogger(RoutingEngine.class);

	private final Map<String, RoutingStrategy> strategies;
	private final Environment environment;

	public RoutingEngine(Map<String, RoutingStrategy> strategies, Environment environment) {
		this.strategies = strategies;
		this.environment = environment;
	}

	@PostConstruct
	void validateConfiguredStrategy() {
		String active = getActiveStrategyName();
		if (!strategies.containsKey(active)) {
			throw new IllegalStateException(
					"Configured routing.strategy '%s' not found. Available: %s"
							.formatted(active, strategies.keySet()));
		}
		log.info("Routing engine ready. Active strategy: {}. Available: {}", active, strategies.keySet());
	}

	public String getActiveStrategyName() {
		return environment.getProperty("routing.strategy", "rule-based");
	}

	public List<RoutingRecommendation> recommend(RoutingContext context) {
		RoutingStrategy strategy = strategies.get(getActiveStrategyName());
		if (strategy == null) {
			throw new IllegalStateException("No routing strategy registered for: " + getActiveStrategyName());
		}
		log.debug("Using routing strategy '{}' for order {}", strategy.getName(), context.order().getId());
		return strategy.recommend(context);
	}

	public List<RoutingRecommendation> recommendWithStrategy(RoutingContext context, String strategyName) {
		RoutingStrategy strategy = strategies.get(strategyName);
		if (strategy == null) {
			throw new IllegalStateException("No routing strategy registered for: " + strategyName);
		}
		return strategy.recommend(context);
	}
}
