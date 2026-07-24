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
    DropdownMenuTrigger,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { DictionaryTypeLabel } from './DictionaryTypeLabel';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import { truncateLabel } from '../../shared/utils/truncateLabel';
import type { DictionaryListItem } from '../types/dictionary';
import { formatDictionaryDate } from '../utils/formatDictionaryDate';

/** Keep name-column descriptions short so Type (incl. Dynamic · Started) stays visible. */
const DESCRIPTION_MAX_LENGTH = 56;

const DEFAULT_PAGE_SIZE = 10;

const SORTABLE_IDS = new Set(['name', 'type', 'properties', 'updated_at', 'deployed_at']);
const DATE_SORTABLE_IDS = new Set(['updated_at', 'deployed_at']);

function toSortableTimestamp(value: unknown): number | null {
    if (value === undefined || value === null || value === '') return null;
    if (typeof value === 'number') {
        return Number.isFinite(value) ? value : null;
    }

    const raw = String(value).trim();
    if (/^\d+$/.test(raw)) {
        const epoch = Number(raw);
        return Number.isFinite(epoch) ? epoch : null;
    }

    const parsed = Date.parse(raw);
    return Number.isNaN(parsed) ? null : parsed;
}

function compareDictionaryValues(field: string, left: unknown, right: unknown): number {
    if (field === 'properties') {
        return (Number(left) || 0) - (Number(right) || 0);
    }

    if (DATE_SORTABLE_IDS.has(field)) {
        const leftTime = toSortableTimestamp(left);
        const rightTime = toSortableTimestamp(right);
        if (leftTime === null && rightTime === null) return 0;
        if (leftTime === null) return 1;
        if (rightTime === null) return -1;
        return leftTime - rightTime;
    }

    return String(left ?? '').localeCompare(String(right ?? ''));
}

function sortDictionaries(items: DictionaryListItem[], sorting: TableSortingState): DictionaryListItem[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_IDS.has(active.id)) return items;
    const field = active.id as keyof DictionaryListItem;
    const direction = active.desc ? -1 : 1;
    return [...items].sort((a, b) => compareDictionaryValues(active.id, a[field], b[field]) * direction);
}

function filterDictionaries(dictionaries: DictionaryListItem[], search: string): DictionaryListItem[] {
    const query = search.trim().toLowerCase();
    if (!query) return dictionaries;
    return dictionaries.filter(
        d =>
            d.name.toLowerCase().includes(query) ||
            (d.key ?? '').toLowerCase().includes(query) ||
            (d.description ?? '').toLowerCase().includes(query),
    );
}

function paginateDictionaries(items: DictionaryListItem[], page: number, pageSize: number): DictionaryListItem[] {
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
}

function DictionaryActionsCell({
    dictionary,
    onDelete,
}: Readonly<{
    dictionary: DictionaryListItem;
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
                    <DropdownMenuItem variant="destructive" onSelect={() => onDelete(dictionary)}>
                        <Trash2Icon className="size-4 mr-2" aria-hidden />
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}

function buildColumns({
    canDelete,
    onOpen,
    onDelete,
}: {
    canDelete: boolean;
    onOpen: (dictionary: DictionaryListItem) => void;
    onDelete: (dictionary: DictionaryListItem) => void;
}): DataTableProps<DictionaryListItem>['columns'] {
    const columns: DataTableProps<DictionaryListItem>['columns'] = [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => {
                const fullDescription = row.original.description?.trim() || '';
                const description = fullDescription ? truncateLabel(fullDescription, DESCRIPTION_MAX_LENGTH) : '—';
                return (
                    <button
                        type="button"
                        className="min-w-0 space-y-0.5 text-left hover:underline focus-visible:outline-none focus-visible:underline"
                        onClick={() => onOpen(row.original)}
                    >
                        <div className="truncate text-sm font-medium text-foreground">{row.original.name}</div>
                        <div className="whitespace-nowrap text-xs text-muted-foreground" title={fullDescription || undefined}>
                            {description}
                        </div>
                    </button>
                );
            },
        },
        {
            id: 'type',
            accessorKey: 'type',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Type" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => <DictionaryTypeLabel type={row.original.type} state={row.original.state} />,
        },
        {
            id: 'properties',
            accessorKey: 'properties',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Properties" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => (
                <span className="text-sm text-muted-foreground">{row.original.properties ?? 0}</span>
            ),
        },
        {
            id: 'updated_at',
            accessorKey: 'updated_at',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Last Updated" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => (
                <span className="whitespace-nowrap text-sm text-muted-foreground">{formatDictionaryDate(row.original.updated_at)}</span>
            ),
        },
        {
            id: 'deployed_at',
            accessorKey: 'deployed_at',
            header: ({ column }: ColHeader<DictionaryListItem>) => <DataTableColumnHeader column={column} title="Last Deployed" />,
            cell: ({ row }: ColCell<DictionaryListItem>) => (
                <span className="whitespace-nowrap text-sm text-muted-foreground">{formatDictionaryDate(row.original.deployed_at)}</span>
            ),
        },
    ];

    if (canDelete) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<DictionaryListItem>) => <DictionaryActionsCell dictionary={row.original} onDelete={onDelete} />,
        });
    }

    return columns;
}

export function DictionariesTable({
    dictionaries,
    canDelete,
    onOpen,
    onDelete,
}: Readonly<{
    dictionaries: DictionaryListItem[];
    canDelete: boolean;
    onOpen: (dictionary: DictionaryListItem) => void;
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

    const columns = useMemo(() => buildColumns({ canDelete, onOpen, onDelete }), [canDelete, onOpen, onDelete]);

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
            <div className="max-w-sm">
                <InputGroup>
                    <InputGroupAddon align="inline-start">
                        <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                    </InputGroupAddon>
                    <InputGroupInput
                        placeholder="Search by key, name, or description…"
                        value={search}
                        onChange={e => handleSearchChange(e.target.value)}
                    />
                </InputGroup>
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
