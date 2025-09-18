/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

/**
 * Normalizes the indentation of a text by removing the common indentation
 * while preserving the syntax of markdown blockquotes.
 *
 * @param rawContent The text to normalize
 * @param tabSize The tab size to use for indentation normalization
 * @returns The text with normalized indentation
 *
 * * @example
 *   // Input:
 *   const text = `
 *       #### Title
 *
 *           Code block
 *
 *       > Quoted text
 *   `;
 *
 *   normalizeIndentation(text);
 *
 *   // Output:
 *   "#### Title
 *
 *       Code block
 *
 *   > Quoted text"
 */
export function normalizeIndentation(rawContent: string, tabSize: number = 4): string {
  // Normalize line endings
  const lines = rawContent.replace(/\r\n/g, '\n').split('\n');

  // Remove leading/trailing empty lines
  while (lines.length && lines[0].trim() === '') lines.shift();
  while (lines.length && lines[lines.length - 1].trim() === '') lines.pop();

  if (!lines.length) return '';

  // Convert tabs to spaces
  const expandTabs = (line: string) => line.replace(/\t/g, ' '.repeat(tabSize));
  const expanded = lines.map(expandTabs);

  // Compute minimum indentation among non-empty lines
  const indents = expanded.filter(l => l.trim().length > 0).map(l => l.match(/^\s*/)?.[0].length || 0);

  const minIndent = indents.length ? Math.min(...indents) : 0;

  // Remove common indentation
  return expanded.map(l => l.slice(minIndent)).join('\n');
}
