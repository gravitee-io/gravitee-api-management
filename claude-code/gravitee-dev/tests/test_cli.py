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

"""CLI smoke tests."""

from typer.testing import CliRunner

from gravitee_dev_cli import app

runner = CliRunner()


class TestCliSmoke:
    """Smoke tests for CLI commands."""

    def test_no_args_shows_help(self) -> None:
        """Running with no args shows help/usage text (exit code 0 or 2)."""
        result = runner.invoke(app, [])
        assert result.exit_code in (0, 2)
        assert "Usage" in result.output or "usage" in result.output.lower()

    def test_help_flag(self) -> None:
        """--help shows usage info."""
        result = runner.invoke(app, ["--help"])
        assert result.exit_code == 0
        assert "Usage" in result.output

    def test_flow_help(self) -> None:
        """flow --help shows command help."""
        result = runner.invoke(app, ["flow", "--help"])
        assert result.exit_code == 0
        assert "Orchestrate" in result.output

    def test_all_commands_have_help(self) -> None:
        """Every CLI command has help text."""
        commands = [
            "commit",
            "implement",
            "create-pr",
            "docs-sync",
            "review-loop",
            "flow",
            "cleanup",
            "worktrees",
        ]
        for cmd in commands:
            result = runner.invoke(app, [cmd, "--help"])
            assert result.exit_code == 0, f"Command '{cmd}' --help failed"

    def test_flow_pause_after_flag(self) -> None:
        """flow --pause-after appears in help."""
        result = runner.invoke(app, ["flow", "--help"])
        assert "--pause-after" in result.output

    def test_flow_skip_review_flag(self) -> None:
        """flow --skip-review appears in help."""
        result = runner.invoke(app, ["flow", "--help"])
        assert "--skip-review" in result.output

    def test_flow_review_interval_flag(self) -> None:
        """flow --review-interval appears in help."""
        result = runner.invoke(app, ["flow", "--help"])
        assert "--review-interval" in result.output

    def test_flow_request_reviewer_flag(self) -> None:
        """flow --request-reviewer appears in help."""
        result = runner.invoke(app, ["flow", "--help"])
        assert "--request-reviewer" in result.output

    def test_review_loop_help(self) -> None:
        """review-loop --help shows command help."""
        result = runner.invoke(app, ["review-loop", "--help"])
        assert result.exit_code == 0
        assert "review" in result.output.lower()
