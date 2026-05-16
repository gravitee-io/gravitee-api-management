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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';

import {
    createNotification,
    deleteNotification,
    listHooks,
    listNotifiers,
    listNotifications,
    updateNotification,
} from '../../../services/apis/notifications';
import type {
    ApiHook,
    ApiNotifier,
    CreateNotificationPayload,
    HookCategory,
    NotificationChannel,
    NotificationSettings,
    UpdateNotificationPayload,
} from '../types/notification';
import { apiNotificationKeys } from '../utils/queryKeys';

export type { HookCategory, NotificationChannel };

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Derive display channel from a notification + its notifier plugin. */
export function resolveChannel(notification: NotificationSettings, notifier: ApiNotifier | undefined): NotificationChannel {
    if (notification.config_type === 'PORTAL') return 'CONSOLE';
    if (notifier?.type === 'EMAIL') return 'EMAIL';
    if (notifier?.type === 'WEBHOOK') return 'WEBHOOK';
    return 'CONSOLE';
}

/** Group a flat hooks array by category, preserving backend order. */
export function groupHooksByCategory(hooks: ApiHook[]): HookCategory[] {
    const seen = new Map<string, ApiHook[]>();
    for (const hook of hooks) {
        const group = seen.get(hook.category) ?? [];
        group.push(hook);
        seen.set(hook.category, group);
    }
    return [...seen.entries()].map(([name, hs]) => ({ name, hooks: hs }));
}

// ─── Data hook ────────────────────────────────────────────────────────────────

export interface NotificationRow {
    /** 'PORTAL' for the default console notification; the notification ID otherwise. */
    key: string;
    notification: NotificationSettings;
    notifier: ApiNotifier | undefined;
    channel: NotificationChannel;
    /** True when managed by Kubernetes — editing not allowed. */
    isReadonly: boolean;
    /** PORTAL notification cannot be deleted. */
    canDelete: boolean;
}

export function useApiNotifications(apiId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && apiId);

    const notificationsQuery = useQuery({
        queryKey: apiNotificationKeys.list(envId, apiId ?? ''),
        queryFn: () => listNotifications(envId, apiId!),
        enabled,
        staleTime: 30_000,
    });

    const notifiersQuery = useQuery({
        queryKey: apiNotificationKeys.notifiers(envId, apiId ?? ''),
        queryFn: () => listNotifiers(envId, apiId!),
        enabled,
        staleTime: 60_000,
    });

    const hooksQuery = useQuery({
        queryKey: apiNotificationKeys.hooks(envId),
        queryFn: () => listHooks(envId),
        enabled,
        staleTime: 5 * 60_000,
    });

    // Build enriched rows only when all data is available
    const rows = useMemo<NotificationRow[]>(() => {
        const notifications = notificationsQuery.data ?? [];
        const notifiers = notifiersQuery.data ?? [];
        return notifications.map(n => {
            const notifier = notifiers.find(nr => nr.id === n.notifier);
            const channel = resolveChannel(n, notifier);
            return {
                key: n.id ?? 'PORTAL',
                notification: n,
                notifier,
                channel,
                isReadonly: Boolean(n.origin && n.origin !== 'MANAGEMENT'),
                canDelete: n.config_type !== 'PORTAL',
            };
        });
    }, [notificationsQuery.data, notifiersQuery.data]);

    const hookCategories = useMemo<HookCategory[]>(() => groupHooksByCategory(hooksQuery.data ?? []), [hooksQuery.data]);

    return {
        rows,
        notifiers: notifiersQuery.data ?? [],
        hookCategories,
        isLoading: notificationsQuery.isLoading || notifiersQuery.isLoading,
        isLoadingHooks: hooksQuery.isLoading,
        isError: notificationsQuery.isError || hooksQuery.isError || notifiersQuery.isError,
    };
}

export function useCreateNotification(apiId: string) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (payload: CreateNotificationPayload) => createNotification(envId, apiId, payload),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: apiNotificationKeys.list(envId, apiId) }),
    });
}

export function useUpdateNotification(apiId: string) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (payload: UpdateNotificationPayload) => updateNotification(envId, apiId, payload),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: apiNotificationKeys.list(envId, apiId) }),
    });
}

export function useDeleteNotification(apiId: string) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (notificationId: string) => deleteNotification(envId, apiId, notificationId),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: apiNotificationKeys.list(envId, apiId) }),
    });
}
