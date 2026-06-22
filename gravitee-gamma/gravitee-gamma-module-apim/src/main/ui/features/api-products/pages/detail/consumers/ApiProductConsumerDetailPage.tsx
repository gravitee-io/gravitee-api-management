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
import { Button, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useNavigate, useParams } from 'react-router-dom';

import { SubscriptionActionsBar } from './api-consumers/subscription-detail/SubscriptionActionsBar';
import { SubscriptionApiKeysCard } from './api-consumers/subscription-detail/SubscriptionApiKeysCard';
import { SubscriptionInfoCard } from './api-consumers/subscription-detail/SubscriptionInfoCard';
import { useSubscriptionDetail } from '../../../../apis/hooks/useSubscriptions';
import type { SubscriptionContext } from '../../../../apis/types/subscription';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';

export function ApiProductConsumerDetailPage() {
    const { productId, subscriptionId } = useParams<{ productId: string; subscriptionId: string }>();
    const navigate = useNavigate();
    const ctx: SubscriptionContext = { type: 'api-product', entityId: productId ?? '' };
    const { canUpdate, canDelete, isLoading: permissionsLoading } = useApiProductResourcePermissions(productId, 'subscription');
    const { data: subscription, isLoading: subscriptionLoading, isError } = useSubscriptionDetail(ctx, subscriptionId);

    if (permissionsLoading) {
        return (
            <div className="space-y-3 p-6">
                <Skeleton className="h-24 w-full rounded-lg" />
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-6 p-6">
            <div className="flex items-center gap-3">
                <Button type="button" variant="ghost" size="sm" className="gap-1.5" onClick={() => navigate('..')}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Go back to your subscriptions
                </Button>
            </div>

            {isError && (
                <p className="text-sm text-muted-foreground">
                    Failed to load subscription. It may have been deleted or you may not have access.
                </p>
            )}

            {subscriptionLoading && (
                <div className="space-y-4">
                    <Skeleton className="h-6 w-48 rounded" />
                    <Skeleton className="h-64 w-full rounded-xl" />
                </div>
            )}

            {subscription && (
                <>
                    <div className="space-y-1">
                        <h1 className="text-2xl font-semibold tracking-tight">{subscription.application.name}</h1>
                        <p className="text-sm text-muted-foreground">
                            Plan: {subscription.plan.name}
                            {subscription.plan.security?.type ? ` (${subscription.plan.security.type})` : ''}
                        </p>
                    </div>

                    {(canUpdate || canDelete) && (
                        <SubscriptionActionsBar ctx={ctx} subscription={subscription} canUpdate={canUpdate} canDelete={canDelete} />
                    )}

                    <SubscriptionInfoCard subscription={subscription} isLoading={false} />

                    <SubscriptionApiKeysCard ctx={ctx} subscription={subscription} canUpdate={canUpdate} />
                </>
            )}
        </div>
    );
}
