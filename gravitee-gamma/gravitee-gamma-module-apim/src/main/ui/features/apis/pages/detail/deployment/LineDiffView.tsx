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
import { Badge, cn } from '@gravitee/graphene-core';

import { DEPLOYMENT_DIFF, diffPaneToneClasses } from './deploymentDiffStyles';
import type { UnifiedLine } from './types';
import { formatDate } from './utils';
import type { ApiEvent } from '../../../types';

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

interface LineDiffViewProps {
    lines: UnifiedLine[];
    leftEvent: ApiEvent;
    rightEvent: ApiEvent;
    leftVersion: string;
    rightVersion: string;
}

export function LineDiffView({ lines, leftEvent, rightEvent, leftVersion, rightVersion }: LineDiffViewProps) {
    const numbered = computeLineNumbers(lines);
    const removedTone = diffPaneToneClasses('removed');
    const addedTone = diffPaneToneClasses('added');
    const neutralTone = diffPaneToneClasses('neutral');

    return (
        <table className={DEPLOYMENT_DIFF.table}>
            <thead>
                <tr className={DEPLOYMENT_DIFF.headerRow}>
                    <th colSpan={4} className="px-4 py-2.5 text-left font-normal">
                        <div className="flex flex-wrap items-center gap-4">
                            <div className="flex items-center gap-2">
                                <Badge variant="outline" className={DEPLOYMENT_DIFF.versionBadge}>
                                    v{leftVersion}
                                </Badge>
                                <span className="text-xs font-semibold">Before</span>
                                <span className="text-xs text-muted-foreground">
                                    {formatDate(leftEvent.createdAt)} · {leftEvent.initiator.displayName}
                                </span>
                            </div>
                            <span className="text-muted-foreground/40">→</span>
                            <div className="flex items-center gap-2">
                                <Badge variant="outline" className={DEPLOYMENT_DIFF.versionBadge}>
                                    v{rightVersion}
                                </Badge>
                                <span className="text-xs font-semibold">After</span>
                                <span className="text-xs text-muted-foreground">
                                    {formatDate(rightEvent.createdAt)} · {rightEvent.initiator.displayName}
                                </span>
                            </div>
                        </div>
                    </th>
                </tr>
            </thead>
            <tbody>
                {numbered.map((line, i) => {
                    const added = line.type === '+';
                    const removed = line.type === '-';
                    const leftTone = removed ? removedTone : neutralTone;
                    const rightTone = added ? addedTone : neutralTone;
                    const gutterTone = added ? addedTone : removed ? removedTone : neutralTone;

                    return (
                        <tr key={i}>
                            <td className={cn(DEPLOYMENT_DIFF.unified.lineNum, leftTone.lineNum)}>{line.leftNum ?? ''}</td>
                            <td className={cn(DEPLOYMENT_DIFF.unified.lineNum, rightTone.lineNum)}>{line.rightNum ?? ''}</td>
                            <td className={cn(DEPLOYMENT_DIFF.unified.gutter, gutterTone.gutter)}>{added ? '+' : removed ? '-' : ''}</td>
                            <td className={cn(DEPLOYMENT_DIFF.unified.content, added && addedTone.content, removed && removedTone.content)}>
                                {line.text || ' '}
                            </td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    );
}
