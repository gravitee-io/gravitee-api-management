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

"""Interactive review loop — poll for PR comments and address them."""

import io
import json
import select
import subprocess
import sys
from pathlib import Path
from typing import TYPE_CHECKING, Callable, Literal

import typer

from gravitee_dev.agents.pr_review import PrReviewAgent
from gravitee_dev.types import RunConfig

if TYPE_CHECKING:
    from gravitee_dev.types import AgentResult

_PR_REVIEW_PROMPT = "Address all open PR review comments"

_UNRESOLVED_QUERY = """
query($owner:String!,$repo:String!,$pr:Int!) {
  repository(owner:$owner,name:$repo) {
    pullRequest(number:$pr) {
      reviewThreads(first:100) {
        nodes { id isResolved isOutdated }
      }
    }
  }
}
""".strip()

_RESOLVE_THREAD_MUTATION = """
mutation($threadId:ID!) {
  resolveReviewThread(input:{threadId:$threadId}) {
    thread { isResolved }
  }
}
""".strip()


def _gh(args: list[str], cwd: Path) -> str:
    """Run a gh CLI command and return stdout.

    Args:
        args: Arguments to pass to gh.
        cwd: Working directory for the command.

    Returns:
        The stdout output as a string.

    Raises:
        subprocess.CalledProcessError: If the command fails.
    """
    result = subprocess.run(
        ["gh", *args],
        capture_output=True,
        text=True,
        check=True,
        cwd=str(cwd),
    )
    return result.stdout.strip()


def _get_pr_number(cwd: Path) -> int:
    """Get the PR number for the current branch.

    Args:
        cwd: Working directory (must be inside a git repo with a PR).

    Returns:
        The pull request number.
    """
    output = _gh(["pr", "view", "--json", "number", "-q", ".number"], cwd)
    return int(output)


def _get_repo_owner_name(cwd: Path) -> tuple[str, str]:
    """Get the owner and repo name from the current git remote.

    Args:
        cwd: Working directory.

    Returns:
        A tuple of (owner, repo_name).
    """
    output = _gh(
        ["repo", "view", "--json", "owner,name", "-q", '[.owner.login,.name] | join(" ")'],
        cwd,
    )
    parts = output.split()
    if len(parts) < 2:
        raise RuntimeError(f"Unexpected output from gh repo view: {output!r}")
    return parts[0], parts[1]


def _get_unresolved_threads(cwd: Path, pr_number: int) -> list[str]:
    """Get IDs of unresolved, non-outdated review threads on a PR.

    Args:
        cwd: Working directory.
        pr_number: The pull request number.

    Returns:
        List of GraphQL node IDs for unresolved threads.
    """
    owner, repo = _get_repo_owner_name(cwd)
    output = _gh(
        [
            "api",
            "graphql",
            "-f",
            f"query={_UNRESOLVED_QUERY}",
            "-f",
            f"owner={owner}",
            "-f",
            f"repo={repo}",
            "-F",
            f"pr={pr_number}",
        ],
        cwd,
    )
    data = json.loads(output)
    if "errors" in data:
        raise RuntimeError(f"GraphQL errors: {data['errors']}")
    repository = data.get("data", {}).get("repository")
    if not repository:
        return []
    threads = repository.get("pullRequest", {}).get("reviewThreads", {}).get("nodes", [])
    return [t["id"] for t in threads if not t.get("isResolved") and not t.get("isOutdated")]


def _resolve_threads(cwd: Path, thread_ids: list[str]) -> None:
    """Resolve review threads by their GraphQL node IDs.

    Args:
        cwd: Working directory.
        thread_ids: List of thread node IDs to resolve.
    """
    for thread_id in thread_ids:
        try:
            _gh(
                [
                    "api",
                    "graphql",
                    "-f",
                    f"query={_RESOLVE_THREAD_MUTATION}",
                    "-f",
                    f"threadId={thread_id}",
                ],
                cwd,
            )
        except subprocess.CalledProcessError:
            typer.echo(f"[review-loop] Warning: could not resolve thread {thread_id}", err=True)


def _mark_ready(cwd: Path) -> None:
    """Mark the current PR as ready for review.

    Args:
        cwd: Working directory.
    """
    _gh(["pr", "ready"], cwd)


