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
import { Card, CardContent, CardHeader, CardTitle, DataTable, DataTableEmptyState, type DataTableProps } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import type { AvailabilityFieldData } from './useHealthCheckDashboard';
import type { HealthField } from '../../../../types/healthCheck';
import { availabilityColorClass } from '../../../../utils/healthCheckDashboard';

interface AvailabilityByFieldTableProps {
    field: HealthField;
    data: AvailabilityFieldData;
}

type AvailabilityRow = AvailabilityFieldData['rows'][number];

type ColCell<T> = { row: { original: T } };

/**
 * The availability / average-response-time `group` map comes from an Elasticsearch
 * `terms` aggregation (size 100), so a proxy with many endpoints/gateways yields up
 * to 100 rows in a single response. These endpoints expose no server-side paging,
 * so — like the classic console — we paginate client-side (default page size 5).
 */
const PAGE_SIZE_OPTIONS = [5, 10, 25];
const DEFAULT_PAGE_SIZE = 5;

const FIELD_LABEL: Record<HealthField, { title: string; description: string; column: string }> = {
    endpoint: {
        title: 'Availability per endpoint',
        description: 'Availability per endpoint where health checks are enabled.',
        column: 'Endpoint',
    },
    gateway: {
        title: 'Availability per gateway',
        description: 'Availability per gateway where health checks are enabled.',
        column: 'Gateway',
    },
};

export function AvailabilityByFieldTable({ field, data }: Readonly<AvailabilityByFieldTableProps>) {
    const labels = FIELD_LABEL[field];
    const { rows, isLoading } = data;

    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const totalCount = rows.length;
    const pageCount = Math.max(1, Math.ceil(totalCount / pageSize));
    // Clamp so a timeframe change that shrinks the result set can't leave us on an empty page.
    const safePage = Math.min(page, pageCount);
    const pageRows = useMemo(() => rows.slice((safePage - 1) * pageSize, safePage * pageSize), [rows, safePage, pageSize]);

    const columns: DataTableProps<AvailabilityRow>['columns'] = [
        {
            id: 'field',
            accessorFn: (row: AvailabilityRow) => row.name,
            header: labels.column,
            enableSorting: false,
            cell: ({ row }: ColCell<AvailabilityRow>) => <span className="font-medium">{row.original.name}</span>,
        },
        {
            id: 'availability',
            header: () => <div className="text-right">Availability</div>,
            enableSorting: false,
            cell: ({ row }: ColCell<AvailabilityRow>) => (
                <div className={`text-right font-medium ${availabilityColorClass(row.original.availabilityPct)}`}>
                    {row.original.availabilityPct} %
                </div>
            ),
        },
        {
            id: 'responseTime',
            header: () => <div className="text-right">Response time</div>,
            enableSorting: false,
            cell: ({ row }: ColCell<AvailabilityRow>) => (
                <div className="text-right text-muted-foreground">
                    {row.original.avgResponseTimeMs !== undefined ? `${row.original.avgResponseTimeMs} ms` : '—'}
                </div>
            ),
        },
    ];

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-base">{labels.title}</CardTitle>
                <p className="text-sm text-muted-foreground">{labels.description}</p>
            </CardHeader>
            <CardContent className="px-4 pb-4 pt-2">
                <DataTable
                    aria-label={labels.title}
                    columns={columns}
                    data={pageRows}
                    loading={isLoading}
                    skeletonCount={DEFAULT_PAGE_SIZE}
                    serverSide
                    pagination={
                        totalCount > 0
                            ? {
                                  page: safePage,
                                  pageSize,
                                  totalCount,
                                  pageSizeOptions: PAGE_SIZE_OPTIONS,
                                  onPageChange: setPage,
                                  onPageSizeChange: size => {
                                      setPageSize(size);
                                      setPage(1);
                                  },
                              }
                            : undefined
                    }
                    emptyMessage={
                        <DataTableEmptyState
                            variant="no-results"
                            title="No data"
                            description="No availability data for the selected period."
                        />
                    }
                />
            </CardContent>
        </Card>
    );
}
