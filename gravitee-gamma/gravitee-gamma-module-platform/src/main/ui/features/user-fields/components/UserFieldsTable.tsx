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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { ClientSideTableSearchField } from '../../../shared/components/ClientSideTableSearchField';
import { clampPage, paginateClientSideTableItems } from '../../../shared/utils/clientSideTableUtils';
import type { ColCell, ColHeader } from '../../../shared/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../../shared/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { UserField } from '../types/userField';

const DEFAULT_PAGE_SIZE = 10;

const SORTABLE_IDS = new Set(['key', 'label']);

function fieldValues(field: UserField): string[] {
    return field.values ?? [];
}

function matchesSearch(field: UserField, query: string): boolean {
    return (
        field.key.toLowerCase().includes(query) ||
        field.label.toLowerCase().includes(query) ||
        fieldValues(field).some(value => value.toLowerCase().includes(query))
    );
}

function sortUserFields(fields: UserField[], sorting: TableSortingState): UserField[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_IDS.has(active.id)) return fields;
    const direction = active.desc ? -1 : 1;
    return [...fields].sort((a, b) => {
        const av = active.id === 'key' ? a.key : a.label;
        const bv = active.id === 'key' ? b.key : b.label;
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
    onEdit: (field: UserField) => void;
    onDelete: (field: UserField) => void;
}): DataTableProps<UserField>['columns'] {
    const columns: DataTableProps<UserField>['columns'] = [
        {
            id: 'key',
            accessorKey: 'key',
            header: ({ column }: ColHeader<UserField>) => <DataTableColumnHeader column={column} title="Key" />,
            cell: ({ row }: ColCell<UserField>) => (
                <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded">{row.original.key}</span>
                    {row.original.required ? (
                        <Badge variant="secondary" className="h-5 px-1.5 text-xs">
                            Required
                        </Badge>
                    ) : null}
                </div>
            ),
        },
        {
            id: 'label',
            accessorKey: 'label',
            header: ({ column }: ColHeader<UserField>) => <DataTableColumnHeader column={column} title="Label" />,
            cell: ({ row }: ColCell<UserField>) => <span className="text-sm font-medium">{row.original.label}</span>,
        },
        {
            id: 'values',
            accessorKey: 'values',
            enableSorting: false,
            header: ({ column }: ColHeader<UserField>) => <DataTableColumnHeader column={column} title="Values" />,
            cell: ({ row }: ColCell<UserField>) => {
                const values = fieldValues(row.original);
                if (values.length === 0) {
                    return <span className="text-sm text-muted-foreground">—</span>;
                }
                return (
                    <div className="space-y-1">
                        {values.map((value, index) => (
                            <div key={`${index}-${value}`} className="text-sm text-muted-foreground">
                                {value}
                            </div>
                        ))}
                    </div>
                );
            },
        },
    ];

    if (!canEdit && !canDelete) return columns;

    columns.push({
        id: 'actions',
        header: () => <span className="sr-only">Actions</span>,
        size: 56,
        enableSorting: false,
        enableHiding: false,
        cell: ({ row }: ColCell<UserField>) => (
            <div className="flex justify-end">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="size-8" aria-label="User field actions">
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

    return columns;
}

export function UserFieldsTable({
    fields,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    fields: UserField[];
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (field: UserField) => void;
    onDelete: (field: UserField) => void;
}>) {
    const [search, setSearch] = useState('');
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) return fields;
        return fields.filter(field => matchesSearch(field, query));
    }, [fields, search]);

    const sorted = useMemo(() => sortUserFields(filtered, sorting), [filtered, sorting]);

    const totalCount = sorted.length;
    const currentPage = clampPage(page, totalCount, pageSize);
    const paginatedData = useMemo(() => paginateClientSideTableItems(sorted, currentPage, pageSize), [sorted, currentPage, pageSize]);

    useEffect(() => {
        setPage(prev => clampPage(prev, totalCount, pageSize));
    }, [totalCount, pageSize]);

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
            <ClientSideTableSearchField
                id="user-fields-search"
                label="Search user fields"
                value={search}
                onChange={handleSearchChange}
                placeholder="Search by key, label, or value…"
            />

            <DataTable
                columns={columns}
                data={paginatedData}
                sorting={sorting}
                onSortingChange={handleSortingChange}
                serverSide
                pagination={{
                    page: currentPage,
                    pageSize,
                    totalCount,
                    pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                    onPageChange: setPage,
                    onPageSizeChange: handlePageSizeChange,
                }}
                emptyMessage={search.trim() ? 'No user fields match your search.' : 'There are no custom user fields'}
            />
        </div>
    );
}
