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

"""Acceptance tests for the DeveloperAgent definition."""

from gravitee_dev.tools.presets import BUILD_TOOLS, FILE_WRITE, GIT_COMMIT


class TestDeveloperAgent:
    """Verify DeveloperAgent class attributes."""

    def test_name(self) -> None:
        from gravitee_dev.agents.developer import DeveloperAgent

        assert DeveloperAgent.name == "developer"

    def test_layer(self) -> None:
        from gravitee_dev.agents.developer import DeveloperAgent

        assert DeveloperAgent.layer == "sub"

    def test_model(self) -> None:
        from gravitee_dev.agents.developer import DeveloperAgent

        assert DeveloperAgent.model == "sonnet"

    def test_allowed_tools_include_file_write(self) -> None:
        from gravitee_dev.agents.developer import DeveloperAgent

        for tool in FILE_WRITE:
            assert tool in DeveloperAgent.allowed_tools

    def test_allowed_tools_include_build_tools(self) -> None:
        from gravitee_dev.agents.developer import DeveloperAgent

        for tool in BUILD_TOOLS:
            assert tool in DeveloperAgent.allowed_tools

    def test_allowed_tools_include_git_commit(self) -> None:
        from gravitee_dev.agents.developer import DeveloperAgent

        for tool in GIT_COMMIT:
            assert tool in DeveloperAgent.allowed_tools

    def test_max_turns(self) -> None:
        from gravitee_dev.agents.developer import DeveloperAgent

        assert DeveloperAgent.max_turns == 30
