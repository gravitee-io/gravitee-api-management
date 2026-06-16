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
    DataTable,
    DataTableEmptyState,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { HealthCheckLogDetailSheet } from './HealthCheckLogDetailSheet';
import { FAILED_LOGS_PAGE_SIZES, useFailedHealthCheckLogs } from './useFailedHealthCheckLogs';
import type { HealthCheckLog } from '../../../../types/healthCheck';
import { formatTimestamp } from '../../../../utils/healthCheckDashboard';
import type { Timeframe } from '../../../../utils/healthTimeframe';

type ColCell<T> = { row: { original: T } };

interface FailedHealthChecksTableProps {
    apiId: string | undefined;
    timeframe: Timeframe;
}

export function FailedHealthChecksTable({ apiId, timeframe }: Readonly<FailedHealthChecksTableProps>) {
    const { logs, totalCount, page, pageSize, isLoading, isError, setPage, setPageSize } = useFailedHealthCheckLogs(apiId, timeframe);
    const [selectedLog, setSelectedLog] = useState<HealthCheckLog | null>(null);

    const columns: DataTableProps<HealthCheckLog>['columns'] = [
        {
            id: 'Timestamp',
            accessorFn: (row: HealthCheckLog) => row.timestamp,
            header: 'Timestamp',
            enableSorting: false,
            cell: ({ row }: ColCell<HealthCheckLog>) => (
                <span className="whitespace-nowrap">{formatTimestamp(row.original.timestamp)}</span>
            ),
        },
        {
            id: 'Endpoint',
            accessorFn: (row: HealthCheckLog) => row.endpointName,
            header: 'Endpoint',
            enableSorting: false,
            cell: ({ row }: ColCell<HealthCheckLog>) => {
                const log = row.original;
                return (
                    <button
                        type="button"
                        className="text-left font-medium hover:underline"
                        aria-label={`View health check detail for ${log.endpointName}`}
                        onClick={() => setSelectedLog(log)}
                    >
                        {log.endpointName}
                    </button>
                );
            },
        },
        {
            id: 'Gateway',
            accessorFn: (row: HealthCheckLog) => row.gatewayId,
            header: 'Gateway',
            enableSorting: false,
            cell: ({ row }: ColCell<HealthCheckLog>) => <span className="text-muted-foreground">{row.original.gatewayId}</span>,
        },
    ];

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-base">Failed health checks</CardTitle>
                <p className="text-sm text-muted-foreground">Recent failures, incidents, or downtimes from the health checks.</p>
            </CardHeader>
            <CardContent className="px-4 pb-4 pt-2">
                {isError ? (
                    <Alert variant="destructive">
                        <AlertDescription>Failed to load health-check logs. Please refresh the page.</AlertDescription>
                    </Alert>
                ) : (
                    <DataTable
                        aria-label="Failed health checks"
                        columns={columns}
                        data={logs}
                        loading={isLoading}
                        skeletonCount={3}
                        serverSide
                        pagination={
                            totalCount > 0
                                ? {
                                      page,
                                      pageSize,
                                      totalCount,
                                      pageSizeOptions: FAILED_LOGS_PAGE_SIZES,
                                      onPageChange: setPage,
                                      onPageSizeChange: setPageSize,
                                  }
                                : undefined
                        }
                        emptyMessage={
                            <DataTableEmptyState
                                variant="no-results"
                                icon={<SearchIcon />}
                                title="No failed health checks"
                                description="No failures, incidents, or downtimes for the selected period."
                            />
                        }
                    />
                )}
            </CardContent>

            <HealthCheckLogDetailSheet log={selectedLog} onClose={() => setSelectedLog(null)} />
        </Card>
    );
}
