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

"""Tests for the audit log."""

import json
from pathlib import Path

from gravitee_dev.audit import append_audit_record
from gravitee_dev.types import AuditRecord


def _make_record(agent_name: str = "test-agent", is_error: bool = False) -> AuditRecord:
    return AuditRecord(
        timestamp="2024-01-01T00:00:00Z",
        agent_name=agent_name,
        prompt_version="1.0.0",
        model="sonnet",
        cwd="/project",
        is_error=is_error,
        cost_usd=0.01,
        duration_ms=500.0,
        outcome="error" if is_error else "success",
    )


class TestAuditLog:
    """Tests for append_audit_record."""

    def test_record_appended_on_success(self, tmp_path: Path) -> None:
        record = _make_record()
        path = append_audit_record(record, tmp_path)

        assert path.exists()
        lines = path.read_text().strip().split("\n")
        assert len(lines) == 1
        data = json.loads(lines[0])
        assert data["agent_name"] == "test-agent"
        assert data["outcome"] == "success"
        assert data["is_error"] is False

    def test_record_appended_on_error(self, tmp_path: Path) -> None:
        record = _make_record(is_error=True)
        path = append_audit_record(record, tmp_path)

        lines = path.read_text().strip().split("\n")
        data = json.loads(lines[0])
        assert data["is_error"] is True
        assert data["outcome"] == "error"

    def test_append_only(self, tmp_path: Path) -> None:
        """Multiple records are appended, not overwritten."""
        append_audit_record(_make_record("agent-1"), tmp_path)
        append_audit_record(_make_record("agent-2"), tmp_path)

        path = tmp_path / ".gravitee" / "audit.jsonl"
        lines = path.read_text().strip().split("\n")
        assert len(lines) == 2
        assert json.loads(lines[0])["agent_name"] == "agent-1"
        assert json.loads(lines[1])["agent_name"] == "agent-2"

    def test_valid_json_per_line(self, tmp_path: Path) -> None:
        """Each line is valid JSON."""
        for i in range(5):
            append_audit_record(_make_record(f"agent-{i}"), tmp_path)

        path = tmp_path / ".gravitee" / "audit.jsonl"
        for line in path.read_text().strip().split("\n"):
            data = json.loads(line)
            assert "agent_name" in data
            assert "timestamp" in data

    def test_creates_gravitee_directory(self, tmp_path: Path) -> None:
        assert not (tmp_path / ".gravitee").exists()
        append_audit_record(_make_record(), tmp_path)
        assert (tmp_path / ".gravitee").is_dir()
