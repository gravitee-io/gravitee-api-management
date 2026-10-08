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
import {
    AlertCircleIcon,
    CircleCheckIcon,
    CircleXIcon,
    LockIcon,
    MoreVerticalIcon,
    RefreshCwIcon,
    SearchIcon,
} from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import { API_LIST_PAGE_SIZE_OPTIONS } from './apiListFilters';
import { ShardingTagsCell } from '../../../../shared/components/ShardingTagsCell';
import { useEnvCategories } from '../../hooks/useEnvCategories';
import type { ApiDeploymentState, ApiListItem, ApiListOriginContext, ApiState } from '../../types';
import { buildApiAnalyticsPath } from '../../utils/analyticsDeepLink';
import { getApiAccessPaths } from '../../utils/apiAccess';
import { getApiProxyTypeLabel } from '../../utils/apiHttpProxy';
import { isFederatedApiListItem } from '../../utils/federatedApi';
import { federatedProviderLabel } from '../../utils/federatedProviderLabels';
import { ApiAvatar } from '../ApiAvatar';

type ColCell<T> = { row: { original: T }; getValue: () => unknown };
type ColHeader<T> = { column: DataTableColumnHeaderProps<T, unknown>['column'] };

// Sortable column id → backend `sortBy` field (api-v2 `/apis/_search`). Columns absent here are not server-sortable.
const SORT_FIELD_BY_COLUMN: Record<string, string> = {
    'API Name': 'name',
    'API Type': 'api_type',
    'Runtime Status': 'status',
    access: 'paths',
    Owner: 'owner',
};

export function toApiListSortBy(sorting: DataTableProps<ApiListItem>['sorting']): string | undefined {
    const sort = sorting?.[0];
    if (!sort) return undefined;
    // Sharding tags use asymmetric sortBy values on the backend (`ApiSortByParam`): `tags_asc` / `-tags_desc`.
    if (sort.id === 'Sharding Tags') return sort.desc ? '-tags_desc' : 'tags_asc';
    const field = SORT_FIELD_BY_COLUMN[sort.id];
    if (!field) return undefined;
    return sort.desc ? `-${field}` : field;
}

type ApiListSortingState = NonNullable<DataTableProps<ApiListItem>['sorting']>;

/** Inverse of {@link toApiListSortBy} for the classic Console `order` query param. */
export function sortingFromApiListOrder(order: string | null | undefined): ApiListSortingState {
    if (!order) return [];
    const desc = order.startsWith('-');
    const field = desc ? order.slice(1) : order;
    if (field === 'tags_asc') return [{ id: 'Sharding Tags', desc: false }];
    if (field === 'tags_desc') return [{ id: 'Sharding Tags', desc: true }];
    const columnId = Object.entries(SORT_FIELD_BY_COLUMN).find(([, backendField]) => backendField === field)?.[0];
    if (!columnId) return [];
    return [{ id: columnId, desc }];
}

// ─── Status helpers ───────────────────────────────────────────────────────────

function RuntimeStatusBadge({ state }: { state: ApiState | undefined }) {
    switch (state) {
        case 'STARTED':
            return (
                <Badge variant="success">
                    <CircleCheckIcon className="size-3 mr-1" aria-hidden />
                    Started
                </Badge>
            );
        case 'STOPPED':
            return (
                <Badge variant="secondary">
                    <CircleXIcon className="size-3 mr-1" aria-hidden />
                    Stopped
                </Badge>
            );
        case 'CLOSED':
            return <Badge variant="outline">Closed</Badge>;
        default:
            return <span className="text-muted-foreground text-xs">—</span>;
    }
}

function SyncStatusBadge({ deploymentState }: { deploymentState: ApiDeploymentState | undefined }) {
    if (!deploymentState) return <span className="text-muted-foreground text-xs">—</span>;
    switch (deploymentState) {
        case 'NEED_REDEPLOY':
            return (
                <Badge variant="warning">
                    <AlertCircleIcon className="size-3 mr-1" aria-hidden />
                    Out of sync
                </Badge>
            );
        case 'DEPLOYED':
            return (
                <Badge variant="success">
                    <RefreshCwIcon className="size-3 mr-1" aria-hidden />
                    In sync
                </Badge>
            );
        default:
            console.warn('[ApiList] Unrecognized API deployment state, rendering the empty-value indicator:', deploymentState);
            return <span className="text-muted-foreground text-xs">—</span>;
    }
}

function OriginIndicator({ originContext }: { originContext: ApiListOriginContext | undefined }) {
    if (originContext?.origin !== 'INTEGRATION') return null;
    return (
        <span className="text-sm" data-testid="api-origin-indicator">
            {originContext.provider ? federatedProviderLabel(originContext.provider) : '—'}
        </span>
    );
}

/** Labels match the API Proxies filter options (HTTP Proxy / TCP Proxy / Federated API). */
function apiListTypeLabel(api: ApiListItem): string {
    if (isFederatedApiListItem(api)) return 'Federated API';
    return getApiProxyTypeLabel(api);
}

// A federated API's detail nav has no Overview section, so its detail page opens on General instead.
function apiDetailLandingPath(api: ApiListItem): string {
    return isFederatedApiListItem(api) ? `${api.id}/general` : `${api.id}/overview`;
}

// ─── Actions dropdown ─────────────────────────────────────────────────────────

