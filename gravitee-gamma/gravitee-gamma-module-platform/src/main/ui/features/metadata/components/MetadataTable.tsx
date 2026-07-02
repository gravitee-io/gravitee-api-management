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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Input,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { MetadataFormatBadge } from './MetadataFormatBadge';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { Metadata } from '../types/metadata';

const DEFAULT_PAGE_SIZE = 10;

const SORTABLE_IDS = new Set(['key', 'name', 'format', 'value']);

function sortMetadata(items: Metadata[], sorting: TableSortingState): Metadata[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_IDS.has(active.id)) return items;
    const direction = active.desc ? -1 : 1;
    return [...items].sort((a, b) => {
        const av = active.id === 'key' ? a.key : active.id === 'name' ? a.name : active.id === 'format' ? a.format : (a.value ?? '');
        const bv = active.id === 'key' ? b.key : active.id === 'name' ? b.name : active.id === 'format' ? b.format : (b.value ?? '');
        return av.localeCompare(bv) * direction;
    });
}

function buildColumns({
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: {
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (metadata: Metadata) => void;
    onDelete: (metadata: Metadata) => void;
}): DataTableProps<Metadata>['columns'] {
    const columns: DataTableProps<Metadata>['columns'] = [
        {
            id: 'key',
            accessorKey: 'key',
            header: ({ column }: ColHeader<Metadata>) => <DataTableColumnHeader column={column} title="Key" />,
            cell: ({ row }: ColCell<Metadata>) => (
                <span className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded">{row.original.key}</span>
            ),
        },
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<Metadata>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<Metadata>) => <span className="text-sm font-medium">{row.original.name}</span>,
        },
        {
            id: 'format',
            accessorKey: 'format',
            header: ({ column }: ColHeader<Metadata>) => <DataTableColumnHeader column={column} title="Format" />,
            cell: ({ row }: ColCell<Metadata>) => <MetadataFormatBadge format={row.original.format} />,
        },
        {
            id: 'value',
            accessorKey: 'value',
            header: ({ column }: ColHeader<Metadata>) => <DataTableColumnHeader column={column} title="Value" />,
            cell: ({ row }: ColCell<Metadata>) =>
                row.original.value ? (
                    <span className="text-sm">{row.original.value}</span>
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
    ];

    if (canEdit || canDelete) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<Metadata>) => (
                <div className="flex justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="size-8" aria-label="Metadata actions">
                                <MoreHorizontalIcon className="size-4" aria-hidden />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                            {canEdit && (
                                <DropdownMenuItem onSelect={() => onEdit(row.original)}>
                                    <PencilIcon className="size-4 mr-2" aria-hidden />
                                    Edit
                                </DropdownMenuItem>
                            )}
                            {canEdit && canDelete && <DropdownMenuSeparator />}
                            {canDelete && (
                                <DropdownMenuItem variant="destructive" onSelect={() => onDelete(row.original)}>
                                    <Trash2Icon className="size-4 mr-2" aria-hidden />
                                    Delete
                                </DropdownMenuItem>
                            )}
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            ),
        });
    }

    return columns;
}

export function MetadataTable({
    metadata,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    metadata: Metadata[];
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (metadata: Metadata) => void;
    onDelete: (metadata: Metadata) => void;
}>) {
    const [search, setSearch] = useState('');
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) return metadata;
        return metadata.filter(m => m.key.toLowerCase().includes(query) || m.name.toLowerCase().includes(query));
    }, [metadata, search]);

    const sorted = useMemo(() => sortMetadata(filtered, sorting), [filtered, sorting]);

    const totalCount = sorted.length;

    const paginatedData = useMemo(() => {
        const start = (page - 1) * pageSize;
        return sorted.slice(start, start + pageSize);
    }, [sorted, page, pageSize]);

    const columns = useMemo(() => buildColumns({ canEdit, canDelete, onEdit, onDelete }), [canEdit, canDelete, onEdit, onDelete]);

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
            <div className="relative max-w-sm">
                <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                <Input
                    className="pl-10"
                    placeholder="Search by key or name…"
                    value={search}
                    onChange={e => handleSearchChange(e.target.value)}
                />
            </div>

            <DataTable
                columns={columns}
                data={paginatedData}
                sorting={sorting}
                onSortingChange={handleSortingChange}
                serverSide
                pagination={{
                    page,
                    pageSize,
                    totalCount,
                    pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                    onPageChange: setPage,
                    onPageSizeChange: handlePageSizeChange,
                }}
                emptyMessage={search.trim() ? 'No metadata matches your search.' : 'No global metadata defined for this environment.'}
            />
        </div>
    );
}
