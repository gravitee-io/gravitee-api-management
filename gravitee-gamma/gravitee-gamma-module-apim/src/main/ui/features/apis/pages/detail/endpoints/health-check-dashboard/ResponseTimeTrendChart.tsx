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
import { AreaChart, BarChart, ChartContainer, LineChart, type ChartConfig, type ChartState } from '@gravitee/graphene-charts';
import { Button, Card, CardContent, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import type { ResponseTimeTrendPoint } from '../../../../types/healthCheck';
import type { Timeframe } from '../../../../utils/healthTimeframe';

interface ResponseTimeTrendChartProps {
    points: ResponseTimeTrendPoint[];
    isLoading: boolean;
    timeframe: Timeframe;
}

type ChartType = 'line' | 'area' | 'bar';

const CHART_TYPES: { id: ChartType; label: string }[] = [
    { id: 'line', label: 'Line' },
    { id: 'area', label: 'Area' },
    { id: 'bar', label: 'Bar' },
];

const RESPONSE_TIME_KEY = 'responseTime';

const CHART_CONFIG: ChartConfig = {
    [RESPONSE_TIME_KEY]: { label: 'Response time (ms)', color: 'var(--chart-1)', unit: 'ms' },
};

const INTRADAY_TIMEFRAMES: readonly Timeframe[] = ['1m', '1h', '1d'];
const INTRADAY_FMT = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' });
const MULTIDAY_FMT = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' });

export function ResponseTimeTrendChart({ points, isLoading, timeframe }: Readonly<ResponseTimeTrendChartProps>) {
    const [chartType, setChartType] = useState<ChartType>('line');

    const data = useMemo(() => {
        const fmt = INTRADAY_TIMEFRAMES.includes(timeframe) ? INTRADAY_FMT : MULTIDAY_FMT;
        return points
            .filter(point => point.responseTime > 0)
            .map(point => ({ category: fmt.format(point.timestamp), [RESPONSE_TIME_KEY]: point.responseTime }));
    }, [points, timeframe]);

    const state: ChartState = isLoading ? 'loading' : data.length === 0 ? 'empty' : 'ready';

    const commonProps = {
        data,
        dataKeys: [RESPONSE_TIME_KEY],
        state,
        showGrid: true,
        valueTickFormatter: (v: number) => String(v),
        ariaLabel: 'Global response time trend',
        margin: { top: 12, right: 24 },
    };

    return (
        <Card>
            <CardHeader className="flex flex-row items-start justify-between gap-4">
                <div className="space-y-1">
                    <CardTitle className="text-base">Global response time trend</CardTitle>
                    <p className="text-sm text-muted-foreground">
                        How quickly endpoints respond to health checks over the selected period.
                    </p>
                </div>
                <div className="flex shrink-0 gap-1" role="group" aria-label="Chart type">
                    {CHART_TYPES.map(option => (
                        <Button
                            key={option.id}
                            type="button"
                            size="sm"
                            variant={chartType === option.id ? 'default' : 'outline'}
                            aria-pressed={chartType === option.id}
                            onClick={() => setChartType(option.id)}
                        >
                            {option.label}
                        </Button>
                    ))}
                </div>
            </CardHeader>
            <CardContent>
                <ChartContainer config={CHART_CONFIG} className="h-72 w-full">
                    {chartType === 'line' && <LineChart {...commonProps} />}
                    {chartType === 'area' && <AreaChart {...commonProps} stacked={false} />}
                    {chartType === 'bar' && <BarChart {...commonProps} barRadius={4} />}
                </ChartContainer>
            </CardContent>
        </Card>
    );
}
