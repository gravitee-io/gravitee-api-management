"""Tests for tool permission presets."""

from gravitee_dev.tools.presets import (
    BUILD_TOOLS,
    FILE_WRITE,
    GH_ISSUE,
    GH_PR,
    GIT_BRANCH,
    GIT_COMMIT,
    GIT_PUSH,
    GIT_REBASE,
    GIT_STATUS,
    NEVER_ALLOW,
    READ_ONLY,
    TASK_TOOL,
)


class TestPresetComposition:
    """Tests for composing tool presets."""

    def test_git_commit_includes_status(self) -> None:
        """GIT_COMMIT is a superset of GIT_STATUS."""
        for tool in GIT_STATUS:
            assert tool in GIT_COMMIT

    def test_git_branch_includes_status(self) -> None:
        """GIT_BRANCH is a superset of GIT_STATUS."""
        for tool in GIT_STATUS:
            assert tool in GIT_BRANCH

    def test_git_commit_has_add_and_commit(self) -> None:
        """GIT_COMMIT includes add and commit commands."""
        tools_str = " ".join(GIT_COMMIT)
        assert "git add" in tools_str
        assert "git commit" in tools_str

    def test_never_allow_blocks_destructive(self) -> None:
        """NEVER_ALLOW blocks destructive commands."""
        tools_str = " ".join(NEVER_ALLOW)
        assert "rm -rf" in tools_str
        assert "sudo" in tools_str
        assert "curl" in tools_str
        assert "wget" in tools_str

    def test_never_allow_expanded_patterns(self) -> None:
        """NEVER_ALLOW has the expanded security perimeter (19+ entries)."""
        assert len(NEVER_ALLOW) >= 19
        tools_str = " ".join(NEVER_ALLOW)
        assert "python -c" in tools_str
        assert "node -e" in tools_str
        assert "bash -c" in tools_str
        assert "sh -c" in tools_str
        assert "eval" in tools_str
        assert "chmod +x" in tools_str
        assert "pip install" in tools_str
        assert "npm install -g" in tools_str
        assert "git filter-branch" in tools_str
        assert "git push --force" in tools_str
        assert "env" in tools_str
        assert "export" in tools_str

    def test_build_tools_preset_exists(self) -> None:
        """BUILD_TOOLS preset exists and contains expected tools."""
        assert isinstance(BUILD_TOOLS, list)
        assert len(BUILD_TOOLS) > 0
        tools_str = " ".join(BUILD_TOOLS)
        assert "uv run" in tools_str
        assert "pytest" in tools_str
        assert "ruff check" in tools_str
        assert "mvn" in tools_str
        assert "task" in tools_str
        assert "cargo build" in tools_str

    def test_gh_pr_includes_auth_status(self) -> None:
        """GH_PR includes gh auth status."""
        assert "Bash(gh auth status)" in GH_PR

    def test_presets_are_lists(self) -> None:
        """All presets are plain lists."""
        for preset in [
            GIT_STATUS,
            GIT_COMMIT,
            GIT_BRANCH,
            GIT_PUSH,
            GIT_REBASE,
            GH_ISSUE,
            GH_PR,
            READ_ONLY,
            FILE_WRITE,
            TASK_TOOL,
            NEVER_ALLOW,
            BUILD_TOOLS,
        ]:
            assert isinstance(preset, list)
            assert all(isinstance(t, str) for t in preset)

    def test_composition_via_unpacking(self) -> None:
        """Presets can be composed via unpacking."""
        combined = [*GIT_COMMIT, *READ_ONLY, *GH_PR]
        # Should contain elements from all three
        assert "Read" in combined
        assert "Bash(git commit *)" in combined
        assert "Bash(gh pr *)" in combined

    def test_no_overlap_between_read_and_write(self) -> None:
        """READ_ONLY and FILE_WRITE have no overlap."""
        assert not set(READ_ONLY) & set(FILE_WRITE)
