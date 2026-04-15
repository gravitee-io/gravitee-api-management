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

"""Acceptance tests for the ImplementOrchestratorAgent definition."""

from gravitee_dev.tools.presets import GIT_COMMIT


class TestImplementOrchestratorAgent:
    """Verify ImplementOrchestratorAgent class attributes."""

    def test_name_backward_compat(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent

        assert ImplementOrchestratorAgent.name == "implement"

    def test_sub_agents_include_test_writer(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent

        assert "test-writer" in ImplementOrchestratorAgent.sub_agents

    def test_sub_agents_include_developer(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent

        assert "developer" in ImplementOrchestratorAgent.sub_agents

    def test_sub_agents_include_validate_plan(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent

        assert "validate-plan" in ImplementOrchestratorAgent.sub_agents

    def test_sub_agents_include_safety_check(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent

        assert "safety-check" in ImplementOrchestratorAgent.sub_agents

    def test_git_commit_in_disallowed_tools(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent

        for tool in GIT_COMMIT:
            assert tool in ImplementOrchestratorAgent.disallowed_tools

    def test_model_is_opus(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent

        assert ImplementOrchestratorAgent.model == "opus"

    def test_prompt_version_is_2(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent

        assert ImplementOrchestratorAgent.prompt_version == "2.0.0"

    def test_hooks_include_tdd_orchestration(self) -> None:
        from gravitee_dev.agents.implement import ImplementOrchestratorAgent
        from gravitee_dev.hooks import tdd_orchestration_hook

        pre_tool_matchers = ImplementOrchestratorAgent.hooks.get("PreToolUse", [])
        all_hooks = []
        for matcher in pre_tool_matchers:
            all_hooks.extend(matcher.hooks)
        assert tdd_orchestration_hook in all_hooks

    def test_backward_compat_alias(self) -> None:
        from gravitee_dev.agents.implement import (
            ImplementAgent,
            ImplementOrchestratorAgent,
        )

        assert ImplementAgent is ImplementOrchestratorAgent
