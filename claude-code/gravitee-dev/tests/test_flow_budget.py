"""Tests for the deterministic flow pipeline logic."""

from gravitee_dev_cli.cli import _PHASES


class TestFlowPhases:
    """Tests for the phase list and validation."""

    def test_phases_order(self) -> None:
        """Phases run in the expected order."""
        assert _PHASES == ["implement", "docs-sync", "create-pr", "review-loop"]

    def test_all_phases_are_valid_pause_targets(self) -> None:
        """Every phase name is a valid --pause-after target."""
        assert "implement" in _PHASES
        assert "docs-sync" in _PHASES
        assert "create-pr" in _PHASES
        assert "review-loop" in _PHASES
        assert "nonexistent" not in _PHASES
