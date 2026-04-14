"""Shared types for the gravitee-dev package."""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Literal


@dataclass(frozen=True)
class RunConfig:
    """Runtime configuration passed to run_agent().

    Args:
        cwd: Working directory for the agent. Defaults to current directory.
        model: Override the agent's default model.
        max_turns: Override the agent's default max_turns.
        max_budget_usd: Maximum budget in USD for the agent run.
        quiet: Suppress all output.
    """

    cwd: Path | None = None
    model: Literal["sonnet", "opus", "haiku"] | None = None
    max_turns: int | None = None
    max_budget_usd: float | None = None
    quiet: bool = False


@dataclass(frozen=True)
class AgentResult:
    """Result from an agent run.

    Args:
        agent_name: Name of the agent that produced this result.
        result_text: The text output from the agent.
        is_error: Whether the agent run failed.
        cost_usd: Total cost of the agent run in USD, if available.
        duration_ms: Duration of the agent run in milliseconds, if available.
    """

    agent_name: str
    result_text: str
    is_error: bool = False
    cost_usd: float | None = None
    duration_ms: float | None = None
    metadata: dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class AuditRecord:
    """Single audit log entry for an agent run.

    Args:
        timestamp: ISO-8601 timestamp of the run.
        agent_name: Name of the agent.
        prompt_version: Prompt version used.
        model: Model used for the run.
        cwd: Working directory.
        is_error: Whether the run failed.
        cost_usd: Cost in USD, if available.
        duration_ms: Duration in milliseconds, if available.
        tool_calls: Number of tool calls made.
        hooks_triggered: Number of hooks that fired.
        outcome: Short description of the outcome.
        metadata: Extra key-value pairs.
    """

    timestamp: str
    agent_name: str
    prompt_version: str
    model: str
    cwd: str
    is_error: bool = False
    cost_usd: float | None = None
    duration_ms: float | None = None
    tool_calls: int = 0
    hooks_triggered: int = 0
    outcome: str = ""
    metadata: dict[str, str] = field(default_factory=dict)