def _request_reviewers(cwd: Path, reviewers: list[str]) -> None:
    """Request reviewers on the current PR.

    Args:
        cwd: Working directory.
        reviewers: List of GitHub logins to request as reviewers.
    """
    for reviewer in reviewers:
        try:
            _gh(["pr", "edit", "--add-reviewer", reviewer], cwd)
        except subprocess.CalledProcessError:
            typer.echo(f"[review-loop] Warning: could not add reviewer '{reviewer}'", err=True)


def _wait_for_input(timeout: float) -> str | None:
    """Wait for user input with a timeout using select.

    Falls back to a simple sleep if stdin is not a real file descriptor
    (e.g. during testing or when piped).

    Args:
        timeout: Seconds to wait before returning None.

    Returns:
        The stripped user input, or None on timeout.
    """
    import time

    try:
        ready, _, _ = select.select([sys.stdin], [], [], timeout)
    except (OSError, io.UnsupportedOperation):
        time.sleep(timeout)
        return None
    if ready:
        return sys.stdin.readline().strip().lower()
    return None


def run_review_loop(
    config: RunConfig,
    *,
    agent_runner: "Callable[[PrReviewAgent, str, RunConfig], AgentResult]",
    poll_interval: int = 30,
    request_reviewers: list[str] | None = None,
    quiet: bool = False,
) -> Literal["ready", "exit"]:
    """Run an interactive review loop until the user decides to proceed.

    Polls for unresolved PR review comments. When found, runs the pr-review
    agent to address them. Between agent runs, the user can type 'c' to mark
    the PR ready and continue, or 'q' to exit the flow.

    Args:
        config: Runtime configuration (must have cwd set to worktree).
        agent_runner: Callable that executes an agent (injected by caller to
            avoid a dependency on the CLI layer from this core module).
        poll_interval: Seconds between polls when idle.
        request_reviewers: GitHub logins to request as reviewers.
        quiet: Suppress output.

    Returns:
        "ready" if user chose to continue, "exit" if user chose to quit.
    """
    cwd = Path(config.cwd) if config.cwd else Path.cwd()

    # Mark PR as ready for review
    if not quiet:
        typer.echo("[review-loop] Marking PR as ready for review...", err=True)
    _mark_ready(cwd)

    # Request reviewers if specified
    if request_reviewers:
        if not quiet:
            typer.echo(
                f"[review-loop] Requesting reviewers: {', '.join(request_reviewers)}", err=True
            )
        _request_reviewers(cwd, request_reviewers)

    pr_number = _get_pr_number(cwd)
    if not quiet:
        typer.echo(f"[review-loop] Watching PR #{pr_number} for review comments.", err=True)

    while True:
        # Check for unresolved comments
        try:
            unresolved_ids = _get_unresolved_threads(cwd, pr_number)
        except (subprocess.CalledProcessError, json.JSONDecodeError, KeyError) as exc:
            typer.echo(f"[review-loop] Warning: failed to fetch comments: {exc}", err=True)
            unresolved_ids = []

        if unresolved_ids:
            if not quiet:
                typer.echo(
                    f"\n[review-loop] {len(unresolved_ids)} unresolved comment(s). "
                    "Running pr-review agent...",
                    err=True,
                )
            agent_runner(PrReviewAgent(), _PR_REVIEW_PROMPT, config)
            # Resolve the threads the agent just addressed so they are not
            # re-processed on the next poll iteration.
            _resolve_threads(cwd, unresolved_ids)
            continue  # Re-check immediately for any new comments

        # No unresolved comments — wait for user input or poll timeout
        if not quiet:
            typer.echo(
                f"\n[review-loop] No unresolved comments. "
                f"Polling again in {poll_interval}s... "
                "(c=mark ready & continue, q=quit)",
                err=True,
            )

        user_input = _wait_for_input(poll_interval)

        if user_input is not None:
            if user_input in ("c", "continue"):
                if not quiet:
                    typer.echo("[review-loop] PR is ready. Continuing.", err=True)
                return "ready"
            elif user_input in ("q", "quit", "exit"):
                if not quiet:
                    typer.echo("[review-loop] Exiting flow.", err=True)
                return "exit"
            elif not quiet:
                typer.echo(
                    f"[review-loop] Unknown input: '{user_input}'. Use 'c' or 'q'.", err=True
                )
