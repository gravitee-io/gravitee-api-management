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
import type { ApiHook, HookCategory } from '../types/notification';
import {
    APIKEY_CLOSE_TO_EXPIRY_HOOK,
    APIKEY_CLOSE_TO_EXPIRY_HOOK_ID,
    DEFAULT_CLOSE_TO_EXPIRY_DAYS,
    SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK,
    SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
    ensureApiKeyCloseToExpiry,
    ensureApiNotificationExtraHooks,
    ensureSubscriptionCloseToExpiry,
    groupVisibleApiProxyHooks,
    parseCloseToExpiryDays,
    persistApiNotificationHooks,
    withCloseToExpiryDaysHook,
    withoutHiddenApiProxyHooks,
} from './subscriptionCloseToExpiry';

const hook = (id: string, category: string): ApiHook => ({
    id,
    label: id,
    description: '',
    scope: 'API',
    category,
});

describe('subscriptionCloseToExpiry', () => {
    it('parses persisted days from the companion hook', () => {
        expect(parseCloseToExpiryDays(['SUBSCRIPTION_NEW', 'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_15'])).toBe(15);
    });

    it('defaults days when no companion hook is stored', () => {
        expect(parseCloseToExpiryDays(['SUBSCRIPTION_NEW'])).toBe(DEFAULT_CLOSE_TO_EXPIRY_DAYS);
    });

    it('appends a companion days hook when close-to-expiry is selected', () => {
        expect(withCloseToExpiryDaysHook([SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, 'SUBSCRIPTION_NEW'], 7)).toEqual([
            SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
            'SUBSCRIPTION_NEW',
            'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_7',
        ]);
    });

    it('drops companion days hooks when close-to-expiry is not selected', () => {
        expect(withCloseToExpiryDaysHook(['SUBSCRIPTION_NEW', 'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_15'], 15)).toEqual(['SUBSCRIPTION_NEW']);
    });

    it('injects the event under Subscription when the catalog omits it', () => {
        const categories: HookCategory[] = [
            {
                name: 'SUBSCRIPTION',
                hooks: [{ id: 'SUBSCRIPTION_NEW', label: 'New Subscription', description: '', scope: 'API', category: 'SUBSCRIPTION' }],
            },
        ];
        const result = ensureSubscriptionCloseToExpiry(categories);
        expect(result[0]?.hooks.map(h => h.id)).toEqual(['SUBSCRIPTION_NEW', SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID]);
    });

    it('adds a Subscription category when none exists', () => {
        const result = ensureSubscriptionCloseToExpiry([]);
        expect(result).toEqual([{ name: 'SUBSCRIPTION', hooks: [SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK] }]);
    });

    it('injects API Key close to expiry under API KEY when the catalog omits it', () => {
        const categories: HookCategory[] = [
            {
                name: 'API KEY',
                hooks: [{ id: 'APIKEY_EXPIRED', label: 'API Key expired', description: '', scope: 'API', category: 'API KEY' }],
            },
        ];
        const result = ensureApiKeyCloseToExpiry(categories);
        expect(result[0]?.hooks.map(h => h.id)).toEqual(['APIKEY_EXPIRED', APIKEY_CLOSE_TO_EXPIRY_HOOK_ID]);
    });

    it('adds an API KEY category when none exists', () => {
        const result = ensureApiKeyCloseToExpiry([]);
        expect(result).toEqual([{ name: 'API KEY', hooks: [APIKEY_CLOSE_TO_EXPIRY_HOOK] }]);
    });

    it('injects both extra close-to-expiry events', () => {
        const result = ensureApiNotificationExtraHooks([]);
        expect(result.map(c => c.name)).toEqual(['SUBSCRIPTION', 'API KEY']);
        expect(result[1]?.hooks).toEqual([APIKEY_CLOSE_TO_EXPIRY_HOOK]);
    });

    it('persists companion days for both close-to-expiry events', () => {
        expect(
            persistApiNotificationHooks([SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, APIKEY_CLOSE_TO_EXPIRY_HOOK_ID], 14, 7),
        ).toEqual([
            SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
            APIKEY_CLOSE_TO_EXPIRY_HOOK_ID,
            'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_14',
            'APIKEY_CLOSE_TO_EXPIRY_DAYS_7',
        ]);
    });

    it('parses API key companion days from a dedicated prefix', () => {
        expect(parseCloseToExpiryDays(['APIKEY_CLOSE_TO_EXPIRY_DAYS_21'], 'APIKEY_CLOSE_TO_EXPIRY_DAYS_')).toBe(21);
    });

    it('omits Support, Rating, and Review categories', () => {
        const categories = groupVisibleApiProxyHooks([
            hook('SUBSCRIPTION_NEW', 'SUBSCRIPTION'),
            hook('NEW_SUPPORT_TICKET', 'SUPPORT'),
            hook('NEW_RATING', 'RATING'),
            hook('ASK_FOR_REVIEW', 'REVIEW'),
            hook('API_STARTED', 'LIFECYCLE'),
        ]);
        expect(categories.map(c => c.name)).toEqual(['SUBSCRIPTION', 'LIFECYCLE']);
    });

    it('strips hidden hook ids from a saved hooks list', () => {
        expect(withoutHiddenApiProxyHooks(['API_STARTED', 'NEW_RATING', 'NEW_SUPPORT_TICKET', 'REVIEW_OK'])).toEqual(['API_STARTED']);
    });
});
