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
import { CircleHelpIcon, MailIcon, MessageSquareIcon, WebhookIcon } from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';

import type { ApplicationNotificationRow } from '../../applications/types/applicationNotification';

export type EnvironmentNotificationChannel = 'CONSOLE' | 'EMAIL' | 'WEBHOOK' | 'UNKNOWN';

export const CHANNEL_ICON: Record<EnvironmentNotificationChannel, ComponentType<{ className?: string }>> = {
    CONSOLE: MessageSquareIcon,
    EMAIL: MailIcon,
    WEBHOOK: WebhookIcon,
    UNKNOWN: CircleHelpIcon,
};

export const CHANNEL_LABEL: Record<EnvironmentNotificationChannel, string> = {
    CONSOLE: 'Console',
    EMAIL: 'Email',
    WEBHOOK: 'Webhook',
    UNKNOWN: 'Unknown',
};

export function resolveNotificationChannel(row: ApplicationNotificationRow): EnvironmentNotificationChannel {
    if (row.notification.config_type === 'PORTAL') {
        return 'CONSOLE';
    }
    const type = row.notifier?.type?.toUpperCase();
    if (type === 'WEBHOOK') {
        return 'WEBHOOK';
    }
    if (type === 'EMAIL') {
        return 'EMAIL';
    }
    const notifierId = row.notification.notifier ?? '';
    if (notifierId.includes('webhook')) {
        return 'WEBHOOK';
    }
    return 'UNKNOWN';
}

export function notificationTarget(row: ApplicationNotificationRow): string {
    if (row.notification.config_type === 'PORTAL') {
        return '—';
    }
    const config = row.notification.config?.trim();
    return config ? config : '—';
}

export function canDeleteNotificationRow(row: ApplicationNotificationRow): boolean {
    return row.notification.config_type !== 'PORTAL' && Boolean(row.notification.id) && !row.isReadonly;
}
