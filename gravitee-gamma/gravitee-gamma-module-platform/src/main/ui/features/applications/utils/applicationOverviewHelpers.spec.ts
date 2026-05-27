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
import { getSubscriptionCountFromPage, hasReviewedGeneralSettings } from './applicationOverviewHelpers';
import type { ApplicationListItem } from '../types/application';

function baseApplication(overrides: Partial<ApplicationListItem> = {}): ApplicationListItem {
    return {
        id: 'app-1',
        name: 'Demo',
        created_at: 1_000,
        updated_at: 1_000,
        ...overrides,
    } as ApplicationListItem;
}

describe('applicationOverviewHelpers', () => {
    describe('hasReviewedGeneralSettings', () => {
        it('returns false for null application', () => {
            expect(hasReviewedGeneralSettings(null)).toBe(false);
        });

        it('returns true when profile fields are set', () => {
            expect(hasReviewedGeneralSettings(baseApplication({ description: 'About this app' }))).toBe(true);
            expect(hasReviewedGeneralSettings(baseApplication({ domain: 'example.com' }))).toBe(true);
            expect(
                hasReviewedGeneralSettings(
                    baseApplication({ settings: { oauth: { client_id: 'client', redirect_uris: ['https://cb'] } } }),
                ),
            ).toBe(true);
        });

        it('returns true when updated after creation', () => {
            expect(hasReviewedGeneralSettings(baseApplication({ created_at: 1_000, updated_at: 2_000 }))).toBe(true);
        });

        it('returns false for untouched default application', () => {
            expect(hasReviewedGeneralSettings(baseApplication())).toBe(false);
        });
    });

    describe('getSubscriptionCountFromPage', () => {
        it('reads total_elements from paginated response', () => {
            expect(getSubscriptionCountFromPage({ page: { total_elements: 12 } })).toBe(12);
        });

        it('returns zero when response or page is missing', () => {
            expect(getSubscriptionCountFromPage(undefined)).toBe(0);
            expect(getSubscriptionCountFromPage({})).toBe(0);
        });
    });
});
