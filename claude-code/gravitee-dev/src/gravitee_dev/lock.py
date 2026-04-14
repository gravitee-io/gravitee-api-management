"""Concurrency lock — prevents parallel flow runs in the same directory."""

import contextlib
import json
import os
import time
from collections.abc import Generator
from contextlib import contextmanager
from pathlib import Path


class FlowLockError(RuntimeError):
    """Raised when a concurrent flow lock is already held."""


@contextmanager
def flow_lock(cwd: str | Path) -> Generator[Path, None, None]:
    """Acquire a file-based lock for the flow agent.

    Creates ``.gravitee/gravitee.lock`` with PID and timestamp.
    Raises ``FlowLockError`` if a lock already exists.
    Cleans up the lock in the ``finally`` block.

    Args:
        cwd: Working directory to place the lock in.

    Yields:
        The path to the lock file.
    """
    lock_dir = Path(cwd) / ".gravitee"
    lock_file = lock_dir / "gravitee.lock"

    if lock_file.exists():
        try:
            data = json.loads(lock_file.read_text())
            pid = data.get("pid")
            ts = data.get("timestamp", "unknown")
            raise FlowLockError(
                f"Flow lock already held by PID {pid} since {ts}. "
                f"Remove {lock_file} if the previous run crashed."
            )
        except (json.JSONDecodeError, OSError) as exc:
            raise FlowLockError(
                f"Stale lock file exists at {lock_file}. Remove it if the previous run crashed."
            ) from exc

    lock_dir.mkdir(parents=True, exist_ok=True)
    lock_data = json.dumps(
        {"pid": os.getpid(), "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}
    )
    lock_file.write_text(lock_data)

    try:
        yield lock_file
    finally:
        with contextlib.suppress(OSError):
            lock_file.unlink(missing_ok=True)
