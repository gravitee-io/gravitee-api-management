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
import { canDeleteNotificationRow, notificationTarget, resolveNotificationChannel } from './notificationChannel';
import type { ApplicationNotificationRow } from '../../applications/types/applicationNotification';

function row(overrides: Partial<ApplicationNotificationRow> = {}): ApplicationNotificationRow {
    return {
        key: 'n-1',
        name: 'Email alerts',
        subscribedEvents: 1,
        notifierName: 'Default Email Notifier',
        notification: {
            id: 'n-1',
            name: 'Email alerts',
            referenceType: 'ENVIRONMENT',
            referenceId: 'env-1',
            notifier: 'default-email',
            config_type: 'GENERIC',
            config: 'ops@example.com',
        },
        notifier: { id: 'default-email', type: 'EMAIL' },
        isReadonly: false,
        ...overrides,
    };
}

describe('resolveNotificationChannel', () => {
    it('maps the PORTAL row to Console', () => {
        expect(
            resolveNotificationChannel(
                row({
                    notification: { ...row().notification, config_type: 'PORTAL' },
                    notifier: undefined,
                }),
            ),
        ).toBe('CONSOLE');
    });

    it('maps EMAIL and WEBHOOK notifier types', () => {
        expect(resolveNotificationChannel(row())).toBe('EMAIL');
        expect(
            resolveNotificationChannel(
                row({
                    notifier: { id: 'default-webhook', type: 'WEBHOOK' },
                    notification: { ...row().notification, notifier: 'default-webhook' },
                }),
            ),
        ).toBe('WEBHOOK');
    });

    it('does not label an unknown or missing notifier as Email', () => {
        expect(
            resolveNotificationChannel(
                row({
                    notifier: undefined,
                    notification: { ...row().notification, notifier: 'slack-default' },
                }),
            ),
        ).toBe('UNKNOWN');
        expect(
            resolveNotificationChannel(
                row({
                    notifier: { id: 'custom-plugin', type: 'SLACK' },
                    notification: { ...row().notification, notifier: 'custom-plugin' },
                }),
            ),
        ).toBe('UNKNOWN');
    });
});

describe('notificationTarget', () => {
    it('returns an em dash for the Console/Portal row', () => {
        expect(notificationTarget(row({ notification: { ...row().notification, config_type: 'PORTAL' } }))).toBe('—');
    });

    it('returns the config string for GENERIC rows, or an em dash when empty', () => {
        expect(notificationTarget(row())).toBe('ops@example.com');
        expect(notificationTarget(row({ notification: { ...row().notification, config: '  ' } }))).toBe('—');
    });
});

describe('canDeleteNotificationRow', () => {
    it('is false for the Console/Portal row and true for GENERIC rows with an id', () => {
        expect(canDeleteNotificationRow(row({ notification: { ...row().notification, config_type: 'PORTAL', id: undefined } }))).toBe(
            false,
        );
        expect(canDeleteNotificationRow(row())).toBe(true);
    });
});
