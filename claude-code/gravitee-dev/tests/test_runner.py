"""Tests for sub-agent resolution via _collect_sub_agents()."""

from claude_agent_sdk import AgentDefinition


class TestCollectSubAgentsWithRealRegistry:
    """Tests using the real agent hierarchy."""

    def test_implement_resolves_sub_agents(self) -> None:
        """ImplementAgent resolves all four sub-agents."""
        from gravitee_dev.agents.implement import ImplementAgent

        result = ImplementAgent()._collect_sub_agents()
        assert "validate-plan" in result
        assert "safety-check" in result
        assert "test-writer" in result
        assert "developer" in result
        assert len(result) == 4

    def test_leaf_agent_has_no_sub_agents(self) -> None:
        """Leaf agents (e.g. CommitAgent) resolve no sub-agents."""
        from gravitee_dev.agents.commit import CommitAgent

        result = CommitAgent()._collect_sub_agents()
        assert result == {}

    def test_to_definition_produces_valid_definitions(self) -> None:
        """to_definition() produces valid AgentDefinition for all agents."""
        from gravitee_dev.agents.registry import ALL_AGENTS

        for name, cls in ALL_AGENTS.items():
            agent = cls()
            defn = agent.to_definition()
            assert isinstance(defn, AgentDefinition), f"{name}.to_definition() failed"
            assert defn.description == cls.description
            assert defn.prompt == cls.prompt
