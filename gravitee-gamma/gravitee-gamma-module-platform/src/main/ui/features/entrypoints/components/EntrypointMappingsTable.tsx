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
import { MoreHorizontalIcon, PencilIcon, PlusIcon, RadioIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { ShardingTagsCell } from './ShardingTagsCell';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { EntrypointMappingRow, EntrypointTarget } from '../types/entrypoint';

const DEFAULT_PAGE_SIZE = 10;
const SORTABLE_IDS = new Set(['value', 'target', 'tags', 'environments']);
const CREATE_TARGETS: EntrypointTarget[] = ['HTTP', 'TCP', 'KAFKA'];

function sortRows(items: EntrypointMappingRow[], sorting: TableSortingState): EntrypointMappingRow[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_IDS.has(active.id)) return items;
    const direction = active.desc ? -1 : 1;
    return [...items].sort((a, b) => {
        let av = '';
        let bv = '';
        if (active.id === 'value') {
            av = a.value;
            bv = b.value;
        } else if (active.id === 'target') {
            av = a.targetLabel;
            bv = b.targetLabel;
        } else if (active.id === 'tags') {
            av = a.tagsName.join(', ');
            bv = b.tagsName.join(', ');
        } else {
            av = a.environmentNames.join(', ');
            bv = b.environmentNames.join(', ');
        }
        return av.localeCompare(bv) * direction;
    });
}

function targetMenuLabel(target: EntrypointTarget): string {
    return target === 'KAFKA' ? 'Kafka' : target;
}

function buildColumns({
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: {
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (row: EntrypointMappingRow) => void;
    onDelete: (row: EntrypointMappingRow) => void;
}): DataTableProps<EntrypointMappingRow>['columns'] {
    const columns: DataTableProps<EntrypointMappingRow>['columns'] = [
        {
            id: 'value',
            accessorKey: 'value',
            header: ({ column }: ColHeader<EntrypointMappingRow>) => <DataTableColumnHeader column={column} title="Entrypoint" />,
            cell: ({ row }: ColCell<EntrypointMappingRow>) => <span className="text-sm font-medium">{row.original.value || '-'}</span>,
        },
        {
            id: 'target',
            accessorKey: 'targetLabel',
            header: ({ column }: ColHeader<EntrypointMappingRow>) => <DataTableColumnHeader column={column} title="Type" />,
            cell: ({ row }: ColCell<EntrypointMappingRow>) => <span className="text-sm">{row.original.targetLabel}</span>,
        },
        {
            id: 'tags',
            accessorFn: (row: EntrypointMappingRow) => row.tagsName.join(', '),
            header: ({ column }: ColHeader<EntrypointMappingRow>) => <DataTableColumnHeader column={column} title="Sharding Tags" />,
            cell: ({ row }: ColCell<EntrypointMappingRow>) => <ShardingTagsCell tags={row.original.tagsName} />,
        },
        {
            id: 'environments',
            accessorFn: (row: EntrypointMappingRow) => row.environmentNames.join(', '),
            header: ({ column }: ColHeader<EntrypointMappingRow>) => <DataTableColumnHeader column={column} title="Environments" />,
            cell: ({ row }: ColCell<EntrypointMappingRow>) => (
                <span className="text-sm">
                    {row.original.environmentNames.length > 0 ? row.original.environmentNames.join(', ') : 'All'}
                </span>
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
            cell: ({ row }: ColCell<EntrypointMappingRow>) => (
                <div className="flex justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="size-8" aria-label="Entrypoint mapping actions">
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

export function CreateMappingButton({ onCreate }: { onCreate: (target: EntrypointTarget) => void }) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button>
                    <PlusIcon className="size-4" aria-hidden />
                    Add a mapping
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                {CREATE_TARGETS.map(target => (
                    <DropdownMenuItem key={target} onSelect={() => onCreate(target)}>
                        {targetMenuLabel(target)}
                    </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

export function EntrypointMappingsTable({
    rows,
    canCreate,
    canEdit,
    canDelete,
    onCreate,
    onEdit,
    onDelete,
}: Readonly<{
    rows: EntrypointMappingRow[];
    canCreate: boolean;
    canEdit: boolean;
    canDelete: boolean;
    onCreate?: (target: EntrypointTarget) => void;
    onEdit?: (row: EntrypointMappingRow) => void;
    onDelete?: (row: EntrypointMappingRow) => void;
}>) {
    const [search, setSearch] = useState('');
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) return rows;
        return rows.filter(row => row.value.toLowerCase().includes(query));
    }, [rows, search]);

    const sorted = useMemo(() => sortRows(filtered, sorting), [filtered, sorting]);
    const totalCount = sorted.length;
    const paginatedData = useMemo(() => sorted.slice((page - 1) * pageSize, page * pageSize), [sorted, page, pageSize]);
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

    if (rows.length === 0 && !search.trim()) {
        return (
            <DataTableEmptyState
                variant="first-use"
                icon={<RadioIcon />}
                title="No entrypoints"
                description="Entrypoint mappings connect gateway listeners to sharding tags for the Developer Portal."
                primaryAction={canCreate && onCreate ? <CreateMappingButton onCreate={onCreate} /> : undefined}
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
                        placeholder="Search by entrypoint..."
                        value={search}
                        onChange={e => handleSearchChange(e.target.value)}
                        aria-label="Search entrypoints"
                    />
                </InputGroup>
            </div>
            <div data-testid="entrypoint-mappings-table-body">
                <DataTable
                    aria-label="Entrypoint mappings"
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
                            title="No entrypoints found"
                            description="Try adjusting your search."
                        />
                    }
                />
            </div>
        </div>
    );
}
