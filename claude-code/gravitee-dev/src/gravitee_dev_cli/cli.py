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

"""Typer CLI — one command per CLI-exposed agent."""

import asyncio
from pathlib import Path
from typing import Annotated, Literal

import typer

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.agents.commit import CommitAgent
from gravitee_dev.agents.create_pr import CreatePrAgent
from gravitee_dev.agents.docs_sync import DocsSyncAgent
from gravitee_dev.agents.implement import ImplementAgent
from gravitee_dev.review_loop import run_review_loop
from gravitee_dev.types import AgentResult, RunConfig
from gravitee_dev.worktree import cleanup_worktree, init_worktree, list_worktrees

app = typer.Typer(
    name="gravitee-dev",
    help="Composable CLI agents for Gravitee development workflows.",
    no_args_is_help=True,
)


# --- Shared option types ---

CwdOption = Annotated[
    Path | None,
    typer.Option("--cwd", "-C", help="Working directory for the agent."),
]
ModelOption = Annotated[
    str | None,
    typer.Option("--model", "-m", help="Override model (sonnet, opus, haiku)."),
]
MaxBudgetOption = Annotated[
    float | None,
    typer.Option("--max-budget", help="Maximum budget in USD."),
]
QuietOption = Annotated[
    bool,
    typer.Option("--quiet", "-q", help="Suppress all output."),
]
BranchOption = Annotated[
    str | None,
    typer.Option("--branch", "-b", help="Explicit branch name for the worktree."),
]
IssueOption = Annotated[
    int | None,
    typer.Option("--issue", "-i", help="GitHub issue number to link."),
]
WorktreePathOption = Annotated[
    Path | None,
    typer.Option("--worktree-path", help="Explicit path to the worktree to remove."),
]


# --- Default prompts ---

_COMMIT_PROMPT = "Commit current changes"
_IMPLEMENT_PROMPT = "Continue implementation from task.md"
_CREATE_PR_PROMPT = "Push and create draft PR"
_DOCS_SYNC_PROMPT = "Sync documentation with code changes"


# --- Helpers ---


def _validate_model(model: str | None) -> Literal["sonnet", "opus", "haiku"] | None:
    """Validate and return a model name, or exit with an error.

    Args:
        model: Raw model string from CLI option.

    Returns:
        The validated model literal, or None if not specified.

    Raises:
        typer.Exit: If the model name is invalid.
    """
    if model is None:
        return None
    if model not in ("sonnet", "opus", "haiku"):
        typer.echo(
            f"Invalid model: {model}. Must be sonnet, opus, or haiku.",
            err=True,
        )
        raise typer.Exit(1)
    return model  # type: ignore[return-value]


def run_agent(agent: GraviteeAgent, prompt: str, config: RunConfig) -> AgentResult:
    """Run an agent instance and handle the result.

    Args:
        agent: The agent instance to run.
        prompt: User prompt to pass to the agent.
        config: Runtime configuration.

    Returns:
        The AgentResult on success.

    Raises:
        typer.Exit: If the agent run fails.
    """
    result: AgentResult = asyncio.run(agent.run(prompt, config))

    if result.is_error:
        typer.echo(
            f"\n[{result.agent_name}] ERROR: {result.result_text}",
            err=True,
        )
        raise typer.Exit(1)

    if not config.quiet and result.duration_ms is not None:
        typer.echo(
            f"\n[{result.agent_name}] completed in {result.duration_ms:.0f}ms",
            err=True,
        )

    return result


def _make_config(
    cwd: Path | None,
    model: str | None,
    max_budget: float | None,
    quiet: bool,
) -> RunConfig:
    """Build a RunConfig from CLI options.

    Args:
        cwd: Working directory override.
        model: Model override.
        max_budget: Budget cap in USD.
        quiet: Suppress all output.

    Returns:
        A RunConfig instance.
    """
    return RunConfig(
        cwd=cwd,
        model=_validate_model(model),
        max_budget_usd=max_budget,
        quiet=quiet,
    )


# --- Agent commands ---


