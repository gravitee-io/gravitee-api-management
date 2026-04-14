"""Tests for GraviteeAgent base class."""

import pytest
from claude_agent_sdk import AgentDefinition

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.types import RunConfig

_PROMPT = "You are a test agent."


class TestGraviteeAgentValidation:
    """Tests for __init_subclass__ validation."""

    def test_missing_name_raises(self) -> None:
        """Subclass without name raises TypeError."""
        with pytest.raises(TypeError, match="must define 'name'"):

            class Bad(GraviteeAgent):
                description = "x"
                prompt = "x"
                prompt_version = "1.0.0"

    def test_missing_description_raises(self) -> None:
        """Subclass without description raises TypeError."""
        with pytest.raises(TypeError, match="must define 'description'"):

            class Bad(GraviteeAgent):
                name = "x"
                prompt = "x"
                prompt_version = "1.0.0"

    def test_missing_prompt_raises(self) -> None:
        """Subclass without prompt raises TypeError."""
        with pytest.raises(TypeError, match="must define 'prompt'"):

            class Bad(GraviteeAgent):
                name = "x"
                description = "x"
                prompt_version = "1.0.0"

    def test_missing_prompt_version_raises(self) -> None:
        """Subclass without prompt_version raises TypeError."""
        with pytest.raises(TypeError, match="must define 'prompt_version'"):

            class Bad(GraviteeAgent):
                name = "x"
                description = "x"
                prompt = "x"


class TestGraviteeAgentDefaults:
    """Tests for default values on concrete subclasses."""

    def test_defaults(self) -> None:
        """Default values are applied correctly."""

        class TestAgent(GraviteeAgent):
            name = "test"
            description = "A test agent"
            prompt = _PROMPT
            prompt_version = "1.0.0"

        assert TestAgent.model == "sonnet"
        assert TestAgent.allowed_tools == []
        assert TestAgent.disallowed_tools == []
        assert TestAgent.sub_agents == {}
        assert TestAgent.cli_command is None
        assert TestAgent.max_turns is None
        assert TestAgent.max_budget_usd is None
        assert TestAgent.hooks == {}
        assert TestAgent.layer == "workflow"

    def test_custom_values(self) -> None:
        """Custom values are stored correctly."""

        class CustomAgent(GraviteeAgent):
            name = "commit"
            description = "Commit helper"
            prompt = _PROMPT
            prompt_version = "2.0.0"
            model = "haiku"
            allowed_tools = ["Read", "Bash(git *)"]
            cli_command = "commit"
            max_turns = 10
            max_budget_usd = 1.0

        assert CustomAgent.name == "commit"
        assert CustomAgent.prompt_version == "2.0.0"
        assert CustomAgent.model == "haiku"
        assert CustomAgent.allowed_tools == ["Read", "Bash(git *)"]
        assert CustomAgent.cli_command == "commit"
        assert CustomAgent.max_turns == 10
        assert CustomAgent.max_budget_usd == 1.0

    def test_prompt_stored_verbatim(self) -> None:
        """The prompt string is stored as-is."""
        text = "# My Agent\nDo stuff."

        class TestAgent(GraviteeAgent):
            name = "x"
            description = "x"
            prompt = text
            prompt_version = "1.0.0"

        assert TestAgent.prompt == text


class TestToDefinition:
    """Tests for to_definition()."""

    def test_returns_agent_definition(self) -> None:
        """to_definition returns a valid AgentDefinition."""

        class TestAgent(GraviteeAgent):
            name = "test"
            description = "A test agent"
            prompt = "Be a test agent."
            prompt_version = "1.0.0"
            model = "haiku"
            allowed_tools = ["Read"]

        agent = TestAgent()
        defn = agent.to_definition()

        assert isinstance(defn, AgentDefinition)
        assert defn.description == "A test agent"
        assert defn.prompt == "Be a test agent."
        assert defn.model == "haiku"
        assert defn.tools == ["Read"]

    def test_inherit_model_becomes_none(self) -> None:
        """model='inherit' maps to None in AgentDefinition."""

        class TestAgent(GraviteeAgent):
            name = "test"
            description = "x"
            prompt = "x"
            prompt_version = "1.0.0"
            model = "inherit"

        defn = TestAgent().to_definition()
        assert defn.model is None

    def test_empty_tools_becomes_none(self) -> None:
        """Empty allowed_tools maps to None in AgentDefinition."""

        class TestAgent(GraviteeAgent):
            name = "test"
            description = "x"
            prompt = "x"
            prompt_version = "1.0.0"

        defn = TestAgent().to_definition()
        assert defn.tools is None


