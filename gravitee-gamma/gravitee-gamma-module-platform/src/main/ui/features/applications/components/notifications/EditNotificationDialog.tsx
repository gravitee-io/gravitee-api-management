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
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Skeleton,
    Switch,
} from '@gravitee/graphene-core';
import { type FormEvent, useMemo, useState } from 'react';

import { NotificationHookCategorySection } from './NotificationHookCategorySection';
import type {
    ApplicationNotificationHookCategory,
    ApplicationNotificationRow,
    UpdateApplicationNotification,
} from '../../types/applicationNotification';

export function EditNotificationDialog({
    row,
    hookCategories,
    isLoadingHooks,
    isSaving,
    onCancel,
    onSave,
}: Readonly<{
    row: ApplicationNotificationRow | null;
    hookCategories: ApplicationNotificationHookCategory[];
    isLoadingHooks: boolean;
    isSaving: boolean;
    onCancel: () => void;
    onSave: (notification: UpdateApplicationNotification) => void;
}>) {
    const notification = row?.notification ?? null;
    const notifier = row?.notifier;
    const groupHookIds = useMemo(() => new Set(notification?.groupHooks ?? []), [notification?.groupHooks]);
    const [selectedHooks, setSelectedHooks] = useState<Set<string>>(new Set());
    const [config, setConfig] = useState('');
    const [useSystemProxy, setUseSystemProxy] = useState(false);

    // Reset form when a different notification is edited (setState-during-render pattern).
    const [prevNotification, setPrevNotification] = useState(notification);
    if (prevNotification !== notification) {
        setPrevNotification(notification);
        setSelectedHooks(new Set([...(notification?.hooks ?? []), ...(notification?.groupHooks ?? [])]));
        setConfig(notification?.config ?? '');
        setUseSystemProxy(Boolean(notification?.useSystemProxy));
    }

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
        if (!notification || row?.isReadonly) {
            return;
        }
        onSave({
            ...notification,
            config,
            useSystemProxy,
            hooks: [...selectedHooks].filter(hookId => !groupHookIds.has(hookId)),
        });
    }

    const needsNotifierConfig = notifier?.type === 'EMAIL' || notifier?.type === 'WEBHOOK';
    const configLabel = notifier?.type === 'EMAIL' ? 'Email list' : 'Webhook';
    const configHelp =
        notifier?.type === 'EMAIL'
            ? "Use space, ',' or ';' to separate emails. EL supported."
            : 'URL (Gravitee will POST datas to this url)';
    const disabled = isSaving || Boolean(row?.isReadonly);

    return (
        <Dialog open={row !== null} onOpenChange={open => !open && onCancel()}>
            <DialogContent style={{ width: 'min(72rem, 98vw)', maxWidth: '98vw' }}>
                <DialogHeader>
                    <DialogTitle>Edit Console Notification</DialogTitle>
                </DialogHeader>
                <form className="space-y-5" onSubmit={handleSubmit}>
                    <div className="grid gap-4 lg:grid-cols-3">
                        {needsNotifierConfig ? (
                            <div className="space-y-2 lg:col-span-2">
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
                        {notifier?.type === 'WEBHOOK' ? (
                            <div className="flex items-center justify-between gap-4 self-end rounded-lg border px-3 py-2">
                                <Label htmlFor="notification-system-proxy">Use system proxy</Label>
                                <Switch
                                    id="notification-system-proxy"
                                    checked={useSystemProxy}
                                    onCheckedChange={setUseSystemProxy}
                                    disabled={disabled}
                                />
                            </div>
                        ) : null}
                    </div>
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
                    <DialogFooter className="border-t px-6 py-4 gap-2">
                        <Button type="button" variant="outline" onClick={onCancel} disabled={isSaving}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={disabled || isLoadingHooks || !notification}>
                            {isSaving ? 'Saving…' : 'Save'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