@app.command()
def commit(
    prompt: Annotated[str, typer.Argument(help="Commit instructions.")] = _COMMIT_PROMPT,
    cwd: CwdOption = None,
    model: ModelOption = None,
    max_budget: MaxBudgetOption = None,
    quiet: QuietOption = False,
) -> None:
    """Commit current changes with a semantic commit message."""
    run_agent(CommitAgent(), prompt, _make_config(cwd, model, max_budget, quiet))


@app.command()
def implement(
    prompt: Annotated[
        str, typer.Argument(help="Implementation instructions.")
    ] = _IMPLEMENT_PROMPT,
    cwd: CwdOption = None,
    model: ModelOption = None,
    max_budget: MaxBudgetOption = None,
    quiet: QuietOption = False,
) -> None:
    """Execute implementation plan with TDD and atomic commits."""
    run_agent(ImplementAgent(), prompt, _make_config(cwd, model, max_budget, quiet))


@app.command(name="create-pr")
def create_pr(
    prompt: Annotated[str, typer.Argument(help="PR creation instructions.")] = _CREATE_PR_PROMPT,
    cwd: CwdOption = None,
    model: ModelOption = None,
    max_budget: MaxBudgetOption = None,
    quiet: QuietOption = False,
) -> None:
    """Push branch and open a draft Pull Request."""
    run_agent(CreatePrAgent(), prompt, _make_config(cwd, model, max_budget, quiet))


@app.command(name="docs-sync")
def docs_sync(
    prompt: Annotated[str, typer.Argument(help="Docs sync instructions.")] = _DOCS_SYNC_PROMPT,
    cwd: CwdOption = None,
    model: ModelOption = None,
    max_budget: MaxBudgetOption = None,
    quiet: QuietOption = False,
) -> None:
    """Sync project documentation with code changes."""
    run_agent(DocsSyncAgent(), prompt, _make_config(cwd, model, max_budget, quiet))


# --- Review loop command ---

ReviewIntervalOption = Annotated[
    int,
    typer.Option("--review-interval", help="Seconds between review polls (default 30)."),
]
RequestReviewerOption = Annotated[
    list[str] | None,
    typer.Option("--request-reviewer", help="GitHub login to request as reviewer (repeatable)."),
]


@app.command(name="review-loop")
def review_loop_cmd(
    cwd: CwdOption = None,
    model: ModelOption = None,
    max_budget: MaxBudgetOption = None,
    quiet: QuietOption = False,
    review_interval: ReviewIntervalOption = 30,
    request_reviewer: RequestReviewerOption = None,
) -> None:
    """Interactive review loop: poll for PR comments and address them.

    Runs until the user types 'c' (mark ready & continue) or 'q' (quit).
    """
    config = _make_config(cwd, model, max_budget, quiet)
    outcome = run_review_loop(
        config,
        agent_runner=run_agent,
        poll_interval=review_interval,
        request_reviewers=request_reviewer,
        quiet=quiet,
    )
    if outcome == "exit":
        raise typer.Exit(0)


# --- Flow orchestrator ---

PauseAfterOption = Annotated[
    str | None,
    typer.Option(
        "--pause-after",
        help="Halt after the named phase (implement, docs-sync, create-pr, or review-loop).",
    ),
]
SkipReviewOption = Annotated[
    bool,
    typer.Option("--skip-review", help="Skip the review loop phase."),
]

_PHASES = ["implement", "docs-sync", "create-pr", "review-loop"]


