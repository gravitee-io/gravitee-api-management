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
    Card,
    CardContent,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { BrainCircuitIcon, GlobeIcon, PlugIcon, UserCogIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { OverviewChecklistCard, type OverviewChecklistItem } from '../../../../shared/components/OverviewChecklistCard';
import { useChecklistOverrides } from '../../../../shared/hooks/useChecklistOverrides';
import { notify } from '../../../../shared/notify';
import { useApiProductDetailContext } from '../../../api-products/context/ApiProductDetailContext';
import { useApiProductMembers } from '../../../api-products/hooks/useApiProductMembers';
import { useAiProductSubscribersCount, useEnsureAccessPlan } from '../../hooks/useAiProductHooks';
import { type BudgetWindow, WINDOW_LABEL } from '../../services/aiProduct';

const WINDOW_OPTIONS: BudgetWindow[] = ['MINUTE', 'HOUR', 'DAY', 'WEEK', 'MONTH'];

export function AiProductOverviewPage() {
    const { productId } = useParams<{ productId: string }>();
    const { product, isLoading } = useApiProductDetailContext();
    const { data: membersData } = useApiProductMembers(productId);
    const { data: subscribersCount } = useAiProductSubscribersCount(productId);
    const { mutate: ensureAccessPlan, isPending: isPublishing } = useEnsureAccessPlan();
    const [budgetWindow, setBudgetWindow] = useState<BudgetWindow>('MONTH');

    const { overrideDone, overrideUndone, toggle } = useChecklistOverrides(productId);

    function handlePublishAccess() {
        if (!productId) return;
        ensureAccessPlan(
            { productId, window: budgetWindow },
            {
                onSuccess: () =>
                    notify.success(
                        `Access plan published (budget resets ${WINDOW_LABEL[budgetWindow]}) — this product is now available on the Developer Portal.`,
                    ),
                onError: error => notify.error(error, 'Failed to publish the access plan.'),
            },
        );
    }

    function itemDone(autoDone: boolean, id: string): boolean {
        return (autoDone && !overrideUndone.has(id)) || overrideDone.has(id);
    }

    const componentCount = product?.apiIds?.length ?? 0;
    const memberCount = membersData?.pagination?.totalCount ?? 0;

    const checklistItems: OverviewChecklistItem[] = [
        {
            id: 'add-components',
            label: 'Add LLM proxies',
            tooltip: 'Attach LLM proxies so developers can access the approved models through this product.',
            to: '../components',
            icon: BrainCircuitIcon,
            actionLabel: 'Open Components',
            done: itemDone(componentCount > 0, 'add-components'),
        },
        {
            id: 'publish-access',
            label: 'Publish access plan',
            tooltip:
                'Publish an API-key access plan so this product appears in the Developer Portal catalog. Each subscriber gets their own key; you set their token + rate limit when you approve them.',
            to: '../plans',
            icon: GlobeIcon,
            actionLabel: 'Open Plans',
            done: itemDone(false, 'publish-access'),
        },
        {
            id: 'first-subscriber',
            label: 'Approve subscribers',
            tooltip:
                'Developers request access from the portal. Approve their requests here and set each one’s personal token budget and rate limit.',
            to: '../users',
            icon: PlugIcon,
            actionLabel: 'Open Subscribers',
            done: itemDone((subscribersCount ?? 0) > 0, 'first-subscriber'),
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
                description="Finish setting up your AI product. Each row links to the right screen."
                items={checklistItems}
                onToggle={toggle}
            />

            <Card>
                <CardContent className="flex items-center justify-between gap-4 pt-6">
                    <div className="flex items-start gap-3">
                        <GlobeIcon className="size-5 text-muted-foreground shrink-0 mt-0.5" aria-hidden />
                        <div className="space-y-1">
                            <p className="text-sm font-medium">Developer Portal access</p>
                            <p className="text-xs text-muted-foreground max-w-xl">
                                Publish an API-key access plan to list this product in the portal catalog. Developers request access, then you{' '}
                                <span className="font-medium">approve each request and set their token budget + rate limit</span>. The budget
                                resets on the window you choose. A separate plan is created per window.
                            </p>
                        </div>
                    </div>
                    <div className="flex items-end gap-3 shrink-0">
                        <div className="space-y-1.5">
                            <Label htmlFor="budget-window" className="text-xs">
                                Budget resets
                            </Label>
                            <Select value={budgetWindow} onValueChange={v => setBudgetWindow(v as BudgetWindow)}>
                                <SelectTrigger id="budget-window" className="w-36">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {WINDOW_OPTIONS.map(w => (
                                        <SelectItem key={w} value={w}>
                                            {WINDOW_LABEL[w]}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <Button onClick={handlePublishAccess} disabled={isPublishing || componentCount === 0}>
                            {isPublishing ? 'Publishing…' : 'Publish access plan'}
                        </Button>
                    </div>
                </CardContent>
            </Card>

            <div className="space-y-3">
                <p className="text-sm font-medium text-muted-foreground">Product snapshot</p>
                <div className="flex gap-4">
                    <Card className="flex-1">
                        <CardContent className="pt-6">
                            <p className="text-sm text-muted-foreground mb-2">AI components</p>
                            <p className="text-2xl font-semibold tracking-tight">{componentCount}</p>
                        </CardContent>
                    </Card>
                    <Card className="flex-1">
                        <CardContent className="pt-6">
                            <p className="text-sm text-muted-foreground mb-2">Subscribers</p>
                            <p className="text-2xl font-semibold tracking-tight">{subscribersCount ?? 0}</p>
                        </CardContent>
                    </Card>
                    <Card className="flex-1">
                        <CardContent className="pt-6">
                            <p className="text-sm text-muted-foreground mb-2">Product members</p>
                            <p className="text-2xl font-semibold tracking-tight">{memberCount}</p>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    );
}
