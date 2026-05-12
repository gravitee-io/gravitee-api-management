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
} from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { LineDiffView } from './LineDiffView';
import { SideBySideView } from './SideBySideView';
import type { DiffMode } from './types';
import { computeSideBySideDiff, computeUnifiedDiff, extractDefinition, formatDate, hasDiffChanges } from './utils';
import type { ApiEvent } from '../../../types/api.types';

type Props = Readonly<{
    left: ApiEvent;
    right: ApiEvent;
    onClose: () => void;
    onRollback: (eventId: string) => Promise<void>;
    isRollingBack: boolean;
}>;

const DIFF_MODES: { value: DiffMode; label: string }[] = [
    { value: 'line-by-line', label: 'Line-by-line' },
    { value: 'side-by-side', label: 'Side-by-side' },
];

function VersionMeta({ event, version, label }: { event: ApiEvent; version: string; label: string }) {
    return (
        <div className="px-6 py-2.5">
            <p className="text-xs font-medium text-muted-foreground">
                v{version} — {label}
            </p>
            <p className="text-sm mt-0.5">
                {formatDate(event.createdAt)}
                <span className="text-muted-foreground"> · {event.initiator.displayName}</span>
                {event.properties.DEPLOYMENT_LABEL ? (
                    <span className="text-muted-foreground"> · {event.properties.DEPLOYMENT_LABEL}</span>
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
                style={{ width: 'min(90vw, 72rem)', maxWidth: 'min(90vw, 72rem)', maxHeight: 'min(90vh, 840px)' }}
                showCloseButton={false}
            >
                <DialogHeader className="flex-none flex-row items-start justify-between border-b px-6 py-4 gap-4">
                    <div>
                        <DialogTitle>
                            Comparing v{leftVersion} → v{rightVersion}
                        </DialogTitle>
                        <DialogDescription className="mt-0.5">
                            {changed ? 'Red lines removed, green lines added.' : 'No differences found between these two versions.'}
                        </DialogDescription>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                        {changed ? (
                            <div className="flex rounded-lg border p-0.5 gap-0.5">
                                {DIFF_MODES.map(({ value, label }) => (
                                    <button
                                        key={value}
                                        type="button"
                                        onClick={() => setMode(value)}
                                        className={`px-2.5 py-1 rounded-md text-xs transition-colors ${
                                            mode === value
                                                ? 'bg-primary text-primary-foreground font-medium'
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

                {/* Version metadata strip */}
                <div className="flex-none grid grid-cols-2 divide-x border-b bg-muted/20">
                    <VersionMeta event={left} version={leftVersion} label="before" />
                    <VersionMeta event={right} version={rightVersion} label="after" />
                </div>

                {/* Scrollable diff area */}
                <div className="flex-1 min-h-0 overflow-y-auto">
                    {!changed ? (
                        <div className="flex items-center justify-center p-10 text-sm text-muted-foreground">
                            No differences found between these two deployments.
                        </div>
                    ) : mode === 'line-by-line' ? (
                        <LineDiffView lines={unifiedLines} />
                    ) : (
                        <SideBySideView lines={sideBySideLines} />
                    )}
                </div>

                <DialogFooter className="flex-none border-t px-6 py-3 bg-muted/50 sm:flex-row sm:justify-between gap-2">
                    <p className="text-xs text-muted-foreground self-center">Rollback redeploys the selected version to the gateway.</p>
                    <div className="flex items-center gap-2">
                        <DialogClose asChild>
                            <Button variant="outline" size="sm">
                                Close
                            </Button>
                        </DialogClose>
                        <Button variant="outline" size="sm" disabled={isRollingBack} onClick={() => onRollback(left.id)}>
                            {isRollingBack ? 'Rolling back…' : `Rollback to v${leftVersion}`}
                        </Button>
                        <Button size="sm" disabled={isRollingBack} onClick={() => onRollback(right.id)}>
                            {isRollingBack ? 'Rolling back…' : `Rollback to v${rightVersion}`}
                        </Button>
                    </div>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
