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
import { Badge, Button, Card, CardContent } from '@gravitee/graphene-core';
import {
    ArrowDownIcon,
    ArrowRightIcon,
    BoxesIcon,
    DollarSignIcon,
    GlobeIcon,
    PlusIcon,
    RadioIcon,
    UsersIcon,
} from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import { FeatureTile } from '../../shared/components';

const SAMPLE_APIS: { readonly Icon: LucideIcon; readonly name: string }[] = [
    { Icon: GlobeIcon, name: 'Flight Status API' },
    { Icon: RadioIcon, name: 'Turnaround Ops API' },
    { Icon: UsersIcon, name: 'Crew Coordination API' },
];

const SUBSCRIPTION_TIERS: { readonly name: string; readonly subscribers: number; readonly price: string }[] = [
    { name: 'Free', subscribers: 892, price: '$0' },
    { name: 'Starter', subscribers: 156, price: '$4,680' },
    { name: 'Enterprise', subscribers: 24, price: '$12,400' },
];

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: BoxesIcon,
        title: 'Bundle APIs',
        description: 'Group related proxies under a single product consumers can discover and subscribe to.',
    },
    {
        Icon: DollarSignIcon,
        title: 'Define plans',
        description: 'Attach rate limits, quotas, and pricing tiers to control how each audience accesses your product.',
    },
    {
        Icon: UsersIcon,
        title: 'Monetize',
        description: 'Track subscriptions across tiers, measure usage, and generate revenue from your API estate.',
    },
];

function ApiRow({ Icon, name }: { Icon: LucideIcon; name: string }) {
    return (
        <div className="flex items-center gap-2 rounded-lg border bg-card px-3 py-2">
            <div className="rounded-md bg-muted p-1 shrink-0">
                <Icon className="size-3 text-muted-foreground" aria-hidden />
            </div>
            <p className="text-xs font-medium truncate">{name}</p>
            <Badge variant="secondary" className="ml-auto shrink-0">
                Proxy
            </Badge>
        </div>
    );
}

function SubscriptionTierCard({ name, subscribers, price }: { name: string; subscribers: number; price: string }) {
    const isEnterprise = name === 'Enterprise';
    return (
        <div className={`rounded-lg border p-3 space-y-2 text-center w-full${isEnterprise ? ' border-primary/20 bg-primary/5' : ''}`}>
            <p className="text-xs font-semibold">{name}</p>
            <p className="text-base font-bold">{price}</p>
            <p className="text-xs text-muted-foreground">{subscribers.toLocaleString()} subscribers</p>
        </div>
    );
}

function ApisPanel() {
    return (
        <div className="flex-1 rounded-xl border p-4 space-y-3">
            <p className="text-xs font-semibold text-muted-foreground">APIs in your network</p>
            <div className="space-y-2">
                {SAMPLE_APIS.map(({ Icon, name }) => (
                    <ApiRow key={name} Icon={Icon} name={name} />
                ))}
            </div>
        </div>
    );
}

function ProductPanel() {
    return (
        <div className="flex-1 rounded-xl border-2 border-primary/20 bg-primary/5 p-4 space-y-3">
            <p className="text-xs font-semibold text-primary">Operations Suite</p>

            {/* Product header */}
            <div className="flex items-center gap-2 rounded-lg border bg-card px-3 py-2">
                <div className="rounded-md bg-primary/10 p-1 shrink-0">
                    <BoxesIcon className="size-3 text-primary" aria-hidden />
                </div>
                <p className="text-xs font-semibold">Operations Suite</p>
                <Badge className="ml-auto shrink-0">Product</Badge>
            </div>

            {/* Arrow down to plans */}
            <div className="flex justify-center">
                <ArrowDownIcon className="size-4 text-primary/60" aria-hidden />
            </div>

            {/* Subscription tiers */}
            <div className="flex flex-row gap-2">
                {SUBSCRIPTION_TIERS.map(tier => (
                    <div key={tier.name} className="flex-1">
                        <SubscriptionTierCard {...tier} />
                    </div>
                ))}
            </div>
        </div>
    );
}

// ─── Public component ────────────────────────────────────────────────────────

export function ApiProductsEmptyLanding({ onCreateProduct }: { onCreateProduct?: () => void }) {
    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">API Products</h1>
                    <p className="text-sm text-muted-foreground">Bundle API proxies into products for monetization and distribution</p>
                </div>
                <Button onClick={onCreateProduct} className="shrink-0">
                    <PlusIcon className="size-4" aria-hidden />
                    Create Product
                </Button>
            </div>

            <Card>
                <CardContent className="pt-6 space-y-6">
                    <div>
                        <h2 className="text-base font-semibold">What is an API product?</h2>
                        <p className="text-xs text-muted-foreground mt-1">
                            An API product bundles one or more proxies under a single entry point with its own plans, rate limits, and
                            subscription tiers — letting you package and monetize your API estate without changing the underlying services.
                        </p>
                    </div>

                    {/* APIs → Product comparison */}
                    <div className="flex flex-row gap-4 items-stretch">
                        <ApisPanel />

                        {/* Connector arrow */}
                        <div className="flex items-center justify-center shrink-0">
                            <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                        </div>

                        <ProductPanel />
                    </div>

                    {/* Feature tiles */}
                    <div className="flex flex-row gap-4 border-t pt-5">
                        {FEATURE_TILES.map(({ Icon, title, description }) => (
                            <div key={title} className="flex-1">
                                <FeatureTile Icon={Icon} title={title} description={description} />
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
