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
import { Card, Skeleton } from '@gravitee/graphene-core';
import { BotIcon, BrainIcon, ShieldCheckIcon, UsersIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import type { ComponentType, SVGProps } from 'react';
import { Link } from 'react-router-dom';
import { authzApiService } from '../../shared/api/authz-api.service';
import { authzQueryKeys } from '../../shared/api/query-keys';
interface KpiCardMeta {
    readonly label: string;
    readonly description: string;
    readonly to?: string;
    readonly linkLabel?: string;
    readonly Icon: ComponentType<SVGProps<SVGSVGElement>>;
    readonly iconClassName: string;
}

const KPI_CARDS: readonly KpiCardMeta[] = [
    {
        label: 'MCP',
        description:
            'Manage access to tools and control what agents can access on behalf of users — clear boundaries for every MCP surface.',
        to: '../mcps',
        linkLabel: 'Manage MCPs',
        Icon: ShieldCheckIcon,
        iconClassName: 'bg-highlight/10 text-highlight',
    },
    {
        label: 'Agents',
        description: 'Manage access to agent skills and workflows so only trusted agents can act with the right scopes and identities.',
        Icon: BotIcon,
        iconClassName: 'bg-success/10 text-success',
    },
    {
        label: 'AI Models',
        description: 'Manage access to providers and models — tailor who can use which model, in which environment, and when.',
        to: '../llms',
        linkLabel: 'Manage AI models',
        Icon: BrainIcon,
        iconClassName: 'bg-primary/10 text-primary',
    },
    {
        label: 'Users and groups',
        description: 'Manage users, groups, and how they relate — membership and hierarchy stay consistent for policy decisions.',
        Icon: UsersIcon,
        iconClassName: 'bg-warning/10 text-warning',
    },
];

function useDashboardCounts(environmentId: string) {
    // Backend has no notion of `type` (UI-derived from entityId prefix), so listPolicies
    // post-filters client-side. perPage must be large enough to capture all rows of any
    // type — otherwise `total` reflects the global count minus what fell off this page.
    const POLICY_FETCH_LIMIT = 500;

    const mcpCount = useQuery({
        queryKey: authzQueryKeys.policies.page(environmentId, 1, POLICY_FETCH_LIMIT, 'MCP'),
        queryFn: () => authzApiService.listPolicies(environmentId, { type: 'MCP', perPage: POLICY_FETCH_LIMIT }),
        enabled: Boolean(environmentId),
        staleTime: 30_000,
    });

    const agentCount = useQuery({
        queryKey: authzQueryKeys.policies.page(environmentId, 1, POLICY_FETCH_LIMIT, 'AGENT'),
        queryFn: () => authzApiService.listPolicies(environmentId, { type: 'AGENT', perPage: POLICY_FETCH_LIMIT }),
        enabled: Boolean(environmentId),
        staleTime: 30_000,
    });

    const llmCount = useQuery({
        queryKey: authzQueryKeys.policies.page(environmentId, 1, POLICY_FETCH_LIMIT, 'LLM'),
        queryFn: () => authzApiService.listPolicies(environmentId, { type: 'LLM', perPage: POLICY_FETCH_LIMIT }),
        enabled: Boolean(environmentId),
        staleTime: 30_000,
    });

    const principalCount = useQuery({
        queryKey: authzQueryKeys.entities.page(environmentId, 1, 1, 'PRINCIPAL'),
        queryFn: () => authzApiService.listEntities(environmentId, { perPage: 1, kind: 'PRINCIPAL' }),
        enabled: Boolean(environmentId),
        staleTime: 30_000,
    });

    return {
        MCP: { total: mcpCount.data?.total, isLoading: mcpCount.isLoading },
        Agents: { total: agentCount.data?.total, isLoading: agentCount.isLoading },
        'AI Models': { total: llmCount.data?.total, isLoading: llmCount.isLoading },
        'Users and groups': { total: principalCount.data?.total, isLoading: principalCount.isLoading },
    };
}

export function DashboardPage() {
    const env = useEnvironment();
    const counts = useDashboardCounts(env?.id ?? '');

    return (
        <div className="flex flex-col gap-6">
            <header className="flex flex-col gap-2">
                <h1 className="text-2xl font-semibold">Dashboard</h1>
                <p className="max-w-3xl text-sm text-muted-foreground">
                    Decide what <span className="font-medium text-foreground">users</span> and{' '}
                    <span className="font-medium text-foreground">agents</span> may do on{' '}
                    <span className="font-medium text-foreground">entity resources</span> — policies, identities, groups, deployments, and
                    PDPs stay aligned so access stays explicit and observable.
                </p>
            </header>

            <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
                {KPI_CARDS.map(kpi => {
                    const count = counts[kpi.label as keyof typeof counts];
                    return (
                        <Card key={kpi.label} className="flex flex-col gap-3 p-5">
                            <div className={`flex size-10 items-center justify-center rounded-lg ${kpi.iconClassName}`}>
                                <kpi.Icon className="size-5" aria-hidden />
                            </div>
                            <div className="flex flex-col gap-0.5">
                                {count.isLoading ? (
                                    <Skeleton className="h-8 w-12" />
                                ) : (
                                    <div className="text-3xl font-semibold leading-none">{count.total ?? '—'}</div>
                                )}
                                <div className="text-sm font-medium">{kpi.label}</div>
                            </div>
                            <p className="text-xs text-muted-foreground">{kpi.description}</p>
                            {kpi.to && kpi.linkLabel ? (
                                <Link to={kpi.to} className="text-sm font-medium text-primary hover:underline">
                                    {kpi.linkLabel} →
                                </Link>
                            ) : (
                                <span className="text-sm font-medium text-muted-foreground">Coming soon</span>
                            )}
                        </Card>
                    );
                })}
            </div>
        </div>
    );
}
