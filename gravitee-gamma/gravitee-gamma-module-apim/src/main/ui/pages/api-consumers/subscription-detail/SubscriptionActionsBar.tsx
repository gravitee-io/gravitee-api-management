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
    Alert,
    AlertDescription,
    Button,
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Textarea,
} from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    CalendarIcon,
    CircleStopIcon,
    CircleXIcon,
    PlayIcon,
    TriangleAlertIcon,
    ZapIcon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';

import { SubscriptionApproveDialog } from './SubscriptionApproveDialog';
import {
    useApproveSubscription,
    useCloseSubscription,
    usePauseSubscription,
    useRejectSubscription,
    useResumeFailedSubscription,
    useResumeSubscription,
    useTransferSubscription,
    useUpdateSubscriptionEndDate,
} from '../../../features/apis/hooks/useSubscriptionActions';
import { useApiPlans } from '../../../features/apis/hooks/useSubscriptions';
import type { ApproveSubscriptionPayload, Subscription, SubscriptionContext } from '../../../features/apis/types/subscription';

type InlineAction = 'pause' | 'resume' | 'close' | 'resumeFailure' | null;

interface SubscriptionActionsBarProps {
    ctx: SubscriptionContext;
    subscription: Subscription;
    canUpdate: boolean;
    canDelete: boolean;
}

