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
import { Card, CardContent, useLayoutConfig } from '@gravitee/graphene-core';
import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { AvailabilityByFieldTable } from './AvailabilityByFieldTable';
import { ChartThemeFix } from './ChartThemeFix';
import { FailedHealthChecksTable } from './FailedHealthChecksTable';
import { HealthCheckFilters } from './HealthCheckFilters';
import { HealthCheckGlobalMetrics } from './HealthCheckGlobalMetrics';
import { ResponseTimeTrendChart } from './ResponseTimeTrendChart';
import { useHealthCheckDashboard } from './useHealthCheckDashboard';
import { useApiDetailContext } from '../../../../context/ApiDetailContext';
import { isHttpProxyApi } from '../../../../utils/apiHttpProxy';
import { DEFAULT_TIMEFRAME, type Timeframe } from '../../../../utils/healthTimeframe';

export function ApiHealthCheckDashboardPage() {
    useLayoutConfig({ contentVariant: 'full-bleed' }, []);
    const { apiId } = useParams<{ apiId: string }>();
    const { api } = useApiDetailContext();
    const [timeframe, setTimeframe] = useState<Timeframe>(DEFAULT_TIMEFRAME);

    const isProxy = isHttpProxyApi(api) && api?.definitionVersion === 'V4';
    // Passing `undefined` apiId keeps every query disabled when not applicable.
    const dashboard = useHealthCheckDashboard(isProxy ? apiId : undefined, timeframe);

    // No-data is the common case (a freshly health-checked API, or a backend that
    // 500s on empty aggregations). Like the classic console, each widget degrades
    // silently to its own 0/empty state — no blocking banner, no error toast.

    if (!isProxy) {
        return (
            <PageShell>
                <MessageCard
                    title="Health checks are available for proxy APIs only"
                    message="Configure an HTTP proxy endpoint with health checks enabled to monitor availability and response times."
                />
            </PageShell>
        );
    }

    if (!dashboard.canRead) {
        return (
            <PageShell>
                <MessageCard
                    title="You don't have permission to view health checks"
                    message="Ask an administrator for the API health read permission to access this dashboard."
                />
            </PageShell>
        );
    }

    return (
        <PageShell>
            <ChartThemeFix />
            <HealthCheckFilters timeframe={timeframe} onTimeframeChange={setTimeframe} onRefresh={dashboard.refresh} />

            <HealthCheckGlobalMetrics availability={dashboard.availability} responseTime={dashboard.responseTime} />

            <ResponseTimeTrendChart points={dashboard.trend.points} isLoading={dashboard.trend.isLoading} timeframe={timeframe} />

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                <AvailabilityByFieldTable field="endpoint" data={dashboard.endpoint} />
                <AvailabilityByFieldTable field="gateway" data={dashboard.gateway} />
            </div>

            <FailedHealthChecksTable apiId={apiId} timeframe={timeframe} />
        </PageShell>
    );
}

function PageShell({ children }: Readonly<{ children: React.ReactNode }>) {
    return (
        <div className="flex flex-col gap-6">
            <div>
                <h1 className="text-2xl font-semibold tracking-tight">Health Check Dashboard</h1>
                <p className="text-sm text-muted-foreground">Monitor endpoint availability, response times, and failed health checks.</p>
            </div>
            {children}
        </div>
    );
}

function MessageCard({ title, message }: Readonly<{ title: string; message: string }>) {
    return (
        <Card>
            <CardContent className="flex flex-col items-center gap-2 py-12 text-center">
                <p className="text-base font-medium">{title}</p>
                <p className="max-w-md text-sm text-muted-foreground">{message}</p>
            </CardContent>
        </Card>
    );
}
