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
    DataTableEmptyState,
    DateCell,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { ExternalLinkIcon, MoreVerticalIcon, PlugIcon, SearchIcon, Wand2Icon } from '@gravitee/graphene-core/icons';
import { useMemo, type Dispatch, type ReactNode, type SetStateAction } from 'react';
import { useNavigate } from 'react-router-dom';

import { truncateLabel } from '../../../shared/utils/truncateLabel';
import type { ApplicationListItem, ApplicationStatus } from '../../types/application';
import { formatApplicationTypeLabel } from '../../utils/applicationFormatters';
import type { ColCell, ColHeader } from '../../utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../utils/paginationConstants';
import type { TableSortingState } from '../../utils/tableSort';
import { ApplicationAvatar } from '../ApplicationAvatar';

function ApplicationActionsMenu({ applicationId, onNavigate }: { applicationId: string; onNavigate: (path: string) => void }) {
    const navigateTo = (path: string) => (event: Event) => {
        event.preventDefault();
        onNavigate(path);
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Application actions" onClick={event => event.stopPropagation()}>
                    <MoreVerticalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-auto min-w-[12rem]" onClick={event => event.stopPropagation()}>
                <DropdownMenuItem className="gap-2" onSelect={navigateTo(`${applicationId}/general`)}>
                    <ExternalLinkIcon className="size-4" aria-hidden />
                    View Details
                </DropdownMenuItem>
                <DropdownMenuItem className="gap-2" onSelect={navigateTo(`${applicationId}/subscriptions`)}>
                    <PlugIcon className="size-4" aria-hidden />
                    Manage Subscriptions
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

function buildActiveColumns(navigate: ReturnType<typeof useNavigate>): DataTableProps<ApplicationListItem>['columns'] {
    return [
        {
            id: 'Name',
            accessorFn: (row: ApplicationListItem) => row.name,
            header: ({ column }: ColHeader<ApplicationListItem>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<ApplicationListItem>) => {
                const name = row.original.name;
                return (
                    <div className="flex items-center gap-2">
                        <ApplicationAvatar src={row.original.picture ?? row.original.picture_url} name={name} />
                        <Button
                            type="button"
                            variant="link"
                            className="h-auto p-0 text-left font-medium text-foreground hover:underline hover:text-foreground"
                            onClick={() => navigate(`${row.original.id}/general`)}
                        >
                            <span title={name}>{truncateLabel(name)}</span>
                        </Button>
                    </div>
                );
            },
        },
        {
            id: 'type',
            accessorFn: (row: ApplicationListItem) => formatApplicationTypeLabel(row),
            header: 'Type',
            cell: ({ row }: ColCell<ApplicationListItem>) => (
                <Badge variant="outline" className="border-border bg-background">
                    {formatApplicationTypeLabel(row.original)}
                </Badge>
            ),
            enableSorting: false,
        },
        {
            id: 'owner',
            accessorFn: (row: ApplicationListItem) => row.owner?.displayName ?? '',
            header: 'Owner',
            cell: ({ row }: ColCell<ApplicationListItem>) => {
                const ownerName = row.original.owner?.displayName;
                return ownerName ? (
                    <span className="block truncate text-sm text-muted-foreground" title={ownerName}>
                        {ownerName}
                    </span>
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                );
            },
            enableSorting: false,
        },
        {
            id: 'actions',
            header: () => <div className="text-right">Actions</div>,
            size: 56,
            cell: ({ row }: ColCell<ApplicationListItem>) => (
                <div className="flex justify-end">
                    <ApplicationActionsMenu applicationId={row.original.id} onNavigate={navigate} />
                </div>
            ),
            enableSorting: false,
            enableHiding: false,
        },
    ];
}

function buildArchivedColumns(
    navigate: ReturnType<typeof useNavigate>,
    canRestore: boolean,
    onRestore: ((application: ApplicationListItem) => void) | undefined,
): DataTableProps<ApplicationListItem>['columns'] {
    return [
        {
            id: 'Name',
            accessorFn: (row: ApplicationListItem) => row.name,
            header: ({ column }: ColHeader<ApplicationListItem>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<ApplicationListItem>) => {
                const name = row.original.name;
                return (
                    <div className="flex items-center gap-2">
                        <ApplicationAvatar src={row.original.picture ?? row.original.picture_url} name={name} />
                        <Button
                            type="button"
                            variant="link"
                            className="h-auto p-0 text-left font-medium text-foreground hover:underline hover:text-foreground"
                            onClick={() => navigate(`${row.original.id}/general`)}
                        >
                            <span title={name}>{truncateLabel(name)}</span>
                        </Button>
                    </div>
                );
            },
        },
        {
            id: 'Archived at',
            accessorFn: (row: ApplicationListItem) => row.updated_at,
            header: ({ column }: ColHeader<ApplicationListItem>) => <DataTableColumnHeader column={column} title="Archived at" />,
            cell: ({ row }: ColCell<ApplicationListItem>) => <DateCell value={new Date(row.original.updated_at)} format="absolute" />,
        },
        {
            id: 'actions',
            header: () => <div className="text-right">Actions</div>,
            size: 56,
            cell: ({ row }: ColCell<ApplicationListItem>) =>
                canRestore && onRestore ? (
                    <div className="flex justify-end">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    aria-label={`Restore ${row.original.name}`}
                                    onClick={() => onRestore(row.original)}
                                >
                                    <Wand2Icon className="size-4" aria-hidden />
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent>Restore application</TooltipContent>
                        </Tooltip>
                    </div>
                ) : null,
            enableSorting: false,
            enableHiding: false,
        },
    ];
}

interface ApplicationListTableProps {
    readonly applications: ApplicationListItem[];
    readonly isLoading: boolean;
    readonly status: ApplicationStatus;
    readonly skeletonRowCount?: number;
    readonly sorting: TableSortingState;
    readonly onSortingChange: Dispatch<SetStateAction<TableSortingState>>;
    readonly canRestore?: boolean;
    readonly onRestore?: (application: ApplicationListItem) => void;
    readonly page?: number;
    readonly pageSize?: number;
    readonly totalCount?: number;
    readonly onPageChange?: (page: number) => void;
    readonly onPageSizeChange?: (pageSize: number) => void;
    readonly toolbar?: ReactNode;
}

export function ApplicationListTable({
    applications,
    isLoading,
    status,
    skeletonRowCount = 5,
    sorting,
    onSortingChange,
    canRestore = false,
    onRestore,
    page = 1,
    pageSize = 10,
    totalCount = 0,
    onPageChange,
    onPageSizeChange,
    toolbar,
}: ApplicationListTableProps) {
    const navigate = useNavigate();
    const isArchived = status === 'ARCHIVED';
    const activeColumns = useMemo(() => buildActiveColumns(navigate), [navigate]);
    const archivedColumns = useMemo(() => buildArchivedColumns(navigate, canRestore, onRestore), [navigate, canRestore, onRestore]);
    const columns = isArchived ? archivedColumns : activeColumns;

    return (
        <DataTable
            aria-label="Applications"
            columns={columns}
            data={applications}
            sorting={sorting}
            onSortingChange={onSortingChange}
            enableColumnVisibility
            serverSide
            loading={isLoading}
            skeletonCount={skeletonRowCount}
            toolbar={toolbar}
            pagination={
                onPageChange && onPageSizeChange
                    ? {
                          page,
                          pageSize,
                          totalCount,
                          pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                          onPageChange,
                          onPageSizeChange,
                      }
                    : undefined
            }
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon />}
                    title="No applications found"
                    description="Try adjusting your search or filters."
                />
            }
        />
    );
}
