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
import type {
    ApplicationMetadata,
    ApplicationNotificationHook,
    ApplicationNotificationSettings,
    ApplicationNotifier,
    CreateApplicationNotification,
    NewApplicationMetadata,
    UpdateApplicationNotification,
    UpdateApplicationMetadata,
} from '../types/applicationNotification';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

const applicationPath = (applicationId: string) => `/applications/${encodeURIComponent(applicationId)}`;

export async function listApplicationNotifications(
    environmentId: string,
    applicationId: string,
): Promise<ApplicationNotificationSettings[]> {
    return apimFetchJsonV1Env<ApplicationNotificationSettings[]>(environmentId, `${applicationPath(applicationId)}/notificationsettings`);
}

export async function listApplicationNotifiers(environmentId: string, applicationId: string): Promise<ApplicationNotifier[]> {
    return apimFetchJsonV1Env<ApplicationNotifier[]>(environmentId, `${applicationPath(applicationId)}/notifiers`);
}

export async function listApplicationNotificationHooks(environmentId: string): Promise<ApplicationNotificationHook[]> {
    return apimFetchJsonV1Env<ApplicationNotificationHook[]>(environmentId, '/applications/hooks');
}

export async function createApplicationNotification(
    environmentId: string,
    applicationId: string,
    notification: CreateApplicationNotification,
): Promise<ApplicationNotificationSettings> {
    return apimFetchJsonV1Env<ApplicationNotificationSettings>(environmentId, `${applicationPath(applicationId)}/notificationsettings`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(notification),
    });
}

export async function updateApplicationNotification(
    environmentId: string,
    applicationId: string,
    notification: UpdateApplicationNotification,
): Promise<ApplicationNotificationSettings> {
    const suffix = notification.config_type === 'PORTAL' ? '/' : `/${encodeURIComponent(notification.id ?? '')}`;
    return apimFetchJsonV1Env<ApplicationNotificationSettings>(
        environmentId,
        `${applicationPath(applicationId)}/notificationsettings${suffix}`,
        {
            method: 'PUT',
            headers: JSON_HEADERS,
            body: JSON.stringify(notification),
        },
    );
}

export async function listApplicationMetadata(environmentId: string, applicationId: string): Promise<ApplicationMetadata[]> {
    return apimFetchJsonV1Env<ApplicationMetadata[]>(environmentId, `${applicationPath(applicationId)}/metadata/`);
}

export async function createApplicationMetadata(
    environmentId: string,
    applicationId: string,
    metadata: NewApplicationMetadata,
): Promise<ApplicationMetadata> {
    return apimFetchJsonV1Env<ApplicationMetadata>(environmentId, `${applicationPath(applicationId)}/metadata/`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(metadata),
    });
}

export async function updateApplicationMetadata(
    environmentId: string,
    applicationId: string,
    metadata: UpdateApplicationMetadata,
): Promise<ApplicationMetadata> {
    return apimFetchJsonV1Env<ApplicationMetadata>(
        environmentId,
        `${applicationPath(applicationId)}/metadata/${encodeURIComponent(metadata.key)}`,
        {
            method: 'PUT',
            headers: JSON_HEADERS,
            body: JSON.stringify(metadata),
        },
    );
}

export async function deleteApplicationMetadata(environmentId: string, applicationId: string, metadataKey: string): Promise<void> {
    await apimFetchJsonV1Env<void>(environmentId, `${applicationPath(applicationId)}/metadata/${encodeURIComponent(metadataKey)}`, {
        method: 'DELETE',
    });
}
