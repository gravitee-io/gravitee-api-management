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
    ApplicationNotificationHook,
    ApplicationNotificationHookCategory,
    ApplicationNotificationRow,
    ApplicationNotificationSettings,
    ApplicationNotifier,
} from '../../types/applicationNotification';

export interface NotificationNotifierOption {
    readonly id: string;
    readonly label: string;
    readonly notifier: ApplicationNotifier;
}

export function notifierTypeLabel(notifier: ApplicationNotifier): string {
    if (notifier.type === 'EMAIL') {
        return 'Default Email Notifier';
    }
    if (notifier.type === 'WEBHOOK') {
        return 'Default Webhook Notifier';
    }
    return notifier.name ?? notifier.id ?? 'Notifier';
}

export const NOTIFICATION_CREATE_ROW_KEY = '__new__';

export function isCreateNotificationRow(row: ApplicationNotificationRow | null): boolean {
    return row?.key === NOTIFICATION_CREATE_ROW_KEY;
}

export function buildNewNotificationRow(applicationId: string, notifiers: ApplicationNotifier[]): ApplicationNotificationRow {
    const defaultNotifier = notifiers.find(item => item.id);
    const notifierId = defaultNotifier?.id ?? '';
    return {
        key: NOTIFICATION_CREATE_ROW_KEY,
        name: '',
        subscribedEvents: 0,
        notifierName: defaultNotifier ? notifierTypeLabel(defaultNotifier) : '—',
        notification: {
            name: '',
            referenceType: 'APPLICATION',
            referenceId: applicationId,
            notifier: notifierId,
            config_type: 'GENERIC',
            hooks: [],
        },
        notifier: defaultNotifier,
        isReadonly: false,
    };
}

export function notificationNotifierOptions(notifiers: ApplicationNotifier[]): NotificationNotifierOption[] {
    return notifiers
        .filter((notifier): notifier is ApplicationNotifier & { id: string } => Boolean(notifier.id))
        .map(notifier => ({ id: notifier.id, label: notifierTypeLabel(notifier), notifier }));
}

export function resolveNotifierName(notification: ApplicationNotificationSettings, notifiers: ApplicationNotifier[]): string {
    const notifier = notifiers.find(item => item.id === notification.notifier);
    if (notifier?.name) {
        return notifier.name;
    }
    if (notification.config_type === 'PORTAL') {
        return 'Console';
    }
    return notification.notifier ?? '—';
}

export function groupHooksByCategory(hooks: ApplicationNotificationHook[]): ApplicationNotificationHookCategory[] {
    const groups = new Map<string, ApplicationNotificationHook[]>();
    for (const hook of hooks) {
        groups.set(hook.category, [...(groups.get(hook.category) ?? []), hook]);
    }
    return [...groups.entries()].map(([name, groupedHooks]) => ({ name, hooks: groupedHooks }));
}

export function mapApplicationNotificationsToRows(
    notifications: ApplicationNotificationSettings[],
    notifiers: ApplicationNotifier[],
): ApplicationNotificationRow[] {
    return notifications.map(notification => ({
        key: notification.id ?? notification.config_type,
        name: notification.name,
        subscribedEvents: (notification.hooks ?? []).length + (notification.groupHooks ?? []).length,
        notifierName: resolveNotifierName(notification, notifiers),
        notification,
        notifier: notifiers.find(item => item.id === notification.notifier),
        isReadonly: Boolean(notification.origin && notification.origin !== 'MANAGEMENT'),
    }));
}
