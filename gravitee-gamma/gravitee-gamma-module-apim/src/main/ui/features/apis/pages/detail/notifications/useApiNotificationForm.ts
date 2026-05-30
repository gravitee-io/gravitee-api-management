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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import {
    type HookCategory,
    type NotificationChannel,
    useApiNotifications,
    useCreateNotification,
    useUpdateNotification,
} from '../../../hooks/useApiNotifications';
import type { ApiNotifier, NotificationSettings } from '../../../types/notification';

/** A single channel a brand-new notification can be created on. Console is a built-in
 *  singleton edited from the list, so it is intentionally not an "add" option here. */
interface ChannelOption {
    notifierId: string;
    label: string;
    type: 'EMAIL' | 'WEBHOOK';
}

function buildAddChannelOptions(notifiers: ApiNotifier[]): ChannelOption[] {
    const options: ChannelOption[] = [];
    for (const n of notifiers) {
        if (n.type === 'EMAIL') options.push({ notifierId: n.id, label: n.name || 'Email', type: 'EMAIL' });
        else if (n.type === 'WEBHOOK') options.push({ notifierId: n.id, label: n.name || 'Webhook', type: 'WEBHOOK' });
    }
    return options;
}

function toMessage(err: unknown, fallback: string): string {
    return err instanceof Error ? err.message : fallback;
}

export interface UseApiNotificationFormReturn {
    isUpdate: boolean;
    /** Permission to be on this screen (create for add, update for edit). */
    allowed: boolean;
    /** Editing a row managed outside the console (e.g. Kubernetes) — fields are read-only. */
    isReadonly: boolean;
    /** Edit mode, data loaded, but the requested notification no longer exists. */
    notFound: boolean;
    isLoading: boolean;
    isLoadingHooks: boolean;
    isPending: boolean;
    saveError: string | null;

    /** Notification name (read-only in edit mode). */
    name: string;
    setName: (value: string) => void;
    nameReadonly: boolean;

    /** Channel selection (add mode only); fixed in edit mode. */
    channel: NotificationChannel;
    channelOptions: ChannelOption[];
    selectedNotifierId: string;
    setSelectedNotifierId: (value: string) => void;

    /** Email address(es) / webhook URL. Only relevant for EMAIL and WEBHOOK channels. */
    needsTarget: boolean;
    config: string;
    setConfig: (value: string) => void;

    /** "Use system proxy" — webhook channel only, mirrors the classic console. */
    showSystemProxy: boolean;
    useSystemProxy: boolean;
    setUseSystemProxy: (value: boolean) => void;

    hookCategories: HookCategory[];
    groupHookIds: Set<string>;
    selectedHooks: Set<string>;
    toggleHook: (hookId: string) => void;

    canSubmit: boolean;
    handleSave: () => void;
    handleCancel: () => void;
}

/**
 * Drives the dedicated Add / Edit notification page.
 *
 * Mirrors the exact mutation logic of the previous inline flow (no behaviour change):
 *  - Edit: PUT the notification with the selected hooks (group hooks stripped) and target.
 *  - Add (email / webhook): POST to create the GENERIC notification, then PUT the events + target —
 *    the same two-call sequence the previous "add then edit events" flow performed.
 */
