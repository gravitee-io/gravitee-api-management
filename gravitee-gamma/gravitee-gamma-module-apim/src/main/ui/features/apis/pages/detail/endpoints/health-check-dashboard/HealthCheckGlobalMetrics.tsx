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
import { ChartContainer, DoughnutChart, type ChartConfig, type ChartState } from '@gravitee/graphene-charts';
import { Card, CardContent, CardHeader, CardTitle, Skeleton } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import type { MetricState } from './useHealthCheckDashboard';
import { availabilityColorClass } from '../../../../utils/healthCheckDashboard';

interface HealthCheckGlobalMetricsProps {
    availability: MetricState;
    responseTime: MetricState;
}

const AVAILABILITY_DONUT_CONFIG: ChartConfig = {
    Available: { label: 'Available' },
    Unavailable: { label: 'Unavailable' },
};

/** loading while fetching; empty when the source query failed or returned no data. */
function chartState(metric: MetricState): ChartState {
    if (metric.isLoading) return 'loading';
    if (metric.isError || metric.value === undefined) return 'empty';
    return 'ready';
}

export function HealthCheckGlobalMetrics({ availability, responseTime }: Readonly<HealthCheckGlobalMetricsProps>) {
    return (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <MetricCard
                title="Global availability over selected period"
                description="Share of successful health checks across all endpoints."
            >
                <AvailabilityDonut availability={availability} />
            </MetricCard>

            <MetricCard title="Global average response time" description="Average health-check response time across all endpoints.">
                <ResponseTimeValue responseTime={responseTime} />
            </MetricCard>
        </div>
    );
}

function MetricCard({ title, description, children }: Readonly<{ title: string; description: string; children: ReactNode }>) {
    return (
        <Card className="h-full">
            <CardHeader>
                <CardTitle className="text-base">{title}</CardTitle>
                <p className="text-sm text-muted-foreground">{description}</p>
            </CardHeader>
            <CardContent>{children}</CardContent>
        </Card>
    );
}

function AvailabilityDonut({ availability }: Readonly<{ availability: MetricState }>) {
    const state = chartState(availability);
    const pct = availability.value ?? 0;
    const data =
        state === 'ready'
            ? [
                  { category: 'Available', value: pct },
                  { category: 'Unavailable', value: Math.max(0, 100 - pct) },
              ]
            : [];

    return (
        <ChartContainer config={AVAILABILITY_DONUT_CONFIG} className="mx-auto h-44 w-44">
            <DoughnutChart
                data={data}
                state={state}
                innerRadius={0.68}
                ariaLabel="Global availability"
                centerContent={<span className={`text-2xl font-semibold ${availabilityColorClass(pct)}`}>{pct} %</span>}
            />
        </ChartContainer>
    );
}

function ResponseTimeValue({ responseTime }: Readonly<{ responseTime: MetricState }>) {
    if (responseTime.isLoading) {
        return <Skeleton className="h-9 w-24 rounded" />;
    }
    if (responseTime.isError || responseTime.value === undefined) {
        return <p className="text-sm text-muted-foreground">No data to display</p>;
    }
    return <span className="text-3xl font-semibold">{responseTime.value} ms</span>;
}
