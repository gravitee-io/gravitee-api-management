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

import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import type { NotificationTemplate } from '../types/notificationTemplate';

const JSON_HEADERS = { 'Content-Type': 'application/json' };
const NOTIFICATION_TEMPLATES_PATH = '/configuration/notification-templates';

export async function listNotificationTemplates(): Promise<NotificationTemplate[]> {
    return apimFetchJsonOrg<NotificationTemplate[]>(NOTIFICATION_TEMPLATES_PATH);
}

export async function searchNotificationTemplates(params: { scope: string; hook: string }): Promise<NotificationTemplate[]> {
    const search = new URLSearchParams();
    search.set('scope', params.scope);
    search.set('hook', params.hook);
    return apimFetchJsonOrg<NotificationTemplate[]>(`${NOTIFICATION_TEMPLATES_PATH}?${search.toString()}`);
}

export async function createNotificationTemplate(template: NotificationTemplate): Promise<NotificationTemplate> {
    const { id: _id, ...payload } = template;
    return apimFetchJsonOrg<NotificationTemplate>(NOTIFICATION_TEMPLATES_PATH, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(payload),
    });
}

export async function updateNotificationTemplate(template: NotificationTemplate): Promise<NotificationTemplate> {
    if (!template.id) {
        throw new Error('Cannot update a notification template without an id');
    }
    return apimFetchJsonOrg<NotificationTemplate>(`${NOTIFICATION_TEMPLATES_PATH}/${encodeURIComponent(template.id)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(template),
    });
}

export async function persistNotificationTemplate(template: NotificationTemplate): Promise<NotificationTemplate> {
    return template.id ? updateNotificationTemplate(template) : createNotificationTemplate(template);
}
