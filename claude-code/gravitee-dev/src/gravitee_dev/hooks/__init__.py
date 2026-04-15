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

"""Programmatic hooks for Claude Agent SDK integration.

Public API
----------
Individual hook functions — compose these directly in each agent's ``hooks`` dict:

- ``protect_files_hook`` — PreToolUse: block edits to sensitive/generated files.
- ``guard_tests_hook`` — PreToolUse: block edits to committed test files (TDD guard).
- ``check_stop_hook`` — Stop: run build check and auto-commit if green.
- ``clean_working_tree_hook`` — UserPromptSubmit: block start if working tree is dirty.
- ``gh_auth_hook`` — PreToolUse: verify GitHub CLI authentication.
- ``secret_scan_hook`` — PreToolUse: scan for secrets before tool execution.
- ``validate_plan_hook`` — UserPromptSubmit: validate plan integrity.
- ``tdd_orchestration_hook`` — PreToolUse: composite TDD phase routing by agent_type.
"""

from gravitee_dev.hooks._check import check_stop_hook
from gravitee_dev.hooks._gh_auth import gh_auth_hook
from gravitee_dev.hooks._guard_tests import guard_tests_hook
from gravitee_dev.hooks._protect_files import protect_files_hook
from gravitee_dev.hooks._safety_check import clean_working_tree_hook
from gravitee_dev.hooks._secret_scan import secret_scan_hook
from gravitee_dev.hooks._tdd_orchestration import tdd_orchestration_hook
from gravitee_dev.hooks._validate_plan import validate_plan_hook

__all__ = [
    "check_stop_hook",
    "clean_working_tree_hook",
    "gh_auth_hook",
    "guard_tests_hook",
    "protect_files_hook",
    "secret_scan_hook",
    "tdd_orchestration_hook",
    "validate_plan_hook",
]
