package com.example.hackathon.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;

@Component("rule-based")
public class RuleBasedRoutingStrategy implements RoutingStrategy {

	@Override
	public String getName() {
		return "rule-based";
	}

	@Override
	public List<RoutingRecommendation> recommend(RoutingContext context) {
		List<Agent> eligible = context.availableAgents().stream()
				.filter(agent -> agent.getStatus() != AgentStatus.OFFLINE)
				.filter(agent -> agent.getMaxCapacity() == null
						|| context.effectiveLoad(agent) < agent.getMaxCapacity())
				.sorted(Comparator.comparingInt(context::effectiveLoad))
				.toList();

		if (eligible.isEmpty()) {
			throw new IllegalStateException("No available agents for reassignment");
		}

		List<RoutingRecommendation> ranked = new ArrayList<>();
		for (int i = 0; i < eligible.size(); i++) {
			Agent agent = eligible.get(i);
			int effective = context.effectiveLoad(agent);
			double confidence = i == 0 ? 1.0 : Math.max(0.5, 1.0 - 0.1 * i);
			String reasoning = "Rule-based: %s has effective load %d (actual=%d, pending=%d) — rank %d of %d."
					.formatted(
							agent.getName(), effective,
							agent.getActiveOrderCount(),
							effective - agent.getActiveOrderCount(),
							i + 1, eligible.size());
			ranked.add(new RoutingRecommendation(agent.getId(), confidence, reasoning));
		}
		return ranked;
	}
}
