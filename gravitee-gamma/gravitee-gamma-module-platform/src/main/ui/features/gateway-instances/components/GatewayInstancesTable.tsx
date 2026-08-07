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

import { Button, DataTable, DataTableEmptyState, DateCell, type DataTableProps } from '@gravitee/graphene-core';
import { ServerIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';
import { Link } from 'react-router-dom';

import { GatewayInstanceStatusIcon } from './GatewayInstanceStatusIcon';
import type { ColCell } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { GatewayInstanceRow } from '../types/instance';

function buildColumns(): DataTableProps<GatewayInstanceRow>['columns'] {
    return [
        {
            id: 'hostname',
            accessorKey: 'hostname',
            header: 'Name',
            enableSorting: false,
            cell: ({ row }: ColCell<GatewayInstanceRow>) => (
                // Classic: hostname is an <a routerLink>; name-only (not full-row) navigation.
                <Button
                    asChild
                    variant="link"
                    className="h-auto cursor-pointer p-0 text-left font-medium text-foreground hover:underline hover:text-foreground"
                >
                    <Link to={`${row.original.id}/environment`} data-testid="gateway-instance-name-link">
                        {row.original.hostname || '—'}
                    </Link>
                </Button>
            ),
        },
        {
            id: 'version',
            accessorKey: 'version',
            header: 'Version',
            enableSorting: false,
            cell: ({ row }: ColCell<GatewayInstanceRow>) => <span className="text-sm">{row.original.version || '—'}</span>,
        },
        {
            id: 'state',
            accessorKey: 'state',
            header: 'Status',
            enableSorting: false,
            cell: ({ row }: ColCell<GatewayInstanceRow>) => <GatewayInstanceStatusIcon state={row.original.state} />,
        },
        {
            id: 'lastHeartbeat',
            accessorKey: 'lastHeartbeat',
            header: 'Last Heartbeat',
            enableSorting: false,
            cell: ({ row }: ColCell<GatewayInstanceRow>) =>
                row.original.lastHeartbeat ? (
                    <DateCell value={row.original.lastHeartbeat} format="absolute" />
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
        {
            id: 'os',
            accessorKey: 'os',
            header: 'OS',
            enableSorting: false,
            cell: ({ row }: ColCell<GatewayInstanceRow>) => <span className="text-sm">{row.original.os || '—'}</span>,
        },
        {
            id: 'ip-port',
            accessorFn: (row: GatewayInstanceRow) => `${row.ip}:${row.port}`,
            header: 'IP and Port',
            enableSorting: false,
            cell: ({ row }: ColCell<GatewayInstanceRow>) => (
                <span className="font-mono text-xs">
                    {row.original.ip || '—'}:{row.original.port || '—'}
                </span>
            ),
        },
        {
            id: 'tenant',
            accessorKey: 'tenant',
            header: 'Tenant',
            enableSorting: false,
            cell: ({ row }: ColCell<GatewayInstanceRow>) => <span className="text-sm">{row.original.tenant || '—'}</span>,
        },
        {
            id: 'tags',
            accessorFn: (row: GatewayInstanceRow) => row.tags.join(', '),
            header: 'Sharding Tags',
            enableSorting: false,
            cell: ({ row }: ColCell<GatewayInstanceRow>) => (
                <span className="text-sm">{row.original.tags.length > 0 ? row.original.tags.join(', ') : '—'}</span>
            ),
        },
    ];
}

export function GatewayInstancesTable({
    rows,
    isLoading,
    page,
    pageSize,
    totalCount,
    onPageChange,
    onPageSizeChange,
}: Readonly<{
    rows: GatewayInstanceRow[];
    isLoading: boolean;
    page: number;
    pageSize: number;
    totalCount: number;
    onPageChange: (page: number) => void;
    onPageSizeChange: (pageSize: number) => void;
}>) {
    const columns = useMemo(() => buildColumns(), []);

    return (
        <DataTable
            aria-label="Gateways"
            columns={columns}
            data={rows}
            serverSide
            loading={isLoading}
            skeletonCount={pageSize}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange,
                onPageSizeChange,
            }}
            emptyMessage={
                <DataTableEmptyState
                    variant="first-use"
                    icon={<ServerIcon />}
                    title="There are no Gateway instances (yet)."
                    description="Gateway instances appear here once they register a heartbeat with this environment."
                />
            }
        />
    );
}
