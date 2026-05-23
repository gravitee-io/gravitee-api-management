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
import { Badge, Button, Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { LineDiffView } from './LineDiffView';
import { SideBySideView } from './SideBySideView';
import type { DiffMode } from './types';
import { computeSideBySideDiff, computeUnifiedDiff, extractDefinition, formatDate, hasDiffChanges } from './utils';
import type { ApiEvent } from '../../../types';

type Props = Readonly<{
    left: ApiEvent;
    right: ApiEvent;
    onClose: () => void;
    onRollback: (eventId: string) => Promise<void>;
    isRollingBack: boolean;
}>;

const DIFF_MODES: { value: DiffMode; label: string }[] = [
    { value: 'side-by-side', label: 'Side-by-side' },
    { value: 'line-by-line', label: 'Line-by-line' },
];

function VersionMeta({ event, version, label }: { event: ApiEvent; version: string; label: string }) {
    return (
        <div className="px-4 py-2.5 space-y-0.5">
            <div className="flex items-center gap-2">
                <Badge variant="outline" className="text-[10px] font-mono px-1.5 py-0">
                    v{version}
                </Badge>
                <span className="text-xs font-medium text-muted-foreground">{label}</span>
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

export function DiffDialog({ left, right, onClose, onRollback, isRollingBack }: Props) {
    const [mode, setMode] = useState<DiffMode>('side-by-side');

    const leftDef = useMemo(() => extractDefinition(left), [left]);
    const rightDef = useMemo(() => extractDefinition(right), [right]);
    const unifiedLines = useMemo(() => computeUnifiedDiff(leftDef, rightDef), [leftDef, rightDef]);
    const sideBySideLines = useMemo(() => computeSideBySideDiff(leftDef, rightDef), [leftDef, rightDef]);
    const changed = hasDiffChanges(unifiedLines);

    const leftVersion = left.properties.DEPLOYMENT_NUMBER ?? '—';
    const rightVersion = right.properties.DEPLOYMENT_NUMBER ?? '—';

    return (
        <Dialog open onOpenChange={open => !open && onClose()}>
            <DialogContent
                className="flex flex-col overflow-hidden p-0 gap-0"
                style={{ width: 'min(92vw, 76rem)', maxWidth: 'min(92vw, 76rem)', maxHeight: 'min(90vh, 860px)' }}
                showCloseButton={false}
            >
                {/* Header */}
                <DialogHeader className="flex-none flex-row items-center justify-between border-b px-6 py-3 gap-4">
                    <DialogTitle className="text-base">
                        Comparing v{leftVersion} → v{rightVersion}
                    </DialogTitle>
                    <div className="flex items-center gap-2 shrink-0">
                        {changed ? (
                            <div className="flex rounded-lg border bg-muted/40 p-0.5 gap-0.5">
                                {DIFF_MODES.map(({ value, label }) => (
                                    <button
                                        key={value}
                                        type="button"
                                        onClick={() => setMode(value)}
                                        className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
                                            mode === value
                                                ? 'bg-background text-foreground shadow-sm'
                                                : 'text-muted-foreground hover:text-foreground'
                                        }`}
                                    >
                                        {label}
                                    </button>
                                ))}
                            </div>
                        ) : null}
                        <DialogClose asChild>
                            <Button variant="ghost" size="icon" className="size-8">
                                <XIcon className="size-4" />
                            </Button>
                        </DialogClose>
                    </div>
                </DialogHeader>

                {/* Version meta strip */}
                <div className="flex flex-none border-b bg-muted/20">
                    <div className="flex-1 border-r">
                        <VersionMeta event={left} version={leftVersion} label="Before" />
                    </div>
                    <div className="flex-1">
                        <VersionMeta event={right} version={rightVersion} label="After" />
                    </div>
                </div>

                {/* Diff body */}
                <div className="flex-1 min-h-0 overflow-auto">
                    {!changed ? (
                        <div className="flex items-center justify-center h-full py-16 text-sm text-muted-foreground">
                            No differences found between these two versions.
                        </div>
                    ) : mode === 'side-by-side' ? (
                        <SideBySideView lines={sideBySideLines} />
                    ) : (
                        <LineDiffView lines={unifiedLines} />
                    )}
                </div>

                <DialogFooter className="flex-none border-t bg-muted/30 px-6 py-3 gap-2">
                    <DialogClose asChild>
                        <Button variant="outline" size="sm">
                            Close
                        </Button>
                    </DialogClose>
                    <Button size="sm" variant="destructive" disabled={isRollingBack} onClick={() => onRollback(right.id)}>
                        {isRollingBack ? 'Rolling back…' : `Rollback to v${rightVersion}`}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
