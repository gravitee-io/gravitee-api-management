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

import { groupHooksByCategory, mapApplicationNotificationsToRows } from '../../applications/components/notifications/notificationHelpers';
import type { ApplicationNotificationRow, UpdateApplicationNotification } from '../../applications/types/applicationNotification';
import {
    createEnvironmentNotification,
    deleteEnvironmentNotification,
    listEnvironmentNotificationHooks,
    listEnvironmentNotifications,
    listEnvironmentNotifiers,
    updateEnvironmentNotification,
} from '../services/environmentNotifications';
import type { CreateEnvironmentNotification } from '../types/environmentNotification';
import { environmentNotificationKeys } from '../utils/queryKeys';

export function useEnvironmentNotifications() {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env);

    const notificationsQuery = useQuery({
        queryKey: environmentNotificationKeys.list(envId),
        queryFn: () => listEnvironmentNotifications(envId),
        enabled,
        staleTime: 30_000,
    });

    const notifiersQuery = useQuery({
        queryKey: environmentNotificationKeys.notifiers(envId),
        queryFn: () => listEnvironmentNotifiers(envId),
        enabled,
        staleTime: 60_000,
    });

    const hooksQuery = useQuery({
        queryKey: environmentNotificationKeys.hooks(envId),
        queryFn: () => listEnvironmentNotificationHooks(envId),
        enabled,
        staleTime: 5 * 60_000,
    });

    const rows = useMemo<ApplicationNotificationRow[]>(
        () => mapApplicationNotificationsToRows(notificationsQuery.data ?? [], notifiersQuery.data ?? []),
        [notificationsQuery.data, notifiersQuery.data],
    );

    const hookCategories = useMemo(() => groupHooksByCategory(hooksQuery.data ?? []), [hooksQuery.data]);

    return {
        rows,
        notifiers: notifiersQuery.data ?? [],
        hookCategories,
        isLoading: notificationsQuery.isLoading || notifiersQuery.isLoading,
        isLoadingHooks: hooksQuery.isLoading,
        isError: notificationsQuery.isError || notifiersQuery.isError || hooksQuery.isError,
        error: notificationsQuery.error ?? notifiersQuery.error ?? hooksQuery.error,
    };
}

export function useCreateEnvironmentNotification() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (notification: CreateEnvironmentNotification) => createEnvironmentNotification(envId, notification),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: environmentNotificationKeys.list(envId) });
        },
    });
}

export function useUpdateEnvironmentNotification() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (notification: UpdateApplicationNotification) => updateEnvironmentNotification(envId, notification),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: environmentNotificationKeys.list(envId) });
        },
    });
}

export function useDeleteEnvironmentNotification() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (notificationId: string) => deleteEnvironmentNotification(envId, notificationId),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: environmentNotificationKeys.list(envId) });
        },
    });
}
