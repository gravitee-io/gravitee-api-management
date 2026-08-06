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
    BadgeCell,
    Button,
    DataTable,
    DataTableColumnHeader,
    DataTableEmptyState,
    DateCell,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    TruncatedCell,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { CheckIcon, MoreVerticalIcon, SearchIcon, ShieldIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { clampPage, paginateClientSideTableItems } from '../../../shared/utils/clientSideTableUtils';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { IdentityProviderRow } from '../types/identityProvider';
import { identityProviderTypeLabel } from '../utils/identityProviderDisplay';
import { filterIdentityProviders, sortFilteredIdentityProviders } from '../utils/identityProviderTableUtils';

const DEFAULT_PAGE_SIZE = 10;

function PresenceCheck({ label }: { readonly label: string }) {
    return (
        <span>
            <CheckIcon className="size-4 text-muted-foreground" aria-hidden />
            <span className="sr-only">{label}</span>
        </span>
    );
}

function canToggleProviderActivation(canActivate: boolean, provider: IdentityProviderRow): boolean {
    return canActivate && provider.activated !== undefined;
}

function IdentityProviderActionsCell({
    provider,
    canActivate,
    canDelete,
    onToggle,
    onDelete,
}: Readonly<{
    provider: IdentityProviderRow;
    canActivate: boolean;
    canDelete: boolean;
    onToggle: (row: IdentityProviderRow) => void;
    onDelete: (row: IdentityProviderRow) => void;
}>) {
    const canToggleActivation = canToggleProviderActivation(canActivate, provider);
    if (!canToggleActivation && !canDelete) {
        return null;
    }

    return (
        <div className="flex justify-end">
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="size-8" aria-label={`Actions for ${provider.name}`}>
                        <MoreVerticalIcon className="size-4" aria-hidden />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="min-w-48">
                    {canToggleActivation ? (
                        <DropdownMenuItem className="whitespace-nowrap" onSelect={() => onToggle(provider)}>
                            <ShieldIcon className="mr-2 size-4 shrink-0" aria-hidden />
                            {provider.activated ? 'Deactivate' : 'Activate'}
                        </DropdownMenuItem>
                    ) : null}
                    {canToggleActivation && canDelete ? <DropdownMenuSeparator /> : null}
                    {canDelete ? (
                        <DropdownMenuItem variant="destructive" className="whitespace-nowrap" onSelect={() => onDelete(provider)}>
                            <Trash2Icon className="mr-2 size-4 shrink-0" aria-hidden />
                            Delete
                        </DropdownMenuItem>
                    ) : null}
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}

function buildColumns({
    rows,
    canActivate,
    canDelete,
    onToggle,
    onDelete,
}: {
    rows: IdentityProviderRow[];
    canActivate: boolean;
    canDelete: boolean;
    onToggle: (row: IdentityProviderRow) => void;
    onDelete: (row: IdentityProviderRow) => void;
}): DataTableProps<IdentityProviderRow>['columns'] {
    const columns: DataTableProps<IdentityProviderRow>['columns'] = [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<IdentityProviderRow>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<IdentityProviderRow>) => (
                <Button asChild variant="link" className="h-auto p-0 text-left text-sm font-medium text-foreground hover:underline">
                    <Link to={row.original.id}>{row.original.name}</Link>
                </Button>
            ),
        },
        {
            id: 'id',
            accessorKey: 'id',
            header: ({ column }: ColHeader<IdentityProviderRow>) => <DataTableColumnHeader column={column} title="Id" />,
            cell: ({ row }: ColCell<IdentityProviderRow>) => <TruncatedCell value={row.original.id} />,
        },
        {
            id: 'activated',
            accessorKey: 'activated',
            header: ({ column }: ColHeader<IdentityProviderRow>) => <DataTableColumnHeader column={column} title="Status" />,
            cell: ({ row }: ColCell<IdentityProviderRow>) =>
                row.original.activated === undefined ? (
                    <span className="text-muted-foreground">—</span>
                ) : (
                    <BadgeCell
                        value={row.original.activated ? 'Activated' : 'Deactivated'}
                        variant={row.original.activated ? 'success' : 'warning'}
                    />
                ),
        },
        {
            id: 'type',
            accessorKey: 'type',
            header: ({ column }: ColHeader<IdentityProviderRow>) => <DataTableColumnHeader column={column} title="Type" />,
            cell: ({ row }: ColCell<IdentityProviderRow>) => (
                <BadgeCell value={identityProviderTypeLabel(row.original.type)} variant="secondary" />
            ),
        },
        {
            id: 'description',
            accessorKey: 'description',
            header: ({ column }: ColHeader<IdentityProviderRow>) => <DataTableColumnHeader column={column} title="Description" />,
            cell: ({ row }: ColCell<IdentityProviderRow>) => <TruncatedCell value={row.original.description.trim() || '—'} />,
        },
        {
            id: 'sync',
            accessorKey: 'sync',
            header: ({ column }: ColHeader<IdentityProviderRow>) => <DataTableColumnHeader column={column} title="Sync" />,
            cell: ({ row }: ColCell<IdentityProviderRow>) => (row.original.sync ? <PresenceCheck label="Synced" /> : null),
        },
        {
            id: 'enabled',
            accessorKey: 'enabled',
            header: ({ column }: ColHeader<IdentityProviderRow>) => (
                <DataTableColumnHeader column={column} title="Available on dev portal" />
            ),
            cell: ({ row }: ColCell<IdentityProviderRow>) =>
                row.original.enabled ? <PresenceCheck label="Available on developer portal" /> : null,
        },
        {
            id: 'updated_at',
            accessorKey: 'updated_at',
            header: ({ column }: ColHeader<IdentityProviderRow>) => <DataTableColumnHeader column={column} title="Updated at" />,
            cell: ({ row }: ColCell<IdentityProviderRow>) => <DateCell value={new Date(row.original.updated_at)} />,
        },
    ];

    if (canDelete || rows.some(row => canToggleProviderActivation(canActivate, row))) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<IdentityProviderRow>) => (
                <IdentityProviderActionsCell
                    provider={row.original}
                    canActivate={canActivate}
                    canDelete={canDelete}
                    onToggle={onToggle}
                    onDelete={onDelete}
                />
            ),
        });
    }

    return columns;
}

