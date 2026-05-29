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
import { computeSideBySideDiff, computeUnifiedDiff, extractDefinition, hasDiffChanges } from './utils';
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

export function DiffDialog({ left, right, onClose, onRollback, isRollingBack }: Props) {
    const [mode, setMode] = useState<DiffMode>('side-by-side');
    const [showRollbackConfirm, setShowRollbackConfirm] = useState(false);

    const leftDef = useMemo(() => extractDefinition(left), [left]);
    const rightDef = useMemo(() => extractDefinition(right), [right]);
    const unifiedLines = useMemo(() => computeUnifiedDiff(leftDef, rightDef), [leftDef, rightDef]);
    const sideBySideLines = useMemo(() => computeSideBySideDiff(leftDef, rightDef), [leftDef, rightDef]);
    const changed = hasDiffChanges(unifiedLines);

    const leftVersion = left.properties.DEPLOYMENT_NUMBER ?? '—';
    const rightVersion = right.properties.DEPLOYMENT_NUMBER ?? '—';

    return (
        <>
            <Dialog open onOpenChange={open => !open && onClose()}>
                <DialogContent
                    className="flex flex-col overflow-hidden p-0 gap-0"
                    style={{ width: 'min(92vw, 76rem)', maxWidth: 'min(92vw, 76rem)', maxHeight: 'min(90vh, 860px)' }}
                    showCloseButton={false}
                >
                    {/* ─── Header ─────────────────────────────────────────────── */}
                    <DialogHeader className="flex-none flex-row items-center justify-between border-b px-6 py-3 gap-4">
                        <DialogTitle className="text-base">
                            Comparing v{leftVersion} → v{rightVersion}
                        </DialogTitle>
                        <div className="flex items-center gap-3 shrink-0">
                            {changed && (
                                <div className="flex rounded-md border bg-muted/40 p-0.5 gap-0.5">
                                    {DIFF_MODES.map(({ value, label }) => (
                                        <button
                                            key={value}
                                            type="button"
                                            onClick={() => setMode(value)}
                                            className={`px-3 py-1 text-xs font-medium rounded-sm transition-colors ${
                                                mode === value
                                                    ? 'bg-background text-foreground shadow-sm'
                                                    : 'text-muted-foreground hover:text-foreground'
                                            }`}
                                        >
                                            {label}
                                        </button>
                                    ))}
                                </div>
                            )}
                            <DialogClose asChild>
                                <Button variant="ghost" size="icon" className="size-8 shrink-0" aria-label="Close dialog">
                                    <XIcon className="size-4" aria-hidden />
                                </Button>
                            </DialogClose>
                        </div>
                    </DialogHeader>

                    {/* ─── Scrollable body ─────────────────────────────────────── */}
                    {/* Version meta is now inside each view's <thead sticky> —      */}
                    {/* the same colgroup controls both, so alignment is guaranteed. */}
                    <div className="flex-1 min-h-0 overflow-auto">
                        {!changed ? (
                            <div className="flex items-center justify-center py-20 text-sm text-muted-foreground">
                                No differences found between these two versions.
                            </div>
                        ) : mode === 'side-by-side' ? (
                            <SideBySideView
                                lines={sideBySideLines}
                                leftEvent={left}
                                rightEvent={right}
                                leftVersion={leftVersion}
                                rightVersion={rightVersion}
                            />
                        ) : (
                            <LineDiffView
                                lines={unifiedLines}
                                leftEvent={left}
                                rightEvent={right}
                                leftVersion={leftVersion}
                                rightVersion={rightVersion}
                            />
                        )}
                    </div>

                    {/* ─── Footer ─────────────────────────────────────────────── */}
                    <div className="flex-none flex items-center border-t bg-muted/30 px-6 py-3 gap-2 justify-between">
                        <DialogClose asChild>
                            <Button variant="outline" size="sm">
                                Close
                            </Button>
                        </DialogClose>
                        {changed && (
                            <Button size="sm" variant="destructive" onClick={() => setShowRollbackConfirm(true)}>
                                Rollback to v{rightVersion}
                            </Button>
                        )}
                    </div>
                </DialogContent>
            </Dialog>

            {/* ─── Rollback confirmation ────────────────────────────────────── */}
            <Dialog open={showRollbackConfirm} onOpenChange={open => !open && !isRollingBack && setShowRollbackConfirm(false)}>
                <DialogContent className="max-w-sm">
                    <DialogHeader>
                        <DialogTitle>Rollback to v{rightVersion}?</DialogTitle>
                        <DialogDescription>
                            This will restore the API to version {rightVersion} and redeploy it to the gateway. This action cannot be
                            undone.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter className="gap-2 sm:justify-end">
                        <Button variant="outline" size="sm" disabled={isRollingBack} onClick={() => setShowRollbackConfirm(false)}>
                            Cancel
                        </Button>
                        <Button
                            size="sm"
                            variant="destructive"
                            disabled={isRollingBack}
                            onClick={async () => {
                                await onRollback(right.id);
                                setShowRollbackConfirm(false);
                            }}
                        >
                            {isRollingBack ? 'Rolling back…' : 'Confirm rollback'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    );
}
