"""Acceptance tests for the TestWriterAgent definition."""

from gravitee_dev.tools.presets import BUILD_TOOLS, FILE_WRITE, GIT_COMMIT


class TestTestWriterAgent:
    """Verify TestWriterAgent class attributes."""

    def test_name(self) -> None:
        from gravitee_dev.agents.test_writer import TestWriterAgent

        assert TestWriterAgent.name == "test-writer"

    def test_layer(self) -> None:
        from gravitee_dev.agents.test_writer import TestWriterAgent

        assert TestWriterAgent.layer == "sub"

    def test_model(self) -> None:
        from gravitee_dev.agents.test_writer import TestWriterAgent

        assert TestWriterAgent.model == "sonnet"

    def test_allowed_tools_include_file_write(self) -> None:
        from gravitee_dev.agents.test_writer import TestWriterAgent

        for tool in FILE_WRITE:
            assert tool in TestWriterAgent.allowed_tools

    def test_allowed_tools_include_build_tools(self) -> None:
        from gravitee_dev.agents.test_writer import TestWriterAgent

        for tool in BUILD_TOOLS:
            assert tool in TestWriterAgent.allowed_tools

    def test_allowed_tools_include_git_commit(self) -> None:
        from gravitee_dev.agents.test_writer import TestWriterAgent

        for tool in GIT_COMMIT:
            assert tool in TestWriterAgent.allowed_tools

    def test_max_turns(self) -> None:
        from gravitee_dev.agents.test_writer import TestWriterAgent

        assert TestWriterAgent.max_turns == 20
