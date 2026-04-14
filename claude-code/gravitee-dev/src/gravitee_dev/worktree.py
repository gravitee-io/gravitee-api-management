"""Worktree helpers — create and remove isolated git worktrees for tasks."""

import json
import shutil
import subprocess
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

WORKTREES_DIR = ".claude/worktrees"
WORKTREE_META = ".gravitee/worktree.json"


@dataclass(frozen=True)
class WorktreeMeta:
    """Metadata written to .gravitee/worktree.json inside each worktree.

    Args:
        origin: Absolute path to the main working tree.
        branch: Branch name created for this worktree.
        worktree: Absolute path to the worktree directory.
        issue: GitHub issue number, if linked.
        created_at: ISO-8601 creation timestamp.
    """

    origin: str
    branch: str
    worktree: str
    issue: int | None
    created_at: str


def _slugify(title: str) -> str:
    """Convert a plan title to a branch-safe slug.

    Args:
        title: The plan title string.

    Returns:
        A lowercase, hyphen-separated slug safe for branch names.
    """
    import re

    slug = re.sub(r"[^a-zA-Z0-9\s-]", "", title)
    slug = re.sub(r"\s+", "-", slug.strip())
    return slug.lower()[:50]


def _read_plan_title(plan_path: Path) -> str | None:
    """Extract the first ## heading from an implementation plan.

    Args:
        plan_path: Path to implementation_plan.md.

    Returns:
        The heading text, or None if not found.
    """
    try:
        for line in plan_path.read_text().splitlines():
            if line.startswith("## "):
                return line.removeprefix("## ").strip()
    except OSError:
        pass
    return None


def _read_plan_issue(plan_path: Path) -> int | None:
    """Extract a linked issue number from the plan file.

    Args:
        plan_path: Path to implementation_plan.md.

    Returns:
        The issue number, or None if not found.
    """
    import re

    try:
        for line in plan_path.read_text().splitlines()[:5]:
            match = re.search(r"Closes\s+#(\d+)", line)
            if match:
                return int(match.group(1))
    except OSError:
        pass
    return None


def _git_user() -> str:
    """Get the current git user name for branch prefixing.

    Returns:
        The git user name, or 'dev' as fallback.
    """
    try:
        result = subprocess.run(
            ["git", "config", "user.name"],
            capture_output=True,
            text=True,
            check=True,
        )
        name = result.stdout.strip().lower().replace(" ", "")
        return name or "dev"
    except (subprocess.CalledProcessError, FileNotFoundError):
        return "dev"


def init_worktree(
    cwd: Path | None = None,
    branch: str | None = None,
    issue: int | None = None,
) -> WorktreeMeta:
    """Create an isolated git worktree for task implementation.

    Reads implementation_plan.md from the current directory, creates a new
    git worktree with a feature branch, and copies plan files into it.

    Args:
        cwd: Working directory (defaults to current directory).
        branch: Explicit branch name. If None, derived from plan title.
        issue: GitHub issue number to link. If None, extracted from plan.

    Returns:
        WorktreeMeta with the worktree details.

    Raises:
        FileNotFoundError: If implementation_plan.md is not found.
        subprocess.CalledProcessError: If git worktree add fails.
    """
    root = (cwd or Path.cwd()).resolve()
    plan_path = root / "implementation_plan.md"
    lock_path = root / ".gravitee" / "plan.lock"

    if not plan_path.exists():
        msg = f"implementation_plan.md not found in {root}"
        raise FileNotFoundError(msg)

    if branch is None:
        title = _read_plan_title(plan_path)
        slug = _slugify(title) if title else "task"
        user = _git_user()
        branch = f"{user}/{slug}"

    if issue is None:
        issue = _read_plan_issue(plan_path)

    worktree_path = (root / WORKTREES_DIR / branch).resolve()

    # Check whether the branch already exists so we can resume interrupted flows.
    branch_exists = (
        subprocess.run(
            ["git", "rev-parse", "--verify", branch],
            cwd=str(root),
            capture_output=True,
        ).returncode
        == 0
    )
    worktree_cmd = (
        ["git", "worktree", "add", str(worktree_path), branch]
        if branch_exists
        else ["git", "worktree", "add", "-b", branch, str(worktree_path)]
    )
    subprocess.run(
        worktree_cmd,
        cwd=str(root),
        check=True,
        capture_output=True,
        text=True,
    )

    # Copy plan files into the worktree
    shutil.copy2(plan_path, worktree_path / "implementation_plan.md")

    gravitee_dir = worktree_path / ".gravitee"
    gravitee_dir.mkdir(parents=True, exist_ok=True)

    if lock_path.exists():
        shutil.copy2(lock_path, gravitee_dir / "plan.lock")

    # Write worktree metadata
    meta = WorktreeMeta(
        origin=str(root),
        branch=branch,
        worktree=str(worktree_path),
        issue=issue,
        created_at=datetime.now(timezone.utc).isoformat(),
    )
    (gravitee_dir / "worktree.json").write_text(json.dumps(asdict(meta), indent=2) + "\n")

    return meta


