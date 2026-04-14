"""Tests for the hooks submodule."""

import subprocess
from pathlib import Path
from unittest.mock import AsyncMock, patch

from claude_agent_sdk.types import (
    HookContext,
    PreToolUseHookInput,
    StopHookInput,
    UserPromptSubmitHookInput,
)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _pre_tool_input(file_path: str, cwd: str = "/project") -> PreToolUseHookInput:
    """Build a minimal PreToolUseHookInput for testing."""
    return PreToolUseHookInput(
        session_id="test-session",
        transcript_path="/tmp/transcript",
        cwd=cwd,
        hook_event_name="PreToolUse",
        tool_name="Edit",
        tool_input={"file_path": file_path},
        tool_use_id="tool-1",
    )


def _stop_input(
    cwd: str = "/project",
    stop_hook_active: bool = False,
) -> StopHookInput:
    """Build a minimal StopHookInput for testing."""
    return StopHookInput(
        session_id="test-session",
        transcript_path="/tmp/transcript",
        cwd=cwd,
        hook_event_name="Stop",
        stop_hook_active=stop_hook_active,
    )


_CTX = HookContext(signal=None)


def _user_prompt_input(cwd: str = "/project") -> UserPromptSubmitHookInput:
    """Build a minimal UserPromptSubmitHookInput for testing."""
    return UserPromptSubmitHookInput(
        session_id="test-session",
        transcript_path="/tmp/transcript",
        cwd=cwd,
        hook_event_name="UserPromptSubmit",
        prompt="do the thing",
    )


# ===========================================================================
# protect_files_hook
# ===========================================================================


class TestProtectFilesHook:
    """Tests for _protect_files.protect_files_hook."""

    async def test_blocks_env_file(self) -> None:
        from gravitee_dev.hooks._protect_files import protect_files_hook

        result = await protect_files_hook(_pre_tool_input("/project/.env"), None, _CTX)
        assert result["decision"] == "block"

    async def test_blocks_pem_file(self) -> None:
        from gravitee_dev.hooks._protect_files import protect_files_hook

        result = await protect_files_hook(_pre_tool_input("/project/certs/server.pem"), None, _CTX)
        assert result["decision"] == "block"

    async def test_blocks_node_modules(self) -> None:
        from gravitee_dev.hooks._protect_files import protect_files_hook

        result = await protect_files_hook(
            _pre_tool_input("/project/node_modules/foo/index.js"), None, _CTX
        )
        assert result["decision"] == "block"

    async def test_blocks_git_directory(self) -> None:
        from gravitee_dev.hooks._protect_files import protect_files_hook

        result = await protect_files_hook(_pre_tool_input("/project/.git/config"), None, _CTX)
        assert result["decision"] == "block"

    async def test_allows_normal_source_file(self) -> None:
        from gravitee_dev.hooks._protect_files import protect_files_hook

        result = await protect_files_hook(_pre_tool_input("/project/src/main.py"), None, _CTX)
        assert result == {}

    async def test_allows_when_no_file_path(self) -> None:
        from gravitee_dev.hooks._protect_files import protect_files_hook

        inp = PreToolUseHookInput(
            session_id="s",
            transcript_path="/tmp/t",
            cwd="/project",
            hook_event_name="PreToolUse",
            tool_name="Edit",
            tool_input={},
            tool_use_id="t1",
        )
        result = await protect_files_hook(inp, None, _CTX)
        assert result == {}

    async def test_custom_patterns(self) -> None:
        from gravitee_dev.hooks._protect_files import protect_files_hook

        result = await protect_files_hook(
            _pre_tool_input("/project/custom_secret.txt"),
            None,
            _CTX,
            patterns=["custom_secret"],
        )
        assert result["decision"] == "block"

    async def test_blocks_lock_file(self) -> None:
        from gravitee_dev.hooks._protect_files import protect_files_hook

        result = await protect_files_hook(
            _pre_tool_input("/project/package-lock.json"), None, _CTX
        )
        assert result["decision"] == "block"


# ===========================================================================
# guard_tests_hook
# ===========================================================================


