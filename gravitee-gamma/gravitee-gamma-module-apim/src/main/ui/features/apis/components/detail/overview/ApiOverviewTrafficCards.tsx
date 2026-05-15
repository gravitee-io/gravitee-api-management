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
import { Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import { ActivityIcon, ArrowUpIcon, ClockIcon, TrendingUpIcon, ZapIcon } from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';

import type { ApiStatsAnalytics } from '../../../../../services/apis/analytics';

function formatCount(n: number | undefined): string {
    if (n === undefined || isNaN(n)) return '—';
    return Math.round(n).toLocaleString();
}

function formatDuration(ms: number | undefined): string {
    if (ms === undefined || isNaN(ms)) return '—';
    const rounded = Math.round(ms);
    return rounded >= 1000 ? `${(rounded / 1000).toFixed(1)}s` : `${rounded}ms`;
}

function formatRps(rps: number | undefined): string {
    if (rps === undefined || isNaN(rps)) return '—';
    if (rps < 1) return '< 1';
    return rps.toFixed(2);
}

interface TrafficCardProps {
    icon: ComponentType<{ className?: string }>;
    label: string;
    value: string;
    isLoading: boolean;
}

function TrafficCard({ icon: Icon, label, value, isLoading }: Readonly<TrafficCardProps>) {
    return (
        <Card className="flex-1">
            <CardContent className="pt-5 pb-5 px-5">
                <div className="flex items-center justify-between mb-3">
                    <p className="text-sm text-muted-foreground">{label}</p>
                    <Icon className="size-4 text-muted-foreground" aria-hidden />
                </div>
                {isLoading ? <Skeleton className="h-8 w-24 rounded" /> : <p className="text-2xl font-semibold tracking-tight">{value}</p>}
            </CardContent>
        </Card>
    );
}

interface ApiOverviewTrafficCardsProps {
    analyticsStats: ApiStatsAnalytics | undefined;
    isLoadingTraffic: boolean;
}

export function ApiOverviewTrafficCards({ analyticsStats, isLoadingTraffic }: Readonly<ApiOverviewTrafficCardsProps>) {
    return (
        <div className="space-y-3">
            <p className="text-sm font-medium text-muted-foreground">Traffic snapshot (last 24 h)</p>
            <div className="flex gap-4">
                <TrafficCard
                    icon={ActivityIcon}
                    label="Total Requests"
                    value={formatCount(analyticsStats?.count)}
                    isLoading={isLoadingTraffic}
                />
                <TrafficCard
                    icon={ClockIcon}
                    label="Min Response Time"
                    value={formatDuration(analyticsStats?.min)}
                    isLoading={isLoadingTraffic}
                />
                <TrafficCard
                    icon={ArrowUpIcon}
                    label="Max Response Time"
                    value={formatDuration(analyticsStats?.max)}
                    isLoading={isLoadingTraffic}
                />
                <TrafficCard
                    icon={TrendingUpIcon}
                    label="Avg Response Time"
                    value={formatDuration(analyticsStats?.avg)}
                    isLoading={isLoadingTraffic}
                />
                <TrafficCard icon={ZapIcon} label="Requests / Second" value={formatRps(analyticsStats?.rps)} isLoading={isLoadingTraffic} />
            </div>
        </div>
    );
}
