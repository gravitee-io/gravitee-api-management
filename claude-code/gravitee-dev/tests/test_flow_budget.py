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
