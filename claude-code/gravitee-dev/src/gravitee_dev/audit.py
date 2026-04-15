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