export function useApiNotificationForm(): UseApiNotificationFormReturn {
    const { apiId, notificationKey } = useParams<{ apiId: string; notificationKey: string }>();
    const navigate = useNavigate();

    const isUpdate = !!notificationKey && notificationKey !== 'new';

    // Guards preserved from the previous inline flow: create gates add, update gates edit.
    const canCreate = useHasPermission({ anyOf: ['api-notification-c'] });
    const canUpdate = useHasPermission({ anyOf: ['api-notification-u'] });
    const allowed = isUpdate ? canUpdate : canCreate;

    const { rows, notifiers, hookCategories, isLoading, isLoadingHooks } = useApiNotifications(apiId);
    const createMutation = useCreateNotification(apiId ?? '');
    const updateMutation = useUpdateNotification(apiId ?? '');

    const editingRow = useMemo(
        () => (isUpdate ? (rows.find(r => r.key === notificationKey) ?? null) : null),
        [isUpdate, rows, notificationKey],
    );

    const channelOptions = useMemo(() => buildAddChannelOptions(notifiers), [notifiers]);

    const [name, setName] = useState('');
    const [notifierIdOverride, setNotifierIdOverride] = useState<string | null>(null);
    const [config, setConfig] = useState('');
    const [useSystemProxy, setUseSystemProxy] = useState(false);
    const [selectedHooks, setSelectedHooks] = useState<Set<string>>(new Set());
    const [saveError, setSaveError] = useState<string | null>(null);

    // Guards against POSTing twice if the follow-up PUT fails and the user retries.
    const createdRef = useRef<NotificationSettings | null>(null);
    // Initialise edit state exactly once per notification (survives background refetches).
    const initializedForRef = useRef<string | undefined>(undefined);

    useEffect(() => {
        if (!isUpdate || !editingRow || initializedForRef.current === notificationKey) return;
        initializedForRef.current = notificationKey;
        setName(editingRow.notification.name);
        setConfig(editingRow.notification.config ?? '');
        setUseSystemProxy(editingRow.notification.useSystemProxy ?? false);
        setSelectedHooks(new Set([...(editingRow.notification.hooks ?? []), ...(editingRow.notification.groupHooks ?? [])]));
    }, [isUpdate, editingRow, notificationKey]);

    // Default the add-channel to the first available notifier until the user picks one.
    const selectedNotifierId = notifierIdOverride ?? channelOptions[0]?.notifierId ?? '';
    const selectedOption = channelOptions.find(o => o.notifierId === selectedNotifierId);

    const channel: NotificationChannel = isUpdate ? (editingRow?.channel ?? 'CONSOLE') : (selectedOption?.type ?? 'CONSOLE');
    const needsTarget = channel === 'EMAIL' || channel === 'WEBHOOK';

    const groupHookIds = useMemo(() => new Set(isUpdate ? (editingRow?.notification.groupHooks ?? []) : []), [isUpdate, editingRow]);

    const isReadonly = Boolean(isUpdate && editingRow?.isReadonly);
    const notFound = isUpdate && !isLoading && !editingRow;

    const toggleHook = useCallback((hookId: string) => {
        setSelectedHooks(prev => {
            const next = new Set(prev);
            if (next.has(hookId)) next.delete(hookId);
            else next.add(hookId);
            return next;
        });
    }, []);

    const isPending = createMutation.isPending || updateMutation.isPending;
    const isNameValid = name.trim().length > 0;
    const hasChannel = isUpdate || Boolean(selectedNotifierId);
    const canSubmit = !isReadonly && hasChannel && (isUpdate || isNameValid) && !isPending;

    const backToList = useCallback(() => navigate('..'), [navigate]);

    const handleSave = useCallback(() => {
        if (!apiId || isReadonly || isPending) return;
        setSaveError(null);

        // ── Edit: PUT with selected hooks (group hooks are not user-owned) + target ──
        if (isUpdate) {
            if (!editingRow) return;
            const userHooks = [...selectedHooks].filter(h => !groupHookIds.has(h));
            updateMutation.mutate(
                // useSystemProxy is webhook-specific; only include it for the WEBHOOK channel.
                { ...editingRow.notification, hooks: userHooks, config, ...(channel === 'WEBHOOK' ? { useSystemProxy } : {}) },
                {
                    onSuccess: backToList,
                    onError: err => setSaveError(toMessage(err, 'Failed to save notification events.')),
                },
            );
            return;
        }

        // ── Add (email / webhook only) ──
        if (!isNameValid || !selectedNotifierId) return;
        const userHooks = [...selectedHooks];

        const persistEvents = (created: NotificationSettings) => {
            // useSystemProxy is webhook-specific; only include it for the WEBHOOK channel.
            updateMutation.mutate(
                { ...created, hooks: userHooks, config, ...(channel === 'WEBHOOK' ? { useSystemProxy } : {}) },
                {
                    onSuccess: backToList,
                    onError: err => setSaveError(toMessage(err, 'Failed to save notification events.')),
                },
            );
        };

        // Resume at the PUT step if the POST already succeeded on a previous attempt.
        if (createdRef.current) {
            persistEvents(createdRef.current);
            return;
        }

        createMutation.mutate(
            {
                name: name.trim(),
                notifier: selectedNotifierId,
                config_type: 'GENERIC',
                hooks: [],
                referenceType: 'API',
                referenceId: apiId,
            },
            {
                onSuccess: created => {
                    createdRef.current = created;
                    persistEvents(created);
                },
                onError: err => setSaveError(toMessage(err, 'Failed to create notification.')),
            },
        );
    }, [
        apiId,
        isReadonly,
        isPending,
        isUpdate,
        editingRow,
        selectedHooks,
        groupHookIds,
        config,
        channel,
        useSystemProxy,
        isNameValid,
        selectedNotifierId,
        name,
        createMutation,
        updateMutation,
        backToList,
    ]);

    return {
        isUpdate,
        allowed,
        isReadonly,
        notFound,
        isLoading,
        isLoadingHooks,
        isPending,
        saveError,
        name,
        setName,
        nameReadonly: isUpdate,
        channel,
        channelOptions,
        selectedNotifierId,
        setSelectedNotifierId: setNotifierIdOverride,
        needsTarget,
        config,
        setConfig,
        showSystemProxy: channel === 'WEBHOOK',
        useSystemProxy,
        setUseSystemProxy,
        hookCategories,
        groupHookIds,
        selectedHooks,
        toggleHook,
        canSubmit,
        handleSave,
        handleCancel: backToList,
    };
}
