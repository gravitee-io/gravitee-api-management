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
import type {
    ApiHook,
    ApiNotifier,
    CreateNotificationPayload,
    NotificationSettings,
    UpdateNotificationPayload,
} from '../../features/apis/types/notification';
import { apimFetchJsonV1Env } from '../../shared/api/apimClient';

const apiPath = (apiId: string) => `/apis/${encodeURIComponent(apiId)}`;

export async function listNotifications(envId: string, apiId: string): Promise<NotificationSettings[]> {
    return apimFetchJsonV1Env<NotificationSettings[]>(envId, `${apiPath(apiId)}/notificationsettings`);
}

export async function listNotifiers(envId: string, apiId: string): Promise<ApiNotifier[]> {
    return apimFetchJsonV1Env<ApiNotifier[]>(envId, `${apiPath(apiId)}/notifiers`);
}

/** Global hook catalog — same for every API in the env. */
export async function listHooks(envId: string): Promise<ApiHook[]> {
    return apimFetchJsonV1Env<ApiHook[]>(envId, '/apis/hooks');
}

export async function createNotification(envId: string, apiId: string, payload: CreateNotificationPayload): Promise<NotificationSettings> {
    return apimFetchJsonV1Env<NotificationSettings>(envId, `${apiPath(apiId)}/notificationsettings`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

/**
 * Update a notification. PORTAL notification has no id and uses the bare
 * collection URL (classic console behaviour).
 */
export async function updateNotification(
    envId: string,
    apiId: string,
    notification: UpdateNotificationPayload,
): Promise<NotificationSettings> {
    const isPortal = notification.config_type === 'PORTAL';
    const suffix = isPortal ? '/' : `/${encodeURIComponent(notification.id!)}`;
    return apimFetchJsonV1Env<NotificationSettings>(envId, `${apiPath(apiId)}/notificationsettings${suffix}`, {
        method: 'PUT',
        body: JSON.stringify(notification),
    });
}

export async function deleteNotification(envId: string, apiId: string, notificationId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(envId, `${apiPath(apiId)}/notificationsettings/${encodeURIComponent(notificationId)}`, {
        method: 'DELETE',
    });
}