@app.command()
def flow(
    prompt: Annotated[str, typer.Argument(help="Task description or issue reference.")],
    cwd: CwdOption = None,
    model: ModelOption = None,
    max_budget: MaxBudgetOption = None,
    quiet: QuietOption = False,
    pause_after: PauseAfterOption = None,
    skip_review: SkipReviewOption = False,
    branch: BranchOption = None,
    issue: IssueOption = None,
    review_interval: ReviewIntervalOption = 30,
    request_reviewer: RequestReviewerOption = None,
) -> None:
    """Orchestrate the full development lifecycle end-to-end.

    Deterministic pipeline: init worktree -> implement -> docs-sync ->
    create-pr -> review-loop. Each phase is a standalone agent call.
    On any failure the pipeline stops immediately.
    """
    if pause_after and pause_after not in _PHASES:
        typer.echo(
            f"Invalid --pause-after value: {pause_after}. Must be one of: {', '.join(_PHASES)}",
            err=True,
        )
        raise typer.Exit(1)

    validated_model = _validate_model(model)

    # --- Phase 0: Init worktree ---
    try:
        meta = init_worktree(cwd=cwd, branch=branch, issue=issue)
    except FileNotFoundError as exc:
        typer.echo(f"Error: {exc}", err=True)
        raise typer.Exit(1) from exc
    except Exception as exc:
        typer.echo(f"Error creating worktree: {exc}", err=True)
        raise typer.Exit(1) from exc

    worktree_cwd = Path(meta.worktree)
    if not quiet:
        typer.echo(f"Worktree ready: {meta.worktree}", err=True)
        typer.echo(f"Branch: {meta.branch}", err=True)
        if meta.issue:
            typer.echo(f"Issue: #{meta.issue}", err=True)

    # Build config scoped to the worktree
    config = RunConfig(
        cwd=worktree_cwd,
        model=validated_model,
        max_budget_usd=max_budget,
        quiet=quiet,
    )

    # --- Pipeline: run each phase sequentially ---
    agents: dict[str, tuple[GraviteeAgent, str]] = {
        "implement": (ImplementAgent(), prompt),
        "docs-sync": (DocsSyncAgent(), _DOCS_SYNC_PROMPT),
        "create-pr": (CreatePrAgent(), _CREATE_PR_PROMPT),
    }

    for phase in _PHASES:
        if phase == "review-loop" and skip_review:
            if not quiet:
                typer.echo(f"\n[flow] Skipping phase: {phase}", err=True)
            continue

        if not quiet:
            typer.echo(f"\n[flow] Starting phase: {phase}", err=True)

        if phase == "review-loop":
            outcome = run_review_loop(
                config,
                agent_runner=run_agent,
                poll_interval=review_interval,
                request_reviewers=request_reviewer,
                quiet=quiet,
            )
            if outcome == "exit":
                typer.echo("\n[flow] Exited by user during review loop.", err=True)
                raise typer.Exit(0)
        else:
            agent, agent_prompt = agents[phase]
            run_agent(agent, agent_prompt, config)

        if pause_after == phase:
            typer.echo(
                f"\n[flow] Paused after '{phase}'. "
                f"Re-run remaining phases manually from: {meta.worktree}",
                err=True,
            )
            raise typer.Exit(0)

    # --- Handoff ---
    typer.echo(
        f"\n[flow] Pipeline complete on branch '{meta.branch}'.",
        err=True,
    )
    typer.echo(
        f"After merge, run: gravitee-dev cleanup --branch {meta.branch}",
        err=True,
    )


# --- Worktree commands ---


@app.command()
def cleanup(
    branch: BranchOption = None,
    worktree_path: WorktreePathOption = None,
    cwd: CwdOption = None,
) -> None:
    """Remove a git worktree and its local branch."""
    if branch is None and worktree_path is None:
        typer.echo("Error: provide --branch or --worktree-path", err=True)
        raise typer.Exit(1)

    try:
        message = cleanup_worktree(
            worktree_path=worktree_path,
            branch=branch,
            cwd=cwd,
        )
    except Exception as exc:
        typer.echo(f"Error: {exc}", err=True)
        raise typer.Exit(1) from exc

    typer.echo(message, err=True)


@app.command()
def worktrees(cwd: CwdOption = None) -> None:
    """List all active gravitee-managed worktrees."""
    items = list_worktrees(cwd=cwd)
    if not items:
        typer.echo("No active worktrees.", err=True)
        return

    for meta in items:
        issue_str = f"  (#{meta.issue})" if meta.issue else ""
        typer.echo(f"  {meta.branch}{issue_str}", err=True)
        typer.echo(f"    path: {meta.worktree}", err=True)
        typer.echo(f"    created: {meta.created_at}", err=True)
        typer.echo(err=True)


if __name__ == "__main__":
    app()
