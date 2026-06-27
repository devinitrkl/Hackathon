package com.example.hackathon.config;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.hackathon.domain.Agent;
import com.example.hackathon.domain.AgentStatus;
import com.example.hackathon.domain.Order;
import com.example.hackathon.domain.OrderStatus;
import com.example.hackathon.repository.AgentRepository;
import com.example.hackathon.repository.OrderRepository;

@Component
public class DataLoader implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

	private final AgentRepository agentRepository;
	private final OrderRepository orderRepository;

	public DataLoader(AgentRepository agentRepository, OrderRepository orderRepository) {
		this.agentRepository = agentRepository;
		this.orderRepository = orderRepository;
	}

	@Override
	public void run(String... args) {
		if (agentRepository.count() > 0) {
			return;
		}

		log.info("Seeding database with hackathon sample data");

		// All agents start with zero load. The activeOrderCount is then derived purely
		// from the orders we actually assign below, so the count can never diverge from
		// the real number of orders an agent holds.
		Agent priya = saveAgent("AGT-001", "Priya Sharma", AgentStatus.AVAILABLE, "NORTH", 5);
		Agent rahul = saveAgent("AGT-002", "Rahul Verma", AgentStatus.AVAILABLE, "EAST", 5);
		Agent ananya = saveAgent("AGT-003", "Ananya Iyer", AgentStatus.AVAILABLE, "SOUTH", 5);
		Agent kiran = saveAgent("AGT-004", "Kiran Nair", AgentStatus.AVAILABLE, "WEST", 5);
		Agent deepak = saveAgent("AGT-005", "Deepak Mehta", AgentStatus.AVAILABLE, "CENTRAL", 5);
		saveAgent("AGT-006", "Sneha Patil", AgentStatus.AVAILABLE, "NORTH", 5);
		Agent arjun = saveAgent("AGT-007", "Arjun Reddy", AgentStatus.AVAILABLE, "EAST", 5);
		saveAgent("AGT-008", "Meera Joshi", AgentStatus.AVAILABLE, "SOUTH", 5);

		// Ananya (AGT-003): 4 orders
		assignOrder("ORD-001", "Electronics — Koramangala to Indiranagar", ananya, "NORTH", "SOUTH", "LIGHT");
		assignOrder("ORD-002", "Groceries — HSR Layout to BTM", ananya, "EAST", "SOUTH", "MEDIUM");
		assignOrder("ORD-003", "Pharma — Whitefield to Marathahalli", ananya, "SOUTH", "WEST", "HEAVY");
		assignOrder("ORD-004", "Documents — MG Road to Jayanagar", ananya, "WEST", "CENTRAL", "LIGHT");

		// Deepak (AGT-005): 3 orders
		assignOrder("ORD-005", "Food — Bellandur to Electronic City", deepak, "CENTRAL", "NORTH", "LIGHT");
		assignOrder("ORD-006", "Apparel — Malleshwaram to Rajajinagar", deepak, "NORTH", "EAST", "MEDIUM");
		assignOrder("ORD-007", "Books — Banashankari to JP Nagar", deepak, "EAST", "WEST", "LIGHT");

		// Arjun (AGT-007): 3 orders
		assignOrder("ORD-008", "Hardware — Peenya to Yeshwanthpur", arjun, "SOUTH", "CENTRAL", "MEDIUM");
		assignOrder("ORD-009", "Cold chain — Hebbal to KR Puram", arjun, "CENTRAL", "EAST", "LIGHT");
		assignOrder("ORD-010", "Furniture — Yelahanka to Hennur", arjun, "WEST", "SOUTH", "HEAVY");

		// Kiran (AGT-004): 2 orders
		assignOrder("ORD-011", "Fashion — Frazer Town to Cox Town", kiran, "NORTH", "WEST", "LIGHT");
		assignOrder("ORD-012", "Auto parts — Bommanahalli to Madiwala", kiran, "SOUTH", "EAST", "HEAVY");

		// Rahul (AGT-002): 1 order
		assignOrder("ORD-013", "Stationery — Shivajinagar to Cantonment", rahul, "EAST", "NORTH", "LIGHT");

		// Priya, Sneha, Meera intentionally left AVAILABLE with zero load.
		log.info("Seed complete: {} agents, {} orders", agentRepository.count(), orderRepository.count());
	}

	private Agent saveAgent(String id, String name, AgentStatus status, String zone, int maxCapacity) {
		Agent agent = new Agent(id, name, 0, status);
		agent.setCurrentZone(zone);
		agent.setMaxCapacity(maxCapacity);
		return agentRepository.save(agent);
	}

	private void assignOrder(String id, String description, Agent agent,
			String pickupZone, String dropoffZone, String weightClass) {
		Order order = new Order(id, description, agent, OrderStatus.ASSIGNED, Instant.now());
		order.setPickupZone(pickupZone);
		order.setDropoffZone(dropoffZone);
		order.setWeightClass(weightClass);
		orderRepository.save(order);

		// Keep the agent's load strictly in sync with the orders it actually holds.
		agent.setActiveOrderCount(agent.getActiveOrderCount() + 1);
		agent.setStatus(AgentStatus.BUSY);
		agentRepository.save(agent);
	}
}
