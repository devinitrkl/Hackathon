# Architecture Decision Records — AI Reassignment Engine

## ADR-1: Where routing logic lives

**Context**  
The system needs a pluggable routing engine called from both an HTTP endpoint (`POST /orders/{id}/suggest`) and an async agentic re-plan handler. Routing must stay separate from persistence and HTTP concerns so sprint 2 can add `ZoneAffinityStrategy` without refactoring controllers.

**Options considered**  
(a) Put routing directly in `OrderController` — fast but couples HTTP to algorithm.  
(b) Embed routing in JPA entities — domain purity but entities shouldn't call LLMs.  
(c) Dedicated `RoutingStrategy` interface + `RoutingEngine` facade in a `routing` package, invoked by `OrderService`.

**Decision**  
Chose (c). `RoutingEngine` selects the active strategy; `OrderService` builds `RoutingContext`, persists suggestions, and owns order state transitions. Controllers stay thin.

**Tradeoffs accepted**  
Extra layer between service and strategies. Worth it for testability and a clear extension point at `RoutingStrategy`.

---

## ADR-2: Runtime strategy switching

**Context**  
Two strategies (rule-based, AI) must be switchable via config without restart. Both the HTTP suggest path and async re-plan must use the same active strategy.

**Options considered**  
(a) `@Qualifier` + static config — requires restart.  
(b) Auto-wired `Map<String, RoutingStrategy>` with strategy name read from `Environment` at call time.  
(c) Manual factory with switch — explicit but modified on every new strategy.

**Decision**  
Chose (b). Beans registered as `@Component("rule-based")` and `@Component("ai")`. `RoutingEngine` reads `routing.strategy` from the environment on each call; `@PostConstruct` validates the configured name exists.

**Tradeoffs accepted**  
Misconfigured strategy names fail at startup (mitigated by validation). Map wiring is slightly implicit compared to a factory, but adding sprint 2's `ZoneAffinityStrategy` is implement + register only.

---

## ADR-3: LLM resilience

**Context**  
Gemini calls can timeout, return malformed JSON, or hallucinate agent IDs. Async re-plan must never silently drop a stranded order.

**Options considered**  
(a) Fail the request and return 503 — breaks ops flow.  
(b) Retry AI calls indefinitely — blocks threads, hides systemic failure.  
(c) Validate response, then fall back to `RuleBasedRoutingStrategy` with reasoning prefixed explaining the fallback.

**Decision**  
Chose (c). `AiRoutingStrategy` wraps rule-based fallback for all failure modes: HTTP errors, parse errors, invalid `agentId`. Fallback reasoning is stored in the suggestion so ops sees what happened.

**Tradeoffs accepted**  
Ops may see rule-based recommendations when AI is configured but unavailable — acceptable because a deterministic suggestion beats no suggestion.

---

## ADR-4: Agentic loop trigger

**Context**  
`PATCH /agents/{id}/status` must return immediately when an agent goes offline, while re-planning runs in the background for all stranded orders.

**Options considered**  
(a) Scheduled poller — not event-driven, delayed detection.  
(b) Inline re-plan in the PATCH handler — blocks the HTTP response.  
(c) `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` listener.

**Decision**  
Chose (c). `AgentService` publishes `AgentOfflineEvent` after persisting OFFLINE status. `ReplanEventListener` runs async after commit, finds ASSIGNED orders, skips duplicates via `existsByOrderAndStatusAndTriggerReason`, and calls `OrderService.createSuggestionForOrder` per order with isolated try/catch.

**Tradeoffs accepted**  
In-process events don't survive restarts mid-re-plan — acceptable for hackathon scope; sprint 3 could move to an outbox/queue.

---

## ADR-5: Extensibility and deliberate exclusions

**Context**  
Sprint 2 adds zone-aware routing and capacity; sprint 3 adds SLA-triggered proactive re-plan and a full dispatch board. Time is limited to 5 hours.

**Options considered**  
(a) Build the full dispatch board now — high visibility, low correctness value for this sprint.  
(b) Build only the reassignment loop with extension seams — meets core agentic requirement.  
(c) Skip placeholder fields — faster now, costly migrations later.

**Decision**  
Chose (b) with nullable placeholders on `Agent` (`currentZone`, `maxCapacity`) and `Order` (`pickupZone`, `dropoffZone`, `weightClass`). Sprint 2's `ZoneAffinityStrategy` implements `RoutingStrategy` and registers as a bean — no changes to `RoutingEngine` selection logic. Deferred full dispatch board, SLA countdown, and SSE streaming (+5 bonus) because the agentic re-plan loop and ADR are correctness requirements; the board is a visibility enhancement.

**Tradeoffs accepted**  
UI shows reassignment queue and agent roster only — sufficient to demo the loop end-to-end, not a production dispatch console.
