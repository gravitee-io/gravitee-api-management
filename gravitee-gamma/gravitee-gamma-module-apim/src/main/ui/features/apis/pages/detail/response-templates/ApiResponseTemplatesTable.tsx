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
import { EyeIcon, MoreVerticalIcon, PencilIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { CLIENT_TABLE_PAGE_SIZE_OPTIONS } from '../../../../../shared/hooks/useClientFilteredPagination';
import type { ResponseTemplateRow } from '../../../types/responseTemplate';

type ColCell<T> = { row: { original: T } };

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
    onEdit: (row: ResponseTemplateRow) => void;
    onDelete: (row: ResponseTemplateRow) => void;
}): DataTableProps<ResponseTemplateRow>['columns'] {
    return [
        {
            id: 'key',
            accessorKey: 'key',
            header: 'Key',
            enableSorting: false,
            cell: ({ row }: ColCell<ResponseTemplateRow>) => (
                <button type="button" className="font-medium hover:underline text-left" onClick={() => onEdit(row.original)}>
                    {row.original.key}
                </button>
            ),
        },
        {
            id: 'contentType',
            accessorKey: 'contentType',
            header: 'Content-Type',
            enableSorting: false,
            cell: ({ row }: ColCell<ResponseTemplateRow>) => <span className="font-mono text-xs">{row.original.contentType}</span>,
        },
        {
            id: 'statusCode',
            accessorKey: 'statusCode',
            header: 'Status Code',
            enableSorting: false,
            cell: ({ row }: ColCell<ResponseTemplateRow>) => <span className="text-sm">{row.original.statusCode ?? '—'}</span>,
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<ResponseTemplateRow>) => {
                const item = row.original;
                const showEdit = canEdit && !readOnly;
                const showView = readOnly || !canEdit;
                const showDelete = canDelete && !readOnly;

                if (!showEdit && !showView && !showDelete) return null;

                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    aria-label={`Actions for ${item.key} ${item.contentType}`}
                                    disabled={isMutating}
                                >
                                    <MoreVerticalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {showEdit ? (
                                    <DropdownMenuItem onSelect={() => onEdit(item)}>
                                        <PencilIcon className="size-4" aria-hidden />
                                        Edit
                                    </DropdownMenuItem>
                                ) : (
                                    <DropdownMenuItem onSelect={() => onEdit(item)}>
                                        <EyeIcon className="size-4" aria-hidden />
                                        View
                                    </DropdownMenuItem>
                                )}
                                {showDelete ? (
                                    <>
                                        <DropdownMenuSeparator />
                                        <DropdownMenuItem variant="destructive" onSelect={() => onDelete(item)}>
                                            <Trash2Icon className="size-4" aria-hidden />
                                            Delete
                                        </DropdownMenuItem>
                                    </>
                                ) : null}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        },
    ];
}

export function ApiResponseTemplatesTable({
    templates,
    totalCount,
    page,
    pageSize,
    search,
    isLoading,
    canEdit,
    canDelete,
    readOnly,
    isMutating,
    onSearchChange,
    onPageChange,
    onPageSizeChange,
    onEdit,
    onDelete,
    onClearSearch,
}: Readonly<{
    templates: ResponseTemplateRow[];
    totalCount: number;
    page: number;
    pageSize: number;
    search: string;
    isLoading: boolean;
    canEdit: boolean;
    canDelete: boolean;
    readOnly: boolean;
    isMutating: boolean;
    onSearchChange: (value: string) => void;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: number) => void;
    onEdit: (row: ResponseTemplateRow) => void;
    onDelete: (row: ResponseTemplateRow) => void;
    onClearSearch: () => void;
}>) {
    const columns = useMemo(
        () => buildColumns({ canEdit, canDelete, readOnly, isMutating, onEdit, onDelete }),
        [canEdit, canDelete, readOnly, isMutating, onEdit, onDelete],
    );

    const hasSearch = search.trim().length > 0;
    const isNoResults = !isLoading && templates.length === 0 && hasSearch;

    return (
        <DataTable
            aria-label="Response templates"
            columns={columns}
            data={templates}
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
                        placeholder="Search key, content-type, or status…"
                        value={search}
                        onChange={e => onSearchChange(e.target.value)}
                        aria-label="Search response templates"
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
                        title="No response templates found"
                        description="No templates match your search."
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
