"""Tests for the secret scan hook."""

from unittest.mock import patch

from claude_agent_sdk.types import HookContext, PreToolUseHookInput

_CTX = HookContext(signal=None)


def _write_input(content: str, cwd: str = "/project") -> PreToolUseHookInput:
    """Build a PreToolUseHookInput with content in tool_input."""
    return PreToolUseHookInput(
        session_id="test-session",
        transcript_path="/tmp/transcript",
        cwd=cwd,
        hook_event_name="PreToolUse",
        tool_name="Write",
        tool_input={"file_path": "/project/config.py", "content": content},
        tool_use_id="tool-1",
    )


def _clean_input(cwd: str = "/project") -> PreToolUseHookInput:
    """Build a clean PreToolUseHookInput with no secrets."""
    return _write_input("x = 42\n", cwd=cwd)


class TestSecretScanHook:
    """Tests for _secret_scan.secret_scan_hook."""

    async def test_clean_content_passes(self) -> None:
        from gravitee_dev.hooks._secret_scan import secret_scan_hook

        with patch(
            "gravitee_dev.hooks._secret_scan._run_gitleaks",
            return_value=(False, "gitleaks-not-found"),
        ):
            result = await secret_scan_hook(_clean_input(), None, _CTX)
        assert result == {}

    async def test_aws_key_blocks(self) -> None:
        from gravitee_dev.hooks._secret_scan import secret_scan_hook

        with patch(
            "gravitee_dev.hooks._secret_scan._run_gitleaks",
            return_value=(False, "gitleaks-not-found"),
        ):
            result = await secret_scan_hook(
                _write_input("key = 'AKIAIOSFODNN7EXAMPLE'"), None, _CTX
            )
        assert result["decision"] == "block"
        assert "AWS" in result.get("reason", "")

    async def test_github_token_blocks(self) -> None:
        from gravitee_dev.hooks._secret_scan import secret_scan_hook

        token = "ghp_" + "A" * 36
        with patch(
            "gravitee_dev.hooks._secret_scan._run_gitleaks",
            return_value=(False, "gitleaks-not-found"),
        ):
            result = await secret_scan_hook(_write_input(f"TOKEN = '{token}'"), None, _CTX)
        assert result["decision"] == "block"
        assert "GitHub" in result.get("reason", "")

    async def test_private_key_header_blocks(self) -> None:
        from gravitee_dev.hooks._secret_scan import secret_scan_hook

        with patch(
            "gravitee_dev.hooks._secret_scan._run_gitleaks",
            return_value=(False, "gitleaks-not-found"),
        ):
            result = await secret_scan_hook(
                _write_input("-----BEGIN RSA PRIVATE KEY-----\nMIIE..."), None, _CTX
            )
        assert result["decision"] == "block"
        assert "Private Key" in result.get("reason", "")

    async def test_gitleaks_detection_blocks(self) -> None:
        from gravitee_dev.hooks._secret_scan import secret_scan_hook

        with patch(
            "gravitee_dev.hooks._secret_scan._run_gitleaks",
            return_value=(True, "Secret detected: aws-access-key"),
        ):
            result = await secret_scan_hook(_clean_input(), None, _CTX)
        assert result["decision"] == "block"

    async def test_gitleaks_fallback_to_regex(self) -> None:
        from gravitee_dev.hooks._secret_scan import secret_scan_hook

        with patch(
            "gravitee_dev.hooks._secret_scan._run_gitleaks",
            return_value=(False, "gitleaks-not-found"),
        ):
            result = await secret_scan_hook(
                _write_input("key = 'AKIAIOSFODNN7EXAMPLE'"), None, _CTX
            )
        assert result["decision"] == "block"

    async def test_timeout_fails_closed(self) -> None:
        from gravitee_dev.hooks._secret_scan import secret_scan_hook

        with patch(
            "gravitee_dev.hooks._secret_scan._run_gitleaks",
            return_value=(True, "gitleaks timed out"),
        ):
            result = await secret_scan_hook(_clean_input(), None, _CTX)
        assert result["decision"] == "block"

    async def test_skip_bypass(self) -> None:
        from gravitee_dev.hooks._secret_scan import secret_scan_hook

        with patch.dict("os.environ", {"SKIP_SECRET_SCAN": "1"}):
            result = await secret_scan_hook(
                _write_input("key = 'AKIAIOSFODNN7EXAMPLE'"), None, _CTX
            )
        assert result == {}
