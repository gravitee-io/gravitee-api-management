/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { ApplicationNotificationHook } from '../types/applicationNotification';
import {
    CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
    CERTIFICATE_EXPIRY_HOOK_ID,
    groupVisibleApplicationHooks,
    persistApplicationNotificationHooks,
    SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
    visibleSubscribedEventCount,
} from './applicationNotificationHooks';

describe('applicationNotificationHooks', () => {
    describe('groupVisibleApplicationHooks', () => {
        const hooks: ApplicationNotificationHook[] = [
            {
                id: 'SUBSCRIPTION_NEW',
                label: 'New Subscription',
                description: '',
                scope: 'APPLICATION',
                category: 'SUBSCRIPTION',
            },
            {
                id: 'NEW_SUPPORT_TICKET',
                label: 'New Support Ticket',
                description: '',
                scope: 'APPLICATION',
                category: 'SUPPORT',
            },
        ];

        it('hides Support and injects subscription close-to-expiry plus Certificate events', () => {
            const grouped = groupVisibleApplicationHooks(hooks);

            expect(grouped.map(category => category.name)).toEqual(['SUBSCRIPTION', 'CERTIFICATE']);
            expect(grouped.find(category => category.name === 'SUBSCRIPTION')?.hooks.map(h => h.id)).toEqual([
                'SUBSCRIPTION_NEW',
                SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
            ]);
            expect(grouped.find(category => category.name === 'CERTIFICATE')?.hooks.map(h => h.id)).toEqual([
                CERTIFICATE_EXPIRY_HOOK_ID,
                CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
            ]);
        });

        it('still injects categories when the hooks API is empty', () => {
            const grouped = groupVisibleApplicationHooks([]);
            expect(grouped.map(category => category.name)).toEqual(['SUBSCRIPTION', 'CERTIFICATE']);
        });
    });

    describe('persistApplicationNotificationHooks', () => {
        it('persists companion day hooks for selected close-to-expiry events', () => {
            expect(
                persistApplicationNotificationHooks(
                    [SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, CERTIFICATE_EXPIRY_HOOK_ID, CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID],
                    new Set(),
                    14,
                    7,
                ),
            ).toEqual([
                SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
                CERTIFICATE_EXPIRY_HOOK_ID,
                CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
                'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_14',
                'CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_7',
            ]);
        });

        it('strips Support and group-inherited hooks', () => {
            expect(
                persistApplicationNotificationHooks(
                    ['NEW_SUPPORT_TICKET', 'SUBSCRIPTION_NEW', 'GROUP_HOOK'],
                    new Set(['GROUP_HOOK']),
                    30,
                    30,
                ),
            ).toEqual(['SUBSCRIPTION_NEW']);
        });
    });

    describe('visibleSubscribedEventCount', () => {
        it('ignores companion day hooks and hidden Support hooks', () => {
            expect(
                visibleSubscribedEventCount(
                    ['SUBSCRIPTION_NEW', 'NEW_SUPPORT_TICKET', 'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_14'],
                    ['GROUP_HOOK'],
                ),
            ).toBe(2);
        });
    });
});
