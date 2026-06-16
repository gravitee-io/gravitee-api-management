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
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import { MoreVerticalIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import type { ApiProductListItem } from '../../types/apiProduct';

type ColCell<T> = { row: { original: T } };
type ColHeader<T> = { column: DataTableColumnHeaderProps<T, unknown>['column'] };

// Sortable column id → backend `sortBy` field (api-v2 `/api-products/_search`).
const SORT_FIELD_BY_COLUMN: Record<string, string> = {
    'Product Name': 'name',
    'Total APIs': 'apis',
    Version: 'version',
    Owner: 'owner',
};

export function toApiProductListSortBy(sorting: DataTableProps<ApiProductListItem>['sorting']): string | undefined {
    const sort = sorting?.[0];
    const field = sort ? SORT_FIELD_BY_COLUMN[sort.id] : undefined;
    if (!sort || !field) return undefined;
    return sort.desc ? `-${field}` : field;
}

// ─── Actions dropdown ─────────────────────────────────────────────────────────

function ProductActionsMenu({ productId, onNavigate }: { productId: string; onNavigate: (path: string) => void }) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Product actions" onClick={e => e.stopPropagation()}>
                    <MoreVerticalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-auto min-w-48">
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
            id: 'Product Name',
            accessorFn: (row: ApiProductListItem) => row.name,
            header: ({ column }: ColHeader<ApiProductListItem>) => <DataTableColumnHeader column={column} title="Product Name" />,
            cell: ({ row }: ColCell<ApiProductListItem>) => {
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
            id: 'Total APIs',
            accessorFn: (row: ApiProductListItem) => row.apiIds?.length ?? 0,
            header: ({ column }: ColHeader<ApiProductListItem>) => <DataTableColumnHeader column={column} title="Total APIs" />,
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
            accessorFn: (row: ApiProductListItem) => row.primaryOwner?.displayName ?? '',
            header: ({ column }: ColHeader<ApiProductListItem>) => <DataTableColumnHeader column={column} title="Owner" />,
            cell: ({ row }: ColCell<ApiProductListItem>) => (
                <span className="text-sm text-muted-foreground">{row.original.primaryOwner?.displayName ?? '—'}</span>
            ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<ApiProductListItem>) => (
                <div className="flex justify-end">
                    <ProductActionsMenu productId={row.original.id} onNavigate={navigate} />
                </div>
            ),
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
    sorting?: DataTableProps<ApiProductListItem>['sorting'];
    onSortingChange?: DataTableProps<ApiProductListItem>['onSortingChange'];
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
    sorting,
    onSortingChange,
    onPageChange,
    onPageSizeChange,
    toolbar,
}: ApiProductListTableProps) {
    const navigate = useNavigate();
    const columns = buildColumns(navigate);

    return (
        <DataTable
            aria-label="API products"
            columns={columns}
            data={products}
            enableColumnVisibility
            loading={isLoading}
            skeletonCount={skeletonRowCount}
            serverSide
            sorting={sorting}
            onSortingChange={onSortingChange}
            toolbar={toolbar}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [10, 25, 50, 100],
                onPageChange,
                onPageSizeChange,
            }}
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon />}
                    title="No API products found"
                    description="Try adjusting your search."
                />
            }
        />
    );
}