class TestCollectSubAgents:
    """Tests for _collect_sub_agents()."""

    def test_no_sub_agents_returns_empty(self) -> None:
        """Agent with no sub_agents returns empty dict."""

        class Leaf(GraviteeAgent):
            name = "leaf"
            description = "leaf"
            prompt = "leaf"
            prompt_version = "1.0.0"

        result = Leaf()._collect_sub_agents()
        assert result == {}

    def test_single_sub_agent(self) -> None:
        """Agent with one sub-agent resolves it."""

        class Child(GraviteeAgent):
            name = "child"
            description = "child desc"
            prompt = "child prompt"
            prompt_version = "1.0.0"

        class Parent(GraviteeAgent):
            name = "parent"
            description = "parent"
            prompt = "parent"
            prompt_version = "1.0.0"
            sub_agents = {"child": Child()}

        result = Parent()._collect_sub_agents()
        assert "child" in result
        assert isinstance(result["child"], AgentDefinition)
        assert result["child"].description == "child desc"

    def test_nested_sub_agents_flattened(self) -> None:
        """Nested sub-agents are flattened recursively."""

        class Leaf(GraviteeAgent):
            name = "leaf"
            description = "leaf"
            prompt = "leaf"
            prompt_version = "1.0.0"

        class Mid(GraviteeAgent):
            name = "mid"
            description = "mid"
            prompt = "mid"
            prompt_version = "1.0.0"
            sub_agents = {"leaf": Leaf()}

        class Root(GraviteeAgent):
            name = "root"
            description = "root"
            prompt = "root"
            prompt_version = "1.0.0"
            sub_agents = {"mid": Mid()}

        result = Root()._collect_sub_agents()
        assert "mid" in result
        assert "leaf" in result

    def test_deduplication(self) -> None:
        """Same sub-agent referenced by multiple parents appears once."""

        class Shared(GraviteeAgent):
            name = "shared"
            description = "shared"
            prompt = "shared"
            prompt_version = "1.0.0"

        _shared = Shared()

        class A(GraviteeAgent):
            name = "a"
            description = "a"
            prompt = "a"
            prompt_version = "1.0.0"
            sub_agents = {"shared": _shared}

        class B(GraviteeAgent):
            name = "b"
            description = "b"
            prompt = "b"
            prompt_version = "1.0.0"
            sub_agents = {"shared": _shared}

        class Root(GraviteeAgent):
            name = "root"
            description = "root"
            prompt = "root"
            prompt_version = "1.0.0"
            sub_agents = {"a": A(), "b": B()}

        result = Root()._collect_sub_agents()
        assert "shared" in result
        assert "a" in result
        assert "b" in result


class TestBuildOptions:
    """Tests for _build_options()."""

    def test_basic_options(self) -> None:
        """_build_options produces valid ClaudeAgentOptions."""

        class TestAgent(GraviteeAgent):
            name = "test"
            description = "test"
            prompt = "test prompt"
            prompt_version = "1.0.0"
            model = "haiku"
            allowed_tools = ["Read"]
            max_turns = 5

        agent = TestAgent()
        config = RunConfig()
        options = agent._build_options(config)

        assert options.model == "haiku"
        assert options.allowed_tools == ["Read"]
        assert options.max_turns == 5

    def test_config_overrides_model(self) -> None:
        """RunConfig.model overrides agent default."""

        class TestAgent(GraviteeAgent):
            name = "test"
            description = "test"
            prompt = "test"
            prompt_version = "1.0.0"
            model = "sonnet"

        config = RunConfig(model="opus")
        options = TestAgent()._build_options(config)
        assert options.model == "opus"


class TestRegisteredAgents:
    """Tests for all registered agents in the registry."""

    def test_all_registered_agents_have_prompts(self) -> None:
        """Every registered agent has a non-empty prompt."""
        from gravitee_dev.agents.registry import ALL_AGENTS

        for name, cls in ALL_AGENTS.items():
            assert len(cls.prompt) > 0, f"Prompt for agent '{name}' is empty"
            assert cls.prompt.strip(), f"Prompt for agent '{name}' is blank"

    def test_all_registered_agents_have_prompt_version(self) -> None:
        """Every registered agent has a non-empty prompt_version."""
        from gravitee_dev.agents.registry import ALL_AGENTS

        for name, cls in ALL_AGENTS.items():
            assert cls.prompt_version, f"prompt_version for agent '{name}' is empty"
