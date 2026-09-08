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
    Avatar,
    AvatarFallback,
    AvatarImage,
    Badge,
    Button,
    DataTable,
    DataTableColumnHeader,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    Skeleton,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { ActivityIcon, CircleCheckIcon, CircleXIcon, GlobeIcon, MoreVerticalIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import { NON_SORTABLE_COLUMN } from '../../applications/utils/dataTableHeaders';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import { useEnvironmentHealthAvailability } from '../hooks/useEnvironmentHealthAvailability';
import type { EnvironmentHealthApi } from '../types';
import type { AvailabilityView } from '../utils/availability';
import { HEALTH_CHECK_FILTER_QUERY } from '../utils/healthCheckQuery';
import { AVAILABILITY_ERROR_THRESHOLD, AVAILABILITY_WARNING_THRESHOLD } from '../utils/reportBuckets';

const WORKFLOW_BADGE: Partial<Record<string, { label: string; className: string }>> = {
    DEPRECATED: { label: 'Deprecated', className: 'border-destructive/20 text-destructive' },
    DRAFT: { label: 'Draft', className: 'border-primary/20 text-primary' },
    IN_REVIEW: { label: 'In Review', className: 'border-destructive/20 text-destructive' },
    REQUEST_FOR_CHANGES: { label: 'Need changes', className: 'border-destructive/20 text-destructive' },
};

function AvailabilityGauge({ pct }: Readonly<{ pct: number }>) {
    const radius = 14;
    const circumference = 2 * Math.PI * radius;
    const bounded = Math.min(100, Math.max(0, pct));
    const offset = circumference * (1 - bounded / 100);
    const tone =
        bounded <= AVAILABILITY_ERROR_THRESHOLD
            ? 'text-destructive'
            : bounded <= AVAILABILITY_WARNING_THRESHOLD
              ? 'text-warning'
              : 'text-success';

    return (
        <div className="flex items-center gap-2" aria-label={`${bounded}% availability`}>
            <svg viewBox="0 0 36 36" className={`size-9 -rotate-90 ${tone}`} aria-hidden>
                <circle
                    cx="18"
                    cy="18"
                    r={radius}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="4"
                    className="text-muted-foreground opacity-20"
                />
                <circle
                    cx="18"
                    cy="18"
                    r={radius}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="4"
                    strokeLinecap="round"
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                />
            </svg>
            <span className={`text-sm font-medium tabular-nums ${tone}`}>{bounded}%</span>
        </div>
    );
}

function AvailabilityStatus({ availability }: Readonly<{ availability?: AvailabilityView }>) {
    if (!availability || availability.type === 'no-data') {
        return <span className="text-muted-foreground text-sm">No data to display</span>;
    }
    return <AvailabilityGauge pct={availability.availabilityPct} />;
}

function AvailabilityCell({
    api,
    from,
    to,
    reloadToken,
}: Readonly<{ api: EnvironmentHealthApi; from: number; to: number; reloadToken: number }>) {
    const { availability, isLoading, isError } = useEnvironmentHealthAvailability({
        apiId: api.id,
        from,
        to,
        enabled: api.healthcheckEnabled,
        reloadToken,
    });

    if (!api.healthcheckEnabled) {
        return <span className="text-muted-foreground text-sm">Health check has not been configured</span>;
    }
    if (isLoading) {
        return <Skeleton className="h-8 w-24 rounded" />;
    }
    if (isError) {
        return <span className="text-destructive text-sm">Failed to load</span>;
    }
    return <AvailabilityStatus availability={availability} />;
}

function ApiStates({ api }: Readonly<{ api: EnvironmentHealthApi }>) {
    const workflow = WORKFLOW_BADGE[api.lifecycleState ?? ''] ?? WORKFLOW_BADGE[api.workflowState ?? ''];
    return (
        <div className="flex flex-wrap items-center gap-1.5">
            {api.state === 'STARTED' ? (
                <Badge variant="outline" className="border-success/20 text-success">
                    <CircleCheckIcon className="mr-1 size-3" aria-hidden />
                    Started
                </Badge>
            ) : (
                <Badge variant="outline" className="border-destructive/20 text-destructive">
                    <CircleXIcon className="mr-1 size-3" aria-hidden />
                    Stopped
                </Badge>
            )}
            {api.lifecycleState === 'PUBLISHED' ? (
                <Badge variant="outline" className="border-success/20 text-success">
                    <GlobeIcon className="mr-1 size-3" aria-hidden />
                    Published
                </Badge>
            ) : (
                <Badge variant="outline" className="text-muted-foreground">
                    Unpublished
                </Badge>
            )}
            {api.origin === 'KUBERNETES' ? (
                <Badge variant="outline" className="text-muted-foreground">
                    Kubernetes
                </Badge>
            ) : null}
            {workflow ? (
                <Badge variant="outline" className={workflow.className}>
                    {workflow.label}
                </Badge>
            ) : null}
        </div>
    );
}

function buildColumns({
    from,
    to,
    reloadToken,
    dashboardHref,
}: {
    from: number;
    to: number;
    reloadToken: number;
    dashboardHref: (apiId: string) => string;
}): DataTableProps<EnvironmentHealthApi>['columns'] {
    const columns: DataTableProps<EnvironmentHealthApi>['columns'] = [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<EnvironmentHealthApi>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<EnvironmentHealthApi>) => (
                <div className="flex items-center gap-3">
                    <Avatar size="sm" className="shrink-0 rounded-md">
                        {row.original.pictureUrl ? (
                            <AvatarImage src={row.original.pictureUrl} alt="" className="rounded-md object-cover" />
                        ) : null}
                        <AvatarFallback className="bg-primary/10 text-primary rounded-md">
                            <GlobeIcon className="size-3.5" aria-hidden />
                        </AvatarFallback>
                    </Avatar>
                    <div className="min-w-0">
                        <p className="truncate font-medium">
                            <span>{row.original.name}</span>{' '}
                            <span className="text-muted-foreground font-normal">({row.original.apiVersion})</span>
                        </p>
                    </div>
                </div>
            ),
        },
        {
            id: 'states',
            ...NON_SORTABLE_COLUMN,
            header: ({ column }: ColHeader<EnvironmentHealthApi>) => <DataTableColumnHeader column={column} title="State" />,
            cell: ({ row }: ColCell<EnvironmentHealthApi>) => <ApiStates api={row.original} />,
        },
        {
            id: 'availability',
            ...NON_SORTABLE_COLUMN,
            header: ({ column }: ColHeader<EnvironmentHealthApi>) => <DataTableColumnHeader column={column} title="API Availability" />,
            cell: ({ row }: ColCell<EnvironmentHealthApi>) => (
                <AvailabilityCell api={row.original} from={from} to={to} reloadToken={reloadToken} />
            ),
        },
    ];

    columns.push({
        id: 'actions',
        ...NON_SORTABLE_COLUMN,
        header: () => <span className="sr-only">Actions</span>,
        size: 56,
        cell: ({ row }: ColCell<EnvironmentHealthApi>) =>
            row.original.healthcheckEnabled ? (
                <div className="flex justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-8"
                                aria-label={`Actions for ${row.original.name}`}
                            >
                                <MoreVerticalIcon className="size-4" aria-hidden />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-auto min-w-56">
                            <DropdownMenuItem asChild className="whitespace-nowrap">
                                <Link to={dashboardHref(row.original.id)}>
                                    <ActivityIcon className="size-4 shrink-0" aria-hidden />
                                    View API-level health check details
                                </Link>
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            ) : null,
    });

    return columns;
}

