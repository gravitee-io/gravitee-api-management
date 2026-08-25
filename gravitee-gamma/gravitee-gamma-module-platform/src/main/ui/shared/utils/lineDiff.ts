/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export interface LineChange {
    value: string;
    added?: boolean;
    removed?: boolean;
}

type LineOp = { type: 'equal' | 'add' | 'remove'; line: string };

/** Line-based diff (LCS) — avoids an extra `diff` dependency for JSON comparison views. */
export function diffLines(oldText: string, newText: string): LineChange[] {
    const oldLines = splitLines(oldText);
    const newLines = splitLines(newText);
    const operations = buildLineOps(oldLines, newLines);
    return groupLineOps(operations);
}

function splitLines(text: string): string[] {
    const lines = text.split('\n');
    if (lines[lines.length - 1] === '') lines.pop();
    return lines;
}

/**
 * Computes line-level diff operations using the longest common subsequence (LCS) algorithm.
 * Builds a dynamic programming matrix to find common lines, then backtracks from bottom-right
 * to top-left, choosing additions over removals when LCS counts are equal (tie-breaking).
 */
function buildLineOps(oldLines: string[], newLines: string[]): LineOp[] {
    const oldLineCount = oldLines.length;
    const newLineCount = newLines.length;
    const commonLineCounts: number[][] = Array.from({ length: oldLineCount + 1 }, () => Array(newLineCount + 1).fill(0));

    for (let i = 1; i <= oldLineCount; i++) {
        for (let j = 1; j <= newLineCount; j++) {
            commonLineCounts[i][j] =
                oldLines[i - 1] === newLines[j - 1]
                    ? commonLineCounts[i - 1][j - 1] + 1
                    : Math.max(commonLineCounts[i - 1][j], commonLineCounts[i][j - 1]);
        }
    }

    const stack: LineOp[] = [];
    let i = oldLineCount;
    let j = newLineCount;
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && oldLines[i - 1] === newLines[j - 1]) {
            stack.push({ type: 'equal', line: oldLines[i - 1] });
            i--;
            j--;
        } else if (j > 0 && (i === 0 || commonLineCounts[i][j - 1] >= commonLineCounts[i - 1][j])) {
            stack.push({ type: 'add', line: newLines[j - 1] });
            j--;
        } else {
            stack.push({ type: 'remove', line: oldLines[i - 1] });
            i--;
        }
    }

    stack.reverse();
    return stack;
}

function groupLineOps(ops: LineOp[]): LineChange[] {
    const changes: LineChange[] = [];
    let idx = 0;

    while (idx < ops.length) {
        const type = ops[idx].type;
        const lines: string[] = [];
        while (idx < ops.length && ops[idx].type === type) {
            lines.push(ops[idx].line);
            idx++;
        }
        const value = `${lines.join('\n')}\n`;
        if (type === 'equal') {
            changes.push({ value });
        } else if (type === 'remove') {
            changes.push({ value, removed: true });
        } else {
            changes.push({ value, added: true });
        }
    }

    return changes;
}
