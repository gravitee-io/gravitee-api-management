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
import { Card, CardContent } from '@gravitee/graphene-core';
import { BoxesIcon, PlugIcon, ShieldIcon, UserCogIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';

import { OverviewChecklistCard, type OverviewChecklistItem } from '../../../../shared/components/OverviewChecklistCard';
import { useChecklistOverrides } from '../../../../shared/hooks/useChecklistOverrides';
import { listPlans } from '../../../apis/services/plans';
import { apiPlanKeys } from '../../../apis/utils/queryKeys';
import { useApiProductDetailContext } from '../../context/ApiProductDetailContext';
import { useApiProductMembers } from '../../hooks/useApiProductMembers';

export function ApiProductOverviewPage() {
    const { productId } = useParams<{ productId: string }>();
    const env = useEnvironment();
    const { product, isLoading } = useApiProductDetailContext();
    const { data: membersData } = useApiProductMembers(productId);

    const { overrideDone, overrideUndone, toggle } = useChecklistOverrides(productId);

    function itemDone(autoDone: boolean, id: string): boolean {
        return (autoDone && !overrideUndone.has(id)) || overrideDone.has(id);
    }

    const apiCount = product?.apiIds?.length ?? 0;
    const memberCount = membersData?.pagination?.totalCount ?? 0;

    const plansCtx = { type: 'api-product' as const, entityId: productId ?? '' };
    const { data: plansData } = useQuery({
        queryKey: apiPlanKeys.list(env?.id ?? '', plansCtx, ['STAGING', 'PUBLISHED', 'DEPRECATED'], 1, 1),
        queryFn: () => listPlans(env!.id, plansCtx, ['STAGING', 'PUBLISHED', 'DEPRECATED'], 1, 1),
        enabled: Boolean(env && productId),
    });
    const hasPlans = (plansData?.pagination?.totalCount ?? 0) > 0;

    const checklistItems: OverviewChecklistItem[] = [
        {
            id: 'add-apis',
            label: 'Add APIs',
            tooltip: 'Attach HTTP API proxies to this product so they share documentation and access through product plans.',
            to: '../apis',
            icon: BoxesIcon,
            actionLabel: 'Open APIs',
            done: itemDone(apiCount > 0, 'add-apis'),
        },
        {
            id: 'add-plans',
            label: 'Add Plans',
            tooltip: 'Create subscription plans with security, quotas, and monetization aligned to how consumers access the bundled APIs.',
            to: '../plans',
            icon: ShieldIcon,
            actionLabel: 'Open Plans',
            done: itemDone(hasPlans, 'add-plans'),
        },
        {
            id: 'first-subscription',
            label: 'Create your first subscription',
            tooltip:
                "Applications create a subscription to a published plan to access this product's bundled APIs. Open Consumers to add subscriptions, approve requests, and manage API keys.",
            to: '../consumers',
            icon: PlugIcon,
            actionLabel: 'Open Consumers',
            done: itemDone(false, 'first-subscription'),
        },
        {
            id: 'team-access',
            label: 'Invite teammates and assign roles',
            tooltip: 'Collaborate on this product — control who can view, edit, publish plans, or own the product.',
            to: '../user-permissions',
            icon: UserCogIcon,
            actionLabel: 'Manage Access',
            done: itemDone(memberCount > 1, 'team-access'),
        },
    ];

    if (isLoading)
        return (
            <div className="p-6">
                <p className="text-sm text-muted-foreground">Loading…</p>
            </div>
        );

    return (
        <div className="space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Overview</h1>
                <p className="text-sm text-muted-foreground">
                    Onboarding checklist and snapshot for{' '}
                    <span title={product?.name && product.name.length > 40 ? product.name : undefined}>
                        {product?.name ? (product.name.length > 40 ? `${product.name.slice(0, 40).trimEnd()}…` : product.name) : '…'}
                    </span>
                    .
                </p>
            </div>

            <OverviewChecklistCard
                description="Finish setting up your API product. Each row links to the right screen."
                items={checklistItems}
                onToggle={toggle}
            />

            <div className="space-y-3">
                <p className="text-sm font-medium text-muted-foreground">Product snapshot</p>
                <div className="flex gap-4">
                    <Card className="flex-1">
                        <CardContent className="pt-6">
                            <p className="text-sm text-muted-foreground mb-2">APIs in product</p>
                            <p className="text-2xl font-semibold tracking-tight">{apiCount}</p>
                        </CardContent>
                    </Card>
                    <Card className="flex-1">
                        <CardContent className="pt-6">
                            <p className="text-sm text-muted-foreground mb-2">Active consumers</p>
                            <p className="text-2xl font-semibold tracking-tight">0</p>
                        </CardContent>
                    </Card>
                    <Card className="flex-1">
                        <CardContent className="pt-6">
                            <p className="text-sm text-muted-foreground mb-2">Total plans</p>
                            <p className="text-2xl font-semibold tracking-tight">0</p>
                        </CardContent>
                    </Card>
                    <Card className="flex-1">
                        <CardContent className="pt-6">
                            <p className="text-sm text-muted-foreground mb-2">Direct product members</p>
                            <p className="text-2xl font-semibold tracking-tight">{memberCount}</p>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    );
}
