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
import { Card, CardContent, Skeleton } from '@gravitee/graphene-core';

import { DUMMY_DASHBOARD_KPIS } from '../storage/dummy-dashboard-stats';

interface PortalDashboardSummaryCardsProps {
    readonly activePortals: number | null;
}

function MetricCard({ label, value }: { readonly label: string; readonly value: string | null }) {
    return (
        <Card>
            <CardContent className="pt-5 pb-5">
                <p className="text-sm text-muted-foreground">{label}</p>
                {value === null ? (
                    <Skeleton className="mt-2 h-8 w-16 rounded" />
                ) : (
                    <p className="mt-1 text-2xl font-semibold tracking-tight">{value}</p>
                )}
            </CardContent>
        </Card>
    );
}

export function PortalDashboardSummaryCards({ activePortals }: PortalDashboardSummaryCardsProps) {
    return (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <MetricCard
                label="Active Portals"
                value={activePortals === null ? null : String(activePortals)}
            />
            <MetricCard
                label="Total Visits (30D)"
                value={DUMMY_DASHBOARD_KPIS.totalVisits30d.toLocaleString()}
            />
            <MetricCard
                label="API Docs Viewed (30D)"
                value={DUMMY_DASHBOARD_KPIS.apiDocsViewed30d.toLocaleString()}
            />
            <MetricCard
                label="Avg. Uptime"
                value={`${DUMMY_DASHBOARD_KPIS.avgUptimePercent.toFixed(2)}%`}
            />
        </div>
    );
}