export function IdentityProvidersTable({
    rows,
    canActivate = false,
    canDelete = false,
    onToggle,
    onDelete,
}: Readonly<{
    rows: IdentityProviderRow[];
    canActivate?: boolean;
    canDelete?: boolean;
    onToggle?: (row: IdentityProviderRow) => void;
    onDelete?: (row: IdentityProviderRow) => void;
}>) {
    const [search, setSearch] = useState('');
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => filterIdentityProviders(rows, search), [rows, search]);
    const sorted = useMemo(() => sortFilteredIdentityProviders(filtered, sorting), [filtered, sorting]);
    const totalCount = sorted.length;
    const currentPage = clampPage(page, totalCount, pageSize);
    const paginatedData = useMemo(() => paginateClientSideTableItems(sorted, currentPage, pageSize), [sorted, currentPage, pageSize]);

    useEffect(() => {
        setPage(prev => clampPage(prev, totalCount, pageSize));
    }, [totalCount, pageSize]);

    const columns = useMemo(
        () =>
            buildColumns({
                rows,
                canActivate,
                canDelete,
                onToggle: onToggle ?? (() => undefined),
                onDelete: onDelete ?? (() => undefined),
            }),
        [rows, canActivate, canDelete, onToggle, onDelete],
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
        <DataTable
            aria-label="Identity Providers"
            columns={columns}
            data={paginatedData}
            skeletonCount={pageSize}
            serverSide
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
            toolbar={
                <div className="h-8 w-64">
                    <InputGroup>
                        <InputGroupAddon align="inline-start">
                            <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                        </InputGroupAddon>
                        <InputGroupInput
                            placeholder="Search by name, id, type, or description..."
                            value={search}
                            onChange={event => handleSearchChange(event.target.value)}
                            aria-label="Search identity providers"
                        />
                    </InputGroup>
                </div>
            }
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon className="size-8" aria-hidden />}
                    title="No identity providers found"
                    description="Try adjusting your search."
                    action={
                        <Button size="sm" variant="outline" onClick={() => handleSearchChange('')}>
                            Clear search
                        </Button>
                    }
                />
            }
        />
    );
}
