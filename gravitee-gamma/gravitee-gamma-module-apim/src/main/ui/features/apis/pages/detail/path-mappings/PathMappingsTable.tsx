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
    type DataTableProps,
    DataTableEmptyState,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Input,
} from '@gravitee/graphene-core';
import { MoreVerticalIcon, PencilIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { CLIENT_TABLE_PAGE_SIZE_OPTIONS } from '../../../../../shared/hooks/useClientFilteredPagination';

type PathRow = { path: string };
type ColCell = { row: { original: PathRow } };

function buildColumns({
    readOnly,
    isMutating,
    onEdit,
    onDelete,
}: {
    readOnly: boolean;
    isMutating: boolean;
    onEdit: (path: string) => void;
    onDelete: (path: string) => void;
}): DataTableProps<PathRow>['columns'] {
    return [
        {
            id: 'path',
            accessorKey: 'path',
            header: 'Path',
            enableSorting: false,
            cell: ({ row }: ColCell) => (
                <span className="font-mono text-sm" data-testid="path-mapping-value">
                    {row.original.path}
                </span>
            ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell) => {
                if (readOnly) return null;
                const path = row.original.path;
                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    aria-label={`Actions for ${path}`}
                                    disabled={isMutating}
                                    data-testid="path-mapping-actions"
                                >
                                    <MoreVerticalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                <DropdownMenuItem onSelect={() => onEdit(path)} data-testid="edit-path-mapping">
                                    <PencilIcon className="size-4" aria-hidden />
                                    Edit
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem variant="destructive" onSelect={() => onDelete(path)} data-testid="delete-path-mapping">
                                    <Trash2Icon className="size-4" aria-hidden />
                                    Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        },
    ];
}

export function PathMappingsTable({
    paths,
    page,
    pageSize,
    totalCount,
    search,
    readOnly,
    isMutating,
    isLoading,
    onSearchChange,
    onPageChange,
    onPageSizeChange,
    onEdit,
    onDelete,
    onClearSearch,
}: Readonly<{
    paths: string[];
    page: number;
    pageSize: number;
    totalCount: number;
    search: string;
    readOnly: boolean;
    isMutating: boolean;
    isLoading: boolean;
    onSearchChange: (value: string) => void;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: number) => void;
    onEdit: (path: string) => void;
    onDelete: (path: string) => void;
    onClearSearch: () => void;
}>) {
    const data = useMemo(() => paths.map(path => ({ path })), [paths]);
    const columns = useMemo(() => buildColumns({ readOnly, isMutating, onEdit, onDelete }), [readOnly, isMutating, onEdit, onDelete]);
    const isNoResults = totalCount === 0 && search.trim().length > 0;

    return (
        <DataTable
            aria-label="Path mappings"
            columns={columns}
            data={data}
            serverSide
            loading={isLoading}
            skeletonCount={pageSize}
            toolbar={
                <div className="relative w-full">
                    <SearchIcon
                        className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
                        aria-hidden
                    />
                    <Input
                        className="h-8 w-full pl-8"
                        placeholder="Search path mappings…"
                        value={search}
                        onChange={e => onSearchChange(e.target.value)}
                        aria-label="Search path mappings"
                        data-testid="path-mappings-search"
                    />
                </div>
            }
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...CLIENT_TABLE_PAGE_SIZE_OPTIONS],
                onPageChange,
                onPageSizeChange,
            }}
            emptyMessage={
                isNoResults ? (
                    <DataTableEmptyState
                        variant="no-results"
                        icon={<SearchIcon className="size-8" aria-hidden />}
                        title="No path mappings found"
                        description="No path mappings match your search."
                        action={
                            <Button type="button" variant="outline" size="sm" onClick={onClearSearch}>
                                Clear search
                            </Button>
                        }
                    />
                ) : undefined
            }
        />
    );
}
