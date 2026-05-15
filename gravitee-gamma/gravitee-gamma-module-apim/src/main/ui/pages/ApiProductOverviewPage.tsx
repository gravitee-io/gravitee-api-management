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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    BoxesIcon,
    ChevronDownIcon,
    ChevronUpIcon,
    CircleCheckIcon,
    InfoIcon,
    PlugIcon,
    ShieldIcon,
    UserCogIcon,
} from '@gravitee/graphene-core/icons';
import { type ComponentType, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { useApiProductDetailContext } from '../features/api-products/context/ApiProductDetailContext';
import { useApiProductMembers } from '../features/api-products/hooks/useApiProductMembers';

interface ChecklistItem {
    id: string;
    label: string;
    tooltip: string;
    to: string;
    icon: ComponentType<{ className?: string }>;
    actionLabel: string;
    done: boolean;
}

function CircularProgress({ pct }: { pct: number }) {
    const r = 42;
    const circumference = 2 * Math.PI * r;
    return (
        <div className="relative w-24 h-24">
            <svg viewBox="0 0 100 100" aria-hidden className="w-24 h-24 -rotate-90 text-primary">
                <circle
                    cx="50"
                    cy="50"
                    r={r}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="8"
                    className="text-muted-foreground opacity-15"
                />
                {/* Progress ring */}
                <circle
                    cx="50"
                    cy="50"
                    r={r}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="8"
                    strokeLinecap="round"
                    strokeDasharray={circumference}
                    strokeDashoffset={circumference * (1 - pct / 100)}
                    style={{ transition: 'stroke-dashoffset 0.5s ease' }}
                />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center" aria-label={`${pct}% complete`}>
                <span className="text-xl font-bold">{pct}%</span>
            </div>
        </div>
    );
}

export function ApiProductOverviewPage() {
    const { productId } = useParams<{ productId: string }>();
    const { product, isLoading } = useApiProductDetailContext();
    const { data: membersData } = useApiProductMembers(productId);

    const [checklistOpen, setChecklistOpen] = useState(true);

    const apiCount = product?.apiIds?.length ?? 0;
    const memberCount = membersData?.pagination?.totalCount ?? 0;

    const checklistItems = useMemo<ChecklistItem[]>(
        () => [
            {
                id: 'add-apis',
                label: 'Add APIs',
                tooltip: 'Attach HTTP API proxies to this product so they share documentation and access through product plans.',
                to: '../apis',
                icon: BoxesIcon,
                actionLabel: 'Open APIs',
                done: apiCount > 0,
            },
            {
                id: 'add-plans',
                label: 'Add Plans',
                tooltip:
                    'Create subscription plans with security, quotas, and monetization aligned to how consumers access the bundled APIs.',
                to: '../plans',
                icon: ShieldIcon,
                actionLabel: 'Open Plans',
                done: false,
            },
            {
                id: 'first-subscription',
                label: 'Create your first subscription',
                tooltip:
                    "Applications create a subscription to a published plan to access this product's bundled APIs. Open Consumers to add subscriptions, approve requests, and manage API keys.",
                to: '../consumers',
                icon: PlugIcon,
                actionLabel: 'Open Consumers',
                done: false,
            },
            {
                id: 'team-access',
                label: 'Invite teammates and assign roles',
                tooltip: 'Collaborate on this product — control who can view, edit, publish plans, or own the product.',
                to: '../user-permissions',
                icon: UserCogIcon,
                actionLabel: 'Manage Access',
                done: memberCount > 1,
            },
        ],
        [apiCount, memberCount],
    );

    const completedCount = checklistItems.filter(i => i.done).length;
    const totalCount = checklistItems.length;
    const completionPct = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

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
                <p className="text-sm text-muted-foreground">Onboarding checklist and snapshot for {product?.name ?? '…'}.</p>
            </div>

            {/* Checklist card */}
            <Card>
                <CardHeader className="pb-0">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="rounded-lg bg-primary/10 p-2">
                                <CircleCheckIcon className="size-4 text-primary" aria-hidden />
                            </div>
                            <div>
                                <CardTitle className="text-base">Checklist</CardTitle>
                                <CardDescription className="mt-0.5">
                                    Finish setting up your API product. Each row links to the right screen.
                                </CardDescription>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <span className="text-sm font-semibold text-muted-foreground">
                                {completedCount}/{totalCount}
                            </span>
                            <button
                                type="button"
                                onClick={() => setChecklistOpen(o => !o)}
                                className="text-muted-foreground hover:text-foreground transition-colors"
                                aria-label={checklistOpen ? 'Collapse checklist' : 'Expand checklist'}
                            >
                                {checklistOpen ? (
                                    <ChevronUpIcon className="size-5" aria-hidden />
                                ) : (
                                    <ChevronDownIcon className="size-5" aria-hidden />
                                )}
                            </button>
                        </div>
                    </div>
                </CardHeader>

                {checklistOpen ? (
                    <CardContent className="pt-4">
                        <div className="flex flex-row gap-8">
                            {/* Checklist rows */}
                            <div className="flex-1 space-y-1 min-w-0">
                                <TooltipProvider delayDuration={200}>
                                    {checklistItems.map(item => {
                                        const ItemIcon = item.icon;
                                        return (
                                            <div
                                                key={item.id}
                                                className={`flex items-center gap-3 rounded-lg px-3 py-2.5 transition-colors${item.done ? ' opacity-60' : ' hover:bg-accent/50'}`}
                                            >
                                                <div className="shrink-0">
                                                    {item.done ? (
                                                        <CircleCheckIcon className="size-4 text-success" aria-hidden />
                                                    ) : (
                                                        <div className="size-4 rounded border-2 border-muted-foreground/30" />
                                                    )}
                                                </div>
                                                <ItemIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                                <Link
                                                    to={item.to}
                                                    className={`text-sm flex-1 min-w-0 text-foreground hover:underline underline-offset-2${item.done ? ' line-through text-muted-foreground' : ''}`}
                                                >
                                                    {item.label}
                                                </Link>
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <button
                                                            type="button"
                                                            className="text-muted-foreground/40 hover:text-muted-foreground transition-colors shrink-0"
                                                            onClick={e => e.stopPropagation()}
                                                            aria-label={`Info: ${item.label}`}
                                                        >
                                                            <InfoIcon className="size-3.5" aria-hidden />
                                                        </button>
                                                    </TooltipTrigger>
                                                    <TooltipContent side="top" className="max-w-xs text-xs">
                                                        {item.tooltip}
                                                    </TooltipContent>
                                                </Tooltip>
                                                <Link
                                                    to={item.to}
                                                    className="shrink-0 inline-flex items-center gap-1 rounded-md px-1.5 py-1 text-xs font-medium text-primary hover:bg-primary/10 transition-colors"
                                                    aria-label={item.actionLabel}
                                                >
                                                    {item.actionLabel}
                                                    <ArrowRightIcon className="size-4 shrink-0" aria-hidden />
                                                </Link>
                                            </div>
                                        );
                                    })}
                                </TooltipProvider>
                            </div>

                            {/* Circular progress — always side-by-side with checklist rows */}
                            <div className="flex flex-col items-center justify-center gap-2 border-l pl-8 pr-2 shrink-0">
                                <CircularProgress pct={completionPct} />
                                <span className="text-xs text-muted-foreground text-center">Completion</span>
                            </div>
                        </div>
                    </CardContent>
                ) : null}
            </Card>

            {/* Product snapshot */}
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
