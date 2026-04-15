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

"""UserPromptSubmit hook — validate plan integrity before execution.

Scans the implementation plan for required sections, consistency issues,
and potential injection patterns. This is a lightweight pre-flight check
that runs before the validate-plan sub-agent for defense-in-depth.
"""

import os
import re

from claude_agent_sdk.types import HookContext, HookInput, SyncHookJSONOutput

_REQUIRED_SECTIONS = [
    re.compile(r"(?i)#.*\b(summary|overview|problem|introduction|goal)\b"),
    re.compile(r"(?i)#.*\b(file|change|approach|implementation|checklist|plan)\b"),
    re.compile(r"(?i)#.*\b(test|criteria|acceptance|verification|checklist)\b"),
]

_INJECTION_PATTERNS = [
    re.compile(r"\$\("),  # command substitution
    re.compile(
        r"(?<!\w)`[^`]*\b(rm|eval|exec|sh|bash|zsh)\b[^`]*`"
    ),  # shell execution in backticks
    re.compile(r"(?i)bypass\s+(permission|hook|safety|guard)"),
    re.compile(r"(?i)disable\s+(hook|guard|check|safety)"),
    re.compile(r"(?i)(curl|wget|fetch)\s+http"),
]


async def validate_plan_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
) -> SyncHookJSONOutput:
    """Validate the implementation plan before agent execution.

    Args:
        hook_input: The UserPromptSubmitHookInput from the SDK.
        tool_name: Unused for UserPromptSubmit hooks.
        context: Hook context (unused).

    Returns:
        Empty dict to allow, or ``{"decision": "block", "reason": ...}`` to block.
    """
    cwd: str = hook_input.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
    plan_path = os.path.join(cwd, "implementation_plan.md")

    if not os.path.exists(plan_path):
        return SyncHookJSONOutput()  # no plan yet — allow (init hasn't run)

    try:
        with open(plan_path) as f:
            content = f.read()
    except OSError:
        return SyncHookJSONOutput(
            decision="block",
            reason="Failed to read implementation_plan.md for validation.",
        )

    # Check required sections
    missing = []
    for pattern in _REQUIRED_SECTIONS:
        if not pattern.search(content):
            missing.append(pattern.pattern)

    if missing:
        return SyncHookJSONOutput(
            decision="block",
            reason=(
                "Implementation plan is missing required sections. "
                f"Expected patterns not found: {', '.join(missing)}"
            ),
        )

    # Check for injection patterns
    for pattern in _INJECTION_PATTERNS:
        match = pattern.search(content)
        if match:
            return SyncHookJSONOutput(
                decision="block",
                reason=(
                    f"Suspicious pattern detected in implementation plan: "
                    f"'{match.group()}'. Review the plan for injection risks."
                ),
            )

    return SyncHookJSONOutput()
