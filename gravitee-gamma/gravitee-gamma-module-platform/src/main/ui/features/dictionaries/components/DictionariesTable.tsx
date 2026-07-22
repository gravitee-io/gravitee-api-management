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

import { DictionaryStateBadge } from './DictionaryStateBadge';
import { DictionaryTypeBadge } from './DictionaryTypeBadge';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { DictionaryListItem } from '../types/dictionary';
import { formatDictionaryDate } from '../utils/formatDictionaryDate';

const DEFAULT_PAGE_SIZE = 10;

const SORTABLE_IDS = new Set(['key', 'name', 'type', 'state', 'updated_at', 'deployed_at']);

function sortDictionaries(items: DictionaryListItem[], sorting: TableSortingState): DictionaryListItem[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_IDS.has(active.id)) return items;
    const direction = active.desc ? -1 : 1;
    return [...items].sort((a, b) => {
        const av = String(a[active.id as keyof DictionaryListItem] ?? '');
        const bv = String(b[active.id as keyof DictionaryListItem] ?? '');
        return av.localeCompare(bv) * direction;
    });
}

function filterDictionaries(dictionaries: DictionaryListItem[], search: string): DictionaryListItem[] {
    const query = search.trim().toLowerCase();
    if (!query) return dictionaries;
    return dictionaries.filter(d => d.name.toLowerCase().includes(query) || (d.key ?? '').toLowerCase().includes(query));
}

function paginateDictionaries(items: DictionaryListItem[], page: number, pageSize: number): DictionaryListItem[] {
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
}

function DictionaryActionsCell({
    dictionary,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    dictionary: DictionaryListItem;
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (dictionary: DictionaryListItem) => void;
    onDelete: (dictionary: DictionaryListItem) => void;
}>) {
    return (
        <div className="flex justify-end">
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="size-8" aria-label="Dictionary actions">
                        <MoreHorizontalIcon className="size-4" aria-hidden />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                    {canEdit && (
                        <DropdownMenuItem onSelect={() => onEdit(dictionary)}>
                            <PencilIcon className="size-4 mr-2" aria-hidden />
                            Edit
                        </DropdownMenuItem>
                    )}
                    {canEdit && canDelete && <DropdownMenuSeparator />}
                    {canDelete && (
                        <DropdownMenuItem variant="destructive" onSelect={() => onDelete(dictionary)}>
                            <Trash2Icon className="size-4 mr-2" aria-hidden />
                            Delete
                        </DropdownMenuItem>
                    )}
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
    onEdit: (dictionary: DictionaryListItem) => void;
    onDelete: (dictionary: DictionaryListItem) => void;
}): DataTableProps<DictionaryListItem>['columns'] {
    const columns: DataTableProps<DictionaryListItem>['columns'] = [
        {
            id: 'key',
            accessorKey: 'key',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Key" />,
            cell: ({ row }: ColCell<DictionaryListItem>) =>
                row.original.key ? (
                    <span className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded">{row.original.key}</span>
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => <span className="text-sm font-medium">{row.original.name}</span>,
        },
        {
            id: 'type',
            accessorKey: 'type',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Type" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => <DictionaryTypeBadge type={row.original.type} />,
        },
        {
            id: 'state',
            accessorKey: 'state',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="State" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => <DictionaryStateBadge state={row.original.state} />,
        },
        {
            id: 'updated_at',
            accessorKey: 'updated_at',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Updated" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => (
                <span className="whitespace-nowrap text-sm text-muted-foreground">{formatDictionaryDate(row.original.updated_at)}</span>
            ),
        },
        {
            id: 'deployed_at',
            accessorKey: 'deployed_at',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Deployed" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => (
                <span className="whitespace-nowrap text-sm text-muted-foreground">{formatDictionaryDate(row.original.deployed_at)}</span>
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
            cell: ({ row }: ColCell<DictionaryListItem>) => (
                <DictionaryActionsCell
                    dictionary={row.original}
                    canEdit={canEdit}
                    canDelete={canDelete}
                    onEdit={onEdit}
                    onDelete={onDelete}
                />
            ),
        });
    }

    return columns;
}

export function DictionariesTable({
    dictionaries,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    dictionaries: DictionaryListItem[];
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (dictionary: DictionaryListItem) => void;
    onDelete: (dictionary: DictionaryListItem) => void;
}>) {
    const [search, setSearch] = useState('');
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => filterDictionaries(dictionaries, search), [dictionaries, search]);
    const sorted = useMemo(() => sortDictionaries(filtered, sorting), [filtered, sorting]);
    const totalCount = sorted.length;
    const paginatedData = useMemo(() => paginateDictionaries(sorted, page, pageSize), [sorted, page, pageSize]);

    const columns = useMemo(() => buildColumns({ canEdit, canDelete, onEdit, onDelete }), [canEdit, canDelete, onEdit, onDelete]);

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function handleSortingChange(updater: TableSortingState | ((prev: TableSortingState) => TableSortingState)) {
        setSorting(prev => {
            if (typeof updater === 'function') {
                return updater(prev);
            }
            return updater;
        });
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
                emptyMessage={search.trim() ? 'No dictionaries match your search.' : 'No dictionaries defined for this environment.'}
            />
        </div>
    );
}
