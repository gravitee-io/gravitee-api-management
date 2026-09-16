/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Button,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    toast,
} from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import type { PromotionReviewData } from '../tasks.types';

export function PromotionReviewSheet({
    open,
    onOpenChange,
    data,
    onProcess,
    onOpenApi,
}: Readonly<{
    open: boolean;
    onOpenChange: (v: boolean) => void;
    data: PromotionReviewData;
    onProcess?: (promotionId: string, accepted: boolean) => Promise<void>;
    onOpenApi?: () => void;
}>) {
    const [pendingAction, setPendingAction] = useState<'accept' | 'reject' | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [confirmingReject, setConfirmingReject] = useState(false);

    // Reset on open, not on close. The sheet is mounted for the lifetime of its row and closes through
    // several paths — the Close button, Escape, the overlay, and Open API, which closes it from the parent
    // without going through this handler. Clearing on the way in is the only place every path passes.
    useEffect(() => {
        if (open) {
            setError(null);
            setConfirmingReject(false);
        }
    }, [open]);

    function handleOpenChange(nextOpen: boolean) {
        // A request in flight owns the sheet: its outcome is reported here, and a sheet closed underneath it
        // would swallow a failure the reviewer has to see.
        if (!nextOpen && pendingAction !== null) {
            return;
        }
        onOpenChange(nextOpen);
    }

    async function handleProcess(accepted: boolean) {
        if (!onProcess) return;
        setPendingAction(accepted ? 'accept' : 'reject');
        setError(null);
        try {
            await onProcess(data.promotionId, accepted);
            toast.success(accepted ? 'API promotion accepted.' : 'API promotion rejected.');
            setPendingAction(null);
            onOpenChange(false);
        } catch (e) {
            const message = e instanceof Error ? e.message : 'Failed to process the promotion.';
            console.error('Failed to process the promotion', e);
            // Both: the sheet shows it next to the buttons that caused it, and the toast survives the sheet
            // being closed straight afterwards.
            toast.error(message);
            setError(message);
            setConfirmingReject(false);
            setPendingAction(null);
        }
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            {/* Fixed width: a Tailwind max-width class loses a cross-remote CSS specificity collision in this
                module-federation setup and silently stretches to near-full viewport width. */}
            <SheetContent side="right" style={{ maxWidth: '32rem' }}>
                <SheetHeader>
                    <SheetTitle>API promotion request</SheetTitle>
                    <SheetDescription>
                        <strong>{data.authorDisplayName}</strong>
                        {data.authorEmail ? ` (${data.authorEmail})` : ''} requested the promotion of <strong>{data.apiName}</strong> from{' '}
                        <strong>{data.sourceEnvironmentName}</strong> to <strong>{data.targetEnvironmentName}</strong>.
                    </SheetDescription>
                </SheetHeader>

                <div className="space-y-3 px-4 py-2">
                    <Alert>
                        <AlertTitle>Sharding tags</AlertTitle>
                        <AlertDescription>The sharding tags of the promotion must exist in this environment.</AlertDescription>
                    </Alert>

                    <p className="text-sm">
                        {data.isApiUpdate
                            ? `Since the API ${data.apiName} has already been promoted to ${data.targetEnvironmentName}, accepting this promotion will update it. Members and groups are not transferred.`
                            : `Accepting this promotion will create ${data.apiName} as a new, stopped and private API in ${data.targetEnvironmentName}. Members and groups are not transferred.`}
                    </p>

                    {confirmingReject && (
                        <Alert variant="warning">
                            <AlertTitle>Reject this promotion?</AlertTitle>
                            <AlertDescription>The requester will need to submit a new promotion request to try again.</AlertDescription>
                        </Alert>
                    )}

                    {error && <p className="text-sm text-destructive">{error}</p>}
                </div>

                <SheetFooter>
                    {onOpenApi && (
                        <Button type="button" variant="ghost" disabled={pendingAction !== null} onClick={onOpenApi}>
                            Open API
                        </Button>
                    )}
                    <Button type="button" variant="outline" disabled={pendingAction !== null} onClick={() => handleOpenChange(false)}>
                        Close
                    </Button>
                    {confirmingReject ? (
                        <>
                            <Button
                                type="button"
                                variant="outline"
                                disabled={pendingAction !== null}
                                onClick={() => setConfirmingReject(false)}
                            >
                                Cancel
                            </Button>
                            <Button
                                type="button"
                                variant="destructive"
                                disabled={pendingAction !== null}
                                onClick={() => void handleProcess(false)}
                            >
                                {pendingAction === 'reject' ? 'Rejecting…' : 'Confirm reject'}
                            </Button>
                        </>
                    ) : (
                        <>
                            <Button
                                type="button"
                                variant="destructive"
                                disabled={pendingAction !== null}
                                onClick={() => setConfirmingReject(true)}
                            >
                                Reject
                            </Button>
                            <Button type="button" disabled={pendingAction !== null} onClick={() => void handleProcess(true)}>
                                {pendingAction === 'accept' ? 'Accepting…' : 'Accept'}
                            </Button>
                        </>
                    )}
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
