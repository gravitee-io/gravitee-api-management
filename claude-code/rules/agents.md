# Agent Construction Guidelines

## Core Protocol

- **A2A**: All agents MUST adhere to the Agent-to-Agent (A2A) protocol.
- **Agent Card**: Every agent must have a `/.well-known/agent-card.json`.

## Design

- **Single Responsibility**: One agent, one domain.
- **Statelessness**: Prefer stateless agents relying on context.

## Tools

- **MCP**: Use Model Context Protocol for tools.
- **Access Control**: Explicitly define tool permissions.

---

# Agentic Team Construction Guidelines

## Architecture

- **Orchestrator Pattern**: Use a central orchestrator (like distinct "Planner" or "Manager" agent) to delegate tasks to sub-agents.
- **LangGraph**: Use LangGraph for defining complex stateful workflows and handoffs between agents.

## Handoffs

- **Explicit Contract**: Define precise input/output schemas for agent-to-agent communication.
- **Feedback Loops**: Mechanisms for a reviewer agent to return a task to a worker agent with comments.

## Collaboration

- **Shared State**: Maintain a shared state object (the "Brain") that travels through the graph, accumulating context and artifacts.
