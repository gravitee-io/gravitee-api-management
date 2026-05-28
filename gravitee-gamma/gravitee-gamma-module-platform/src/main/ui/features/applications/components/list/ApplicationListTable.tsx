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
    DropdownMenuTrigger,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { AppWindowIcon, ExternalLinkIcon, MoreHorizontalIcon, PlugIcon, Wand2Icon } from '@gravitee/graphene-core/icons';
import { useMemo, type Dispatch, type SetStateAction } from 'react';
import { useNavigate } from 'react-router-dom';

import type { ApplicationListItem, ApplicationStatus } from '../../types/application';
import { formatApplicationDateTime, formatApplicationTypeLabel } from '../../utils/applicationFormatters';
import type { ColCell, ColHeader } from '../../utils/dataTableTypes';
import type { TableSortingState } from '../../utils/tableSort';

function ApplicationActionsMenu({ applicationId, onNavigate }: { applicationId: string; onNavigate: (path: string) => void }) {
    const navigateTo = (path: string) => (event: Event) => {
        event.preventDefault();
        onNavigate(path);
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Application actions" onClick={event => event.stopPropagation()}>
                    <MoreHorizontalIcon className="size-4" aria-hidden />
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
            accessorKey: 'name',
            header: ({ column }: ColHeader<ApplicationListItem>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<ApplicationListItem>) => (
                <button
                    type="button"
                    className="flex items-center gap-2 font-medium text-left hover:underline"
                    onClick={() => navigate(`${row.original.id}/general`)}
                >
                    <AppWindowIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                    {row.original.name}
                </button>
            ),
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
            cell: ({ row }: ColCell<ApplicationListItem>) => (
                <span className="text-sm text-muted-foreground">{row.original.owner?.displayName ?? '—'}</span>
            ),
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
    canRestore: boolean,
    onRestore: ((application: ApplicationListItem) => void) | undefined,
): DataTableProps<ApplicationListItem>['columns'] {
    return [
        {
            accessorKey: 'name',
            header: ({ column }: ColHeader<ApplicationListItem>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<ApplicationListItem>) => (
                <div className="flex items-center gap-2 font-medium">
                    <AppWindowIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                    {row.original.name}
                </div>
            ),
        },
        {
            accessorKey: 'updated_at',
            header: ({ column }: ColHeader<ApplicationListItem>) => <DataTableColumnHeader column={column} title="Archived at" />,
            cell: ({ row }: ColCell<ApplicationListItem>) => (
                <span className="text-sm text-muted-foreground">{formatApplicationDateTime(row.original.updated_at)}</span>
            ),
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
}: ApplicationListTableProps) {
    const navigate = useNavigate();
    const isArchived = status === 'ARCHIVED';
    const activeColumns = useMemo(() => buildActiveColumns(navigate), [navigate]);
    const archivedColumns = useMemo(() => buildArchivedColumns(canRestore, onRestore), [canRestore, onRestore]);
    const columns = isArchived ? archivedColumns : activeColumns;

    return (
        <DataTable
            columns={columns}
            data={applications}
            sorting={sorting}
            onSortingChange={onSortingChange}
            enableColumnVisibility
            loading={isLoading}
            skeletonCount={skeletonRowCount}
            emptyMessage="No applications found."
        />
    );
}