class TestGuardTestsHook:
    """Tests for _guard_tests.guard_tests_hook."""

    async def test_allows_non_test_file(self) -> None:
        from gravitee_dev.hooks._guard_tests import guard_tests_hook

        result = await guard_tests_hook(_pre_tool_input("/project/src/main.py"), None, _CTX)
        assert result == {}

    async def test_blocks_tracked_test_file(self, tmp_path: Path) -> None:
        """A tracked test file should be blocked."""
        from gravitee_dev.hooks._guard_tests import guard_tests_hook

        # Set up a git repo with a tracked test file
        subprocess.run(["git", "init"], cwd=tmp_path, capture_output=True)
        subprocess.run(
            ["git", "config", "user.email", "test@test.com"],
            cwd=tmp_path,
            capture_output=True,
        )
        subprocess.run(
            ["git", "config", "user.name", "Test"],
            cwd=tmp_path,
            capture_output=True,
        )
        test_file = tmp_path / "test_example.py"
        test_file.write_text("def test_foo(): pass")
        subprocess.run(["git", "add", "test_example.py"], cwd=tmp_path, capture_output=True)
        subprocess.run(
            ["git", "commit", "-m", "add test"],
            cwd=tmp_path,
            capture_output=True,
        )

        result = await guard_tests_hook(
            _pre_tool_input("test_example.py", cwd=str(tmp_path)),
            None,
            _CTX,
        )
        assert result["decision"] == "block"
        assert "TDD" in result.get("reason", "")

    async def test_allows_untracked_test_file(self, tmp_path: Path) -> None:
        """Untracked test files (RED phase) should be allowed."""
        from gravitee_dev.hooks._guard_tests import guard_tests_hook

        subprocess.run(["git", "init"], cwd=tmp_path, capture_output=True)
        # File exists but is NOT tracked
        test_file = tmp_path / "test_new.py"
        test_file.write_text("def test_bar(): pass")

        result = await guard_tests_hook(
            _pre_tool_input("test_new.py", cwd=str(tmp_path)),
            None,
            _CTX,
        )
        assert result == {}

    async def test_detects_spec_file_pattern(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._guard_tests import guard_tests_hook

        subprocess.run(["git", "init"], cwd=tmp_path, capture_output=True)
        subprocess.run(
            ["git", "config", "user.email", "test@test.com"],
            cwd=tmp_path,
            capture_output=True,
        )
        subprocess.run(
            ["git", "config", "user.name", "Test"],
            cwd=tmp_path,
            capture_output=True,
        )
        spec_file = tmp_path / "app.spec.ts"
        spec_file.write_text("it('works', () => {})")
        subprocess.run(["git", "add", "app.spec.ts"], cwd=tmp_path, capture_output=True)
        subprocess.run(
            ["git", "commit", "-m", "add spec"],
            cwd=tmp_path,
            capture_output=True,
        )

        result = await guard_tests_hook(
            _pre_tool_input("app.spec.ts", cwd=str(tmp_path)),
            None,
            _CTX,
        )
        assert result["decision"] == "block"

    async def test_detects_java_test_pattern(self, tmp_path: Path) -> None:
        from gravitee_dev.hooks._guard_tests import guard_tests_hook

        subprocess.run(["git", "init"], cwd=tmp_path, capture_output=True)
        subprocess.run(
            ["git", "config", "user.email", "test@test.com"],
            cwd=tmp_path,
            capture_output=True,
        )
        subprocess.run(
            ["git", "config", "user.name", "Test"],
            cwd=tmp_path,
            capture_output=True,
        )
        java_test = tmp_path / "FooTest.java"
        java_test.write_text("class FooTest {}")
        subprocess.run(["git", "add", "FooTest.java"], cwd=tmp_path, capture_output=True)
        subprocess.run(
            ["git", "commit", "-m", "add java test"],
            cwd=tmp_path,
            capture_output=True,
        )

        result = await guard_tests_hook(
            _pre_tool_input("FooTest.java", cwd=str(tmp_path)),
            None,
            _CTX,
        )
        assert result["decision"] == "block"

    async def test_allows_when_no_file_path(self) -> None:
        from gravitee_dev.hooks._guard_tests import guard_tests_hook

        inp = PreToolUseHookInput(
            session_id="s",
            transcript_path="/tmp/t",
            cwd="/project",
            hook_event_name="PreToolUse",
            tool_name="Edit",
            tool_input={},
            tool_use_id="t1",
        )
        result = await guard_tests_hook(inp, None, _CTX)
        assert result == {}


# ===========================================================================
# check_stop_hook
# ===========================================================================


class TestCheckStopHook:
    """Tests for _check.check_stop_hook."""

    async def test_reentry_commits_on_build_success(self) -> None:
        """On re-entry with passing build, changes are still committed."""
        from gravitee_dev.hooks._check import check_stop_hook
        from gravitee_dev.types import AgentResult

        mock_run = AsyncMock(return_value=AgentResult(agent_name="commit", result_text="ok"))
        with (
            patch(
                "gravitee_dev.hooks._check._run",
                side_effect=[
                    ("M src/main.py", 0),  # git status
                    ("OK", 0),  # check command passes
                ],
            ),
            patch(
                "gravitee_dev.agents.commit.CommitAgent.run",
                mock_run,
            ),
        ):
            result = await check_stop_hook(_stop_input(stop_hook_active=True), None, _CTX)

        assert result == {}
        mock_run.assert_awaited_once()

    async def test_reentry_allows_stop_on_build_failure(self) -> None:
        """On re-entry with failing build, stop is allowed (no infinite loop)."""
        from gravitee_dev.hooks._check import check_stop_hook

        with patch(
            "gravitee_dev.hooks._check._run",
            side_effect=[
                ("M src/main.py", 0),  # git status
                ("FAIL: test_foo", 1),  # check command fails
            ],
        ):
            result = await check_stop_hook(_stop_input(stop_hook_active=True), None, _CTX)

        assert result == {}  # allowed, not blocked

    async def test_reentry_allows_stop_when_no_changes(self) -> None:
        """On re-entry with no changes, stop is allowed immediately."""
        from gravitee_dev.hooks._check import check_stop_hook

        with patch(
            "gravitee_dev.hooks._check._run",
            return_value=("", 0),
        ):
            result = await check_stop_hook(_stop_input(stop_hook_active=True), None, _CTX)
        assert result == {}

    async def test_allows_stop_when_no_changes(self) -> None:
        from gravitee_dev.hooks._check import check_stop_hook

        with patch(
            "gravitee_dev.hooks._check._run",
            return_value=("", 0),
        ):
            result = await check_stop_hook(_stop_input(), None, _CTX)
        assert result == {}

    async def test_blocks_stop_on_build_failure(self) -> None:
        from gravitee_dev.hooks._check import check_stop_hook

        with patch(
            "gravitee_dev.hooks._check._run",
            side_effect=[
                ("M src/main.py", 0),  # git status
                ("FAIL: test_foo", 1),  # check command
            ],
        ):
            result = await check_stop_hook(_stop_input(), None, _CTX)
        assert result["decision"] == "block"
        assert "FAIL" in result.get("reason", "")

    async def test_commits_on_build_success(self) -> None:
        from gravitee_dev.hooks._check import check_stop_hook
        from gravitee_dev.types import AgentResult

        mock_run = AsyncMock(return_value=AgentResult(agent_name="commit", result_text="ok"))
        with (
            patch(
                "gravitee_dev.hooks._check._run",
                side_effect=[
                    ("M src/main.py", 0),  # git status
                    ("OK", 0),  # check command
                ],
            ),
            patch(
                "gravitee_dev.agents.commit.CommitAgent.run",
                mock_run,
            ),
        ):
            result = await check_stop_hook(_stop_input(), None, _CTX)

        assert result == {}
        mock_run.assert_awaited_once()

    async def test_custom_check_command(self) -> None:
        from gravitee_dev.hooks._check import check_stop_hook
        from gravitee_dev.types import AgentResult

        calls: list[str] = []

        def fake_run(cmd: str, cwd: str, timeout: int = 120) -> tuple[str, int]:
            calls.append(cmd)
            if "status" in cmd:
                return "M file.py", 0
            return "OK", 0

        mock_run = AsyncMock(return_value=AgentResult(agent_name="commit", result_text="ok"))
        with (
            patch("gravitee_dev.hooks._check._run", side_effect=fake_run),
            patch("gravitee_dev.agents.commit.CommitAgent.run", mock_run),
        ):
            await check_stop_hook(
                _stop_input(),
                None,
                _CTX,
                check_command="make ci",
            )

        assert "make ci" in calls


# ===========================================================================
# clean_working_tree_hook
# ===========================================================================


class TestCleanWorkingTreeHook:
    """Tests for _safety_check.clean_working_tree_hook."""

    async def test_allows_when_tree_is_clean(self) -> None:
        from gravitee_dev.hooks._safety_check import clean_working_tree_hook

        with patch(
            "gravitee_dev.hooks._safety_check.subprocess.run",
            return_value=type("R", (), {"stdout": "", "returncode": 0})(),
        ):
            result = await clean_working_tree_hook(_user_prompt_input(), None, _CTX)
        assert result == {}

    async def test_blocks_when_tree_is_dirty(self) -> None:
        from gravitee_dev.hooks._safety_check import clean_working_tree_hook

        with patch(
            "gravitee_dev.hooks._safety_check.subprocess.run",
            return_value=type(
                "R", (), {"stdout": " M src/main.py\n?? scratch.txt\n", "returncode": 0}
            )(),
        ):
            result = await clean_working_tree_hook(_user_prompt_input(), None, _CTX)
        assert result["decision"] == "block"
        assert "src/main.py" in result.get("reason", "")
        assert "scratch.txt" in result.get("reason", "")

    async def test_fails_closed_on_subprocess_error(self) -> None:
        from gravitee_dev.hooks._safety_check import clean_working_tree_hook

        with patch(
            "gravitee_dev.hooks._safety_check.subprocess.run",
            side_effect=OSError("git not found"),
        ):
            result = await clean_working_tree_hook(_user_prompt_input(), None, _CTX)
        assert result["decision"] == "block"

    async def test_fails_closed_on_timeout(self) -> None:
        from gravitee_dev.hooks._safety_check import clean_working_tree_hook

        with patch(
            "gravitee_dev.hooks._safety_check.subprocess.run",
            side_effect=subprocess.TimeoutExpired("git", 10),
        ):
            result = await clean_working_tree_hook(_user_prompt_input(), None, _CTX)
        assert result["decision"] == "block"

    async def test_uses_real_git(self, tmp_path: Path) -> None:
        """Integration: clean repo allows, dirty repo blocks."""
        from gravitee_dev.hooks._safety_check import clean_working_tree_hook

        subprocess.run(["git", "init"], cwd=tmp_path, capture_output=True)
        subprocess.run(
            ["git", "config", "user.email", "t@t.com"], cwd=tmp_path, capture_output=True
        )
        subprocess.run(["git", "config", "user.name", "T"], cwd=tmp_path, capture_output=True)

        # Clean repo — allow
        result = await clean_working_tree_hook(_user_prompt_input(cwd=str(tmp_path)), None, _CTX)
        assert result == {}

        # Write an untracked file — dirty
        (tmp_path / "work.py").write_text("x = 1")
        result = await clean_working_tree_hook(_user_prompt_input(cwd=str(tmp_path)), None, _CTX)
        assert result["decision"] == "block"
        assert "work.py" in result.get("reason", "")


# ===========================================================================
# gh_auth_hook
# ===========================================================================


class TestGhAuthHook:
    """Tests for _gh_auth.gh_auth_hook."""

    async def test_allows_on_zero_exit(self) -> None:
        from gravitee_dev.hooks._gh_auth import gh_auth_hook

        with patch(
            "gravitee_dev.hooks._gh_auth.subprocess.run",
            return_value=type("R", (), {"returncode": 0, "stdout": "", "stderr": ""})(),
        ):
            result = await gh_auth_hook(_pre_tool_input("/project/src/main.py"), None, _CTX)
        assert result == {}

    async def test_blocks_on_nonzero_exit(self) -> None:
        from gravitee_dev.hooks._gh_auth import gh_auth_hook

        with patch(
            "gravitee_dev.hooks._gh_auth.subprocess.run",
            return_value=type(
                "R", (), {"returncode": 1, "stdout": "", "stderr": "not logged in"}
            )(),
        ):
            result = await gh_auth_hook(_pre_tool_input("/project/src/main.py"), None, _CTX)
        assert result["decision"] == "block"
        assert "not authenticated" in result.get("reason", "").lower()

    async def test_blocks_on_exception(self) -> None:
        from gravitee_dev.hooks._gh_auth import gh_auth_hook

        with patch(
            "gravitee_dev.hooks._gh_auth.subprocess.run",
            side_effect=OSError("gh not found"),
        ):
            result = await gh_auth_hook(_pre_tool_input("/project/src/main.py"), None, _CTX)
        assert result["decision"] == "block"


# ===========================================================================
# Public API re-exports
# ===========================================================================


class TestHookReExports:
    """Verify all hook functions are importable from the package root."""

    def test_hook_re_exports(self) -> None:
        from gravitee_dev.hooks import (
            check_stop_hook,
            clean_working_tree_hook,
            gh_auth_hook,
            guard_tests_hook,
            protect_files_hook,
            secret_scan_hook,
            tdd_orchestration_hook,
        )

        assert callable(check_stop_hook)
        assert callable(clean_working_tree_hook)
        assert callable(gh_auth_hook)
        assert callable(guard_tests_hook)
        assert callable(protect_files_hook)
        assert callable(secret_scan_hook)
        assert callable(tdd_orchestration_hook)