function ApiActionsMenu({ api, onNavigate }: { api: ApiListItem; onNavigate: (path: string) => void }) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="API actions" onClick={e => e.stopPropagation()}>
                    <MoreVerticalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-auto min-w-48">
                <DropdownMenuItem onSelect={() => onNavigate(apiDetailLandingPath(api))}>View Details</DropdownMenuItem>
                <DropdownMenuItem onSelect={() => onNavigate(`${api.id}/general`)}>Edit Configuration</DropdownMenuItem>
                {/* A federated API never runs on a gateway, so the analytics dashboard has no data to show for it. */}
                {!isFederatedApiListItem(api) && (
                    <DropdownMenuItem onSelect={() => onNavigate(buildApiAnalyticsPath(api.id))}>View Analytics</DropdownMenuItem>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

// ─── Column definitions ───────────────────────────────────────────────────────

function buildColumns(
    navigate: ReturnType<typeof useNavigate>,
    categoryNames: ReadonlyMap<string, string>,
): DataTableProps<ApiListItem>['columns'] {
    return [
        {
            id: 'API Name',
            accessorFn: (row: ApiListItem) => row.name,
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="API Name" />,
            cell: ({ row }: ColCell<ApiListItem>) => {
                const api = row.original;
                const name = api.name;
                const truncated = name.length > 40;
                const truncatedTitle = api.apiVersion ? `${name} (${api.apiVersion})` : name;
                return (
                    <div className="flex items-center gap-2 min-w-0">
                        <ApiAvatar src={api._links?.pictureUrl} name={name} />
                        <div className="flex flex-wrap items-center gap-2 min-w-0">
                            <button
                                type="button"
                                className="text-left font-medium hover:underline"
                                title={truncated ? truncatedTitle : undefined}
                                onClick={() => navigate(apiDetailLandingPath(api))}
                            >
                                {truncated ? `${name.slice(0, 40).trimEnd()}…` : name}
                            </button>
                            {api.apiVersion ? (
                                <Badge variant="outline" className="text-xs font-normal">
                                    {api.apiVersion}
                                </Badge>
                            ) : null}
                        </div>
                    </div>
                );
            },
        },
        {
            id: 'API Type',
            accessorFn: (row: ApiListItem) => apiListTypeLabel(row),
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="API Type" />,
            cell: ({ getValue }: ColCell<ApiListItem>) => <span className="text-sm">{String(getValue() ?? '')}</span>,
        },
        {
            id: 'Origin',
            accessorFn: (row: ApiListItem) => (row.originContext?.origin === 'INTEGRATION' ? (row.originContext.provider ?? '') : ''),
            header: 'Origin',
            enableSorting: false,
            cell: ({ row }: ColCell<ApiListItem>) => <OriginIndicator originContext={row.original.originContext} />,
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
            accessorFn: (row: ApiListItem) => getApiAccessPaths(row)[0] ?? null,
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Access" />,
            cell: ({ row }: ColCell<ApiListItem>) => <ShardingTagsCell tags={getApiAccessPaths(row.original)} sort={false} firstAsCode />,
        },
        {
            id: 'Sharding Tags',
            accessorFn: (row: ApiListItem) => row.tags ?? [],
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Sharding Tags" />,
            cell: ({ row }: ColCell<ApiListItem>) => <ShardingTagsCell tags={row.original.tags} />,
        },
        {
            id: 'Categories',
            accessorFn: (row: ApiListItem) => row.categories ?? [],
            header: ({ column }: ColHeader<ApiListItem>) => <DataTableColumnHeader column={column} title="Categories" />,
            cell: ({ row }: ColCell<ApiListItem>) => (
                <ShardingTagsCell tags={(row.original.categories ?? []).map(key => categoryNames.get(key) ?? key)} />
            ),
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
                    <ApiActionsMenu api={row.original} onNavigate={navigate} />
                </div>
            ),
        },
    ];
}

function apiListEmptyMessage(loadFailed: boolean, forbidden: boolean): React.ReactNode {
    if (forbidden) {
        return (
            <DataTableEmptyState
                variant="no-results"
                icon={<LockIcon />}
                title="You don't have permission to view API proxies"
                description="Ask an administrator for the API read permission on this environment."
            />
        );
    }
    if (loadFailed) return null;
    return (
        <DataTableEmptyState
            variant="no-results"
            icon={<SearchIcon />}
            title="No APIs found"
            description="Try adjusting your search or filters."
        />
    );
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
    readonly loadFailed?: boolean;
    readonly forbidden?: boolean;
}

export function ApiListTable({
    apis,
    isLoading,
    skeletonRowCount = 5,
    page = 1,
    pageSize = 25,
    totalCount = 0,
    sorting,
    onSortingChange,
    onPageChange,
    onPageSizeChange,
    toolbar,
    loadFailed = false,
    forbidden = false,
}: ApiListTableProps) {
    const navigate = useNavigate();
    const { data: envCategories } = useEnvCategories();
    const categoryNames = new Map((envCategories ?? []).map(category => [category.key, category.name]));
    const columns = buildColumns(navigate, categoryNames);

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
                          pageSizeOptions: [...API_LIST_PAGE_SIZE_OPTIONS],
                          onPageChange,
                          onPageSizeChange,
                      }
                    : undefined
            }
            emptyMessage={apiListEmptyMessage(loadFailed, forbidden)}
        />
    );
}
