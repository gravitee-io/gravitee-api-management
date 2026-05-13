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
import { cn } from '@gravitee/graphene-core';

import type { UnifiedLine } from './types';

type NumberedLine = UnifiedLine & { leftNum: number | null; rightNum: number | null };

function computeLineNumbers(lines: UnifiedLine[]): NumberedLine[] {
    let left = 0;
    let right = 0;
    return lines.map(line => {
        const added = line.type === '+';
        const removed = line.type === '-';
        if (!added) left++;
        if (!removed) right++;
        return { ...line, leftNum: added ? null : left, rightNum: removed ? null : right };
    });
}

export function LineDiffView({ lines }: { lines: UnifiedLine[] }) {
    const numbered = computeLineNumbers(lines);

    return (
        <div className="overflow-x-auto">
            <table className="min-w-full border-collapse font-mono text-xs">
                <colgroup>
                    <col style={{ width: '3rem' }} />
                    <col style={{ width: '3rem' }} />
                    <col style={{ width: '1.5rem' }} />
                    <col />
                </colgroup>
                <tbody>
                    {numbered.map((line, i) => {
                        const added = line.type === '+';
                        const removed = line.type === '-';
                        return (
                            <tr key={i} className="group">
                                {/* Left line number */}
                                <td
                                    className={cn(
                                        'select-none text-right pr-3 pl-2 tabular-nums leading-5 align-top border-r text-[10px]',
                                        removed
                                            ? 'bg-red-100 text-red-500 dark:bg-red-950/50 dark:text-red-400 border-red-200 dark:border-red-800'
                                            : 'bg-muted/40 text-muted-foreground/40 border-border/50',
                                    )}
                                >
                                    {line.leftNum ?? ''}
                                </td>
                                {/* Right line number */}
                                <td
                                    className={cn(
                                        'select-none text-right pr-3 pl-2 tabular-nums leading-5 align-top border-r text-[10px]',
                                        added
                                            ? 'bg-green-100 text-green-500 dark:bg-green-950/50 dark:text-green-400 border-green-200 dark:border-green-800'
                                            : 'bg-muted/40 text-muted-foreground/40 border-border/50',
                                    )}
                                >
                                    {line.rightNum ?? ''}
                                </td>
                                {/* Gutter symbol */}
                                <td
                                    className={cn(
                                        'select-none text-center leading-5 align-top border-r font-bold',
                                        added &&
                                            'bg-green-200 text-green-800 dark:bg-green-900/60 dark:text-green-300 border-green-300 dark:border-green-700',
                                        removed &&
                                            'bg-red-200 text-red-700 dark:bg-red-900/60 dark:text-red-300 border-red-300 dark:border-red-700',
                                        !added && !removed && 'bg-muted/40 text-muted-foreground/30 border-border/50',
                                    )}
                                >
                                    {added ? '+' : removed ? '-' : ''}
                                </td>
                                {/* Content */}
                                <td
                                    className={cn(
                                        'px-3 py-0 leading-5 whitespace-pre align-top',
                                        added && 'bg-green-50 text-green-900 dark:bg-green-950/30 dark:text-green-100',
                                        removed && 'bg-red-50 text-red-900 dark:bg-red-950/30 dark:text-red-100',
                                    )}
                                >
                                    {line.text || ' '}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
