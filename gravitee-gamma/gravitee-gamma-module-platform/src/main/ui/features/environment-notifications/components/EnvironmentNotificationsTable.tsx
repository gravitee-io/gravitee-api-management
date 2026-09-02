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
    Alert,
    AlertDescription,
    Badge,
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    DataTable,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Skeleton,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import type { ColCell } from '../../../shared/utils/dataTableTypes';
import type { ApplicationNotificationRow } from '../../applications/types/applicationNotification';
import {
    canDeleteNotificationRow,
    CHANNEL_ICON,
    CHANNEL_LABEL,
    notificationTarget,
    resolveNotificationChannel,
} from '../utils/notificationChannel';

function ChannelBadge({ row }: Readonly<{ row: ApplicationNotificationRow }>) {
    const channel = resolveNotificationChannel(row);
    const Icon = CHANNEL_ICON[channel];
    return (
        <Badge variant="outline" className="gap-1 font-normal">
            <Icon className="size-3" aria-hidden />
            {CHANNEL_LABEL[channel]}
        </Badge>
    );
}

function EventCountBadge({ count }: Readonly<{ count: number }>) {
    if (count === 0) {
        return <span className="text-xs italic text-muted-foreground">None</span>;
    }
    return (
        <Badge variant="secondary" className="text-xs font-normal">
            {count} event{count === 1 ? '' : 's'}
        </Badge>
    );
}

function rowActionVisibility(
    row: ApplicationNotificationRow,
    canUpdate: (row: ApplicationNotificationRow) => boolean,
    canDelete: boolean,
): { showEdit: boolean; showDelete: boolean } {
    return {
        showEdit: canUpdate(row) && !row.isReadonly,
        showDelete: canDelete && canDeleteNotificationRow(row),
    };
}

function NotificationRowActions({
    row,
    canUpdate,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    row: ApplicationNotificationRow;
    canUpdate: (row: ApplicationNotificationRow) => boolean;
    canDelete: boolean;
    onEdit: (row: ApplicationNotificationRow) => void;
    onDelete: (row: ApplicationNotificationRow) => void;
}>) {
    const { showEdit, showDelete } = rowActionVisibility(row, canUpdate, canDelete);
    if (!showEdit && !showDelete) {
        return null;
    }
    return (
        <div className="flex justify-end">
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button type="button" variant="ghost" size="icon" className="size-8" aria-label={`Actions for ${row.name}`}>
                        <MoreHorizontalIcon className="size-4" aria-hidden />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                    {showEdit ? (
                        <DropdownMenuItem onSelect={() => onEdit(row)}>
                            <PencilIcon className="size-4" aria-hidden />
                            Edit
                        </DropdownMenuItem>
                    ) : null}
                    {showDelete ? (
                        <>
                            {showEdit ? <DropdownMenuSeparator /> : null}
                            <DropdownMenuItem variant="destructive" onSelect={() => onDelete(row)}>
                                <Trash2Icon className="size-4" aria-hidden />
                                Delete
                            </DropdownMenuItem>
                        </>
                    ) : null}
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}

function buildColumns({
    canUpdate,
    canDelete,
    onEdit,
    onDelete,
    hasActionsColumn,
}: {
    canUpdate: (row: ApplicationNotificationRow) => boolean;
    canDelete: boolean;
    onEdit: (row: ApplicationNotificationRow) => void;
    onDelete: (row: ApplicationNotificationRow) => void;
    hasActionsColumn: boolean;
}): DataTableProps<ApplicationNotificationRow>['columns'] {
    const columns: DataTableProps<ApplicationNotificationRow>['columns'] = [
        {
            accessorKey: 'name',
            header: 'Name',
            enableSorting: false,
            cell: ({ row }: ColCell<ApplicationNotificationRow>) => <span className="font-medium">{row.original.name}</span>,
        },
        {
            id: 'channel',
            header: 'Channel',
            enableSorting: false,
            cell: ({ row }: ColCell<ApplicationNotificationRow>) => <ChannelBadge row={row.original} />,
        },
        {
            accessorKey: 'subscribedEvents',
            header: 'Events',
            enableSorting: false,
            cell: ({ row }: ColCell<ApplicationNotificationRow>) => <EventCountBadge count={row.original.subscribedEvents} />,
        },
        {
            id: 'target',
            header: 'Target',
            enableSorting: false,
            cell: ({ row }: ColCell<ApplicationNotificationRow>) => (
                <span className="block max-w-72 truncate font-mono text-xs text-muted-foreground">{notificationTarget(row.original)}</span>
            ),
        },
    ];

    if (hasActionsColumn) {
        columns.push({
            id: 'actions',
            header: () => <div className="text-right">Actions</div>,
            size: 96,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<ApplicationNotificationRow>) => (
                <NotificationRowActions
                    row={row.original}
                    canUpdate={canUpdate}
                    canDelete={canDelete}
                    onEdit={onEdit}
                    onDelete={onDelete}
                />
            ),
        });
    }

    return columns;
}

export function EnvironmentNotificationsTable({
    rows,
    isLoading,
    isError,
    canUpdate,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    rows: ApplicationNotificationRow[];
    isLoading: boolean;
    isError: boolean;
    canUpdate: (row: ApplicationNotificationRow) => boolean;
    canDelete: boolean;
    onEdit: (row: ApplicationNotificationRow) => void;
    onDelete: (row: ApplicationNotificationRow) => void;
}>) {
    const hasActionsColumn = rows.some(row => {
        const { showEdit, showDelete } = rowActionVisibility(row, canUpdate, canDelete);
        return showEdit || showDelete;
    });

    const columns = useMemo(
        () => buildColumns({ canUpdate, canDelete, onEdit, onDelete, hasActionsColumn }),
        [canUpdate, canDelete, onEdit, onDelete, hasActionsColumn],
    );

    return (
        <Card>
            <CardHeader className="pb-0">
                <CardTitle className="text-base">Configured notifications</CardTitle>
            </CardHeader>
            <CardContent className="p-0 pt-2">
                {isError ? (
                    <div className="px-6 pb-6">
                        <Alert variant="destructive">
                            <AlertDescription>Failed to load notification settings. Please refresh the page.</AlertDescription>
                        </Alert>
                    </div>
                ) : isLoading ? (
                    <div className="space-y-2 px-6 pb-6">
                        {Array.from({ length: 3 }).map((_, index) => (
                            <Skeleton key={index} className="h-10 rounded-lg" />
                        ))}
                    </div>
                ) : (
                    <DataTable columns={columns} data={rows} emptyMessage="No notifications configured." />
                )}
            </CardContent>
        </Card>
    );
}
