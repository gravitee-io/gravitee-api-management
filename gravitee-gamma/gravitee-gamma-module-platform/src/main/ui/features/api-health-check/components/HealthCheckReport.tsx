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
import { Alert, AlertDescription, AlertTitle } from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleXIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';

import type { HealthCheckReport } from '../types';
import { AVAILABILITY_ERROR_THRESHOLD, AVAILABILITY_WARNING_THRESHOLD } from '../utils/reportBuckets';

function pluralize(count: number): string {
    return count === 1 ? '1 API is' : `${count} APIs are`;
}

/**
 * Classic's report banner, not a summary chart.
 *
 * It names only the APIs that are degraded. There is no backend aggregate for this page and the table is
 * paginated, so an "operational" total would be a number the UI cannot stand behind -- which is why Classic
 * never shows one either.
 */
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
                <AlertTitle>API Health Check Report</AlertTitle>
                <AlertDescription>Failed to load the API Health Check report.</AlertDescription>
            </Alert>
        );
    }

    const inError = report?.inError ?? 0;
    const inWarning = report?.inWarning ?? 0;
    const degraded = !isLoading && (inError > 0 || inWarning > 0);

    const variant = degraded && inError > 0 ? 'destructive' : degraded ? 'warning' : 'default';
    const Icon = degraded && inError > 0 ? CircleXIcon : degraded ? TriangleAlertIcon : CircleCheckIcon;

    return (
        <Alert variant={variant}>
            <Icon className="size-4" aria-hidden />
            <AlertTitle>API Health Check Report</AlertTitle>
            <AlertDescription>
                {isLoading ? (
                    'Loading...'
                ) : !degraded ? (
                    'All APIs are operational'
                ) : (
                    <span className="flex flex-col">
                        {inError > 0 && (
                            <span>
                                {pluralize(inError)} in error (HealthCheck availability &lt;= {AVAILABILITY_ERROR_THRESHOLD}%)
                            </span>
                        )}
                        {inWarning > 0 && (
                            <span>
                                {pluralize(inWarning)} in warning (HealthCheck availability &lt;= {AVAILABILITY_WARNING_THRESHOLD}%)
                            </span>
                        )}
                    </span>
                )}
            </AlertDescription>
        </Alert>
    );
}
