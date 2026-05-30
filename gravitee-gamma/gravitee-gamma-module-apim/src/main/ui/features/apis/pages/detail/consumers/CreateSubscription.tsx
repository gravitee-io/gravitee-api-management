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
    Badge,
    Button,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Separator,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { ApplicationSearchList } from './ApplicationSearchList';
import { useApiPlans } from '../../../hooks/useSubscriptions';
import type { Application, Plan, SubscriptionContext } from '../../../types/subscription';

interface CreateSubscriptionProps {
    ctx: SubscriptionContext;
    open: boolean;
    isPending: boolean;
    error: string | null;
    onConfirm: (applicationId: string, planId: string) => void;
    onClose: () => void;
}

function SubscriptionSummary({ app, plan }: { app: Application; plan: Plan }) {
    return (
        <div className="rounded-lg border p-4 space-y-3">
            <p className="text-sm font-medium">Subscription Summary</p>
            <dl className="space-y-3 text-sm">
                <div className="min-w-0">
                    <dt className="text-xs text-muted-foreground">Application</dt>
                    <dd className="font-medium" style={{ overflowWrap: 'anywhere' }}>
                        {app.name}
                    </dd>
                </div>
                <div className="min-w-0">
                    <dt className="text-xs text-muted-foreground">Plan</dt>
                    <dd className="flex min-w-0 flex-wrap items-center gap-1.5">
                        <span className="min-w-0 font-medium" style={{ overflowWrap: 'anywhere' }}>
                            {plan.name}
                        </span>
                        {plan.security?.type && (
                            <Badge variant="secondary" className="shrink-0 text-xs">
                                {plan.security.type}
                            </Badge>
                        )}
                    </dd>
                </div>
                {app.primaryOwner?.displayName && (
                    <div className="min-w-0">
                        <dt className="text-xs text-muted-foreground">Owner</dt>
                        <dd style={{ overflowWrap: 'anywhere' }}>{app.primaryOwner.displayName}</dd>
                    </div>
                )}
                {app.type && (
                    <div className="min-w-0">
                        <dt className="text-xs text-muted-foreground">Type</dt>
                        <dd>
                            <Badge variant="secondary" className="text-xs">
                                {app.type}
                            </Badge>
                        </dd>
                    </div>
                )}
            </dl>
        </div>
    );
}

export function CreateSubscription({ ctx, open, isPending, error, onConfirm, onClose }: Readonly<CreateSubscriptionProps>) {
    const [selectedApp, setSelectedApp] = useState<Application | null>(null);
    const [selectedPlanId, setSelectedPlanId] = useState('');

    const entityLabel = ctx.type === 'api-product' ? 'API product' : 'API';
    const { data: plans = [], isLoading: isLoadingPlans } = useApiPlans(ctx);

    const onlyKeyless = !isLoadingPlans && plans.length === 0;
    const selectedPlan = plans.find(p => p.id === selectedPlanId) ?? null;
    const canSubmit = Boolean(selectedApp && selectedPlanId && !onlyKeyless);

    const handleClose = useCallback(() => {
        setSelectedApp(null);
        setSelectedPlanId('');
        onClose();
    }, [onClose]);

    const handleConfirm = useCallback(() => {
        if (!canSubmit || !selectedApp) return;
        onConfirm(selectedApp.id, selectedPlanId);
    }, [canSubmit, selectedApp, selectedPlanId, onConfirm]);

    return (
        <Sheet open={open} onOpenChange={open ? handleClose : undefined}>
            <SheetContent side="right" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Create Subscription</SheetTitle>
                    <SheetDescription>
                        Select an environment-level application and assign a plan to subscribe it to this {entityLabel}.
                    </SheetDescription>
                </SheetHeader>

                <div className="space-y-5 px-4" style={{ overflowY: 'auto', flex: 1, minHeight: 0 }}>
                    <div className="space-y-2">
                        <Label>Select Application</Label>
                        <ApplicationSearchList selected={selectedApp} onSelect={app => setSelectedApp(app)} />
                    </div>

                    <Separator />

                    <div className="space-y-2">
                        <Label htmlFor="sub-plan">Subscription Plan</Label>
                        <Select value={selectedPlanId} onValueChange={setSelectedPlanId} disabled={isLoadingPlans || onlyKeyless}>
                            <SelectTrigger id="sub-plan" className="w-full">
                                <SelectValue
                                    placeholder={
                                        isLoadingPlans ? 'Loading plans…' : onlyKeyless ? 'No subscribable plans' : 'Select a plan'
                                    }
                                />
                            </SelectTrigger>
                            <SelectContent>
                                {plans.map(p => (
                                    <SelectItem key={p.id} value={p.id}>
                                        {p.name}
                                        {p.description ? ` — ${p.description}` : ''}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        {onlyKeyless && (
                            <p className="text-xs text-muted-foreground">
                                This API only offers keyless plans. Subscriptions are not required.
                            </p>
                        )}
                        {!onlyKeyless && (
                            <p className="text-xs text-muted-foreground">
                                Plans define rate limits, quotas, and access policies for this subscription.
                            </p>
                        )}
                    </div>

                    {selectedApp && selectedPlan && <SubscriptionSummary app={selectedApp} plan={selectedPlan} />}

                    {error && (
                        <Alert variant="destructive">
                            <AlertDescription>{error}</AlertDescription>
                        </Alert>
                    )}
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleConfirm} disabled={!canSubmit || isPending}>
                        {!isPending && <PlusIcon className="size-4" aria-hidden />}
                        {isPending ? 'Creating…' : 'Create subscription'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
