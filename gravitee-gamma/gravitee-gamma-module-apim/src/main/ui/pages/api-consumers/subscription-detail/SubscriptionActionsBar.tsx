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
} from '@gravitee/graphene-core';
import { ArrowRightIcon, CalendarIcon, CircleStopIcon, CircleXIcon, PlayIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';

import {
    useCloseSubscription,
    usePauseSubscription,
    useResumeSubscription,
    useTransferSubscription,
    useUpdateSubscriptionEndDate,
} from '../../../features/apis/hooks/useSubscriptionActions';
import { useApiPlans } from '../../../features/apis/hooks/useSubscriptions';
import type { Subscription, SubscriptionContext } from '../../../features/apis/types/subscription';

type PendingAction = 'pause' | 'resume' | 'close' | null;

interface SubscriptionActionsBarProps {
    ctx: SubscriptionContext;
    subscription: Subscription;
    canUpdate: boolean;
    canDelete: boolean;
}

export function SubscriptionActionsBar({ ctx, subscription, canUpdate, canDelete }: Readonly<SubscriptionActionsBarProps>) {
    const [pending, setPending] = useState<PendingAction>(null);
    const [transferOpen, setTransferOpen] = useState(false);
    const [endDateOpen, setEndDateOpen] = useState(false);
    const [selectedPlanId, setSelectedPlanId] = useState('');
    const [endDate, setEndDate] = useState('');

    const { data: plans = [] } = useApiPlans(ctx);
    const transferPlans = useMemo(
        () => plans.filter(p => p.id !== subscription.plan.id && p.security?.type === subscription.plan.security?.type),
        [plans, subscription.plan.id, subscription.plan.security?.type],
    );

    const pauseMutation = usePauseSubscription(ctx, subscription.id);
    const resumeMutation = useResumeSubscription(ctx, subscription.id);
    const closeMutation = useCloseSubscription(ctx, subscription.id);
    const transferMutation = useTransferSubscription(ctx, subscription.id);
    const endDateMutation = useUpdateSubscriptionEndDate(ctx, subscription.id);

    const isKubernetes = subscription.origin === 'KUBERNETES';
    const { status } = subscription;

    const confirmAction = useCallback(() => {
        if (pending === 'pause') pauseMutation.mutate(undefined, { onSuccess: () => setPending(null) });
        else if (pending === 'resume') resumeMutation.mutate(undefined, { onSuccess: () => setPending(null) });
        else if (pending === 'close') closeMutation.mutate(undefined, { onSuccess: () => setPending(null) });
    }, [pending, pauseMutation, resumeMutation, closeMutation]);

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

    const actionError =
        pauseMutation.error ?? resumeMutation.error ?? closeMutation.error ?? transferMutation.error ?? endDateMutation.error;

    if (isKubernetes) {
        return (
            <Alert>
                <AlertDescription>This subscription is managed by Kubernetes and cannot be modified here.</AlertDescription>
            </Alert>
        );
    }

    return (
        <div className="space-y-3">
            <div className="flex flex-wrap gap-2">
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
                    <Button type="button" variant="outline" size="sm" onClick={() => setPending('pause')}>
                        <CircleStopIcon className="size-3.5" aria-hidden />
                        Pause
                    </Button>
                )}
                {canUpdate && status === 'PAUSED' && (
                    <Button type="button" variant="outline" size="sm" onClick={() => setPending('resume')}>
                        <PlayIcon className="size-3.5" aria-hidden />
                        Resume
                    </Button>
                )}
                {canDelete && (status === 'ACCEPTED' || status === 'PAUSED' || status === 'PENDING') && (
                    <Button type="button" variant="destructive" size="sm" onClick={() => setPending('close')}>
                        <CircleXIcon className="size-3.5" aria-hidden />
                        Close subscription
                    </Button>
                )}
            </div>

            {pending && (
                <div
                    className="flex items-center gap-3 rounded-lg px-4 py-3 text-sm"
                    style={{
                        backgroundColor:
                            pending === 'close'
                                ? 'color-mix(in oklab, var(--color-destructive) 8%, transparent)'
                                : 'color-mix(in oklab, var(--color-muted) 60%, transparent)',
                        border: `1px solid ${pending === 'close' ? 'color-mix(in oklab, var(--color-destructive) 20%, transparent)' : 'var(--color-border)'}`,
                    }}
                >
                    <p className="flex-1">
                        {pending === 'pause' && 'Pausing will suspend access for this consumer. Confirm?'}
                        {pending === 'resume' && 'The consumer will regain access to the API. Confirm?'}
                        {pending === 'close' &&
                            'This will permanently close the subscription and invalidate all associated API keys. Confirm?'}
                    </p>
                    <Button type="button" variant="ghost" size="sm" onClick={() => setPending(null)}>
                        Cancel
                    </Button>
                    <Button
                        type="button"
                        variant={pending === 'close' ? 'destructive' : 'default'}
                        size="sm"
                        onClick={confirmAction}
                        disabled={pauseMutation.isPending || resumeMutation.isPending || closeMutation.isPending}
                    >
                        Confirm
                    </Button>
                </div>
            )}

            {actionError && (
                <Alert variant="destructive">
                    <AlertDescription>{actionError?.message}</AlertDescription>
                </Alert>
            )}

            {/* Transfer dialog */}
            <Dialog open={transferOpen} onOpenChange={setTransferOpen}>
                <DialogContent style={{ maxWidth: '384px', width: '90vw' }}>
                    <DialogHeader>
                        <DialogTitle>Transfer Subscription</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-2 py-2">
                        <Label htmlFor="transfer-plan">New Plan</Label>
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
