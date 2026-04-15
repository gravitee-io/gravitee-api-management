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

"""Acceptance tests for the TDD orchestration composite hook."""

import subprocess
from pathlib import Path

from claude_agent_sdk.types import HookContext, PreToolUseHookInput

_CTX = HookContext(signal=None)


def _pre_tool_input(
    file_path: str,
    cwd: str = "/project",
    agent_type: str | None = None,
) -> PreToolUseHookInput:
    """Build a PreToolUseHookInput with optional agent_type."""
    inp = PreToolUseHookInput(
        session_id="test-session",
        transcript_path="/tmp/transcript",
        cwd=cwd,
        hook_event_name="PreToolUse",
        tool_name="Write",
        tool_input={"file_path": file_path},
        tool_use_id="tool-1",
    )
    if agent_type is not None:
        inp["agent_type"] = agent_type
    return inp


# ===========================================================================
# Orchestrator (no agent_type)
# ===========================================================================


class TestOrchestratorRouting:
    """Orchestrator (no agent_type) can only write to .gravitee/context/."""

    async def test_orchestrator_allowed_to_write_context(self) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        result = await tdd_orchestration_hook(
            _pre_tool_input(".gravitee/context/chunk-001.md"), None, _CTX
        )
        assert result == {}

    async def test_orchestrator_blocked_from_source_files(self) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        result = await tdd_orchestration_hook(_pre_tool_input("src/main.py"), None, _CTX)
        assert result["decision"] == "block"
        assert ".gravitee/context/" in result.get("reason", "")


# ===========================================================================
# Test-writer agent
# ===========================================================================


class TestTestWriterRouting:
    """Test-writer agent: can only write test files + context reports."""

    async def test_allowed_to_write_test_file_with_context(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        # Set up chunk context
        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("001")
        (ctx_dir / "chunk-001.md").write_text("requirements")

        result = await tdd_orchestration_hook(
            _pre_tool_input("test_foo.py", cwd=str(tmp_path), agent_type="test-writer"),
            None,
            _CTX,
        )
        assert result == {}

    async def test_blocked_from_non_test_file(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("001")
        (ctx_dir / "chunk-001.md").write_text("requirements")

        result = await tdd_orchestration_hook(
            _pre_tool_input("main.py", cwd=str(tmp_path), agent_type="test-writer"),
            None,
            _CTX,
        )
        assert result["decision"] == "block"
        assert "test file" in result.get("reason", "").lower()

    async def test_blocked_when_current_chunk_missing(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        # No .gravitee/context/ at all
        result = await tdd_orchestration_hook(
            _pre_tool_input("test_foo.py", cwd=str(tmp_path), agent_type="test-writer"),
            None,
            _CTX,
        )
        assert result["decision"] == "block"
        assert ".current-chunk" in result.get("reason", "")

    async def test_blocked_when_chunk_md_missing(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("001")
        # chunk-001.md does NOT exist

        result = await tdd_orchestration_hook(
            _pre_tool_input("test_foo.py", cwd=str(tmp_path), agent_type="test-writer"),
            None,
            _CTX,
        )
        assert result["decision"] == "block"
        assert "chunk-001.md" in result.get("reason", "")

    async def test_allowed_to_write_context_report(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("001")
        (ctx_dir / "chunk-001.md").write_text("requirements")

        result = await tdd_orchestration_hook(
            _pre_tool_input(
                ".gravitee/context/chunk-001.tests.md",
                cwd=str(tmp_path),
                agent_type="test-writer",
            ),
            None,
            _CTX,
        )
        assert result == {}


# ===========================================================================
# Developer agent
# ===========================================================================


class TestDeveloperRouting:
    """Developer agent: can write source files, blocked from committed tests."""

    async def test_allowed_to_write_source_with_context(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("001")
        (ctx_dir / "chunk-001.md").write_text("requirements")
        (ctx_dir / "chunk-001.tests.md").write_text("test report")

        result = await tdd_orchestration_hook(
            _pre_tool_input("main.py", cwd=str(tmp_path), agent_type="developer"),
            None,
            _CTX,
        )
        assert result == {}

    async def test_blocked_from_committed_test_file(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        # Set up git repo with tracked test
        subprocess.run(["git", "init"], cwd=tmp_path, capture_output=True)
        subprocess.run(
            ["git", "config", "user.email", "t@t.com"], cwd=tmp_path, capture_output=True
        )
        subprocess.run(["git", "config", "user.name", "T"], cwd=tmp_path, capture_output=True)
        test_file = tmp_path / "test_example.py"
        test_file.write_text("def test_foo(): pass")
        subprocess.run(["git", "add", "test_example.py"], cwd=tmp_path, capture_output=True)
        subprocess.run(["git", "commit", "-m", "add test"], cwd=tmp_path, capture_output=True)

        # Set up context
        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("001")
        (ctx_dir / "chunk-001.md").write_text("requirements")
        (ctx_dir / "chunk-001.tests.md").write_text("test report")

        result = await tdd_orchestration_hook(
            _pre_tool_input("test_example.py", cwd=str(tmp_path), agent_type="developer"),
            None,
            _CTX,
        )
        assert result["decision"] == "block"
        assert "test" in result.get("reason", "").lower()

    async def test_blocked_when_tests_md_missing(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("001")
        (ctx_dir / "chunk-001.md").write_text("requirements")
        # chunk-001.tests.md does NOT exist

        result = await tdd_orchestration_hook(
            _pre_tool_input("main.py", cwd=str(tmp_path), agent_type="developer"),
            None,
            _CTX,
        )
        assert result["decision"] == "block"
        assert "chunk-001.tests.md" in result.get("reason", "")

    async def test_allowed_to_write_context_report(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("001")
        (ctx_dir / "chunk-001.md").write_text("requirements")
        (ctx_dir / "chunk-001.tests.md").write_text("test report")

        result = await tdd_orchestration_hook(
            _pre_tool_input(
                ".gravitee/context/chunk-001.impl.md",
                cwd=str(tmp_path),
                agent_type="developer",
            ),
            None,
            _CTX,
        )
        assert result == {}


# ===========================================================================
# Unknown agent_type (fail open)
# ===========================================================================


class TestUnknownAgentType:
    """Unknown agent_type should be allowed (fail open)."""

    async def test_unknown_agent_type_allowed(self) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        result = await tdd_orchestration_hook(
            _pre_tool_input("src/anything.py", agent_type="some-future-agent"),
            None,
            _CTX,
        )
        assert result == {}


# ===========================================================================
# Block reason quality
# ===========================================================================


class TestBlockReasonQuality:
    """Block reasons should contain actionable messages."""

    async def test_orchestrator_block_mentions_context_dir(self) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        result = await tdd_orchestration_hook(_pre_tool_input("src/main.py"), None, _CTX)
        assert ".gravitee/context/" in result.get("reason", "")

    async def test_test_writer_missing_chunk_mentions_file(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook

        ctx_dir = tmp_path / ".gravitee" / "context"
        ctx_dir.mkdir(parents=True)
        (ctx_dir / ".current-chunk").write_text("002")

        result = await tdd_orchestration_hook(
            _pre_tool_input("test_foo.py", cwd=str(tmp_path), agent_type="test-writer"),
            None,
            _CTX,
        )
        assert "chunk-002.md" in result.get("reason", "")
