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

"""PreToolUse hook — block edits to sensitive files.

Checks ``tool_input.file_path`` against a list of protected substring
patterns.  If any pattern matches, the edit is blocked.
"""

from claude_agent_sdk.types import HookContext, HookInput, SyncHookJSONOutput

DEFAULT_PATTERNS: list[str] = [
    # Secrets & credentials
    ".env",
    "credentials.json",
    ".pem",
    ".key",
    # Lock files (managed by package managers)
    "package-lock.json",
    "pnpm-lock.yaml",
    "yarn.lock",
    "poetry.lock",
    "uv.lock",
    # Git internals
    ".git/",
    # Build artifacts & caches
    "node_modules/",
    "dist/",
    "build/",
    "target/",
    "__pycache__/",
    # IDE configuration
    ".idea/",
]


async def protect_files_hook(
    hook_input: HookInput,
    tool_name: str | None,
    context: HookContext,
    *,
    patterns: list[str] | None = None,
) -> SyncHookJSONOutput:
    """Block edits to files matching protected patterns.

    Args:
        hook_input: The PreToolUseHookInput from the SDK.
        tool_name: The tool being invoked (unused).
        context: Hook context (unused).
        patterns: Optional custom pattern list. Defaults to DEFAULT_PATTERNS.

    Returns:
        Empty dict to allow, or ``{"decision": "block", "reason": ...}`` to block.
    """
    file_path: str = hook_input.get("tool_input", {}).get("file_path", "")
    if not file_path:
        return SyncHookJSONOutput()

    active_patterns = patterns if patterns is not None else DEFAULT_PATTERNS

    for pattern in active_patterns:
        if pattern in file_path:
            return SyncHookJSONOutput(
                decision="block",
                reason=f"Blocked: '{file_path}' matches protected pattern '{pattern}'",
            )

    return SyncHookJSONOutput()
