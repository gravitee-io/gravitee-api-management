"""Audit log — append-only JSON Lines file for agent run records."""

import json
from dataclasses import asdict
from pathlib import Path

from gravitee_dev.types import AuditRecord


def append_audit_record(record: AuditRecord, cwd: str | Path) -> Path:
    """Append an audit record to ``.gravitee/audit.jsonl``.

    Creates the ``.gravitee/`` directory if it doesn't exist.

    Args:
        record: The audit record to append.
        cwd: Working directory where ``.gravitee/`` lives.

    Returns:
        Path to the audit log file.
    """
    audit_dir = Path(cwd) / ".gravitee"
    audit_dir.mkdir(parents=True, exist_ok=True)

    audit_file = audit_dir / "audit.jsonl"
    line = json.dumps(asdict(record), default=str) + "\n"

    with open(audit_file, "a") as f:
        f.write(line)

    return audit_file
