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

import {
    DEPLOYMENT_DIFF,
    DIFF_NBSP,
    diffPaneToneClasses,
    resolveDiffPaneTone,
    resolveDiffPaneVariant,
    type DiffPaneSide,
} from './deploymentDiffStyles';
import type { SideLine } from './types';
import { formatDate } from './utils';
import type { ApiEvent } from '../../../types';

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

interface PaneCellProps {
    lineNum: number | null;
    text: string | null;
    isChanged: boolean;
    side: DiffPaneSide;
}

function PaneCell({ lineNum, text, isChanged, side }: PaneCellProps) {
    const isEmpty = text === null;
    const variant = resolveDiffPaneVariant(isEmpty, isChanged);
    const tone = resolveDiffPaneTone(side, variant);
    const toneClasses = diffPaneToneClasses(tone);
    const gutterSymbol = side === 'left' ? '-' : '+';
    const gutterContent = !isEmpty && isChanged ? gutterSymbol : DIFF_NBSP;

    return (
        <div className={DEPLOYMENT_DIFF.pane.row}>
            <span className={cn(DEPLOYMENT_DIFF.pane.lineNum, toneClasses.lineNum)}>{lineNum ?? DIFF_NBSP}</span>
            <span className={cn(DEPLOYMENT_DIFF.pane.gutter, toneClasses.gutter)}>{gutterContent}</span>
            <pre className={cn(DEPLOYMENT_DIFF.pane.content, toneClasses.content)}>{text ?? DIFF_NBSP}</pre>
        </div>
    );
}

function MetaCell({ event, version, label }: { event: ApiEvent; version: string; label: string }) {
    return (
        <div className="px-4 py-2.5 space-y-0.5">
            <div className="flex items-center gap-2">
                <Badge variant="outline" className={DEPLOYMENT_DIFF.versionBadge}>
                    v{version}
                </Badge>
                <span className="text-xs font-semibold">{label}</span>
            </div>
            <p className="text-xs text-muted-foreground">
                {formatDate(event.createdAt)}
                <span className="mx-1">·</span>
                {event.initiator.displayName}
                {event.properties.DEPLOYMENT_LABEL ? (
                    <>
                        <span className="mx-1">·</span>
                        <span className="italic">{event.properties.DEPLOYMENT_LABEL}</span>
                    </>
                ) : null}
            </p>
        </div>
    );
}

interface SideBySideViewProps {
    lines: SideLine[];
    leftEvent: ApiEvent;
    rightEvent: ApiEvent;
    leftVersion: string;
    rightVersion: string;
}

export function SideBySideView({ lines, leftEvent, rightEvent, leftVersion, rightVersion }: SideBySideViewProps) {
    const numbered = computeLineNumbers(lines);

    return (
        <table className={DEPLOYMENT_DIFF.table}>
            <thead>
                <tr className={DEPLOYMENT_DIFF.headerRow}>
                    <th className={cn(DEPLOYMENT_DIFF.headerCell, DEPLOYMENT_DIFF.bodyCellDivider)}>
                        <MetaCell event={leftEvent} version={leftVersion} label="Before" />
                    </th>
                    <th className={DEPLOYMENT_DIFF.headerCell}>
                        <MetaCell event={rightEvent} version={rightVersion} label="After" />
                    </th>
                </tr>
            </thead>
            <tbody>
                {numbered.map((line, i) => {
                    const leftChanged = line.kind !== 'equal' && line.left !== null;
                    const rightChanged = line.kind !== 'equal' && line.right !== null;
                    return (
                        <tr key={i}>
                            <td className={cn(DEPLOYMENT_DIFF.bodyCell, DEPLOYMENT_DIFF.bodyCellDivider)}>
                                <PaneCell lineNum={line.leftNum} text={line.left} isChanged={leftChanged} side="left" />
                            </td>
                            <td className={DEPLOYMENT_DIFF.bodyCell}>
                                <PaneCell lineNum={line.rightNum} text={line.right} isChanged={rightChanged} side="right" />
                            </td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    );
}
