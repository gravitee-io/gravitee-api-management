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
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Separator,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { ApplicationSearchList } from './ApplicationSearchList';
import { useApiPlans } from '../../../hooks/useSubscriptions';
import type { Application, Plan, SubscriptionContext } from '../../../types/subscription';

interface CreateSubscriptionDialogProps {
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
            <div className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
                <div>
                    <p className="text-xs text-muted-foreground">Application</p>
                    <p className="font-medium">{app.name}</p>
                </div>
                <div>
                    <p className="text-xs text-muted-foreground">Plan</p>
                    <div className="flex items-center gap-1.5">
                        <p className="font-medium">{plan.name}</p>
                        {plan.security?.type && (
                            <Badge variant="secondary" className="text-xs">
                                {plan.security.type}
                            </Badge>
                        )}
                    </div>
                </div>
                {app.primaryOwner?.displayName && (
                    <div>
                        <p className="text-xs text-muted-foreground">Owner</p>
                        <p>{app.primaryOwner.displayName}</p>
                    </div>
                )}
                {app.type && (
                    <div>
                        <p className="text-xs text-muted-foreground">Type</p>
                        <Badge variant="secondary" className="text-xs">
                            {app.type}
                        </Badge>
                    </div>
                )}
            </div>
        </div>
    );
}

export function CreateSubscriptionDialog({ ctx, open, isPending, error, onConfirm, onClose }: Readonly<CreateSubscriptionDialogProps>) {
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
        <Dialog open={open} onOpenChange={open ? handleClose : undefined}>
            <DialogContent style={{ maxWidth: '480px', width: '90vw', maxHeight: '85vh', display: 'flex', flexDirection: 'column' }}>
                <DialogHeader>
                    <DialogTitle>Create Subscription</DialogTitle>
                    <p className="text-sm text-muted-foreground">
                        Select an environment-level application and assign a plan to subscribe it to this {entityLabel}.
                    </p>
                </DialogHeader>

                <div className="space-y-5 py-2" style={{ overflowY: 'auto', flex: 1, minHeight: 0 }}>
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

                <DialogFooter>
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleConfirm} disabled={!canSubmit || isPending}>
                        {!isPending && <PlusIcon className="size-4" aria-hidden />}
                        {isPending ? 'Creating…' : 'Create subscription'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
