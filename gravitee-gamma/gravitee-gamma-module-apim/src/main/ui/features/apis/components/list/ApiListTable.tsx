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
import { AlertCircleIcon, CircleCheckIcon, CircleXIcon, MoreVerticalIcon, RefreshCwIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import type { ApiDeploymentState, ApiListItem, ApiState } from '../../types';
import { getApiAccessPath } from '../../utils/apiAccess';
import { ApiAvatar } from '../ApiAvatar';

type ColCell<T> = { row: { original: T } };
type ColHeader<T> = { column: DataTableColumnHeaderProps<T, unknown>['column'] };

// Sortable column id → backend `sortBy` field (api-v2 `/apis/_search`). Columns absent here are not server-sortable.
const SORT_FIELD_BY_COLUMN: Record<string, string> = {
    'API Name': 'name',
    'Runtime Status': 'status',
    access: 'paths',
    Owner: 'owner',
};

export function toApiListSortBy(sorting: DataTableProps<ApiListItem>['sorting']): string | undefined {
    const sort = sorting?.[0];
    const field = sort ? SORT_FIELD_BY_COLUMN[sort.id] : undefined;
    if (!sort || !field) return undefined;
    return sort.desc ? `-${field}` : field;
}

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
                    <MoreVerticalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-auto min-w-48">
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
            id: 'API Name',
            accessorFn: (row: ApiListItem) => row.name,
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="API Name" />,
            cell: ({ row }: ColCell<ApiListItem>) => {
                const api = row.original;
                const name = api.name;
                const truncated = name.length > 40;
                return (
                    <div className="flex items-center gap-2">
                        <ApiAvatar src={api._links?.pictureUrl} name={name} />
                        <button
                            type="button"
                            className="text-left font-medium hover:underline"
                            title={truncated ? name : undefined}
                            onClick={() => navigate(`${api.id}/overview`)}
                        >
                            {truncated ? `${name.slice(0, 40).trimEnd()}…` : name}
                        </button>
                    </div>
                );
            },
        },
        {
            id: 'Runtime Status',
            accessorFn: (row: ApiListItem) => row.state,
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Runtime Status" />,
            cell: ({ row }: ColCell<ApiListItem>) => <RuntimeStatusBadge state={row.original.state} />,
        },
        {
            id: 'Sync Status',
            accessorFn: (row: ApiListItem) => row.deploymentState,
            header: 'Sync Status',
            enableSorting: false,
            cell: ({ row }: ColCell<ApiListItem>) => <SyncStatusBadge deploymentState={row.original.deploymentState} />,
        },
        {
            id: 'access',
            accessorFn: (row: ApiListItem) => getApiAccessPath(row),
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Access" />,
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
        },
        {
            id: 'Owner',
            accessorFn: (row: ApiListItem) => row.primaryOwner?.displayName ?? '',
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Owner" />,
            cell: ({ row }: ColCell<ApiListItem>) => (
                <span className="text-sm text-muted-foreground">{row.original.primaryOwner?.displayName ?? '—'}</span>
            ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<ApiListItem>) => (
                <div className="flex justify-end">
                    <ApiActionsMenu apiId={row.original.id} onNavigate={navigate} />
                </div>
            ),
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
    readonly sorting?: DataTableProps<ApiListItem>['sorting'];
    readonly onSortingChange?: DataTableProps<ApiListItem>['onSortingChange'];
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
    sorting,
    onSortingChange,
    onPageChange,
    onPageSizeChange,
    toolbar,
}: ApiListTableProps) {
    const navigate = useNavigate();
    const columns = buildColumns(navigate);

    return (
        <DataTable
            aria-label="API proxies"
            columns={columns}
            data={apis}
            enableColumnVisibility
            loading={isLoading}
            skeletonCount={skeletonRowCount}
            serverSide
            sorting={sorting}
            onSortingChange={onSortingChange}
            toolbar={toolbar}
            pagination={
                onPageChange && onPageSizeChange
                    ? {
                          page,
                          pageSize,
                          totalCount,
                          pageSizeOptions: [10, 25, 50, 100],
                          onPageChange,
                          onPageSizeChange,
                      }
                    : undefined
            }
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon />}
                    title="No APIs found"
                    description="Try adjusting your search."
                />
            }
        />
    );
}
