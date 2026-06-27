# AI Reassignment Engine

ZipRun hackathon solution — when a delivery agent goes offline, the system automatically queues AI-assisted reassignment suggestions for ops approval.

## Stack

- **Backend:** Java 21, Spring Boot 4.1, JPA, PostgreSQL
- **Frontend:** React 18 + Vite
- **LLM:** Gemini 2.5 Flash (falls back to rule-based automatically)

## Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL running locally

## Local Setup

### 1. Database

```bash
createdb hackathon
```

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

Runs at `http://localhost:8080`. Seed data loads automatically on first start.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs at `http://localhost:5173`.

### 4. Enable AI routing (optional)

```bash
export GEMINI_API_KEY=your-key
export ROUTING_STRATEGY=ai
cd backend && ./mvnw spring-boot:run
```

## Environment Variables

All config is passed via environment variables. Defaults work out of the box for local dev.

| Env var | Default | Description |
|---------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/hackathon` | Postgres JDBC URL |
| `DB_USERNAME` | `devinitrkl` | Postgres username |
| `DB_PASSWORD` | *(empty)* | Postgres password |
| `GEMINI_API_KEY` | *(empty)* | Gemini API key |
| `ROUTING_STRATEGY` | `ai` | `rule-based` or `ai` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed frontend origin |

## Architecture

See `ADR.md` for architecture decisions.
