#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""GraviteeAgent — base class for all agents."""

import json
import time
from abc import ABC
from typing import Any, Literal

from claude_agent_sdk import (
    AgentDefinition,
    AssistantMessage,
    ClaudeAgentOptions,
    ClaudeSDKClient,
    ResultMessage,
    SystemMessage,
    TextBlock,
    ThinkingBlock,
    ToolResultBlock,
    ToolUseBlock,
    UserMessage,
)
from claude_agent_sdk.types import HookEvent, HookMatcher, StreamEvent
from rich.console import Console
from rich.markdown import Markdown
from rich.rule import Rule
from rich.text import Text

from gravitee_dev.audit import append_audit_record
from gravitee_dev.types import AgentResult, AuditRecord, RunConfig

_console = Console()
_err = Console(stderr=True)

_SETTINGS = json.dumps(
    {
        "attribution": {
            "commit": "",
            "pr": "",
        }
    }
)


class GraviteeAgent(ABC):
    """Base class for all Gravitee agents.

    Subclasses must define: name, description, prompt, prompt_version.
    Optional class attributes have sensible defaults.

    Args:
        name: Unique agent identifier (e.g. "commit", "flow").
        description: Short human-readable description for CLI help text.
        prompt: The agent's system prompt text.
        prompt_version: Semantic version of the prompt (e.g. "1.0.0").
        layer: Agent layer — determines grouping in the list command.
        model: Default model for the agent. ``"inherit"`` defers to runner.
        allowed_tools: Tools this agent is allowed to use.
        disallowed_tools: Tools this agent is explicitly denied.
        sub_agents: Dict of sub-agent instances, keyed by agent name.
        cli_command: If set, exposed as a CLI command with this name.
        max_turns: Default max agentic turns.
        max_budget_usd: Default max budget in USD.
        hooks: Programmatic hooks keyed by HookEvent.
    """

    # Required — subclasses must set these as class attributes.
    name: str
    description: str
    prompt: str
    prompt_version: str

    # Optional — sensible defaults.
    layer: Literal["sub", "workflow", "orchestrator"] = "workflow"
    model: Literal["sonnet", "opus", "haiku", "inherit"] = "sonnet"
    allowed_tools: list[str] = []
    disallowed_tools: list[str] = []
    sub_agents: dict[str, "GraviteeAgent"] = {}
    cli_command: str | None = None
    max_turns: int | None = None
    max_budget_usd: float | None = None
    hooks: dict[HookEvent, list[HookMatcher]] = {}

    def __init_subclass__(cls, **kwargs: object) -> None:
        """Validate that concrete subclasses define all required attributes."""
        super().__init_subclass__(**kwargs)
        for attr in ("name", "description", "prompt", "prompt_version"):
            if not hasattr(cls, attr):
                msg = f"{cls.__name__} must define '{attr}'"
                raise TypeError(msg)

    def to_definition(self) -> AgentDefinition:
        """Convert this agent to an SDK AgentDefinition for sub-agent usage.

        Returns:
            An AgentDefinition dataclass instance for ClaudeAgentOptions.agents.
        """
        return AgentDefinition(
            description=self.description,
            prompt=self.prompt,
            model=self.model if self.model != "inherit" else None,
            tools=self.allowed_tools if self.allowed_tools else None,
        )

    def _collect_sub_agents(self) -> dict[str, AgentDefinition]:
        """Recursively flatten sub-agents into a flat dict of AgentDefinitions.

        Returns:
            A flat dict mapping agent name to SDK AgentDefinition instances.
        """
        result: dict[str, AgentDefinition] = {}
        seen: set[str] = set()

        def _collect(agent: GraviteeAgent) -> None:
            for sub_name, sub in agent.sub_agents.items():
                if sub_name in seen:
                    continue
                seen.add(sub_name)
                result[sub_name] = sub.to_definition()
                _collect(sub)

        _collect(self)
        return result

    def _effective_prompt(self, config: RunConfig) -> str:
        """Return the effective system prompt. Override for runtime augmentation.

        Args:
            config: Runtime configuration.

        Returns:
            The system prompt string to use.
        """
        return self.prompt

    def _build_options(self, config: RunConfig) -> ClaudeAgentOptions:
        """Build ClaudeAgentOptions from class attrs + RunConfig overrides.

        Can be overridden by subclasses (e.g. for orchestrator
        prompt augmentation).

        Args:
            config: Runtime configuration overrides.

        Returns:
            A ClaudeAgentOptions instance ready for the SDK.
        """
        sub_agents = self._collect_sub_agents()
        model = config.model or (self.model if self.model != "inherit" else "sonnet")
        cwd = str(config.cwd) if config.cwd else None
        budget = config.max_budget_usd or self.max_budget_usd
        effective_prompt = self._effective_prompt(config)

        return ClaudeAgentOptions(
            model=model,
            permission_mode="bypassPermissions",
            allowed_tools=self.allowed_tools or None,
            disallowed_tools=self.disallowed_tools or None,
            agents=sub_agents or None,
            cwd=cwd,
            max_turns=config.max_turns or self.max_turns,
            max_budget_usd=budget,
            system_prompt={
                "type": "preset",
                "preset": "claude_code",
                "append": effective_prompt,
            },
            hooks=self.hooks or None,
            settings=_SETTINGS,
        )

    async def run(self, prompt: str, config: RunConfig) -> AgentResult:
        """Execute this agent via the Claude Agent SDK ClaudeSDKClient.

        Args:
            prompt: The user prompt to pass to the agent.
            config: Runtime configuration overrides.

        Returns:
            An AgentResult with the agent's output.
        """
        options = self._build_options(config)
        model = options.model or "sonnet"
        cwd = options.cwd or "."

        if not config.quiet:
            _err.print(
                Rule(
                    f"[bold blue]gravitee-dev[/] · [cyan]{self.name}[/] · [dim]{model}[/]",
                    style="dim blue",
                )
            )
            if options.agents:
                _err.print(f"  [dim]sub-agents:[/] {', '.join(options.agents.keys())}")
            if self.hooks:
                hook_events = ", ".join(self.hooks.keys())
                _err.print(f"  [dim]hooks:[/] {hook_events}")

        start = time.monotonic()
        result: ResultMessage | None = None

        async with ClaudeSDKClient(options) as client:
            await client.query(prompt)
            async for message in client.receive_response():
                if isinstance(message, ResultMessage):
                    result = message
                if not config.quiet:
                    _display_message(message)

        duration_ms = (time.monotonic() - start) * 1000

        if result is None:
            agent_result = AgentResult(
                agent_name=self.name,
                result_text="Agent produced no result.",
                is_error=True,
                duration_ms=duration_ms,
            )
        elif result.num_turns == 0 and not result.is_error:
            # A hook (e.g. UserPromptSubmit) blocked the agent before the
            # first LLM turn.  The SDK does not set is_error in this case,
            # so we synthesise a clear error from stop_reason / result text.
            reason = result.stop_reason or result.result or "unknown"
            agent_result = AgentResult(
                agent_name=self.name,
                result_text=f"Agent blocked before first turn: {reason}",
                is_error=True,
                cost_usd=result.total_cost_usd,
                duration_ms=duration_ms,
            )
        else:
            agent_result = AgentResult(
                agent_name=self.name,
                result_text=result.result or "",
                is_error=result.is_error,
                cost_usd=result.total_cost_usd,
                duration_ms=duration_ms,
            )

        self._write_audit(model, cwd, agent_result)
        return agent_result

    def _write_audit(self, model: str, cwd: str, result: AgentResult) -> None:
        """Write an audit record for the completed agent run.

        Args:
            model: The model used for the run.
            cwd: Working directory.
            result: The agent result.
        """
        record = AuditRecord(
            timestamp=time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            agent_name=self.name,
            prompt_version=self.prompt_version,
            model=model,
            cwd=cwd,
            is_error=result.is_error,
            cost_usd=result.cost_usd,
            duration_ms=result.duration_ms,
            outcome="error" if result.is_error else "success",
        )
        try:
            append_audit_record(record, cwd)
        except OSError:
            _err.print("[yellow]warning:[/] failed to write audit record")


