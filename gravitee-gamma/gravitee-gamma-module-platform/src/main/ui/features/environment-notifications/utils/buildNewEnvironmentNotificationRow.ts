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
import { notifierTypeLabel } from '../../applications/components/notifications/notificationHelpers';
import type { ApplicationNotificationRow, ApplicationNotifier } from '../../applications/types/applicationNotification';

export const NEW_ENVIRONMENT_NOTIFICATION_ROW_KEY = '__new__';

/** Environment counterpart of `buildNewNotificationRow` (applications): same shape, `referenceType: 'ENVIRONMENT'`. */
export function buildNewEnvironmentNotificationRow(environmentId: string, notifiers: ApplicationNotifier[]): ApplicationNotificationRow {
    const defaultNotifier = notifiers.find(item => item.id);
    const notifierId = defaultNotifier?.id ?? '';
    return {
        key: NEW_ENVIRONMENT_NOTIFICATION_ROW_KEY,
        name: '',
        subscribedEvents: 0,
        notifierName: defaultNotifier ? notifierTypeLabel(defaultNotifier) : '—',
        notification: {
            name: '',
            referenceType: 'ENVIRONMENT',
            referenceId: environmentId,
            notifier: notifierId,
            config_type: 'GENERIC',
            hooks: [],
        },
        notifier: defaultNotifier,
        isReadonly: false,
    };
}