export function SubscriptionActionsBar({ ctx, subscription, canUpdate, canDelete }: Readonly<SubscriptionActionsBarProps>) {
    const [inlineAction, setInlineAction] = useState<InlineAction>(null);
    const [approveOpen, setApproveOpen] = useState(false);
    const [rejectOpen, setRejectOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const [transferOpen, setTransferOpen] = useState(false);
    const [endDateOpen, setEndDateOpen] = useState(false);
    const [selectedPlanId, setSelectedPlanId] = useState('');
    const [endDate, setEndDate] = useState('');

    const { data: plans = [] } = useApiPlans(ctx);
    const transferPlans = useMemo(
        () => plans.filter(p => p.id !== subscription.plan.id && p.security?.type === subscription.plan.security?.type),
        [plans, subscription.plan.id, subscription.plan.security?.type],
    );

    const approveMutation = useApproveSubscription(ctx, subscription.id);
    const rejectMutation = useRejectSubscription(ctx, subscription.id);
    const pauseMutation = usePauseSubscription(ctx, subscription.id);
    const resumeMutation = useResumeSubscription(ctx, subscription.id);
    const resumeFailedMutation = useResumeFailedSubscription(ctx, subscription.id);
    const closeMutation = useCloseSubscription(ctx, subscription.id);
    const transferMutation = useTransferSubscription(ctx, subscription.id);
    const endDateMutation = useUpdateSubscriptionEndDate(ctx, subscription.id);

    const isKubernetes = subscription.origin === 'KUBERNETES';
    const { status, consumerStatus } = subscription;
    const isApiKeyPlan = subscription.plan.security?.type === 'API_KEY';

    const anyInlinePending =
        pauseMutation.isPending || resumeMutation.isPending || closeMutation.isPending || resumeFailedMutation.isPending;

    const inlineActionError = pauseMutation.error ?? resumeMutation.error ?? closeMutation.error ?? resumeFailedMutation.error ?? null;

    const confirmInline = useCallback(() => {
        if (inlineAction === 'pause') pauseMutation.mutate(undefined, { onSuccess: () => setInlineAction(null) });
        else if (inlineAction === 'resume') resumeMutation.mutate(undefined, { onSuccess: () => setInlineAction(null) });
        else if (inlineAction === 'close') closeMutation.mutate(undefined, { onSuccess: () => setInlineAction(null) });
        else if (inlineAction === 'resumeFailure') resumeFailedMutation.mutate(undefined, { onSuccess: () => setInlineAction(null) });
    }, [inlineAction, pauseMutation, resumeMutation, closeMutation, resumeFailedMutation]);

    const handleApprove = useCallback(
        (payload: ApproveSubscriptionPayload) => {
            approveMutation.mutate(payload, { onSuccess: () => setApproveOpen(false) });
        },
        [approveMutation],
    );

    const handleApproveClose = useCallback(() => {
        approveMutation.reset();
        setApproveOpen(false);
    }, [approveMutation]);

    const handleReject = useCallback(() => {
        if (!rejectReason.trim()) return;
        rejectMutation.mutate(rejectReason.trim(), {
            onSuccess: () => {
                setRejectOpen(false);
                setRejectReason('');
            },
        });
    }, [rejectReason, rejectMutation]);

    const handleRejectClose = useCallback(() => {
        rejectMutation.reset();
        setRejectReason('');
        setRejectOpen(false);
    }, [rejectMutation]);

    const handleTransfer = useCallback(() => {
        if (!selectedPlanId) return;
        transferMutation.mutate(selectedPlanId, {
            onSuccess: () => {
                setTransferOpen(false);
                setSelectedPlanId('');
            },
        });
    }, [selectedPlanId, transferMutation]);

    const handleEndDate = useCallback(() => {
        endDateMutation.mutate(endDate || null, {
            onSuccess: () => {
                setEndDateOpen(false);
                setEndDate('');
            },
        });
    }, [endDate, endDateMutation]);

    if (isKubernetes) {
        return (
            <Alert>
                <AlertDescription>This subscription is managed by Kubernetes and cannot be modified here.</AlertDescription>
            </Alert>
        );
    }

    return (
        <div className="space-y-3">
            {/* Action buttons */}
            <div className="flex flex-wrap gap-2">
                {/* PENDING actions */}
                {canUpdate && status === 'PENDING' && (
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="text-success border-success/40 hover:text-success"
                        onClick={() => setApproveOpen(true)}
                    >
                        <ZapIcon className="size-3.5" aria-hidden />
                        Approve
                    </Button>
                )}
                {canUpdate && status === 'PENDING' && (
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="text-destructive border-destructive/40 hover:text-destructive"
                        onClick={() => setRejectOpen(true)}
                    >
                        <CircleXIcon className="size-3.5" aria-hidden />
                        Reject
                    </Button>
                )}

                {/* ACCEPTED actions */}
                {canUpdate && status === 'ACCEPTED' && (
                    <Button type="button" variant="outline" size="sm" onClick={() => setTransferOpen(true)}>
                        <ArrowRightIcon className="size-3.5" aria-hidden />
                        Transfer
                    </Button>
                )}
                {canUpdate && (status === 'ACCEPTED' || status === 'PAUSED') && (
                    <Button type="button" variant="outline" size="sm" onClick={() => setEndDateOpen(true)}>
                        <CalendarIcon className="size-3.5" aria-hidden />
                        Change end date
                    </Button>
                )}
                {canUpdate && status === 'ACCEPTED' && (
                    <Button type="button" variant="outline" size="sm" onClick={() => setInlineAction('pause')}>
                        <CircleStopIcon className="size-3.5" aria-hidden />
                        Pause
                    </Button>
                )}

                {/* PAUSED actions */}
                {canUpdate && status === 'PAUSED' && (
                    <Button type="button" variant="outline" size="sm" onClick={() => setInlineAction('resume')}>
                        <PlayIcon className="size-3.5" aria-hidden />
                        Resume
                    </Button>
                )}

                {/* FAILURE recovery */}
                {canUpdate && consumerStatus === 'FAILURE' && (
                    <Button type="button" variant="outline" size="sm" onClick={() => setInlineAction('resumeFailure')}>
                        <TriangleAlertIcon className="size-3.5" aria-hidden />
                        Resume after failure
                    </Button>
                )}

                {/* Close — available for non-terminal states */}
                {canDelete && (status === 'ACCEPTED' || status === 'PAUSED' || status === 'PENDING') && (
                    <Button type="button" variant="destructive" size="sm" onClick={() => setInlineAction('close')}>
                        <CircleXIcon className="size-3.5" aria-hidden />
                        Close subscription
                    </Button>
                )}
            </div>

            {/* Inline confirmation strip */}
            {inlineAction && (
                <div
                    className="flex items-center gap-3 rounded-lg px-4 py-3 text-sm"
                    style={{
                        backgroundColor:
                            inlineAction === 'close'
                                ? 'color-mix(in oklab, var(--color-destructive) 8%, transparent)'
                                : 'color-mix(in oklab, var(--color-muted) 60%, transparent)',
                        border: `1px solid ${inlineAction === 'close' ? 'color-mix(in oklab, var(--color-destructive) 20%, transparent)' : 'var(--color-border)'}`,
                    }}
                >
                    <p className="flex-1">
                        {inlineAction === 'pause' && 'Pausing will suspend access for this consumer. Confirm?'}
                        {inlineAction === 'resume' && 'The consumer will regain access to the API. Confirm?'}
                        {inlineAction === 'resumeFailure' &&
                            'Resume this subscription after its failure? The consumer will regain access. Confirm?'}
                        {inlineAction === 'close' &&
                            'This will permanently close the subscription and invalidate all associated API keys. Confirm?'}
                    </p>
                    <Button type="button" variant="ghost" size="sm" onClick={() => setInlineAction(null)}>
                        Cancel
                    </Button>
                    <Button
                        type="button"
                        variant={inlineAction === 'close' ? 'destructive' : 'default'}
                        size="sm"
                        onClick={confirmInline}
                        disabled={anyInlinePending}
                    >
                        Confirm
                    </Button>
                </div>
            )}

            {inlineActionError && (
                <Alert variant="destructive">
                    <AlertDescription>{inlineActionError.message}</AlertDescription>
                </Alert>
            )}

            {/* Approve dialog */}
            <SubscriptionApproveDialog
                open={approveOpen}
                isApiKeyPlan={isApiKeyPlan}
                isPending={approveMutation.isPending}
                error={approveMutation.error?.message ?? null}
                onConfirm={handleApprove}
                onClose={handleApproveClose}
            />

            {/* Reject dialog — simple reason field */}
            <Dialog open={rejectOpen} onOpenChange={rejectOpen ? handleRejectClose : undefined}>
                <DialogContent style={{ maxWidth: '400px', width: '90vw' }}>
                    <DialogHeader>
                        <DialogTitle>Reject Subscription</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-2 py-2">
                        <Label htmlFor="reject-reason">Reason *</Label>
                        <Textarea
                            id="reject-reason"
                            value={rejectReason}
                            onChange={e => setRejectReason(e.target.value)}
                            placeholder="Explain why this subscription is rejected…"
                            rows={3}
                        />
                        {rejectMutation.error && (
                            <Alert variant="destructive">
                                <AlertDescription>{rejectMutation.error.message}</AlertDescription>
                            </Alert>
                        )}
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={handleRejectClose} disabled={rejectMutation.isPending}>
                            Cancel
                        </Button>
                        <Button
                            type="button"
                            variant="destructive"
                            onClick={handleReject}
                            disabled={!rejectReason.trim() || rejectMutation.isPending}
                        >
                            {rejectMutation.isPending ? 'Rejecting…' : 'Reject'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Transfer dialog */}
            <Dialog open={transferOpen} onOpenChange={setTransferOpen}>
                <DialogContent style={{ maxWidth: '384px', width: '90vw' }}>
                    <DialogHeader>
                        <DialogTitle>Transfer Subscription</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-2 py-2">
                        <Label htmlFor="transfer-plan">New plan</Label>
                        <Select value={selectedPlanId} onValueChange={setSelectedPlanId}>
                            <SelectTrigger id="transfer-plan">
                                <SelectValue placeholder="Select a plan" />
                            </SelectTrigger>
                            <SelectContent>
                                {transferPlans.map(p => (
                                    <SelectItem key={p.id} value={p.id}>
                                        {p.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        {transferPlans.length === 0 && (
                            <p className="text-xs text-muted-foreground">No compatible plans available for transfer.</p>
                        )}
                        {transferMutation.error && (
                            <Alert variant="destructive">
                                <AlertDescription>{transferMutation.error.message}</AlertDescription>
                            </Alert>
                        )}
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => setTransferOpen(false)}>
                            Cancel
                        </Button>
                        <Button type="button" onClick={handleTransfer} disabled={!selectedPlanId || transferMutation.isPending}>
                            {transferMutation.isPending ? 'Transferring…' : 'Transfer'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Change end date dialog */}
            <Dialog open={endDateOpen} onOpenChange={setEndDateOpen}>
                <DialogContent style={{ maxWidth: '384px', width: '90vw' }}>
                    <DialogHeader>
                        <DialogTitle>Change End Date</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-2 py-2">
                        <Label htmlFor="end-date">End date (leave empty to remove)</Label>
                        <Input id="end-date" type="date" value={endDate} onChange={e => setEndDate(e.target.value)} />
                        {endDateMutation.error && (
                            <Alert variant="destructive">
                                <AlertDescription>{endDateMutation.error.message}</AlertDescription>
                            </Alert>
                        )}
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => setEndDateOpen(false)}>
                            Cancel
                        </Button>
                        <Button type="button" onClick={handleEndDate} disabled={endDateMutation.isPending}>
                            {endDateMutation.isPending ? 'Saving…' : 'Save'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
