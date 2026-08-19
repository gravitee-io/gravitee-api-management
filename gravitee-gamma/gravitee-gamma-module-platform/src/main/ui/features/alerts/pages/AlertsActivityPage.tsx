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
import {
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { Link } from 'react-router-dom';

import { SeverityBadge } from '../components/SeverityBadge';
import {
    ALERT_ACTIVITY_TIME_RANGES,
    type AlertActivityTimeRangeId,
    DEFAULT_ALERT_ACTIVITY_TIME_RANGE_ID,
    activityTimeWindow,
    getAlertActivityTimeRange,
} from '../constants/activityTimeRanges';
import { getPlatformAlertAnalytics } from '../services/alerts';
import { platformAlertKeys } from '../utils/queryKeys';

function severityCount(bySeverity: Record<string, number> | undefined, key: string): number {
    return bySeverity?.[key] ?? 0;
}

export function AlertsActivityPage() {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';
    const timeRangeSelectId = useId();

    const [rangeId, setRangeId] = useState<AlertActivityTimeRangeId>(DEFAULT_ALERT_ACTIVITY_TIME_RANGE_ID);
    const selectedRange = getAlertActivityTimeRange(rangeId);

    const { data, isLoading, isError, isFetching, refetch } = useQuery({
        queryKey: platformAlertKeys.analytics(environmentId, rangeId),
        queryFn: () => {
            const { from, to } = activityTimeWindow(getAlertActivityTimeRange(rangeId).rangeMs);
            return getPlatformAlertAnalytics(environmentId, from, to);
        },
        enabled: !!environmentId,
        placeholderData: keepPreviousData,
    });

    const info = severityCount(data?.bySeverity, 'INFO');
    const warning = severityCount(data?.bySeverity, 'WARNING');
    const critical = severityCount(data?.bySeverity, 'CRITICAL');
    const total = info + warning + critical;

    const summaryCards = [
        { key: 'total', label: 'Total Alerts', value: total },
        { key: 'INFO', label: 'Info', value: info },
        { key: 'WARNING', label: 'Warning', value: warning },
        { key: 'CRITICAL', label: 'Critical', value: critical },
    ];

    const alerts = data?.alerts ?? [];

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Alerts board</h1>
                    <p className="text-sm text-muted-foreground">All alert events in {selectedRange.title.toLowerCase()}</p>
                </div>
                <div className="flex shrink-0 items-end gap-2">
                    <div className="space-y-1.5">
                        <Label htmlFor={timeRangeSelectId} className="text-xs">
                            Quick time range
                        </Label>
                        <Select value={rangeId} onValueChange={val => setRangeId(val as AlertActivityTimeRangeId)}>
                            <SelectTrigger id={timeRangeSelectId} aria-label="Quick time range" className="w-44">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {ALERT_ACTIVITY_TIME_RANGES.map(range => (
                                    <SelectItem key={range.id} value={range.id}>
                                        {range.title}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <Button type="button" variant="outline" size="sm" disabled={isFetching} onClick={() => void refetch()}>
                        {isFetching ? 'Refreshing…' : 'Refresh'}
                    </Button>
                </div>
            </div>

            {isLoading && !data && (
                <div className="space-y-3">
                    <div className="flex gap-4">
                        {[1, 2, 3, 4].map(i => (
                            <Skeleton key={i} className="h-24 flex-1 rounded-lg" />
                        ))}
                    </div>
                    <Skeleton className="h-40 w-full rounded-lg" />
                </div>
            )}

            {isError && !data && (
                <Card>
                    <CardContent className="pt-4 pb-4">
                        <p className="text-sm text-destructive">Failed to load alert activity. Please try again.</p>
                    </CardContent>
                </Card>
            )}

            {data && (
                <>
                    {isError && (
                        <Alert variant="destructive">
                            <AlertDescription>Failed to load alert activity. Please try again.</AlertDescription>
                        </Alert>
                    )}
                    <div className="flex flex-wrap gap-4">
                        {summaryCards.map(card => (
                            <Card key={card.key} className="min-w-[10rem] flex-1">
                                <CardContent className="pt-5 pb-4">
                                    <p className="text-sm font-medium text-muted-foreground">{card.label}</p>
                                    <p className="mt-0.5 text-2xl font-semibold">{card.value.toLocaleString()}</p>
                                </CardContent>
                            </Card>
                        ))}
                    </div>

                    {alerts.length === 0 ? (
                        <Card>
                            <CardContent className="pt-4 pb-4">
                                <p className="text-sm text-muted-foreground">No alert events</p>
                            </CardContent>
                        </Card>
                    ) : (
                        <div className="rounded-lg border">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Alert Name</TableHead>
                                        <TableHead>Severity</TableHead>
                                        <TableHead>Total Alerts Triggered</TableHead>
                                        <TableHead>History</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {alerts.map(alert => (
                                        <TableRow key={alert.id}>
                                            <TableCell>
                                                <p className="text-sm font-medium">{alert.name}</p>
                                            </TableCell>
                                            <TableCell>
                                                <SeverityBadge severity={alert.severity} />
                                            </TableCell>
                                            <TableCell>
                                                <span className="text-sm">{alert.events_count}</span>
                                            </TableCell>
                                            <TableCell>
                                                <Button asChild variant="link" className="h-auto px-0">
                                                    <Link to={`../${alert.id}?tab=history`} aria-label={`View history for ${alert.name}`}>
                                                        View history
                                                    </Link>
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
