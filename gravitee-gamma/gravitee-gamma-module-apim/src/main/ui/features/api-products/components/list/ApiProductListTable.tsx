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
    DataTablePagination,
    type DataTableColumnHeaderProps,
    type DataTableProps,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type { ApiProductListItem } from '../../types/apiProduct';

// Column type helpers derived entirely from graphene's exported types
type ColHeader<T> = { column: DataTableColumnHeaderProps<T, unknown>['column'] };
type ColCell<T> = { row: { original: T } };

// ─── Actions dropdown ─────────────────────────────────────────────────────────

function ProductActionsMenu({ productId, onNavigate }: { productId: string; onNavigate: (path: string) => void }) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Product actions" onClick={e => e.stopPropagation()}>
                    <MoreHorizontalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-auto min-w-[12rem]">
                <DropdownMenuItem onSelect={() => onNavigate(`${productId}/overview`)}>View Details</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => onNavigate(`${productId}/general`)}>Edit Configuration</DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

// ─── Column definitions ───────────────────────────────────────────────────────

function buildColumns(navigate: ReturnType<typeof useNavigate>): DataTableProps<ApiProductListItem>['columns'] {
    return [
        {
            // Column id doubles as the label shown in the DataTable "View" (column visibility)
            // menu, which falls back to the id for non-string headers — keep it identical to
            // the visible header title so both read the same (incl. casing).
            id: 'Product Name',
            accessorFn: (row: ApiProductListItem) => row.name,
            header: ({ column }: ColHeader<ApiProductListItem>) => <DataTableColumnHeader column={column} title="Product Name" />,
            cell: ({ row }: ColCell<ApiProductListItem>) => (
                <button
                    type="button"
                    className="font-medium text-left hover:underline"
                    onClick={() => navigate(`${row.original.id}/overview`)}
                >
                    {row.original.name}
                </button>
            ),
        },
        {
            id: 'Total APIs',
            header: ({ column }: ColHeader<ApiProductListItem>) => <DataTableColumnHeader column={column} title="Total APIs" />,
            accessorFn: (row: ApiProductListItem) => row.apiIds?.length ?? 0,
            cell: ({ row }: ColCell<ApiProductListItem>) => (
                <Badge variant="secondary" className="text-xs tabular-nums">
                    {row.original.apiIds?.length ?? 0}
                </Badge>
            ),
        },
        {
            id: 'Version',
            accessorFn: (row: ApiProductListItem) => row.version,
            header: ({ column }: ColHeader<ApiProductListItem>) => <DataTableColumnHeader column={column} title="Version" />,
            cell: ({ row }: ColCell<ApiProductListItem>) => (
                <Badge variant="outline" className="font-mono text-xs">
                    {row.original.version}
                </Badge>
            ),
        },
        {
            id: 'Owner',
            header: ({ column }: ColHeader<ApiProductListItem>) => <DataTableColumnHeader column={column} title="Owner" />,
            accessorFn: (row: ApiProductListItem) => row.primaryOwner?.displayName ?? '',
            cell: ({ row }: ColCell<ApiProductListItem>) => (
                <span className="text-sm text-muted-foreground">{row.original.primaryOwner?.displayName ?? '—'}</span>
            ),
        },
        {
            id: 'actions',
            header: () => <div className="text-right">Actions</div>,
            size: 56,
            cell: ({ row }: ColCell<ApiProductListItem>) => (
                <div className="flex justify-end">
                    <ProductActionsMenu productId={row.original.id} onNavigate={navigate} />
                </div>
            ),
            enableSorting: false,
            enableHiding: false,
        },
    ];
}

// ─── Table ────────────────────────────────────────────────────────────────────

interface ApiProductListTableProps {
    products: ApiProductListItem[];
    isLoading: boolean;
    skeletonRowCount?: number;
    page: number;
    pageSize: number;
    totalCount: number;
    onPageChange: (page: number) => void;
    onPageSizeChange: (pageSize: number) => void;
    toolbar?: React.ReactNode;
}

export function ApiProductListTable({
    products,
    isLoading,
    skeletonRowCount = 5,
    page,
    pageSize,
    totalCount,
    onPageChange,
    onPageSizeChange,
    toolbar,
}: ApiProductListTableProps) {
    const navigate = useNavigate();
    const [sorting, setSorting] = useState([{ id: 'Product Name', desc: false }]);
    const columns = buildColumns(navigate);

    const renderPagination = () => (
        <DataTablePagination
            page={page}
            pageSize={pageSize}
            totalCount={totalCount}
            pageSizeOptions={[10, 25, 50, 100]}
            onPageChange={onPageChange}
            onPageSizeChange={onPageSizeChange}
        />
    );

    const compositeToolbar = (
        <div className="flex items-center gap-4 flex-1 min-w-0">
            {toolbar}
            <div className="ml-auto shrink-0">{renderPagination()}</div>
        </div>
    );

    return (
        <DataTable
            columns={columns}
            data={products}
            sorting={sorting}
            onSortingChange={setSorting}
            enableColumnVisibility
            loading={isLoading}
            skeletonCount={skeletonRowCount}
            toolbar={compositeToolbar}
            footer={renderPagination()}
            emptyMessage="No API products found."
        />
    );
}
