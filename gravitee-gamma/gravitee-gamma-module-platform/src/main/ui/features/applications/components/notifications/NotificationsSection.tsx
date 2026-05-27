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
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { PencilIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { type FormEvent, useEffect, useMemo, useState } from 'react';

import { notificationNotifierOptions } from './notificationHelpers';
import type { ApplicationNotificationRow, ApplicationNotifier } from '../../types/applicationNotification';
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
                    <Table aria-label="Application notification settings">
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>
                                <TableHead>Events subscribed</TableHead>
                                <TableHead>Notifier</TableHead>
                                <TableHead className="w-16 text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {rows.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={4} className="h-16 text-center text-sm text-muted-foreground">
                                        No notifications configured.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                rows.map(row => (
                                    <TableRow key={row.key}>
                                        <TableCell className="font-medium">{row.name}</TableCell>
                                        <TableCell>
                                            <span className="inline-flex rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">
                                                {row.subscribedEvents} events
                                            </span>
                                        </TableCell>
                                        <TableCell>
                                            <span className="inline-flex rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">
                                                {row.notifierName}
                                            </span>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <div className="flex justify-end gap-1">
                                                {canUpdate ? (
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8"
                                                        aria-label={`Edit ${row.name} notification`}
                                                        disabled={row.isReadonly}
                                                        onClick={() => onEdit(row)}
                                                    >
                                                        <PencilIcon className="size-4" aria-hidden />
                                                    </Button>
                                                ) : null}
                                                {canDelete && row.notification.config_type !== 'PORTAL' && Boolean(row.notification.id) ? (
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8 text-destructive hover:text-destructive"
                                                        aria-label={`Delete ${row.name} notification`}
                                                        disabled={row.isReadonly}
                                                        onClick={() => onDelete(row)}
                                                    >
                                                        <Trash2Icon className="size-4" aria-hidden />
                                                    </Button>
                                                ) : null}
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                )}
            </CardContent>
        </Card>
    );
}
