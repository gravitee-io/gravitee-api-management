---
name: add-a2a-infrastructure
description: Adds A2A protocol-compliant agent infrastructure with LangGraph, Express server, and LLM provider abstraction. Use when creating a new agent project or adding agent capabilities to an existing TypeScript project.
---

# Add A2A Infrastructure

Adds full A2A (Agent-to-Agent) protocol infrastructure including LangGraph orchestration, Express server, and multi-provider LLM abstraction.

## When to use this skill

- Creating a new agent project from scratch
- Adding A2A protocol support to an existing TypeScript project
- Setting up LangGraph-based agent orchestration

## Instructions

### 1. Install dependencies

```bash
pnpm add @a2a-js/sdk @langchain/core @langchain/langgraph @langchain/openai @langchain/anthropic @langchain/google-genai body-parser cors dotenv express uuid zod
pnpm add -D @types/body-parser @types/cors @types/express @types/uuid tsx concurrently
```

### 2. Create directory structure

```bash
mkdir -p src/{lib/llm,types,graph,agents,a2a} tests/agents public/.well-known
```

### 3. Copy files from resources/

| Source                         | Destination                       |
| ------------------------------ | --------------------------------- |
| `resources/provider.ts`        | `src/lib/llm/provider.ts`         |
| `resources/agent.ts`           | `src/types/agent.ts`              |
| `resources/state.ts`           | `src/graph/state.ts`              |
| `resources/supervisor.ts`      | `src/agents/supervisor.ts`        |
| `resources/worker.ts`          | `src/agents/worker.ts`            |
| `resources/graph.ts`           | `src/graph/index.ts`              |
| `resources/agent-card.ts`      | `src/a2a/agent-card.ts`           |
| `resources/protocol.ts`        | `src/a2a/protocol.ts`             |
| `resources/app.ts`             | `src/app.ts`                      |
| `resources/supervisor.test.ts` | `tests/agents/supervisor.test.ts` |
| `resources/.env.example`       | `.env.example`                    |

### 4. Update package.json

Add `"dev"` and `"start"` scripts using `tsx` and `node`.
