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
    Badge,
    Button,
    DataTable,
    DataTableColumnHeader,
    type DataTableColumnHeaderProps,
    DataTableEmptyState,
    type DataTableProps,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { DatabaseIcon, MoreVerticalIcon, PencilIcon, PlusIcon, RefreshCwIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { MetadataFormatBadge } from './MetadataFormatBadge';
import type { ApiMetadata, MetadataSource } from '../../../types/metadata';
import {
    canDeleteOrResetMetadata,
    DEFAULT_METADATA_PAGE_SIZE,
    displayMetadataValue,
    isInheritedGlobal,
    isResettableMetadata,
    METADATA_PAGE_SIZE_OPTIONS,
    METADATA_SOURCE_FILTER_ALL,
} from '../../../utils/apiMetadata';

type ColCell<T> = { row: { original: T } };
type ColHeader<T> = { column: DataTableColumnHeaderProps<T, unknown>['column'] };
type TableSortingState = { id: string; desc: boolean }[];

function GlobalBadge() {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Badge variant="secondary">Global</Badge>
            </TooltipTrigger>
            <TooltipContent>Inherited global metadata</TooltipContent>
        </Tooltip>
    );
}

function buildColumns({
    canEdit,
    canDelete,
    readOnly,
    isMutating,
    onEdit,
    onDelete,
}: {
    canEdit: boolean;
    canDelete: boolean;
    readOnly: boolean;
    isMutating: boolean;
    onEdit: (metadata: ApiMetadata) => void;
    onDelete: (metadata: ApiMetadata) => void;
}): DataTableProps<ApiMetadata>['columns'] {
    const columns: DataTableProps<ApiMetadata>['columns'] = [
        {
            id: 'key',
            accessorKey: 'key',
            header: ({ column }: ColHeader<ApiMetadata>) => <DataTableColumnHeader column={column} title="Key" />,
            cell: ({ row }: ColCell<ApiMetadata>) => (
                <div className="flex items-center gap-2">
                    <span className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded">{row.original.key}</span>
                    {isInheritedGlobal(row.original) ? <GlobalBadge /> : null}
                </div>
            ),
        },
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<ApiMetadata>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<ApiMetadata>) => <span className="text-sm font-medium">{row.original.name}</span>,
        },
        {
            id: 'format',
            accessorKey: 'format',
            header: ({ column }: ColHeader<ApiMetadata>) => <DataTableColumnHeader column={column} title="Format" />,
            cell: ({ row }: ColCell<ApiMetadata>) => <MetadataFormatBadge format={row.original.format} />,
        },
        {
            id: 'value',
            accessorFn: (row: ApiMetadata) => displayMetadataValue(row),
            header: ({ column }: ColHeader<ApiMetadata>) => <DataTableColumnHeader column={column} title="Value" />,
            cell: ({ row }: ColCell<ApiMetadata>) => {
                const value = displayMetadataValue(row.original);
                return value ? <span className="text-sm">{value}</span> : <span className="text-sm text-muted-foreground">—</span>;
            },
        },
    ];

    if ((canEdit || canDelete) && !readOnly) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<ApiMetadata>) => {
                const item = row.original;
                const canEditItem = canEdit && !readOnly;
                const canRemoveItem = canDelete && !readOnly && canDeleteOrResetMetadata(item);
                const isReset = isResettableMetadata(item);
                const actionCount = (canEditItem ? 1 : 0) + (canRemoveItem ? 1 : 0);

                if (actionCount === 0) return null;

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
                                    aria-label={isReset ? `Reset ${item.name} metadata` : `Delete ${item.name} metadata`}
                                    disabled={isMutating}
                                    onClick={() => onDelete(item)}
                                >
                                    {isReset ? (
                                        <RefreshCwIcon className="size-4" aria-hidden />
                                    ) : (
                                        <Trash2Icon className="size-4" aria-hidden />
                                    )}
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
                                <DropdownMenuSeparator />
                                <DropdownMenuItem variant="destructive" onSelect={() => onDelete(item)} disabled={isMutating}>
                                    {isReset ? (
                                        <RefreshCwIcon className="size-4" aria-hidden />
                                    ) : (
                                        <Trash2Icon className="size-4" aria-hidden />
                                    )}
                                    {isReset ? 'Reset' : 'Delete'}
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        });
    }

    return columns;
}

