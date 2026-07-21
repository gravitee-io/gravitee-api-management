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
import { Card, CardContent } from '@gravitee/graphene-core';

import { DUMMY_TRAFFIC_OVERVIEW, TRAFFIC_SERIES_KEY } from '../storage/dummy-dashboard-stats';

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

/** Subtle horizontal guides — Graphene Grid uses stroke-border @ 50% and is hard to see. */
function ChartHorizontalGuides() {
    return (
        <div className="pointer-events-none absolute inset-x-2 top-2 bottom-10 flex flex-col justify-between" aria-hidden>
            {Array.from({ length: 4 }, (_, index) => (
                <div key={index} className="border-t border-border/60" />
            ))}
        </div>
    );
}

export function PortalTrafficOverview() {
    return (
        <Card className="flex flex-col">
            <CardContent className="flex h-full flex-col gap-3 pt-5 pb-4">
                <div>
                    <p className="text-sm font-semibold">Traffic Overview</p>
                    <p className="text-xs text-muted-foreground">Gateway requests (30D)</p>
                </div>
                <div className="relative">
                    <ChartHorizontalGuides />
                    <ChartContainer config={CHART_CONFIG} className="relative z-10 h-56 w-full">
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
                <div className="flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
                    <span className="size-2 shrink-0 rounded-[2px]" style={{ backgroundColor: SERIES_COLOR }} aria-hidden />
                    HTTP_REQUESTS
                </div>
            </CardContent>
        </Card>
    );
}
