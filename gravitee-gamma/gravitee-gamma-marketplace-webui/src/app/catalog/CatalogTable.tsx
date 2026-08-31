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
import { Badge, Button, DataTable, DataTableEmptyState, type DataTableProps } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

import { isMcpAgent, protocolLabel } from './protocol';
import { CATALOG_PAGE_SIZE_OPTIONS } from '../../api/catalog';
import type { Api } from '../../api/types';

type ColCell = { row: { original: Api } };

function buildColumns(): DataTableProps<Api>['columns'] {
    return [
        {
            id: 'name',
            accessorFn: (row: Api) => row.name,
            header: 'Name',
            enableSorting: false,
            cell: ({ row }: ColCell) => (
                <Link to={`/catalog/${row.original.id}`} className="font-medium hover:underline">
                    {row.original.name}
                </Link>
            ),
        },
        {
            id: 'protocol',
            accessorFn: (row: Api) => protocolLabel(row.type),
            header: 'Protocol',
            enableSorting: false,
            cell: ({ row }: ColCell) => <Badge variant="secondary">{protocolLabel(row.original.type)}</Badge>,
        },
        {
            id: 'version',
            accessorFn: (row: Api) => row.version,
            header: 'Version',
            enableSorting: false,
        },
        {
            id: 'labels',
            accessorFn: (row: Api) => row.labels?.join(', ') ?? '',
            header: 'Labels',
            enableSorting: false,
            cell: ({ row }: ColCell) => (
                <span className="text-sm text-muted-foreground">{row.original.labels?.join(', ') || '—'}</span>
            ),
        },
        {
            id: 'category',
            accessorFn: (row: Api) => row.categories?.join(', ') ?? '',
            header: 'Category',
            enableSorting: false,
            cell: ({ row }: ColCell) => (
                <span className="text-sm text-muted-foreground">{row.original.categories?.join(', ') || '—'}</span>
            ),
        },
        {
            id: 'mcp',
            accessorFn: (row: Api) => (isMcpAgent(row) ? 'MCP' : ''),
            header: 'MCP',
            enableSorting: false,
            cell: ({ row }: ColCell) =>
                isMcpAgent(row.original) ? (
                    <Badge variant="outline">MCP</Badge>
                ) : (
                    <span className="text-muted-foreground">—</span>
                ),
        },
    ];
}

interface CatalogTableProps {
    readonly agents: Api[];
    readonly loading: boolean;
    readonly page: number;
    readonly pageSize: number;
    readonly totalCount: number;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (pageSize: number) => void;
    readonly emptyMessage: ReactNode;
}

export function CatalogTable({
    agents,
    loading,
    page,
    pageSize,
    totalCount,
    onPageChange,
    onPageSizeChange,
    emptyMessage,
}: CatalogTableProps) {
    return (
        <DataTable
            aria-label="Agent catalog"
            columns={buildColumns()}
            data={agents}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            emptyMessage={emptyMessage}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...CATALOG_PAGE_SIZE_OPTIONS],
                onPageChange,
                onPageSizeChange: size => {
                    onPageSizeChange(size);
                },
            }}
        />
    );
}

export function CatalogNoResultsState({ onClear }: { onClear: () => void }) {
    return (
        <DataTableEmptyState
            variant="no-results"
            icon={<SearchIcon />}
            title="No agents match your search"
            description="Try adjusting your search terms or clearing filters."
            action={
                <Button type="button" size="sm" variant="outline" onClick={onClear}>
                    Clear filters
                </Button>
            }
        />
    );
}
