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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { Label, Skeleton, Switch, useLayoutConfig } from '@gravitee/graphene-core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

import { CreatePlanDropdown } from './CreatePlanDropdown';
import { PlansLearningPage } from './PlansLearningPage';
import { PlansListPage } from './PlansListPage';
import { ConfirmDialog } from '../../../../../shared/components';
import { notify } from '../../../../../shared/notify';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { usePlanStatusCounts } from '../../../hooks/usePlans';
import { updateAllowMultiJwtOauth2Subscriptions } from '../../../services/apis';
import type { PlanContext } from '../../../types/plan';
import { apiDetailKeys } from '../../../utils/queryKeys';

interface PlansPageProps {
    ctx: PlanContext;
    canRead: boolean;
    canCreate: boolean;
    canUpdate: boolean;
    canDelete: boolean;
}

function AllowMultiSubscriptionsToggle({ apiId, canUpdate }: { apiId: string; canUpdate: boolean }) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { data: api } = useApiDetail(apiId);
    const [confirmOpen, setConfirmOpen] = useState(false);

    const mutation = useMutation({
        mutationFn: (allowed: boolean) => updateAllowMultiJwtOauth2Subscriptions(env?.id ?? '', apiId, allowed),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId) });
            setConfirmOpen(false);
            notify.success('Subscription setting updated');
        },
        onError: error => notify.error(error, 'Failed to update subscription setting.'),
    });

    const currentValue = api?.allowMultiJwtOauth2Subscriptions ?? false;

    function handleToggle(checked: boolean) {
        if (checked) {
            setConfirmOpen(true);
        } else {
            mutation.mutate(false);
        }
    }

    return (
        <>
            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                <div className="space-y-0.5">
                    <Label htmlFor="allow-multi-subscriptions" className="text-sm font-medium">
                        Allow multi JWT/OAuth2 subscriptions per application
                    </Label>
                    <p className="text-xs text-muted-foreground">
                        Allow an application to subscribe to more than one JWT or OAuth2 plan simultaneously.
                    </p>
                </div>
                <Switch
                    id="allow-multi-subscriptions"
                    checked={currentValue}
                    onCheckedChange={handleToggle}
                    disabled={!canUpdate || mutation.isPending || !api || !env}
                />
            </div>

            <ConfirmDialog
                open={confirmOpen}
                onOpenChange={open => {
                    if (!open && !mutation.isPending) setConfirmOpen(false);
                }}
                title="Allow multiple JWT/OAuth2 subscriptions?"
                description="By turning on this option, you will allow an application to subscribe to more than one JWT/OAuth2 plan. Be sure you understand the consequences, and you have configured Selection Rules or Sharding Tags on plans. Otherwise, it cannot be predicted which plan will be used to secure requests."
                confirmLabel="Enable"
                pendingLabel="Saving…"
                isPending={mutation.isPending}
                onConfirm={() => mutation.mutate(true)}
            />
        </>
    );
}

export function PlansPage({ ctx, canRead, canCreate, canUpdate }: Readonly<PlansPageProps>) {
    useLayoutConfig({ contentVariant: 'wide' }, []);
    const counts = usePlanStatusCounts(ctx);

    if (!canRead) {
        return (
            <div className="flex flex-col gap-6">
                <h1 className="text-2xl font-semibold tracking-tight">Plans</h1>
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view plans.</p>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-6">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Plans</h1>
                    <p className="text-sm text-muted-foreground">Manage subscription plans and their lifecycle.</p>
                </div>
                {canCreate && <CreatePlanDropdown ctx={ctx} />}
            </div>

            {/* Allow multi JWT/OAuth2 subscriptions — API only */}
            {ctx.type === 'api' && <AllowMultiSubscriptionsToggle apiId={ctx.entityId} canUpdate={canUpdate} />}

            {counts.isLoading ? (
                <div className="space-y-3">
                    <Skeleton className="h-24 w-full rounded-lg" />
                    <Skeleton className="h-64 w-full rounded-lg" />
                </div>
            ) : counts.total === 0 ? (
                <PlansLearningPage ctx={ctx} />
            ) : (
                <PlansListPage ctx={ctx} counts={counts} canUpdate={canUpdate} />
            )}
        </div>
    );
}
