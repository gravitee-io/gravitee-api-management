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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    MonoCell,
} from '@gravitee/graphene-core';
import { MoreVerticalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
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
                <span data-testid="path-mapping-value">
                    <MonoCell value={row.original.path} />
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
    readOnly,
    isMutating,
    isLoading,
    onPageChange,
    onPageSizeChange,
    onEdit,
    onDelete,
}: Readonly<{
    paths: string[];
    page: number;
    pageSize: number;
    totalCount: number;
    readOnly: boolean;
    isMutating: boolean;
    isLoading: boolean;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: number) => void;
    onEdit: (path: string) => void;
    onDelete: (path: string) => void;
}>) {
    const data = useMemo(() => paths.map(path => ({ path })), [paths]);
    const columns = useMemo(() => buildColumns({ readOnly, isMutating, onEdit, onDelete }), [readOnly, isMutating, onEdit, onDelete]);

    return (
        <DataTable
            aria-label="Path mappings"
            columns={columns}
            data={data}
            serverSide
            loading={isLoading}
            skeletonCount={pageSize}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...CLIENT_TABLE_PAGE_SIZE_OPTIONS],
                onPageChange,
                onPageSizeChange,
            }}
        />
    );
}