def _extract_text(block: TextBlock | ToolResultBlock) -> str:
    """Extract plain text from a TextBlock or ToolResultBlock.

    Args:
        block: The content block to extract text from.

    Returns:
        The text content as a string.
    """
    if isinstance(block, TextBlock):
        return block.text
    if isinstance(block.content, str):
        return block.content
    return "\n".join(b.text for b in block.content if isinstance(b, TextBlock))


_MAX_TOOL_SUMMARY_LEN = 80


def _tool_use_summary(tool_name: str, tool_input: dict[str, Any]) -> str:
    """Return a short human-readable summary of a tool invocation.

    Args:
        tool_name: The name of the tool (e.g. "Bash", "Edit", "Read").
        tool_input: The input dict sent to the tool.

    Returns:
        A concise string describing the call, or empty string if nothing useful.
    """
    raw = ""
    if tool_name == "Bash":
        raw = tool_input.get("command", "")
    elif tool_name in ("Edit", "Write", "Read"):
        raw = tool_input.get("file_path", "")
    elif tool_name in ("Grep", "Glob"):
        raw = tool_input.get("pattern", "")
    else:
        raw = tool_input.get("description", "")

    # Take first line only and truncate.
    summary = raw.split("\n", 1)[0].strip()
    if len(summary) > _MAX_TOOL_SUMMARY_LEN:
        summary = summary[:_MAX_TOOL_SUMMARY_LEN] + "…"
    return summary


