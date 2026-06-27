package com.example.hackathon.routing;

import java.util.List;

public interface RoutingStrategy {

	String getName();

	/**
	 * Returns an ordered list of candidate agents, best first. Callers that need a
	 * single answer take the head of the list; the remainder act as ranked fallbacks.
	 */
	List<RoutingRecommendation> recommend(RoutingContext context);
}
