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
import { preserveExpandedSubscription } from './preserveExpandedSubscription';
import type { Subscription } from '../types/subscription';

function subscription(overrides: Partial<Subscription> = {}): Subscription {
    return {
        id: 'sub-1',
        status: 'ACCEPTED',
        plan: { id: 'plan-1', name: 'API Key', security: { type: 'API_KEY' } },
        application: { id: 'app-1', name: 'App', apiKeyMode: 'SHARED' },
        subscribedBy: { displayName: 'admin' },
        ...overrides,
    };
}

describe('preserveExpandedSubscription', () => {
    it('keeps the API key mode, plan security, and subscriber when the update omits them', () => {
        const merged = preserveExpandedSubscription(
            subscription(),
            subscription({
                endingAt: '2026-12-01T12:00:00Z',
                plan: { id: 'plan-1', name: 'API Key' },
                application: { id: 'app-1', name: 'App' },
                subscribedBy: undefined,
            }),
        );

        expect(merged.endingAt).toBe('2026-12-01T12:00:00Z');
        expect(merged.application.apiKeyMode).toBe('SHARED');
        expect(merged.plan.security?.type).toBe('API_KEY');
        expect(merged.subscribedBy?.displayName).toBe('admin');
    });

    it('keeps an API key mode that the update itself returns', () => {
        const merged = preserveExpandedSubscription(
            subscription(),
            subscription({ application: { id: 'app-1', name: 'App', apiKeyMode: 'EXCLUSIVE' } }),
        );

        expect(merged.application.apiKeyMode).toBe('EXCLUSIVE');
    });

    it('returns the update when nothing was cached', () => {
        const next = subscription();
        expect(preserveExpandedSubscription(undefined, next)).toBe(next);
    });
});
