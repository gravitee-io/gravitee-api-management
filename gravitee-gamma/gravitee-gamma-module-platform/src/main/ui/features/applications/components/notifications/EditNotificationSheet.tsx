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
    Button,
    Input,
    Label,
    ScrollArea,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Skeleton,
    Switch,
} from '@gravitee/graphene-core';
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react';

import { isCreateNotificationRow, notificationNotifierOptions } from './notificationHelpers';
import { NotificationHookCategorySection } from './NotificationHookCategorySection';
import type {
    ApplicationNotificationHookCategory,
    ApplicationNotificationRow,
    ApplicationNotifier,
    UpdateApplicationNotification,
} from '../../types/applicationNotification';
import { RequiredLabel } from '../notification-settings/RequiredLabel';

export type NotificationSheetCreatePayload = {
    name: string;
    notifier: string;
    config?: string;
    useSystemProxy?: boolean;
    hooks: string[];
};

export function EditNotificationSheet({
    row,
    notifiers,
    hookCategories,
    isLoadingHooks,
    isSaving,
    onCancel,
    onSave,
    onCreate,
}: Readonly<{
    row: ApplicationNotificationRow | null;
    notifiers: ApplicationNotifier[];
    hookCategories: ApplicationNotificationHookCategory[];
    isLoadingHooks: boolean;
    isSaving: boolean;
    onCancel: () => void;
    onSave: (notification: UpdateApplicationNotification) => void;
    onCreate: (payload: NotificationSheetCreatePayload) => void;
}>) {
    const isCreate = isCreateNotificationRow(row);
    const notification = row?.notification ?? null;
    const notifierOptions = useMemo(() => notificationNotifierOptions(notifiers), [notifiers]);
    const groupHookIds = useMemo(() => new Set(notification?.groupHooks ?? []), [notification?.groupHooks]);

    const [name, setName] = useState('');
    const [notifierId, setNotifierId] = useState('');
    const [selectedHooks, setSelectedHooks] = useState<Set<string>>(new Set());
    const [config, setConfig] = useState('');
    const [useSystemProxy, setUseSystemProxy] = useState(false);

    useEffect(() => {
        if (!notification) {
            setName('');
            setNotifierId('');
            setSelectedHooks(new Set());
            setConfig('');
            setUseSystemProxy(false);
            return;
        }
        if (isCreate) {
            setName(notification.name ?? '');
            setNotifierId(notification.notifier ?? notifierOptions[0]?.id ?? '');
        }
        setSelectedHooks(new Set([...(notification.hooks ?? []), ...(notification.groupHooks ?? [])]));
        setConfig(notification.config ?? '');
        setUseSystemProxy(Boolean(notification.useSystemProxy));
    }, [isCreate, notification, notifierOptions]);

    const activeNotifier = useMemo(() => {
        if (isCreate) {
            return notifierOptions.find(option => option.id === notifierId)?.notifier;
        }
        return row?.notifier;
    }, [isCreate, notifierId, notifierOptions, row?.notifier]);

    function toggleHook(hookId: string) {
        setSelectedHooks(prev => {
            const next = new Set(prev);
            if (next.has(hookId)) {
                next.delete(hookId);
            } else {
                next.add(hookId);
            }
            return next;
        });
    }

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (row?.isReadonly) {
            return;
        }

        const hooks = [...selectedHooks].filter(hookId => !groupHookIds.has(hookId));

        if (isCreate) {
            if (!name.trim() || !notifierId) {
                return;
            }
            onCreate({
                name: name.trim(),
                notifier: notifierId,
                config,
                useSystemProxy,
                hooks,
            });
            return;
        }

        if (!notification) {
            return;
        }
        onSave({
            ...notification,
            config,
            useSystemProxy,
            hooks,
        });
    }

    const needsNotifierConfig = activeNotifier?.type === 'EMAIL' || activeNotifier?.type === 'WEBHOOK';
    const configLabel = activeNotifier?.type === 'EMAIL' ? 'Email list' : 'Webhook';
    const configHelp =
        activeNotifier?.type === 'EMAIL'
            ? "Use space, ',' or ';' to separate emails. EL supported."
            : 'URL (Gravitee will POST datas to this url)';
    const disabled = isSaving || Boolean(row?.isReadonly);
    const canSubmit = isCreate ? Boolean(name.trim() && notifierId) : Boolean(notification);
    const sheetSubject = isCreate ? name.trim() || 'this notification' : row?.name || 'this notification';

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onCancel();
        },
        [onCancel],
    );

    return (
        <Sheet open={row !== null} onOpenChange={handleOpenChange}>
            <SheetContent
                side="right"
                className="flex max-h-full flex-col"
                style={{ width: 'min(98vw, 51.84rem)', maxWidth: 'min(98vw, 51.84rem)' }}
            >
                <SheetHeader>
                    <SheetTitle>Edit Console Notification</SheetTitle>
                    <SheetDescription>Configure notifier settings and subscribed events for {sheetSubject}.</SheetDescription>
                </SheetHeader>

                <form id="edit-notification-form" className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
                    <ScrollArea className="min-h-0 flex-1">
                        <div className="space-y-5 px-4 pb-4">
                            {isCreate ? (
                                <div className="grid gap-4 md:grid-cols-2">
                                    <div className="space-y-2">
                                        <RequiredLabel htmlFor="notification-name">Name</RequiredLabel>
                                        <Input
                                            id="notification-name"
                                            value={name}
                                            onChange={event => setName(event.target.value)}
                                            placeholder="Notification name"
                                            disabled={disabled}
                                            required
                                            aria-required="true"
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <RequiredLabel htmlFor="notification-notifier">Notifier</RequiredLabel>
                                        <Select
                                            value={notifierId}
                                            onValueChange={setNotifierId}
                                            disabled={disabled || notifierOptions.length === 0}
                                            required
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
                            ) : null}
                            {needsNotifierConfig ? (
                                <div className="space-y-2">
                                    <Label htmlFor="notification-config">{configLabel}</Label>
                                    <Input
                                        id="notification-config"
                                        value={config}
                                        onChange={event => setConfig(event.target.value)}
                                        disabled={disabled}
                                    />
                                    <p className="text-xs text-muted-foreground">{configHelp}</p>
                                </div>
                            ) : null}
                            {activeNotifier?.type === 'WEBHOOK' ? (
                                <div className="flex items-center justify-between gap-4 rounded-lg border px-3 py-2">
                                    <Label htmlFor="notification-system-proxy">Use system proxy</Label>
                                    <Switch
                                        id="notification-system-proxy"
                                        checked={useSystemProxy}
                                        onCheckedChange={setUseSystemProxy}
                                        disabled={disabled}
                                    />
                                </div>
                            ) : null}
                            <div className="space-y-4">
                                <h3 className="text-sm font-semibold">Event subscribed</h3>
                                {isLoadingHooks ? (
                                    <div className="space-y-2">
                                        {Array.from({ length: 3 }).map((_, index) => (
                                            <Skeleton key={index} className="h-16 rounded-lg" />
                                        ))}
                                    </div>
                                ) : (
                                    hookCategories.map(category => (
                                        <NotificationHookCategorySection
                                            key={category.name}
                                            category={category}
                                            selectedHooks={selectedHooks}
                                            groupHookIds={groupHookIds}
                                            disabled={disabled}
                                            onToggle={toggleHook}
                                        />
                                    ))
                                )}
                            </div>
                        </div>
                    </ScrollArea>
                </form>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onCancel} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="edit-notification-form" disabled={disabled || isLoadingHooks || !canSubmit}>
                        {isSaving ? 'Saving…' : isCreate ? 'Add notification' : 'Save'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
