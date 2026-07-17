/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const MARKDOWN_PATTERNS = [
    /^#{1,6}\s+/m,
    /^\s*[-*+]\s+/m,
    /^\s*\d+\.\s+/m,
    /^\s*-\s\[[ xX]\]\s+/m,
    /```/,
    /!\[[^\]]*\]\([^)]+\)/,
    /\[[^\]]+\]\([^)]+\)/,
    /\*\*[^*\n]+\*\*/,
    /(?<!\*)\*[^*\n]+\*(?!\*)/,
    /~~[^~\n]+~~/,
    /`[^`\n]+`/,
    /^\s*>/m,
    /^ {0,3}(-{3,}|\*{3,}|_{3,})\s*$/m,
    /^\s*\|(.+\|)+\s*$/m,
];

/**
 * Returns true when plain text likely contains Markdown structure worth parsing.
 */
export function looksLikeMarkdown(text: string): boolean {
    const trimmed = text.trim();
    if (!trimmed) {
        return false;
    }

    return MARKDOWN_PATTERNS.some(pattern => pattern.test(trimmed));
}
