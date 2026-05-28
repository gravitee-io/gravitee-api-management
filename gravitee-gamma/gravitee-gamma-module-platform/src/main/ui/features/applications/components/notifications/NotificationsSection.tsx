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
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    DataTable,
    Skeleton,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { PencilIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { type FormEvent, useEffect, useMemo, useState } from 'react';

import { notificationNotifierOptions } from './notificationHelpers';
import type { ApplicationNotificationRow, ApplicationNotifier } from '../../types/applicationNotification';
import { NON_SORTABLE_COLUMN } from '../../utils/dataTableHeaders';
import type { ColCell } from '../../utils/dataTableTypes';
import { RequiredLabel } from '../notification-settings/RequiredLabel';

export function NotificationsSection({
    rows,
    notifiers,
    isLoading,
    isError,
    canCreate,
    canUpdate,
    canDelete,
    isCreating,
    onAdd,
    onEdit,
    onDelete,
}: {
    readonly rows: ApplicationNotificationRow[];
    readonly notifiers: ApplicationNotifier[];
    readonly isLoading: boolean;
    readonly isError: boolean;
    readonly canCreate: boolean;
    readonly canUpdate: boolean;
    readonly canDelete: boolean;
    readonly isCreating: boolean;
    readonly onAdd: (name: string, notifierId: string) => Promise<boolean>;
    readonly onEdit: (row: ApplicationNotificationRow) => void;
    readonly onDelete: (row: ApplicationNotificationRow) => void;
}) {
    const notifierOptions = useMemo(() => notificationNotifierOptions(notifiers), [notifiers]);
    const [name, setName] = useState('');
    const [notifierId, setNotifierId] = useState('');

    useEffect(() => {
        if (!notifierId && notifierOptions[0]) {
            setNotifierId(notifierOptions[0].id);
        }
    }, [notifierId, notifierOptions]);

    const canSubmit = Boolean(canCreate && name.trim() && notifierId);

    const notificationColumns = useMemo((): DataTableProps<ApplicationNotificationRow>['columns'] => {
        return [
            {
                accessorKey: 'name',
                header: 'Name',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ApplicationNotificationRow>) => <span className="font-medium">{row.original.name}</span>,
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

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!canSubmit) {
            return;
        }
        const created = await onAdd(name.trim(), notifierId);
        if (created) {
            setName('');
            setNotifierId(notifierOptions[0]?.id ?? '');
        }
    }

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-base">Notifications</CardTitle>
                <CardDescription>Events that trigger notifiers for this application.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                {canCreate ? (
                    <form className="space-y-3" onSubmit={handleSubmit}>
                        <div className="grid gap-3 md:grid-cols-2">
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="notification-name">Name</RequiredLabel>
                                <Input
                                    id="notification-name"
                                    value={name}
                                    onChange={event => setName(event.target.value)}
                                    placeholder="Notification name"
                                    disabled={isCreating}
                                    required
                                    aria-required="true"
                                />
                            </div>
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="notification-notifier">Notifier</RequiredLabel>
                                <Select
                                    value={notifierId}
                                    onValueChange={setNotifierId}
                                    disabled={isCreating || notifierOptions.length === 0}
                                >
                                    <SelectTrigger id="notification-notifier" className="w-full" aria-required="true">
                                        <SelectValue placeholder="Select a notifier" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {notifierOptions.map(option => (
                                            <SelectItem key={option.id} value={option.id}>
                                                {option.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                        <Button type="submit" size="sm" disabled={!canSubmit || isCreating}>
                            <PlusIcon className="size-4" aria-hidden />
                            {isCreating ? 'Adding…' : 'Add notification'}
                        </Button>
                    </form>
                ) : null}
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
