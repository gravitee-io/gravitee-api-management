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
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    BarChart3Icon,
    CircleCheckIcon,
    CircleXIcon,
    MonitorIcon,
    PlusIcon,
    RadioIcon,
    ServerIcon,
    ShieldIcon,
    SlidersHorizontalIcon,
} from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import { FeatureTile } from '../../shared/components';

// ─── Static data ─────────────────────────────────────────────────────────────

const NO_PROXY_CONS = [
    'No platform authentication or app-level throttling',
    'No unified request logs or analytics',
    'Harder to add policies without touching upstream code',
] as const;

const PROXY_PROS = [
    'Authentication, authorization, and key management',
    'Rate limits, quotas, and request shaping',
    'Logs, metrics, and end-to-end tracing',
] as const;

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: ShieldIcon,
        title: 'Security',
        description: 'Enforce who can call your API, validate tokens, and add encryption at the edge.',
    },
    {
        Icon: SlidersHorizontalIcon,
        title: 'Traffic control',
        description: 'Throttle traffic, set quotas, and protect backends from abuse or misbehaving clients.',
    },
    {
        Icon: BarChart3Icon,
        title: 'Observability',
        description: 'Capture every request, debug failures fast, and measure health across environments.',
    },
];

function FlowNode({ Icon, label }: { Icon: LucideIcon; label: string }) {
    return (
        <div className="flex flex-col items-center text-center">
            <div className="rounded-lg bg-muted p-2">
                <Icon className="size-4 text-muted-foreground" aria-hidden />
            </div>
            <p className="text-xs font-medium mt-1">{label}</p>
        </div>
    );
}

function ProxyNode() {
    return (
        <div className="flex flex-col items-center text-center rounded-lg border-2 border-primary/30 bg-card px-3 py-2">
            <div className="rounded-lg bg-primary/10 p-1">
                <RadioIcon className="size-3 text-primary" aria-hidden />
            </div>
            <p className="text-xs font-semibold mt-1">Proxy</p>
        </div>
    );
}

function ComparisonLine({ label, variant }: { label: string; variant: 'positive' | 'negative' }) {
    return (
        <li className="flex items-center gap-1 text-xs text-muted-foreground">
            {variant === 'positive' ? (
                <CircleCheckIcon className="size-3 shrink-0 text-success" aria-hidden />
            ) : (
                <CircleXIcon className="size-3 shrink-0 text-destructive" aria-hidden />
            )}
            {label}
        </li>
    );
}

export function ApisEmptyLanding({ onCreateProxy, canCreate }: { onCreateProxy?: () => void; canCreate?: boolean }) {
    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">API Proxies</h1>
                    <p className="text-sm text-muted-foreground">Manage and monitor your API proxies</p>
                </div>
                {canCreate && (
                    <Button onClick={onCreateProxy} className="shrink-0">
                        <PlusIcon className="size-4" aria-hidden />
                        Create New Proxy
                    </Button>
                )}
            </div>

            <Card>
                <CardContent className="pt-6 space-y-6">
                    <div>
                        <h2 className="text-base font-semibold">Why add an API proxy?</h2>
                        <p className="text-xs text-muted-foreground mt-1">
                            A proxy sits in front of your backend so you can secure traffic, control usage, and observe requests — without
                            changing upstream services.
                        </p>
                    </div>

                    {/* Direct vs proxied comparison */}
                    <div className="flex flex-row gap-4 items-stretch">
                        {/* No-proxy panel */}
                        <div className="flex-1 rounded-xl border p-4 space-y-3">
                            <p className="text-xs font-semibold text-muted-foreground">Direct access (no proxy)</p>
                            <div className="flex items-center justify-center gap-2">
                                <FlowNode Icon={MonitorIcon} label="Client" />
                                <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" aria-hidden />
                                <FlowNode Icon={ServerIcon} label="Backend" />
                            </div>
                            <ul className="space-y-1">
                                {NO_PROXY_CONS.map(label => (
                                    <ComparisonLine key={label} label={label} variant="negative" />
                                ))}
                            </ul>
                        </div>

                        {/* Connector arrow */}
                        <div className="flex items-center justify-center shrink-0">
                            <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                        </div>

                        {/* With-proxy panel */}
                        <div className="flex-1 rounded-xl border-2 border-primary/20 bg-primary/5 p-4 space-y-3">
                            <p className="text-xs font-semibold text-primary">With an API proxy</p>
                            <div className="flex items-center justify-center gap-2">
                                <FlowNode Icon={MonitorIcon} label="Client" />
                                <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                                <ProxyNode />
                                <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                                <FlowNode Icon={ServerIcon} label="Backend" />
                            </div>
                            <ul className="space-y-1">
                                {PROXY_PROS.map(label => (
                                    <ComparisonLine key={label} label={label} variant="positive" />
                                ))}
                            </ul>
                        </div>
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
