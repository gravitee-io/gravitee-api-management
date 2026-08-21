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
    Button,
    DataTable,
    DataTableColumnHeader,
    DataTableEmptyState,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { clampPage, paginateClientSideTableItems } from '../../../shared/utils/clientSideTableUtils';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { Tenant } from '../types/tenant';
import { filterTenants } from '../utils/tenantTableUtils';

const DEFAULT_PAGE_SIZE = 10;
const SORTABLE_IDS = new Set(['key', 'name', 'description']);

function sortRows(items: Tenant[], sorting: TableSortingState): Tenant[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_IDS.has(active.id)) return items;
    const direction = active.desc ? -1 : 1;
    return [...items].sort((a, b) => {
        const av = active.id === 'key' ? a.key : active.id === 'name' ? a.name : (a.description ?? '');
        const bv = active.id === 'key' ? b.key : active.id === 'name' ? b.name : (b.description ?? '');
        return av.localeCompare(bv) * direction;
    });
}

function TenantActionsCell({
    tenant,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    tenant: Tenant;
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (row: Tenant) => void;
    onDelete: (row: Tenant) => void;
}>) {
    const ariaLabel = tenant.key ? `Actions for ${tenant.key}` : 'Tenant actions';

    return (
        <div className="flex justify-end">
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="size-8" aria-label={ariaLabel}>
                        <MoreHorizontalIcon className="size-4" aria-hidden />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="min-w-48">
                    {canEdit ? (
                        <DropdownMenuItem className="whitespace-nowrap" onSelect={() => onEdit(tenant)}>
                            <PencilIcon className="size-4 mr-2 shrink-0" aria-hidden />
                            Edit
                        </DropdownMenuItem>
                    ) : null}
                    {canEdit && canDelete ? <DropdownMenuSeparator /> : null}
                    {canDelete ? (
                        <DropdownMenuItem variant="destructive" className="whitespace-nowrap" onSelect={() => onDelete(tenant)}>
                            <Trash2Icon className="size-4 mr-2 shrink-0" aria-hidden />
                            Delete
                        </DropdownMenuItem>
                    ) : null}
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}

function buildColumns({
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: {
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (row: Tenant) => void;
    onDelete: (row: Tenant) => void;
}): DataTableProps<Tenant>['columns'] {
    const columns: DataTableProps<Tenant>['columns'] = [
        {
            id: 'key',
            accessorKey: 'key',
            header: ({ column }: ColHeader<Tenant>) => <DataTableColumnHeader column={column} title="Key" />,
            cell: ({ row }: ColCell<Tenant>) => <span className="text-sm font-medium">{row.original.key || '—'}</span>,
        },
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<Tenant>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<Tenant>) => <span className="text-sm">{row.original.name || '—'}</span>,
        },
        {
            id: 'description',
            accessorKey: 'description',
            header: ({ column }: ColHeader<Tenant>) => <DataTableColumnHeader column={column} title="Description" />,
            cell: ({ row }: ColCell<Tenant>) => <span className="text-sm text-muted-foreground">{row.original.description || '—'}</span>,
        },
    ];

    if (canEdit || canDelete) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            enableSorting: false,
            cell: ({ row }: ColCell<Tenant>) => (
                <TenantActionsCell tenant={row.original} canEdit={canEdit} canDelete={canDelete} onEdit={onEdit} onDelete={onDelete} />
            ),
        });
    }

    return columns;
}

export function TenantsTable({
    rows,
    canEdit = false,
    canDelete = false,
    onEdit,
    onDelete,
}: Readonly<{
    rows: Tenant[];
    canEdit?: boolean;
    canDelete?: boolean;
    onEdit?: (row: Tenant) => void;
    onDelete?: (row: Tenant) => void;
}>) {
    const [search, setSearch] = useState('');
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => filterTenants(rows, search), [rows, search]);
    const sorted = useMemo(() => sortRows(filtered, sorting), [filtered, sorting]);
    const totalCount = sorted.length;
    // Clamped during render as well as in the effect: a row count that shrinks would otherwise
    // slice past the end and flash the no-results state for one render.
    const currentPage = clampPage(page, totalCount, pageSize);
    const paginatedData = useMemo(() => paginateClientSideTableItems(sorted, currentPage, pageSize), [sorted, currentPage, pageSize]);

    useEffect(() => {
        setPage(prev => clampPage(prev, totalCount, pageSize));
    }, [totalCount, pageSize]);
    const columns = useMemo(
        () =>
            buildColumns({
                canEdit,
                canDelete,
                onEdit: onEdit ?? (() => undefined),
                onDelete: onDelete ?? (() => undefined),
            }),
        [canEdit, canDelete, onEdit, onDelete],
    );

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function handleSortingChange(updater: TableSortingState | ((prev: TableSortingState) => TableSortingState)) {
        setSorting(prev => (typeof updater === 'function' ? updater(prev) : updater));
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    return (
        <div className="space-y-3">
            <div className="max-w-sm">
                <InputGroup>
                    <InputGroupAddon align="inline-start">
                        <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                    </InputGroupAddon>
                    <InputGroupInput
                        placeholder="Search by key, name, or description..."
                        value={search}
                        onChange={e => handleSearchChange(e.target.value)}
                        aria-label="Search tenants"
                    />
                </InputGroup>
            </div>
            <div data-testid="tenants-table-body">
                <DataTable
                    aria-label="Tenants"
                    columns={columns}
                    data={paginatedData}
                    sorting={sorting}
                    onSortingChange={handleSortingChange}
                    pagination={{
                        page: currentPage,
                        pageSize,
                        totalCount,
                        pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                        onPageChange: setPage,
                        onPageSizeChange: handlePageSizeChange,
                    }}
                    emptyMessage={
                        <DataTableEmptyState
                            variant="no-results"
                            icon={<SearchIcon />}
                            title="No tenants found"
                            description="Try adjusting your search."
                        />
                    }
                />
            </div>
        </div>
    );
}
