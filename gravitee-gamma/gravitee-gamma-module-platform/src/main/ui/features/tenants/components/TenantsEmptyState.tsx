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
import { Card, CardContent } from '@gravitee/graphene-core';
import { ArrowRightIcon, CircleCheckIcon, CircleXIcon, GlobeIcon, RadioIcon, ServerIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import { FeatureTile } from '../../shared/components';

const WITHOUT_TENANT_CONS = [
    'Every gateway proxies every endpoint on the API',
    'No way to keep US traffic on US backends',
    'Regional failover has to be configured by hand',
] as const;

const WITH_TENANT_PROS = [
    'Tag a gateway with one tenant in gravitee.yml',
    'Assign endpoints to the regions they should serve',
    'A request only reaches backends that match that gateway',
] as const;

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: ServerIcon,
        title: 'Tag the gateway',
        description: 'Set tenant: usa (or eu) on each gateway so it only loads matching endpoints.',
    },
    {
        Icon: RadioIcon,
        title: 'Pin an endpoint',
        description: 'On an API, attach a tenant to an endpoint so only those gateways will proxy to it.',
    },
    {
        Icon: GlobeIcon,
        title: 'Keep traffic local',
        description: 'US gateways stay on US backends, EU on EU — without deploying a second API.',
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

function TenantTag() {
    return <span className="rounded-md border border-primary bg-primary/10 px-2 py-1 text-xs font-semibold text-primary">usa</span>;
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

export function TenantsEmptyState({ canCreate = false }: Readonly<{ canCreate?: boolean }>) {
    return (
        <Card>
            <CardContent className="pt-6 space-y-6">
                <div>
                    <h2 className="text-base font-semibold">Why create a tenant?</h2>
                    <p className="text-xs text-muted-foreground mt-1">
                        A tenant is a label you put on a gateway and on the endpoints that gateway should reach. Without one, every gateway
                        deploys every endpoint. Create the first tenant, copy its key into gravitee.yml, then attach it to the endpoints
                        that belong in that region.
                    </p>
                </div>

                <div className="flex flex-col gap-4 items-stretch md:flex-row">
                    <div className="flex-1 rounded-xl border p-4 space-y-3">
                        <p className="text-xs font-semibold text-muted-foreground">Without tenants</p>
                        <div className="flex items-center justify-center gap-2">
                            <FlowNode Icon={ServerIcon} label="Gateway" />
                            <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" aria-hidden />
                            <FlowNode Icon={GlobeIcon} label="Every endpoint" />
                        </div>
                        <ul className="space-y-1">
                            {WITHOUT_TENANT_CONS.map(label => (
                                <ComparisonLine key={label} label={label} variant="negative" />
                            ))}
                        </ul>
                    </div>

                    <div className="flex-1 rounded-xl border-2 border-primary/40 bg-primary/5 p-4 space-y-3">
                        <p className="text-xs font-semibold text-primary">With tenants</p>
                        <div className="flex items-center justify-center gap-2">
                            <FlowNode Icon={ServerIcon} label="USA gateway" />
                            <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                            <TenantTag />
                            <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                            <FlowNode Icon={GlobeIcon} label="US backends" />
                        </div>
                        <ul className="space-y-1">
                            {WITH_TENANT_PROS.map(label => (
                                <ComparisonLine key={label} label={label} variant="positive" />
                            ))}
                        </ul>
                    </div>
                </div>

                <div className="flex flex-col gap-4 border-t pt-5 md:flex-row">
                    {FEATURE_TILES.map(({ Icon, title, description }) => (
                        <div key={title} className="flex-1">
                            <FeatureTile Icon={Icon} title={title} description={description} />
                        </div>
                    ))}
                </div>

                {canCreate ? (
                    <p className="text-xs text-muted-foreground border-t pt-5">
                        Create the first tenant, then paste its key into gravitee.yml.
                    </p>
                ) : null}
            </CardContent>
        </Card>
    );
}
