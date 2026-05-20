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

import type { SideLine } from './types';

type NumberedSideLine = SideLine & { leftNum: number | null; rightNum: number | null };

function computeLineNumbers(lines: SideLine[]): NumberedSideLine[] {
    let left = 0;
    let right = 0;
    return lines.map(line => {
        const hasLeft = line.left !== null;
        const hasRight = line.right !== null;
        if (hasLeft) left++;
        if (hasRight) right++;
        return { ...line, leftNum: hasLeft ? left : null, rightNum: hasRight ? right : null };
    });
}

function PaneCell({
    lineNum,
    text,
    isChanged,
    side,
}: {
    lineNum: number | null;
    text: string | null;
    isChanged: boolean;
    side: 'left' | 'right';
}) {
    const isEmpty = text === null;
    const symbol = side === 'left' ? '-' : '+';

    return (
        <div className="flex items-stretch h-full min-h-[1.25rem]">
            {/* Line number */}
            <span
                className={cn(
                    'select-none shrink-0 w-10 text-right pr-2 pl-1 tabular-nums text-[10px] leading-5 border-r',
                    isChanged &&
                        side === 'left' &&
                        'bg-red-100 text-red-400 border-red-200 dark:bg-red-950/50 dark:text-red-500 dark:border-red-800',
                    isChanged &&
                        side === 'right' &&
                        'bg-green-100 text-green-500 border-green-200 dark:bg-green-950/50 dark:text-green-400 dark:border-green-800',
                    !isChanged && 'bg-muted/40 text-muted-foreground/40 border-border/50',
                )}
            >
                {lineNum ?? ''}
            </span>
            {/* Gutter */}
            <span
                className={cn(
                    'select-none shrink-0 w-5 text-center leading-5 font-bold text-xs border-r',
                    isChanged &&
                        side === 'left' &&
                        'bg-red-200 text-red-700 border-red-300 dark:bg-red-900/60 dark:text-red-300 dark:border-red-700',
                    isChanged &&
                        side === 'right' &&
                        'bg-green-200 text-green-800 border-green-300 dark:bg-green-900/60 dark:text-green-200 dark:border-green-700',
                    !isChanged && 'bg-muted/40 text-transparent border-border/50',
                )}
            >
                {!isEmpty && isChanged ? symbol : ' '}
            </span>
            {/* Content */}
            <pre
                className={cn(
                    'flex-1 px-3 leading-5 text-xs overflow-hidden text-ellipsis',
                    isEmpty && 'bg-muted/20',
                    isChanged && side === 'left' && !isEmpty && 'bg-red-50 text-red-900 dark:bg-red-950/30 dark:text-red-100',
                    isChanged && side === 'right' && !isEmpty && 'bg-green-50 text-green-900 dark:bg-green-950/30 dark:text-green-100',
                )}
            >
                {text ?? ''}
            </pre>
        </div>
    );
}

export function SideBySideView({ lines }: { lines: SideLine[] }) {
    const numbered = computeLineNumbers(lines);

    return (
        <div className="overflow-x-auto">
            <table className="min-w-full border-collapse font-mono text-xs" style={{ tableLayout: 'fixed', minWidth: '900px' }}>
                <colgroup>
                    <col style={{ width: '50%' }} />
                    <col style={{ width: '50%' }} />
                </colgroup>
                <thead>
                    <tr className="border-b bg-muted/60">
                        <th className="text-left px-3 py-1.5 text-xs font-medium text-muted-foreground border-r">Before</th>
                        <th className="text-left px-3 py-1.5 text-xs font-medium text-muted-foreground">After</th>
                    </tr>
                </thead>
                <tbody>
                    {numbered.map((line, i) => {
                        const leftChanged = line.kind !== 'equal' && line.left !== null;
                        const rightChanged = line.kind !== 'equal' && line.right !== null;
                        return (
                            <tr key={i}>
                                <td className="px-0 py-0 align-top border-r border-border/50">
                                    <PaneCell lineNum={line.leftNum} text={line.left} isChanged={leftChanged} side="left" />
                                </td>
                                <td className="px-0 py-0 align-top">
                                    <PaneCell lineNum={line.rightNum} text={line.right} isChanged={rightChanged} side="right" />
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
