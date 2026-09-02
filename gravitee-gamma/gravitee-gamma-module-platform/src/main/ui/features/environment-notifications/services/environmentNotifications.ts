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
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import { notificationSettingsUpdatePath } from '../../../shared/utils/notificationSettingsUpdatePath';
import type {
    ApplicationNotificationHook,
    ApplicationNotificationSettings,
    ApplicationNotifier,
    UpdateApplicationNotification,
} from '../../applications/types/applicationNotification';
import type { CreateEnvironmentNotification } from '../types/environmentNotification';

const JSON_HEADERS = { 'Content-Type': 'application/json' };
const NOTIFICATION_SETTINGS_BASE_PATH = '/configuration';
const NOTIFICATION_SETTINGS_PATH = `${NOTIFICATION_SETTINGS_BASE_PATH}/notificationsettings`;

export async function listEnvironmentNotifications(environmentId: string): Promise<ApplicationNotificationSettings[]> {
    return apimFetchJsonV1Env<ApplicationNotificationSettings[]>(environmentId, NOTIFICATION_SETTINGS_PATH);
}

export async function listEnvironmentNotifiers(environmentId: string): Promise<ApplicationNotifier[]> {
    return apimFetchJsonV1Env<ApplicationNotifier[]>(environmentId, '/configuration/notifiers');
}

export async function listEnvironmentNotificationHooks(environmentId: string): Promise<ApplicationNotificationHook[]> {
    return apimFetchJsonV1Env<ApplicationNotificationHook[]>(environmentId, '/configuration/hooks');
}

export async function createEnvironmentNotification(
    environmentId: string,
    notification: CreateEnvironmentNotification,
): Promise<ApplicationNotificationSettings> {
    return apimFetchJsonV1Env<ApplicationNotificationSettings>(environmentId, NOTIFICATION_SETTINGS_PATH, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(notification),
    });
}

export async function updateEnvironmentNotification(
    environmentId: string,
    notification: UpdateApplicationNotification,
): Promise<ApplicationNotificationSettings> {
    return apimFetchJsonV1Env<ApplicationNotificationSettings>(
        environmentId,
        notificationSettingsUpdatePath(NOTIFICATION_SETTINGS_BASE_PATH, notification),
        {
            method: 'PUT',
            headers: JSON_HEADERS,
            body: JSON.stringify(notification),
        },
    );
}

export async function deleteEnvironmentNotification(environmentId: string, notificationId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(environmentId, `${NOTIFICATION_SETTINGS_PATH}/${encodeURIComponent(notificationId)}`, {
        method: 'DELETE',
    });
}
