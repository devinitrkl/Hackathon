# AI Reassignment Engine

ZipRun hackathon solution: when a delivery agent goes offline, the system automatically queues AI-assisted reassignment suggestions for ops approval.

## Stack

- **Backend:** Java 17+, Spring Boot 3.x/4.x, JPA, PostgreSQL
- **Frontend:** React 18 + Vite
- **LLM:** Gemini 2.5 Flash (optional — rule-based fallback works without a key)

## Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL running locally
- (Optional) Gemini API key for AI routing

## Quick start (< 5 minutes)

### 1. Database

```bash
createdb hackathon
```

Update credentials in `backend/src/main/resources/application.properties` if needed.

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

API runs at `http://localhost:8080`. On first start, seed data loads automatically (5 agents, 8 orders).

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI runs at `http://localhost:5173`.

### 4. (Optional) Enable AI routing

```bash
export LLM_API_KEY=your-gemini-key
export ROUTING_STRATEGY=ai
cd backend && ./mvnw spring-boot:run
```

Default strategy is `rule-based` — no API key required.

## Demo path (re-plan loop)

1. Open the ops UI at `http://localhost:5173`
2. In **Agent Roster**, click **Set Offline** on `Priya Sharma (AGT-001)` — she has 3 assigned orders
3. Within ~5 seconds (polling), **Pending Reassignments** shows suggestions with an **Agentic Re-plan** badge
4. Review AI/rule-based reasoning, then click **Accept** or **Reject**
5. On accept, the order moves to `REASSIGNED` and agent load counts update

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/orders` | Create pre-assigned order |
| GET | `/orders?status=` | List/filter orders |
| POST | `/orders/{id}/suggest` | On-demand reassignment suggestion |
| GET | `/agents` | Agent roster |
| PATCH | `/agents/{id}/status` | Update agent status (OFFLINE triggers re-plan) |
| GET | `/suggestions?status=` | List suggestions |
| PATCH | `/suggestions/{id}` | Accept or reject suggestion |

## Project structure

```text
backend/     Spring Boot API
frontend/    React ops interface
ADR.md       Architecture decision records
```

## Configuration

| Property / Env | Default | Description |
|----------------|---------|-------------|
| `routing.strategy` / `ROUTING_STRATEGY` | `rule-based` | Active routing strategy (`rule-based` or `ai`) |
| `LLM_API_KEY` | — | Gemini API key (required for `ai` strategy) |
| `llm.model` | `gemini-2.5-flash` | Gemini model name |

## Submission checklist

- [ ] Public GitHub repo with `/backend` and `/frontend`
- [ ] 5-minute demo video showing agent-offline re-plan path
- [ ] `ADR.md` included
