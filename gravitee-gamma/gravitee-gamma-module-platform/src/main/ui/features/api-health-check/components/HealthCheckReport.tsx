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
import { Alert, AlertDescription, Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleXIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';

import type { HealthCheckReport } from '../types';
import { AVAILABILITY_ERROR_THRESHOLD, AVAILABILITY_WARNING_THRESHOLD } from '../utils/reportBuckets';

function MetricCard({
    label,
    hint,
    value,
    tone,
    isLoading,
}: Readonly<{
    label: string;
    hint: string;
    value: number;
    tone: 'success' | 'warning' | 'error';
    isLoading: boolean;
}>) {
    const toneClass = tone === 'error' ? 'text-destructive' : tone === 'warning' ? 'text-warning' : 'text-success';
    const Icon = tone === 'error' ? CircleXIcon : tone === 'warning' ? TriangleAlertIcon : CircleCheckIcon;

    return (
        <div className="rounded-xl border p-4">
            <div className="flex items-start justify-between gap-2">
                {isLoading ? (
                    <Skeleton className="h-7 w-10 rounded" />
                ) : (
                    <p className={`text-2xl font-semibold tabular-nums ${toneClass}`}>{value}</p>
                )}
                <Icon className={`size-4 shrink-0 ${toneClass}`} aria-hidden />
            </div>
            <p className="mt-1 text-sm font-medium">{label}</p>
            <p className="text-muted-foreground mt-0.5 text-xs">{hint}</p>
        </div>
    );
}

export function HealthCheckReport({
    report,
    isLoading,
    isError,
}: Readonly<{
    report?: HealthCheckReport;
    isLoading: boolean;
    isError: boolean;
}>) {
    if (isError) {
        return (
            <Alert variant="destructive">
                <TriangleAlertIcon className="size-4" aria-hidden />
                <AlertDescription>Failed to load the API Health Check report.</AlertDescription>
            </Alert>
        );
    }

    const operational = report?.operational ?? 0;
    const inError = report?.inError ?? 0;
    const inWarning = report?.inWarning ?? 0;
    const checked = operational + inError + inWarning;
    const summary =
        checked === 0
            ? 'No availability samples in this timeframe yet.'
            : inError === 0 && inWarning === 0
              ? 'All checked APIs are operational.'
              : 'Availability for APIs with health check enabled in this timeframe.';

    return (
        <Card>
            <CardContent className="space-y-4 pt-6">
                <div>
                    <h2 className="text-base font-semibold">API Health Check Report</h2>
                    <p className="text-muted-foreground mt-1 text-xs">{isLoading ? 'Loading…' : summary}</p>
                </div>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <MetricCard
                        label="Operational"
                        hint={`Availability > ${AVAILABILITY_WARNING_THRESHOLD}%`}
                        value={operational}
                        tone="success"
                        isLoading={isLoading}
                    />
                    <MetricCard
                        label="In warning"
                        hint={`Availability ≤ ${AVAILABILITY_WARNING_THRESHOLD}%`}
                        value={inWarning}
                        tone="warning"
                        isLoading={isLoading}
                    />
                    <MetricCard
                        label="In error"
                        hint={`Availability ≤ ${AVAILABILITY_ERROR_THRESHOLD}%`}
                        value={inError}
                        tone="error"
                        isLoading={isLoading}
                    />
                </div>
            </CardContent>
        </Card>
    );
}