export function HealthCheckApisTable({
    apis,
    totalCount,
    loading,
    query,
    page,
    pageSize,
    sorting,
    from,
    to,
    reloadToken,
    dashboardHref,
    onSearchChange,
    onPageChange,
    onPageSizeChange,
    onSortingChange,
}: Readonly<{
    apis: EnvironmentHealthApi[];
    totalCount: number;
    loading: boolean;
    query: string;
    page: number;
    pageSize: number;
    sorting: TableSortingState;
    from: number;
    to: number;
    reloadToken: number;
    dashboardHref: (apiId: string) => string;
    onSearchChange: (value: string) => void;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: number) => void;
    onSortingChange: (updater: TableSortingState | ((previous: TableSortingState) => TableSortingState)) => void;
}>) {
    const columns = buildColumns({ from, to, reloadToken, dashboardHref });
    const emptyMessage = query.includes(HEALTH_CHECK_FILTER_QUERY)
        ? 'No APIs with health check enabled.'
        : query.trim()
          ? 'No APIs match your search.'
          : 'No APIs to display.';

    return (
        <div className="space-y-4">
            <div className="max-w-sm">
                <InputGroup>
                    <InputGroupAddon align="inline-start">
                        <SearchIcon className="text-muted-foreground size-3.5" aria-hidden />
                    </InputGroupAddon>
                    <InputGroupInput
                        placeholder='Search APIs | name:"My api *" ownerName:admin'
                        value={query}
                        onChange={event => onSearchChange(event.target.value)}
                    />
                </InputGroup>
            </div>
            <DataTable
                aria-label="API Health Check"
                columns={columns}
                data={apis}
                loading={loading}
                skeletonCount={pageSize}
                serverSide
                sorting={sorting}
                onSortingChange={onSortingChange}
                pagination={{
                    page,
                    pageSize,
                    totalCount,
                    pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                    onPageChange,
                    onPageSizeChange: size => {
                        onPageSizeChange(size);
                        onPageChange(1);
                    },
                }}
                emptyMessage={emptyMessage}
            />
        </div>
    );
}
