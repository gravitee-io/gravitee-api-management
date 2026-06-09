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
import {
    Alert,
    AlertDescription,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    DataTablePagination,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { useState } from 'react';

import { HealthCheckLogDetailSheet } from './HealthCheckLogDetailSheet';
import { FAILED_LOGS_PAGE_SIZES, useFailedHealthCheckLogs } from './useFailedHealthCheckLogs';
import type { HealthCheckLog } from '../../../../types/healthCheck';
import { formatTimestamp } from '../../../../utils/healthCheckDashboard';
import type { Timeframe } from '../../../../utils/healthTimeframe';

interface FailedHealthChecksTableProps {
    apiId: string | undefined;
    timeframe: Timeframe;
}

export function FailedHealthChecksTable({ apiId, timeframe }: Readonly<FailedHealthChecksTableProps>) {
    const { logs, totalCount, page, pageSize, isLoading, isError, setPage, setPageSize } = useFailedHealthCheckLogs(apiId, timeframe);
    const [selectedLog, setSelectedLog] = useState<HealthCheckLog | null>(null);

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-base">Failed health checks</CardTitle>
                <p className="text-sm text-muted-foreground">Recent failures, incidents, or downtimes from the health checks.</p>
            </CardHeader>
            <CardContent className="space-y-3 p-0 pt-2">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Timestamp</TableHead>
                            <TableHead>Endpoint</TableHead>
                            <TableHead>Gateway</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {isLoading ? (
                            <LoadingRows />
                        ) : isError ? (
                            <TableRow>
                                <TableCell colSpan={3}>
                                    <Alert variant="destructive">
                                        <AlertDescription>Failed to load health-check logs. Please refresh the page.</AlertDescription>
                                    </Alert>
                                </TableCell>
                            </TableRow>
                        ) : logs.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={3} className="text-sm text-muted-foreground">
                                    No failed health checks for the selected period.
                                </TableCell>
                            </TableRow>
                        ) : (
                            logs.map(log => (
                                <TableRow
                                    key={log.id}
                                    className="cursor-pointer"
                                    onClick={() => setSelectedLog(log)}
                                    tabIndex={0}
                                    role="button"
                                    aria-label={`View health check detail for ${log.endpointName}`}
                                    onKeyDown={event => {
                                        if (event.key === 'Enter' || event.key === ' ') {
                                            event.preventDefault();
                                            setSelectedLog(log);
                                        }
                                    }}
                                >
                                    <TableCell className="whitespace-nowrap">{formatTimestamp(log.timestamp)}</TableCell>
                                    <TableCell className="font-medium">{log.endpointName}</TableCell>
                                    <TableCell className="text-muted-foreground">{log.gatewayId}</TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>

                {totalCount > 0 && (
                    <div className="flex justify-end px-4 pb-4">
                        <DataTablePagination
                            page={page}
                            pageSize={pageSize}
                            totalCount={totalCount}
                            pageSizeOptions={FAILED_LOGS_PAGE_SIZES}
                            onPageChange={setPage}
                            onPageSizeChange={setPageSize}
                        />
                    </div>
                )}
            </CardContent>

            <HealthCheckLogDetailSheet log={selectedLog} onClose={() => setSelectedLog(null)} />
        </Card>
    );
}

function LoadingRows() {
    return (
        <>
            {[1, 2, 3].map(i => (
                <TableRow key={i}>
                    <TableCell>
                        <Skeleton className="h-4 w-40 rounded" />
                    </TableCell>
                    <TableCell>
                        <Skeleton className="h-4 w-32 rounded" />
                    </TableCell>
                    <TableCell>
                        <Skeleton className="h-4 w-24 rounded" />
                    </TableCell>
                </TableRow>
            ))}
        </>
    );
}
