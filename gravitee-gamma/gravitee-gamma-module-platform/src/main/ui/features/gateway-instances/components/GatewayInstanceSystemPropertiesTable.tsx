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

import { DataTable, InputGroup, InputGroupAddon, InputGroupInput, type DataTableProps } from '@gravitee/graphene-core';
import { ListIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo } from 'react';

import type { ColCell } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import { useClientFilteredPagination } from '../hooks/useClientFilteredPagination';

export interface SystemPropertyRow {
    name: string;
    value: string;
}

function buildColumns(): DataTableProps<SystemPropertyRow>['columns'] {
    return [
        {
            id: 'name',
            accessorKey: 'name',
            header: 'Name',
            enableSorting: false,
            cell: ({ row }: ColCell<SystemPropertyRow>) => <span className="font-mono text-sm whitespace-nowrap">{row.original.name}</span>,
        },
        {
            id: 'value',
            accessorKey: 'value',
            header: 'Value',
            enableSorting: false,
            cell: ({ row }: ColCell<SystemPropertyRow>) => (
                <span className="text-sm text-muted-foreground break-all">{row.original.value}</span>
            ),
        },
    ];
}

export function GatewayInstanceSystemPropertiesTable({ properties }: Readonly<{ properties: SystemPropertyRow[] }>) {
    const matchesSearch = useCallback(
        (item: SystemPropertyRow, query: string) => item.name.toLowerCase().includes(query) || item.value.toLowerCase().includes(query),
        [],
    );
    const { search, page, pageSize, totalCount, pageData, handleSearchChange, setPage, handlePageSizeChange } = useClientFilteredPagination(
        properties,
        matchesSearch,
    );
    const columns = useMemo(() => buildColumns(), []);

    return (
        <section className="space-y-3 rounded-xl border bg-card p-4" data-testid="gateway-instance-system-properties">
            <h2 className="flex items-center gap-2 text-base font-semibold">
                <ListIcon className="size-5" aria-hidden />
                System properties
            </h2>
            <div className="max-w-sm">
                <InputGroup>
                    <InputGroupAddon align="inline-start">
                        <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                    </InputGroupAddon>
                    <InputGroupInput
                        placeholder="Search"
                        value={search}
                        onChange={e => handleSearchChange(e.target.value)}
                        aria-label="Search system properties"
                    />
                </InputGroup>
            </div>
            <DataTable
                columns={columns}
                data={pageData}
                serverSide
                pagination={{
                    page,
                    pageSize,
                    totalCount,
                    pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                    onPageChange: setPage,
                    onPageSizeChange: handlePageSizeChange,
                }}
                emptyMessage={search.trim() ? 'No properties match your search.' : 'No property'}
            />
        </section>
    );
}
