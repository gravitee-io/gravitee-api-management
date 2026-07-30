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
    DropdownMenuTrigger,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { LockIcon, MoreHorizontalIcon, PencilIcon, PlusIcon, SearchIcon, RadioIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { ShardingTagRow } from '../types/entrypoint';
import { filterShardingTags } from '../utils/shardingTags';

const DEFAULT_PAGE_SIZE = 10;
const SORTABLE_IDS = new Set(['key', 'name', 'description', 'restrictedGroups']);

function sortRows(items: ShardingTagRow[], sorting: TableSortingState): ShardingTagRow[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_IDS.has(active.id)) return items;
    const direction = active.desc ? -1 : 1;
    return [...items].sort((a, b) => {
        let av = '';
        let bv = '';
        if (active.id === 'key') {
            av = a.key;
            bv = b.key;
        } else if (active.id === 'name') {
            av = a.name;
            bv = b.name;
        } else if (active.id === 'description') {
            av = a.description;
            bv = b.description;
        } else {
            av = a.restrictedGroupNames.join(', ');
            bv = b.restrictedGroupNames.join(', ');
        }
        return av.localeCompare(bv) * direction;
    });
}

function ShardingTagActionsCell({
    tag,
    canEdit,
    onEdit,
}: Readonly<{
    tag: ShardingTagRow;
    canEdit: boolean;
    onEdit: (row: ShardingTagRow) => void;
}>) {
    const ariaLabel = tag.key ? `Actions for ${tag.key}` : 'Sharding tag actions';

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
                        <DropdownMenuItem className="whitespace-nowrap" onSelect={() => onEdit(tag)}>
                            <PencilIcon className="size-4 mr-2 shrink-0" aria-hidden />
                            Edit
                        </DropdownMenuItem>
                    ) : null}
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}

function buildColumns({
    onOpenDetail,
    canEdit,
    onEdit,
}: {
    onOpenDetail: (row: ShardingTagRow) => void;
    canEdit: boolean;
    onEdit: (row: ShardingTagRow) => void;
}): DataTableProps<ShardingTagRow>['columns'] {
    const columns: DataTableProps<ShardingTagRow>['columns'] = [
        {
            id: 'key',
            accessorKey: 'key',
            header: ({ column }: ColHeader<ShardingTagRow>) => <DataTableColumnHeader column={column} title="Key" />,
            cell: ({ row }: ColCell<ShardingTagRow>) => (
                <button type="button" className="text-sm font-medium text-left hover:underline" onClick={() => onOpenDetail(row.original)}>
                    {row.original.key || '—'}
                </button>
            ),
        },
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<ShardingTagRow>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<ShardingTagRow>) => (
                <button type="button" className="text-sm text-left hover:underline" onClick={() => onOpenDetail(row.original)}>
                    {row.original.name || '—'}
                </button>
            ),
        },
        {
            id: 'description',
            accessorKey: 'description',
            header: ({ column }: ColHeader<ShardingTagRow>) => <DataTableColumnHeader column={column} title="Description" />,
            cell: ({ row }: ColCell<ShardingTagRow>) => (
                <span className="text-sm text-muted-foreground">{row.original.description || '—'}</span>
            ),
        },
        {
            id: 'restrictedGroups',
            accessorFn: (row: ShardingTagRow) => row.restrictedGroupNames.join(', '),
            header: ({ column }: ColHeader<ShardingTagRow>) => <DataTableColumnHeader column={column} title="Restricted groups" />,
            cell: ({ row }: ColCell<ShardingTagRow>) => (
                <span className="text-sm">
                    {row.original.restrictedGroupNames.length > 0 ? row.original.restrictedGroupNames.join(', ') : '—'}
                </span>
            ),
        },
    ];

    if (canEdit) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            enableSorting: false,
            cell: ({ row }: ColCell<ShardingTagRow>) => <ShardingTagActionsCell tag={row.original} canEdit={canEdit} onEdit={onEdit} />,
        });
    }

    return columns;
}

export function CreateShardingTagButton({
    hasLicense,
    onCreate,
    onUpgrade,
}: Readonly<{
    hasLicense: boolean;
    onCreate?: () => void;
    onUpgrade: () => void;
}>) {
    return (
        <Button onClick={hasLicense ? onCreate : onUpgrade}>
            {hasLicense ? <PlusIcon className="size-4" aria-hidden /> : <LockIcon className="size-4" aria-hidden />}
            Add a tag
        </Button>
    );
}

export function ShardingTagsTable({
    rows,
    canCreate,
    hasLicense,
    canEdit = false,
    onOpenDetail,
    onEdit,
    onCreate,
    onUpgrade,
}: Readonly<{
    rows: ShardingTagRow[];
    canCreate: boolean;
    hasLicense: boolean;
    canEdit?: boolean;
    onOpenDetail: (row: ShardingTagRow) => void;
    onEdit?: (row: ShardingTagRow) => void;
    onCreate?: () => void;
    onUpgrade: () => void;
}>) {
    const [search, setSearch] = useState('');
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => filterShardingTags(rows, search), [rows, search]);
    const sorted = useMemo(() => sortRows(filtered, sorting), [filtered, sorting]);
    const totalCount = sorted.length;
    const paginatedData = useMemo(() => sorted.slice((page - 1) * pageSize, page * pageSize), [sorted, page, pageSize]);
    const columns = useMemo(
        () => buildColumns({ onOpenDetail, canEdit, onEdit: onEdit ?? (() => undefined) }),
        [onOpenDetail, canEdit, onEdit],
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

    if (rows.length === 0 && !search.trim()) {
        return (
            <DataTableEmptyState
                variant="first-use"
                icon={<RadioIcon />}
                title="No sharding tags"
                description="Sharding tags let you restrict entrypoints to specific gateway groups."
                primaryAction={
                    canCreate ? <CreateShardingTagButton hasLicense={hasLicense} onCreate={onCreate} onUpgrade={onUpgrade} /> : undefined
                }
            />
        );
    }

    return (
        <div className="space-y-3">
            <div className="max-w-sm">
                <InputGroup>
                    <InputGroupAddon align="inline-start">
                        <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                    </InputGroupAddon>
                    <InputGroupInput
                        placeholder="Search..."
                        value={search}
                        onChange={e => handleSearchChange(e.target.value)}
                        aria-label="Search sharding tags"
                    />
                </InputGroup>
            </div>
            <div data-testid="sharding-tags-table-body">
                <DataTable
                    aria-label="Sharding tags"
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
                    emptyMessage={
                        <DataTableEmptyState
                            variant="no-results"
                            icon={<SearchIcon />}
                            title="No sharding tags found"
                            description="Try adjusting your search."
                        />
                    }
                />
            </div>
        </div>
    );
}