def cleanup_worktree(
    worktree_path: Path | None = None,
    branch: str | None = None,
    cwd: Path | None = None,
) -> str:
    """Remove a git worktree and its local branch.

    Args:
        worktree_path: Explicit path to the worktree to remove.
        branch: Branch name to clean up. Used to locate the worktree if
            worktree_path is not given.
        cwd: Working directory for git commands (defaults to current directory).

    Returns:
        A summary message of what was cleaned up.

    Raises:
        FileNotFoundError: If no worktree can be located.
        subprocess.CalledProcessError: If git commands fail.
    """
    root = (cwd or Path.cwd()).resolve()

    if worktree_path is None and branch is not None:
        worktree_path = (root / WORKTREES_DIR / branch).resolve()

    if worktree_path is None:
        msg = "Provide --worktree-path or --branch to identify the worktree"
        raise FileNotFoundError(msg)

    worktree_path = worktree_path.resolve()

    # Read metadata to get branch name if not provided
    meta_path = worktree_path / WORKTREE_META
    if branch is None and meta_path.exists():
        meta = json.loads(meta_path.read_text())
        branch = meta.get("branch")

    # Remove the worktree — try graceful first, fail clearly if dirty
    try:
        subprocess.run(
            ["git", "worktree", "remove", str(worktree_path)],
            cwd=str(root),
            check=True,
            capture_output=True,
            text=True,
        )
    except subprocess.CalledProcessError as exc:
        stderr = exc.stderr.strip() if exc.stderr else ""
        if "untracked" in stderr or "modified" in stderr or "changes" in stderr:
            msg = (
                f"Worktree '{worktree_path}' has uncommitted changes. "
                "Commit or discard them first, or use 'git worktree remove --force' manually."
            )
            raise RuntimeError(msg) from exc
        raise

    # Delete the local branch (already merged)
    messages = [f"Removed worktree: {worktree_path}"]
    if branch:
        try:
            subprocess.run(
                ["git", "branch", "-d", branch],
                cwd=str(root),
                check=True,
                capture_output=True,
                text=True,
            )
            messages.append(f"Deleted branch: {branch}")
        except subprocess.CalledProcessError:
            messages.append(f"Branch '{branch}' not deleted (may not be fully merged)")

    return "\n".join(messages)


def list_worktrees(cwd: Path | None = None) -> list[WorktreeMeta]:
    """List all gravitee-managed worktrees.

    Args:
        cwd: Working directory (defaults to current directory).

    Returns:
        A list of WorktreeMeta for each managed worktree found.
    """
    root = (cwd or Path.cwd()).resolve()
    worktrees_dir = root / WORKTREES_DIR
    results: list[WorktreeMeta] = []

    if not worktrees_dir.exists():
        return results

    for meta_file in worktrees_dir.rglob("worktree.json"):
        try:
            data = json.loads(meta_file.read_text())
            results.append(
                WorktreeMeta(
                    origin=data["origin"],
                    branch=data["branch"],
                    worktree=data["worktree"],
                    issue=data.get("issue"),
                    created_at=data["created_at"],
                )
            )
        except (json.JSONDecodeError, KeyError, OSError):
            continue

    return results
