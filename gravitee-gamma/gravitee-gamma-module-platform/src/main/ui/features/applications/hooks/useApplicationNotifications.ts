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

import { groupHooksByCategory, mapApplicationNotificationsToRows } from '../components/notifications/notificationHelpers';
import {
    createApplicationNotification,
    createApplicationMetadata,
    deleteApplicationMetadata,
    deleteApplicationNotification,
    listApplicationMetadata,
    listApplicationNotificationHooks,
    listApplicationNotifications,
    listApplicationNotifiers,
    updateApplicationNotification,
    updateApplicationMetadata,
} from '../services/applicationNotifications';
import type {
    ApplicationNotificationRow,
    CreateApplicationNotification,
    NewApplicationMetadata,
    UpdateApplicationNotification,
    UpdateApplicationMetadata,
} from '../types/applicationNotification';
import { applicationNotificationKeys } from '../utils/queryKeys';

export function useApplicationNotifications(applicationId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && applicationId);

    const notificationsQuery = useQuery({
        queryKey: applicationNotificationKeys.list(envId, applicationId ?? ''),
        queryFn: () => listApplicationNotifications(envId, applicationId!),
        enabled,
        staleTime: 30_000,
    });

    const notifiersQuery = useQuery({
        queryKey: applicationNotificationKeys.notifiers(envId, applicationId ?? ''),
        queryFn: () => listApplicationNotifiers(envId, applicationId!),
        enabled,
        staleTime: 60_000,
    });

    const hooksQuery = useQuery({
        queryKey: applicationNotificationKeys.hooks(envId),
        queryFn: () => listApplicationNotificationHooks(envId),
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
    };
}

export function useCreateApplicationNotification(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (notification: CreateApplicationNotification) => createApplicationNotification(envId, applicationId!, notification),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: applicationNotificationKeys.list(envId, applicationId ?? '') });
        },
    });
}

export function useUpdateApplicationNotification(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (notification: UpdateApplicationNotification) => updateApplicationNotification(envId, applicationId!, notification),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: applicationNotificationKeys.list(envId, applicationId ?? '') });
        },
    });
}

export function useDeleteApplicationNotification(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (notificationId: string) => deleteApplicationNotification(envId, applicationId!, notificationId),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: applicationNotificationKeys.list(envId, applicationId ?? '') });
        },
    });
}

export function useApplicationMetadata(applicationId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useQuery({
        queryKey: applicationNotificationKeys.metadata(envId, applicationId ?? ''),
        queryFn: () => listApplicationMetadata(envId, applicationId!),
        enabled: Boolean(env && applicationId),
        staleTime: 30_000,
    });
}

export function useCreateApplicationMetadata(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (metadata: NewApplicationMetadata) => createApplicationMetadata(envId, applicationId!, metadata),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: applicationNotificationKeys.metadata(envId, applicationId ?? '') });
        },
    });
}

export function useDeleteApplicationMetadata(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (metadataKey: string) => deleteApplicationMetadata(envId, applicationId!, metadataKey),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: applicationNotificationKeys.metadata(envId, applicationId ?? '') });
        },
    });
}

export function useUpdateApplicationMetadata(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const envId = env?.id ?? '';

    return useMutation({
        mutationFn: (metadata: UpdateApplicationMetadata) => updateApplicationMetadata(envId, applicationId!, metadata),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: applicationNotificationKeys.metadata(envId, applicationId ?? '') });
        },
    });
}