def _display_message(
    message: AssistantMessage | ResultMessage | SystemMessage | UserMessage | StreamEvent,
) -> None:
    """Render a single SDK message to the appropriate console.

    Args:
        message: The SDK message to display.
    """
    match message:
        case ResultMessage():
            cost = (
                f"${message.total_cost_usd:.4f}" if message.total_cost_usd is not None else "n/a"
            )
            _err.print(
                Text.assemble(
                    ("turns ", "dim"),
                    (str(message.num_turns), "bold green"),
                    ("  cost ", "dim"),
                    (cost, "bold green"),
                    ("  session ", "dim"),
                    (str(message.session_id), "dim"),
                )
            )
        case AssistantMessage():
            for block in message.content:
                match block:
                    case TextBlock() if block.text:
                        _console.print(Markdown(block.text))
                    case ThinkingBlock():
                        _err.print(Text(block.thinking, style="italic dim blue"))
                    case ToolUseBlock():
                        detail = _tool_use_summary(block.name, block.input)
                        label = f"  [bold yellow]⚙[/]  {block.name}"
                        if detail:
                            label += f"  [dim]{detail}[/]"
                        _err.print(label)
                    case ToolResultBlock():
                        icon, clr = ("✗", "red") if block.is_error else ("✓", "green")
                        _err.print(f"  [{clr}]{icon}[/]  [dim]{block.tool_use_id}[/]")
        case UserMessage():
            content = message.content
            texts: list[str] = (
                [content]
                if isinstance(content, str)
                else [
                    _extract_text(item)
                    for item in content
                    if isinstance(item, TextBlock | ToolResultBlock)
                ]
            )
            for text in texts:
                lines = text.splitlines()
                if len(lines) > 5:
                    text = "\n".join(lines[:5]) + "\n..."
                _err.print(Text(text, style="dim"))
        case SystemMessage():
            _err.print(f"[dim]⬡ system: {message.subtype}[/]")
        case StreamEvent():
            pass  # streaming noise — intentionally ignored
