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
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    toast,
} from '@gravitee/graphene-core';
import { useState } from 'react';

import type { PromotionReviewData } from '../tasks.types';

export function PromotionReviewDialog({
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

    function handleOpenChange(nextOpen: boolean) {
        onOpenChange(nextOpen);
        if (!nextOpen) {
            setError(null);
            setConfirmingReject(false);
        }
    }

    async function handleProcess(accepted: boolean) {
        if (!onProcess) return;
        setPendingAction(accepted ? 'accept' : 'reject');
        setError(null);
        try {
            await onProcess(data.promotionId, accepted);
            toast.success(accepted ? 'API promotion accepted.' : 'API promotion rejected.');
            handleOpenChange(false);
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to process the promotion.');
            setConfirmingReject(false);
        } finally {
            setPendingAction(null);
        }
    }

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            {/* Fixed width: sm:max-w-lg loses a cross-remote CSS specificity collision in this
                module-federation setup and silently stretches to near-full viewport width. */}
            <DialogContent style={{ maxWidth: '32rem' }}>
                <DialogHeader>
                    <DialogTitle>API promotion request</DialogTitle>
                    <DialogDescription>
                        <strong>{data.authorDisplayName}</strong> requested the promotion of <strong>{data.apiName}</strong> from{' '}
                        <strong>{data.sourceEnvironmentName}</strong> to <strong>{data.targetEnvironmentName}</strong>.
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-3 py-2">
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

                <DialogFooter>
                    {onOpenApi && (
                        <Button type="button" variant="ghost" onClick={onOpenApi}>
                            Open API
                        </Button>
                    )}
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Close
                        </Button>
                    </DialogClose>
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
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
