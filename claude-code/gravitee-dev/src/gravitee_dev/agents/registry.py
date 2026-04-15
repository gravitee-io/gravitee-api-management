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

"""Agent registry — ALL_AGENTS and CLI_AGENTS."""

from gravitee_dev.agents._base import GraviteeAgent
from gravitee_dev.agents.commit import CommitAgent
from gravitee_dev.agents.create_pr import CreatePrAgent
from gravitee_dev.agents.developer import DeveloperAgent
from gravitee_dev.agents.docs_sync import DocsSyncAgent
from gravitee_dev.agents.implement import ImplementAgent
from gravitee_dev.agents.pr_review import PrReviewAgent
from gravitee_dev.agents.safety_check import SafetyCheckAgent
from gravitee_dev.agents.test_writer import TestWriterAgent
from gravitee_dev.agents.validate_plan import ValidatePlanAgent

ALL_AGENTS: dict[str, type[GraviteeAgent]] = {
    cls.name: cls
    for cls in [
        # Sub-agents
        CommitAgent,
        DeveloperAgent,
        SafetyCheckAgent,
        TestWriterAgent,
        ValidatePlanAgent,
        # Workflow agents
        ImplementAgent,
        CreatePrAgent,
        DocsSyncAgent,
        PrReviewAgent,
    ]
}

CLI_AGENTS: dict[str, type[GraviteeAgent]] = {
    cls.cli_command: cls for cls in ALL_AGENTS.values() if cls.cli_command is not None
}
