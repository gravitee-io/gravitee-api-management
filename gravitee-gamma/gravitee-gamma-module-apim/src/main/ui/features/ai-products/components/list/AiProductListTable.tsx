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

import { SyncStatusBadge } from '../../../api-products/components/SyncStatusBadge';
import type { AiProduct } from '../../types/aiProduct';

type ColHeader<T> = { column: DataTableColumnHeaderProps<T, unknown>['column'] };
type ColCell<T> = { row: { original: T } };

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
                <DropdownMenuItem onSelect={() => onNavigate(`${productId}/components`)}>Manage Components</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => onNavigate(`${productId}/general`)}>Edit Configuration</DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

function buildColumns(navigate: ReturnType<typeof useNavigate>): DataTableProps<AiProduct>['columns'] {
    return [
        {
            id: 'Product Name',
            accessorFn: (row: AiProduct) => row.name,
            header: ({ column }: ColHeader<AiProduct>) => <DataTableColumnHeader column={column} title="Product Name" />,
            cell: ({ row }: ColCell<AiProduct>) => {
                const name = row.original.name;
                const truncated = name.length > 40;
                return (
                    <button
                        type="button"
                        className="text-left font-medium hover:underline"
                        title={truncated ? name : undefined}
                        onClick={() => navigate(`${row.original.id}/overview`)}
                    >
                        {truncated ? `${name.slice(0, 40).trimEnd()}…` : name}
                    </button>
                );
            },
        },
        {
            id: 'Components',
            header: ({ column }: ColHeader<AiProduct>) => <DataTableColumnHeader column={column} title="Components" />,
            accessorFn: (row: AiProduct) => row.apiIds?.length ?? 0,
            cell: ({ row }: ColCell<AiProduct>) => (
                <Badge variant="secondary" className="text-xs tabular-nums">
                    {row.original.apiIds?.length ?? 0}
                </Badge>
            ),
        },
        {
            id: 'Sync Status',
            header: ({ column }: ColHeader<AiProduct>) => <DataTableColumnHeader column={column} title="Sync Status" />,
            accessorFn: (row: AiProduct) => row.deploymentState ?? '',
            cell: ({ row }: ColCell<AiProduct>) => <SyncStatusBadge state={row.original.deploymentState} compact />,
        },
        {
            id: 'Version',
            accessorFn: (row: AiProduct) => row.version,
            header: ({ column }: ColHeader<AiProduct>) => <DataTableColumnHeader column={column} title="Version" />,
            cell: ({ row }: ColCell<AiProduct>) => (
                <Badge variant="outline" className="font-mono text-xs">
                    {row.original.version}
                </Badge>
            ),
        },
        {
            id: 'Owner',
            header: ({ column }: ColHeader<AiProduct>) => <DataTableColumnHeader column={column} title="Owner" />,
            accessorFn: (row: AiProduct) => row.primaryOwner?.displayName ?? '',
            cell: ({ row }: ColCell<AiProduct>) => (
                <span className="text-sm text-muted-foreground">{row.original.primaryOwner?.displayName ?? '—'}</span>
            ),
        },
        {
            id: 'actions',
            header: () => <div className="text-right">Actions</div>,
            size: 56,
            cell: ({ row }: ColCell<AiProduct>) => (
                <div className="flex justify-end">
                    <ProductActionsMenu productId={row.original.id} onNavigate={navigate} />
                </div>
            ),
            enableSorting: false,
            enableHiding: false,
        },
    ];
}

interface AiProductListTableProps {
    products: AiProduct[];
    isLoading: boolean;
    skeletonRowCount?: number;
    page: number;
    pageSize: number;
    totalCount: number;
    onPageChange: (page: number) => void;
    onPageSizeChange: (pageSize: number) => void;
    toolbar?: React.ReactNode;
}

export function AiProductListTable({
    products,
    isLoading,
    skeletonRowCount = 5,
    page,
    pageSize,
    totalCount,
    onPageChange,
    onPageSizeChange,
    toolbar,
}: AiProductListTableProps) {
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
            emptyMessage="No AI products found."
        />
    );
}
