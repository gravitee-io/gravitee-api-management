/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ChartContainer, LineChart, type ChartConfig } from '@gravitee/graphene-charts';
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import { useState } from 'react';

import {
    DUMMY_DASHBOARD_KPIS,
    DUMMY_TRAFFIC_OVERVIEW,
    TRAFFIC_SERIES_KEY,
} from '../../portals/storage/dummy-dashboard-stats';
import { MODULE_CONFIG_SECTION_META } from '../types';

const SERIES_COLOR = 'var(--chart-1, var(--primary))';

const CHART_CONFIG: ChartConfig = {
    [TRAFFIC_SERIES_KEY]: {
        label: 'HTTP_REQUESTS',
        color: SERIES_COLOR,
    },
};

const TRAFFIC_DATA = DUMMY_TRAFFIC_OVERVIEW.map(point => ({
    category: point.category,
    [TRAFFIC_SERIES_KEY]: point[TRAFFIC_SERIES_KEY],
}));

type DashboardId = 'traffic' | 'auth' | 'subscriptions';

interface NamedDashboard {
    readonly id: DashboardId;
    readonly title: string;
    readonly description: string;
}

const NAMED_DASHBOARDS: readonly NamedDashboard[] = [
    {
        id: 'traffic',
        title: 'Traffic',
        description: 'Gateway requests and portal page views over the last 30 days.',
    },
    {
        id: 'auth',
        title: 'Authentication',
        description: 'Login successes, failures, and SSO provider usage.',
    },
    {
        id: 'subscriptions',
        title: 'Subscriptions',
        description: 'New subscriptions, approvals, and rejection rates.',
    },
];

const DASHBOARD_WIDGETS: Record<
    DashboardId,
    readonly { readonly label: string; readonly value: string }[]
> = {
    traffic: [
        { label: 'Requests (30D)', value: '34.2k' },
        { label: 'Peak RPS', value: '128' },
        { label: 'Error rate', value: '0.4%' },
    ],
    auth: [
        { label: 'Successful logins', value: '8,421' },
        { label: 'Failed logins', value: '312' },
        { label: 'SSO share', value: '67%' },
    ],
    subscriptions: [
        { label: 'New subscriptions', value: '214' },
        { label: 'Approved', value: '189' },
        { label: 'Rejected', value: '25' },
    ],
};

export function ModuleDashboardsPage() {
    const [activeDashboard, setActiveDashboard] = useState<DashboardId>('traffic');
    const meta = MODULE_CONFIG_SECTION_META.dashboards;
    const widgets = DASHBOARD_WIDGETS[activeDashboard];

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                <p className="text-sm text-muted-foreground">{meta.description}</p>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <MetricCard label="Total visits (30D)" value={DUMMY_DASHBOARD_KPIS.totalVisits30d.toLocaleString()} />
                <MetricCard
                    label="API docs viewed (30D)"
                    value={DUMMY_DASHBOARD_KPIS.apiDocsViewed30d.toLocaleString()}
                />
                <MetricCard label="Avg. uptime" value={`${DUMMY_DASHBOARD_KPIS.avgUptimePercent.toFixed(2)}%`} />
                <MetricCard label="Active dashboards" value={String(NAMED_DASHBOARDS.length)} />
            </div>

            <div className="grid gap-4 md:grid-cols-3">
                {NAMED_DASHBOARDS.map(dashboard => (
                    <Card
                        key={dashboard.id}
                        className={activeDashboard === dashboard.id ? 'border-primary/60' : undefined}
                    >
                        <CardContent className="flex h-full flex-col gap-3 pt-5">
                            <div className="space-y-1">
                                <h2 className="text-sm font-semibold">{dashboard.title}</h2>
                                <p className="text-xs text-muted-foreground">{dashboard.description}</p>
                            </div>
                            <Button
                                type="button"
                                variant={activeDashboard === dashboard.id ? 'default' : 'outline'}
                                size="sm"
                                className="mt-auto self-start"
                                onClick={() => setActiveDashboard(dashboard.id)}
                            >
                                {activeDashboard === dashboard.id ? 'Viewing' : 'Open'}
                            </Button>
                        </CardContent>
                    </Card>
                ))}
            </div>

            <Card>
                <CardContent className="space-y-4 pt-5 pb-4">
                    <div>
                        <p className="text-sm font-semibold">
                            {NAMED_DASHBOARDS.find(item => item.id === activeDashboard)?.title} overview
                        </p>
                        <p className="text-xs text-muted-foreground">Static widgets for this POC dashboard</p>
                    </div>
                    <div className="grid gap-3 sm:grid-cols-3">
                        {widgets.map(widget => (
                            <div
                                key={widget.label}
                                className="rounded-md border border-border/70 bg-muted/20 px-4 py-3"
                            >
                                <p className="text-xs text-muted-foreground">{widget.label}</p>
                                <p className="mt-1 text-xl font-semibold tracking-tight">{widget.value}</p>
                            </div>
                        ))}
                    </div>
                    {activeDashboard === 'traffic' ? (
                        <div className="relative pt-2">
                            <ChartContainer config={CHART_CONFIG} className="h-56 w-full">
                                <LineChart
                                    data={TRAFFIC_DATA}
                                    dataKeys={[TRAFFIC_SERIES_KEY]}
                                    state="ready"
                                    curveType="monotone"
                                    showGrid={false}
                                    showCategoryAxis
                                    showValueAxis={false}
                                    ariaLabel="Gateway requests over the last 30 days"
                                    margin={{ top: 12, right: 8, left: 8, bottom: 8 }}
                                />
                            </ChartContainer>
                        </div>
                    ) : null}
                </CardContent>
            </Card>
        </div>
    );
}

function MetricCard({ label, value }: { readonly label: string; readonly value: string }) {
    return (
        <Card>
            <CardContent className="pt-5 pb-5">
                <p className="text-sm text-muted-foreground">{label}</p>
                <p className="mt-1 text-2xl font-semibold tracking-tight">{value}</p>
            </CardContent>
        </Card>
    );
}
