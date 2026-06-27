package com.example.hackathon.routing;

public record RoutingRecommendation(
		String agentId,
		double confidence,
		String reasoning) {
}
