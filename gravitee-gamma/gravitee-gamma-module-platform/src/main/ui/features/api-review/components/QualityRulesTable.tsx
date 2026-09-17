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
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import type { ColCell, ColHeader } from '../../../shared/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../../shared/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { QualityRule } from '../types/qualityRule';

const DEFAULT_PAGE_SIZE = 10;

type SortableColumn = 'name' | 'description';

function isSortableColumn(id: string): id is SortableColumn {
    return id === 'name' || id === 'description';
}

function sortRules(rules: QualityRule[], sorting: TableSortingState): QualityRule[] {
    const active = sorting[0];
    if (!active?.id || !isSortableColumn(active.id)) return rules;
    const column = active.id;
    const direction = active.desc ? -1 : 1;
    return [...rules].sort((a, b) => a[column].localeCompare(b[column]) * direction);
}

function buildColumns({
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: {
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (rule: QualityRule) => void;
    onDelete: (rule: QualityRule) => void;
}): DataTableProps<QualityRule>['columns'] {
    const columns: DataTableProps<QualityRule>['columns'] = [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<QualityRule>) => <DataTableColumnHeader column={column} title="Rule name" />,
            cell: ({ row }: ColCell<QualityRule>) => <span className="text-sm font-medium">{row.original.name}</span>,
        },
        {
            id: 'description',
            accessorKey: 'description',
            header: ({ column }: ColHeader<QualityRule>) => <DataTableColumnHeader column={column} title="Description" />,
            cell: ({ row }: ColCell<QualityRule>) => <span className="text-sm text-muted-foreground">{row.original.description}</span>,
        },
    ];

    if (canEdit || canDelete) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<QualityRule>) => (
                <div className="flex justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="size-8" aria-label={`Actions for ${row.original.name}`}>
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

export function QualityRulesTable({
    rules,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    rules: QualityRule[];
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (rule: QualityRule) => void;
    onDelete: (rule: QualityRule) => void;
}>) {
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const sorted = useMemo(() => sortRules(rules, sorting), [rules, sorting]);
    const paginated = useMemo(() => {
        const start = (page - 1) * pageSize;
        return sorted.slice(start, start + pageSize);
    }, [sorted, page, pageSize]);

    const columns = useMemo(() => buildColumns({ canEdit, canDelete, onEdit, onDelete }), [canEdit, canDelete, onEdit, onDelete]);

    function handleSortingChange(updater: TableSortingState | ((prev: TableSortingState) => TableSortingState)) {
        setSorting(prev => (typeof updater === 'function' ? updater(prev) : updater));
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    return (
        <DataTable
            columns={columns}
            data={paginated}
            sorting={sorting}
            onSortingChange={handleSortingChange}
            serverSide
            pagination={{
                page,
                pageSize,
                totalCount: sorted.length,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange: setPage,
                onPageSizeChange: handlePageSizeChange,
            }}
            emptyMessage="No manual rules to display."
        />
    );
}