export function ApiMetadataTable({
    metadata,
    totalCount,
    page,
    pageSize,
    sorting,
    source,
    isLoading,
    canCreate,
    canEdit,
    canDelete,
    readOnly,
    isMutating,
    onPageChange,
    onPageSizeChange,
    onSortingChange,
    onSourceChange,
    onResetFilters,
    onCreate,
    onEdit,
    onDelete,
}: Readonly<{
    metadata: ApiMetadata[];
    totalCount: number;
    page: number;
    pageSize: number;
    sorting: TableSortingState;
    source: MetadataSource | undefined;
    isLoading: boolean;
    canCreate: boolean;
    canEdit: boolean;
    canDelete: boolean;
    readOnly: boolean;
    isMutating: boolean;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: number) => void;
    onSortingChange: (updater: TableSortingState | ((prev: TableSortingState) => TableSortingState)) => void;
    onSourceChange: (source: MetadataSource | undefined) => void;
    onResetFilters: () => void;
    onCreate: () => void;
    onEdit: (metadata: ApiMetadata) => void;
    onDelete: (metadata: ApiMetadata) => void;
}>) {
    const columns = useMemo(
        () => buildColumns({ canEdit, canDelete, readOnly, isMutating, onEdit, onDelete }),
        [canEdit, canDelete, readOnly, isMutating, onEdit, onDelete],
    );

    const hasSourceFilter = source !== undefined;
    const isFirstUse = !isLoading && totalCount === 0 && !hasSourceFilter;
    const isNoResults = !isLoading && metadata.length === 0 && hasSourceFilter;

    if (isFirstUse) {
        return (
            <div className="rounded-lg border">
                <DataTableEmptyState
                    variant="first-use"
                    icon={<DatabaseIcon className="size-8" aria-hidden />}
                    title="No API metadata"
                    description="Add metadata to this API for Markdown templating and portal display."
                    primaryAction={
                        canCreate && !readOnly ? (
                            <Button type="button" onClick={onCreate}>
                                <PlusIcon className="size-4" aria-hidden />
                                Add API Metadata
                            </Button>
                        ) : undefined
                    }
                />
            </div>
        );
    }

    return (
        <TooltipProvider delayDuration={200}>
            <DataTable
                aria-label="API metadata"
                columns={columns}
                data={metadata}
                sorting={sorting}
                onSortingChange={onSortingChange}
                serverSide
                loading={isLoading}
                skeletonCount={pageSize || DEFAULT_METADATA_PAGE_SIZE}
                toolbar={
                    <div className="flex flex-wrap items-center gap-2">
                        <Select
                            value={source ?? METADATA_SOURCE_FILTER_ALL}
                            onValueChange={value =>
                                onSourceChange(value === METADATA_SOURCE_FILTER_ALL ? undefined : (value as MetadataSource))
                            }
                        >
                            <SelectTrigger className="w-48" aria-label="Filter by source">
                                <SelectValue placeholder="Filter by source" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value={METADATA_SOURCE_FILTER_ALL}>All sources</SelectItem>
                                <SelectItem value="GLOBAL">Global</SelectItem>
                                <SelectItem value="API">API</SelectItem>
                            </SelectContent>
                        </Select>
                        {hasSourceFilter ? (
                            <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                className="gap-1.5 text-muted-foreground"
                                onClick={onResetFilters}
                            >
                                <RefreshCwIcon className="size-4" aria-hidden />
                                Reset filters
                            </Button>
                        ) : null}
                    </div>
                }
                pagination={{
                    page,
                    pageSize,
                    totalCount,
                    pageSizeOptions: [...METADATA_PAGE_SIZE_OPTIONS],
                    onPageChange,
                    onPageSizeChange,
                }}
                emptyMessage={
                    isNoResults ? (
                        <DataTableEmptyState
                            variant="no-results"
                            icon={<SearchIcon className="size-8" aria-hidden />}
                            title="No metadata found"
                            description="No metadata matches the selected source filter."
                            action={
                                <Button type="button" variant="outline" size="sm" onClick={onResetFilters}>
                                    Clear filters
                                </Button>
                            }
                        />
                    ) : undefined
                }
            />
        </TooltipProvider>
    );
}
