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
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreVerticalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { DEFAULT_METADATA_PAGE_SIZE, METADATA_PAGE_SIZE_OPTIONS } from './metadataConstants';
import type { ApplicationMetadata } from '../../types/applicationNotification';
import { APPLICATION_METADATA_SORTABLE_IDS } from '../../utils/applicationTableSortParity';
import type { ColCell, ColHeader } from '../../utils/dataTableTypes';
import type { TableSortingState } from '../../utils/tableSort';

/** Client-side sort on the full metadata list (console: gio-metadata with filterLocally), then paginate. */
function sortMetadata(items: ApplicationMetadata[], sorting: TableSortingState): ApplicationMetadata[] {
    const active = sorting[0];
    if (!active?.id || !APPLICATION_METADATA_SORTABLE_IDS.has(active.id)) return items;

    const direction = active.desc ? -1 : 1;
    return [...items].sort((left, right) => {
        const leftValue =
            active.id === 'name'
                ? (left.name ?? '')
                : active.id === 'format'
                  ? (left.format ?? 'STRING')
                  : active.id === 'value'
                    ? (left.value ?? left.defaultValue ?? '')
                    : left.key;
        const rightValue =
            active.id === 'name'
                ? (right.name ?? '')
                : active.id === 'format'
                  ? (right.format ?? 'STRING')
                  : active.id === 'value'
                    ? (right.value ?? right.defaultValue ?? '')
                    : right.key;
        return String(leftValue).localeCompare(String(rightValue)) * direction;
    });
}

function buildColumns({
    hasActions,
    canUpdate,
    canDelete,
    isMutating,
    onEdit,
    onDelete,
}: {
    hasActions: boolean;
    canUpdate: boolean;
    canDelete: boolean;
    isMutating: boolean;
    onEdit: (metadata: ApplicationMetadata) => void;
    onDelete: (metadata: ApplicationMetadata) => void;
}): DataTableProps<ApplicationMetadata>['columns'] {
    const columns: DataTableProps<ApplicationMetadata>['columns'] = [
        {
            accessorKey: 'key',
            header: ({ column }: ColHeader<ApplicationMetadata>) => <DataTableColumnHeader column={column} title="Key" />,
            cell: ({ row }: ColCell<ApplicationMetadata>) => <span className="font-medium">{row.original.key}</span>,
        },
        {
            accessorKey: 'name',
            header: ({ column }: ColHeader<ApplicationMetadata>) => <DataTableColumnHeader column={column} title="Name" />,
        },
        {
            id: 'format',
            accessorFn: (row: ApplicationMetadata) => row.format ?? 'STRING',
            header: ({ column }: ColHeader<ApplicationMetadata>) => <DataTableColumnHeader column={column} title="Format" />,
            cell: ({ row }: ColCell<ApplicationMetadata>) => (
                <span className="text-sm text-muted-foreground">{row.original.format ?? 'STRING'}</span>
            ),
        },
        {
            id: 'value',
            accessorFn: (row: ApplicationMetadata) => row.value ?? row.defaultValue ?? '',
            header: ({ column }: ColHeader<ApplicationMetadata>) => <DataTableColumnHeader column={column} title="Value" />,
            cell: ({ row }: ColCell<ApplicationMetadata>) => row.original.value ?? row.original.defaultValue ?? '—',
        },
    ];

    if (hasActions) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            cell: ({ row }: ColCell<ApplicationMetadata>) => {
                const item = row.original;
                const canEditItem = canUpdate;
                const canDeleteItem = canDelete && item.value !== undefined;
                const actionCount = (canEditItem ? 1 : 0) + (canDeleteItem ? 1 : 0);

                if (actionCount === 0) return null;

                // Single-action exception: a lone action is a direct icon button (no dropdown).
                if (actionCount === 1) {
                    return (
                        <div className="flex justify-end">
                            {canEditItem ? (
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    aria-label={`Edit ${item.name} metadata`}
                                    disabled={isMutating}
                                    onClick={() => onEdit(item)}
                                >
                                    <PencilIcon className="size-4" aria-hidden />
                                </Button>
                            ) : (
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    className="size-8 text-destructive hover:text-destructive"
                                    aria-label={`Delete ${item.name} metadata`}
                                    disabled={isMutating}
                                    onClick={() => onDelete(item)}
                                >
                                    <Trash2Icon className="size-4" aria-hidden />
                                </Button>
                            )}
                        </div>
                    );
                }

                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    aria-label={`Actions for ${item.name} metadata`}
                                    disabled={isMutating}
                                >
                                    <MoreVerticalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="min-w-48">
                                <DropdownMenuItem onSelect={() => onEdit(item)} disabled={isMutating}>
                                    <PencilIcon className="size-4" aria-hidden />
                                    Edit
                                </DropdownMenuItem>
                                <DropdownMenuItem className="text-destructive" onSelect={() => onDelete(item)} disabled={isMutating}>
                                    <Trash2Icon className="size-4" aria-hidden />
                                    Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
            enableSorting: false,
            enableHiding: false,
        });
    }

    return columns;
}

export function MetadataTable({
    metadata,
    isLoading,
    canUpdate,
    canDelete,
    isMutating,
    onEdit,
    onDelete,
}: {
    readonly metadata: ApplicationMetadata[];
    readonly isLoading: boolean;
    readonly canUpdate: boolean;
    readonly canDelete: boolean;
    readonly isMutating: boolean;
    readonly onEdit: (metadata: ApplicationMetadata) => void;
    readonly onDelete: (metadata: ApplicationMetadata) => void;
}) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_METADATA_PAGE_SIZE);
    const [sorting, setSorting] = useState<TableSortingState>([{ id: 'key', desc: false }]);

    const sortedMetadata = useMemo(() => sortMetadata(metadata, sorting), [metadata, sorting]);
    const totalCount = sortedMetadata.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
    const paginatedMetadata = useMemo(() => {
        const start = (page - 1) * pageSize;
        return sortedMetadata.slice(start, start + pageSize);
    }, [page, pageSize, sortedMetadata]);

    useEffect(() => {
        if (page > totalPages) {
            setPage(totalPages);
        }
    }, [page, totalPages]);

    const handleSortingChange = (updater: TableSortingState | ((prev: TableSortingState) => TableSortingState)) => {
        setSorting(previous => (typeof updater === 'function' ? updater(previous) : updater));
        setPage(1);
    };

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    const hasActions = canUpdate || canDelete;
    const columns = useMemo(
        () => buildColumns({ hasActions, canUpdate, canDelete, isMutating, onEdit, onDelete }),
        [hasActions, canUpdate, canDelete, isMutating, onEdit, onDelete],
    );

    return (
        <DataTable
            aria-label="Application metadata"
            columns={columns}
            data={paginatedMetadata}
            sorting={sorting}
            onSortingChange={handleSortingChange}
            serverSide
            loading={isLoading}
            skeletonCount={pageSize}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: METADATA_PAGE_SIZE_OPTIONS,
                onPageChange: setPage,
                onPageSizeChange: handlePageSizeChange,
            }}
            emptyMessage="No metadata configured."
        />
    );
}
