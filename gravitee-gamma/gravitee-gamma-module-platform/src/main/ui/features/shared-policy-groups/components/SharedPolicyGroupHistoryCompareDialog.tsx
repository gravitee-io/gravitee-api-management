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
import {
    Button,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    ScrollArea,
    cn,
} from '@gravitee/graphene-core';
import { useMemo } from 'react';

import { WIDE_DIALOG_CONTENT_STYLE, WIDE_DIALOG_STYLE } from '../../../shared/layout/dialogLayout';
import { diffLines, type LineChange } from '../../../shared/utils/lineDiff';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

/** JSON indentation must survive rendering, and long values wrap instead of scrolling sideways. */
const DIFF_LINE_STYLE = { whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' } as const;

interface SharedPolicyGroupHistoryCompareDialogProps {
    readonly open: boolean;
    readonly left?: SharedPolicyGroup;
    readonly right?: SharedPolicyGroup;
    readonly rightIsPending?: boolean;
    readonly onOpenChange: (open: boolean) => void;
}

interface DiffRow {
    readonly left?: string;
    readonly right?: string;
    readonly changed: boolean;
}

function toLines(value: string): string[] {
    const lines = value.split('\n');
    if (lines[lines.length - 1] === '') {
        lines.pop();
    }
    return lines;
}

/** Aligns the unified diff into rows so each removed line faces the added line that replaced it. */
function buildDiffRows(changes: LineChange[]): DiffRow[] {
    const rows: DiffRow[] = [];
    let removedLines: string[] = [];
    let addedLines: string[] = [];

    function flushChangedLines() {
        for (let index = 0; index < Math.max(removedLines.length, addedLines.length); index++) {
            rows.push({ left: removedLines[index], right: addedLines[index], changed: true });
        }
        removedLines = [];
        addedLines = [];
    }

    for (const change of changes) {
        if (change.removed) {
            removedLines.push(...toLines(change.value));
        } else if (change.added) {
            addedLines.push(...toLines(change.value));
        } else {
            flushChangedLines();
            for (const line of toLines(change.value)) {
                rows.push({ left: line, right: line, changed: false });
            }
        }
    }
    flushChangedLines();

    return rows;
}

function DiffLine({ line, changed, side }: Readonly<{ line?: string; changed: boolean; side: 'removed' | 'added' }>) {
    const isAddedSide = side === 'added';

    if (line === undefined) {
        return <div className={cn('bg-muted/30', isAddedSide && 'border-l')} aria-hidden />;
    }

    return (
        <div
            className={cn(
                'px-3',
                isAddedSide && 'border-l',
                changed && (isAddedSide ? 'bg-success/20 text-success' : 'bg-destructive/20 text-destructive'),
            )}
            style={DIFF_LINE_STYLE}
            aria-label={changed ? (isAddedSide ? 'Added lines' : 'Removed lines') : undefined}
        >
            {line || ' '}
        </div>
    );
}

export function SharedPolicyGroupHistoryCompareDialog({
    open,
    left,
    right,
    rightIsPending = false,
    onOpenChange,
}: SharedPolicyGroupHistoryCompareDialogProps) {
    const rows = useMemo(() => {
        if (!left || !right) {
            return [];
        }
        return buildDiffRows(diffLines(JSON.stringify(left, null, 2), JSON.stringify(right, null, 2)));
    }, [left, right]);

    if (!left || !right) {
        return null;
    }

    const rightVersionLabel = rightIsPending ? 'to be deployed' : (right.version ?? '—');
    const title = `Comparing version ${left.version ?? '—'} with version ${rightVersionLabel}`;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={WIDE_DIALOG_STYLE}>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>JSON changes between the selected Shared Policy Group versions.</DialogDescription>
                </DialogHeader>
                <div className="overflow-hidden rounded-lg border bg-muted/30">
                    <div className="grid grid-cols-2 border-b bg-background text-xs font-medium">
                        <div className="px-3 py-2">Version {left.version ?? '—'}</div>
                        <div className="border-l px-3 py-2">Version {rightVersionLabel}</div>
                    </div>
                    <ScrollArea style={WIDE_DIALOG_CONTENT_STYLE}>
                        <div className="font-mono text-xs leading-5" aria-label="Shared Policy Group JSON differences">
                            {rows.map((row, index) => (
                                // Rows keep their diff order; duplicate line content is valid.
                                <div key={index} className="grid grid-cols-2">
                                    <DiffLine line={row.left} changed={row.changed} side="removed" />
                                    <DiffLine line={row.right} changed={row.changed} side="added" />
                                </div>
                            ))}
                        </div>
                    </ScrollArea>
                </div>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button">Close</Button>
                    </DialogClose>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
