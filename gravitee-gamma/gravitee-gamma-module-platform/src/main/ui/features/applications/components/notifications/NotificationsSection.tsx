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
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    DataTable,
    Skeleton,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { PencilIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import type { ApplicationNotificationRow } from '../../types/applicationNotification';
import { NON_SORTABLE_COLUMN } from '../../utils/dataTableHeaders';
import type { ColCell } from '../../utils/dataTableTypes';

export function NotificationsSection({
    rows,
    isLoading,
    isError,
    canCreate,
    canUpdate,
    canDelete,
    onCreateClick,
    onEdit,
    onDelete,
}: {
    readonly rows: ApplicationNotificationRow[];
    readonly isLoading: boolean;
    readonly isError: boolean;
    readonly canCreate: boolean;
    readonly canUpdate: boolean;
    readonly canDelete: boolean;
    readonly onCreateClick: () => void;
    readonly onEdit: (row: ApplicationNotificationRow) => void;
    readonly onDelete: (row: ApplicationNotificationRow) => void;
}) {
    const notificationColumns = useMemo((): DataTableProps<ApplicationNotificationRow>['columns'] => {
        return [
            {
                accessorKey: 'name',
                header: 'Name',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ApplicationNotificationRow>) => (
                    <span className="block truncate font-medium" title={row.original.name}>
                        {row.original.name}
                    </span>
                ),
            },
            {
                accessorKey: 'subscribedEvents',
                header: 'Events subscribed',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ApplicationNotificationRow>) => (
                    <span className="inline-flex rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">
                        {row.original.subscribedEvents} events
                    </span>
                ),
            },
            {
                accessorKey: 'notifierName',
                header: 'Notifier',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ApplicationNotificationRow>) => (
                    <span className="inline-flex rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">
                        {row.original.notifierName}
                    </span>
                ),
            },
            {
                id: 'actions',
                header: () => <div className="text-right">Actions</div>,
                size: 96,
                cell: ({ row }: ColCell<ApplicationNotificationRow>) => (
                    <div className="flex justify-end gap-1">
                        {canUpdate ? (
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-8"
                                aria-label={`Edit ${row.original.name} notification`}
                                disabled={row.original.isReadonly}
                                onClick={() => onEdit(row.original)}
                            >
                                <PencilIcon className="size-4" aria-hidden />
                            </Button>
                        ) : null}
                        {canDelete && row.original.notification.config_type !== 'PORTAL' && Boolean(row.original.notification.id) ? (
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-8 text-destructive hover:text-destructive"
                                aria-label={`Delete ${row.original.name} notification`}
                                disabled={row.original.isReadonly}
                                onClick={() => onDelete(row.original)}
                            >
                                <Trash2Icon className="size-4" aria-hidden />
                            </Button>
                        ) : null}
                    </div>
                ),
                enableSorting: false,
                enableHiding: false,
            },
        ];
    }, [canDelete, canUpdate, onDelete, onEdit]);

    return (
        <Card>
            <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                <div className="space-y-1.5">
                    <CardTitle className="text-base">Notifications</CardTitle>
                    <CardDescription>Events that trigger notifiers for this application.</CardDescription>
                </div>
                {canCreate ? (
                    <Button type="button" size="sm" className="shrink-0" onClick={onCreateClick}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add notification
                    </Button>
                ) : null}
            </CardHeader>
            <CardContent className="space-y-6">
                {isError ? (
                    <Alert variant="destructive">
                        <AlertDescription>Failed to load notification settings. Please refresh the page.</AlertDescription>
                    </Alert>
                ) : isLoading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 3 }).map((_, index) => (
                            <Skeleton key={index} className="h-10 rounded-lg" />
                        ))}
                    </div>
                ) : (
                    <DataTable columns={notificationColumns} data={rows} emptyMessage="No notifications configured." />
                )}
            </CardContent>
        </Card>
    );
}
