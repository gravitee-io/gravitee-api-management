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
import {
    groupHooksByCategory,
    mapApplicationNotificationsToRows,
    notificationNotifierOptions,
    notifierTypeLabel,
    resolveNotifierName,
} from './notificationHelpers';
import type {
    ApplicationNotificationHook,
    ApplicationNotificationSettings,
    ApplicationNotifier,
} from '../../types/applicationNotification';

describe('notificationHelpers', () => {
    describe('notifierTypeLabel', () => {
        it('labels built-in notifier types', () => {
            expect(notifierTypeLabel({ id: 'e1', type: 'EMAIL' })).toBe('Default Email Notifier');
            expect(notifierTypeLabel({ id: 'w1', type: 'WEBHOOK' })).toBe('Default Webhook Notifier');
        });

        it('falls back to name or id', () => {
            expect(notifierTypeLabel({ id: 'custom', type: 'SLACK', name: 'Slack' })).toBe('Slack');
            expect(notifierTypeLabel({ id: 'custom', type: 'SLACK' })).toBe('custom');
        });
    });

    describe('notificationNotifierOptions', () => {
        it('keeps all notifiers with ids (including custom types)', () => {
            const notifiers: ApplicationNotifier[] = [
                { id: 'e1', type: 'EMAIL' },
                { id: 'w1', type: 'WEBHOOK' },
                { id: 's1', type: 'SLACK', name: 'Slack' },
                { type: 'EMAIL' },
            ];
            expect(notificationNotifierOptions(notifiers).map(option => option.id)).toEqual(['e1', 'w1', 's1']);
        });
    });

    describe('resolveNotifierName', () => {
        it('uses notifier name when available', () => {
            const notification = { notifier: 'n1', config_type: 'DEFAULT' } as ApplicationNotificationSettings;
            const notifiers: ApplicationNotifier[] = [{ id: 'n1', type: 'EMAIL', name: 'Team inbox' }];
            expect(resolveNotifierName(notification, notifiers)).toBe('Team inbox');
        });

        it('returns Console for PORTAL config', () => {
            expect(resolveNotifierName({ config_type: 'PORTAL' } as ApplicationNotificationSettings, [])).toBe('Console');
        });
    });

    describe('groupHooksByCategory', () => {
        it('groups hooks by category name', () => {
            const hooks: ApplicationNotificationHook[] = [
                { id: 'h1', category: 'Subscription', label: 'Accepted', description: '', scope: 'APPLICATION' },
                { id: 'h2', category: 'Subscription', label: 'Closed', description: '', scope: 'APPLICATION' },
                { id: 'h3', category: 'API Key', label: 'Expired', description: '', scope: 'APPLICATION' },
            ];
            const grouped = groupHooksByCategory(hooks);
            expect(grouped).toHaveLength(2);
            expect(grouped.find(category => category.name === 'Subscription')?.hooks).toHaveLength(2);
        });
    });

    describe('mapApplicationNotificationsToRows', () => {
        it('maps notification settings to table rows', () => {
            const notifications: ApplicationNotificationSettings[] = [
                {
                    id: 'n1',
                    name: 'Sub events',
                    config_type: 'DEFAULT',
                    notifier: 'e1',
                    hooks: ['SUBSCRIPTION_NEW'],
                    origin: 'MANAGEMENT',
                },
            ];
            const notifiers: ApplicationNotifier[] = [{ id: 'e1', type: 'EMAIL', name: 'Email' }];

            const rows = mapApplicationNotificationsToRows(notifications, notifiers);

            expect(rows).toEqual([
                expect.objectContaining({
                    key: 'n1',
                    name: 'Sub events',
                    subscribedEvents: 1,
                    notifierName: 'Email',
                    isReadonly: false,
                }),
            ]);
        });

        it('marks non-management origin as readonly', () => {
            const rows = mapApplicationNotificationsToRows(
                [{ id: 'n1', name: 'Portal', config_type: 'PORTAL', origin: 'PORTAL' } as ApplicationNotificationSettings],
                [],
            );
            expect(rows[0].isReadonly).toBe(true);
            expect(rows[0].notifierName).toBe('Console');
        });
    });
});
