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
import { AlertCircleIcon, CircleCheckIcon, CircleXIcon, MoreHorizontalIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type { ApiDeploymentState, ApiListItem, ApiState } from '../../types';
import { getApiAccessPath } from '../../utils/apiAccess';

// Column type helpers derived entirely from graphene's exported types
type ColHeader<T> = { column: DataTableColumnHeaderProps<T, unknown>['column'] };
type ColCell<T> = { row: { original: T } };

// ─── Status helpers ───────────────────────────────────────────────────────────

function RuntimeStatusBadge({ state }: { state: ApiState | undefined }) {
    switch (state) {
        case 'STARTED':
            return (
                <Badge variant="outline" className="border-success/20 text-success">
                    <CircleCheckIcon className="size-3 mr-1" aria-hidden />
                    Started
                </Badge>
            );
        case 'STOPPED':
            return (
                <Badge variant="outline" className="border-destructive/20 text-destructive">
                    <CircleXIcon className="size-3 mr-1" aria-hidden />
                    Stopped
                </Badge>
            );
        case 'CLOSED':
            return (
                <Badge variant="outline" className="text-muted-foreground">
                    Closed
                </Badge>
            );
        default:
            return <span className="text-muted-foreground text-xs">—</span>;
    }
}

function SyncStatusBadge({ deploymentState }: { deploymentState: ApiDeploymentState | undefined }) {
    if (deploymentState === 'NEED_REDEPLOY') {
        return (
            <Badge variant="outline" className="border-warning/30 text-warning">
                <AlertCircleIcon className="size-3 mr-1" aria-hidden />
                Out of sync
            </Badge>
        );
    }
    return (
        <Badge variant="outline" className="border-success/20 text-success">
            <RefreshCwIcon className="size-3 mr-1" aria-hidden />
            In sync
        </Badge>
    );
}

// ─── Actions dropdown ─────────────────────────────────────────────────────────

function ApiActionsMenu({ apiId, onNavigate }: { apiId: string; onNavigate: (path: string) => void }) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="API actions" onClick={e => e.stopPropagation()}>
                    <MoreHorizontalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-auto min-w-[12rem]">
                <DropdownMenuItem onSelect={() => onNavigate(`${apiId}/overview`)}>View Details</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => onNavigate(`${apiId}/general`)}>Edit Configuration</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => onNavigate(`${apiId}/analytics`)}>View Analytics</DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

// ─── Column definitions ───────────────────────────────────────────────────────

function buildColumns(navigate: ReturnType<typeof useNavigate>): DataTableProps<ApiListItem>['columns'] {
    return [
        {
            accessorKey: 'name',
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="API Name" />,
            cell: ({ row }: ColCell<ApiListItem>) => (
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
            accessorKey: 'state',
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Runtime Status" />,
            cell: ({ row }: ColCell<ApiListItem>) => <RuntimeStatusBadge state={row.original.state} />,
        },
        {
            accessorKey: 'deploymentState',
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Sync Status" />,
            cell: ({ row }: ColCell<ApiListItem>) => <SyncStatusBadge deploymentState={row.original.deploymentState} />,
        },
        {
            id: 'access',
            header: 'Access',
            cell: ({ row }: ColCell<ApiListItem>) => {
                const path = getApiAccessPath(row.original);
                return path ? (
                    <Badge variant="outline" className="font-mono text-xs">
                        {path}
                    </Badge>
                ) : (
                    <span className="text-muted-foreground text-xs">—</span>
                );
            },
            enableSorting: false,
        },
        {
            id: 'owner',
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Owner" />,
            accessorFn: (row: ApiListItem) => row.primaryOwner?.displayName ?? '',
            cell: ({ row }: ColCell<ApiListItem>) => (
                <span className="text-sm text-muted-foreground">{row.original.primaryOwner?.displayName ?? '—'}</span>
            ),
        },
        {
            id: 'actions',
            header: () => <div className="text-right">Actions</div>,
            size: 56,
            cell: ({ row }: ColCell<ApiListItem>) => (
                <div className="flex justify-end">
                    <ApiActionsMenu apiId={row.original.id} onNavigate={navigate} />
                </div>
            ),
            enableSorting: false,
            enableHiding: false,
        },
    ];
}

// ─── Table ────────────────────────────────────────────────────────────────────

interface ApiListTableProps {
    readonly apis: ApiListItem[];
    readonly isLoading: boolean;
    readonly skeletonRowCount?: number;
    readonly page?: number;
    readonly pageSize?: number;
    readonly totalCount?: number;
    readonly onPageChange?: (page: number) => void;
    readonly onPageSizeChange?: (pageSize: number) => void;
    readonly toolbar?: React.ReactNode;
}

export function ApiListTable({
    apis,
    isLoading,
    skeletonRowCount = 5,
    page = 1,
    pageSize = 10,
    totalCount = 0,
    onPageChange,
    onPageSizeChange,
    toolbar,
}: ApiListTableProps) {
    const navigate = useNavigate();
    const [sorting, setSorting] = useState([{ id: 'name', desc: false }]);
    const columns = buildColumns(navigate);

    const paginationEl =
        onPageChange && onPageSizeChange ? (
            <DataTablePagination
                page={page}
                pageSize={pageSize}
                totalCount={totalCount}
                pageSizeOptions={[10, 25, 50, 100]}
                onPageChange={onPageChange}
                onPageSizeChange={onPageSizeChange}
            />
        ) : null;

    const compositeToolbar = paginationEl ? (
        <div className="flex items-center gap-4 flex-1 min-w-0">
            {toolbar}
            <div className="ml-auto shrink-0">{paginationEl}</div>
        </div>
    ) : (
        toolbar
    );

    return (
        <DataTable
            columns={columns}
            data={apis}
            sorting={sorting}
            onSortingChange={setSorting}
            enableColumnVisibility
            loading={isLoading}
            skeletonCount={skeletonRowCount}
            toolbar={compositeToolbar}
            emptyMessage="No APIs found."
        />
    );
}
