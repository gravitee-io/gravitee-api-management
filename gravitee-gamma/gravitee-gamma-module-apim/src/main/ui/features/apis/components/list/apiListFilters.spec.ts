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
import { apiListFilterSelectionsEqual, EMPTY_API_LIST_FILTERS, sanitizeApiListFilters } from './apiListFilters';

describe('apiListFilters', () => {
    it('strips api types and statuses outside the allowlist', () => {
        const filters = {
            ...EMPTY_API_LIST_FILTERS,
            apiTypes: ['FEDERATED', 'V4_TCP_PROXY'],
            statuses: ['STARTED', 'CLOSED'],
        };

        expect(sanitizeApiListFilters(filters, { includeFederated: false })).toEqual({
            ...EMPTY_API_LIST_FILTERS,
            apiTypes: ['V4_TCP_PROXY'],
            statuses: ['STARTED'],
        });
    });

    it('treats filter arrays as order-insensitive when comparing', () => {
        const a = { ...EMPTY_API_LIST_FILTERS, apiTypes: ['V4_TCP_PROXY', 'V4_HTTP_PROXY'] };
        const b = { ...EMPTY_API_LIST_FILTERS, apiTypes: ['V4_HTTP_PROXY', 'V4_TCP_PROXY'] };

        expect(apiListFilterSelectionsEqual(a, b)).toBe(true);
    });

    it('does not prune tags while tag keys are still loading', () => {
        const filters = { ...EMPTY_API_LIST_FILTERS, tags: ['eu-west'] };

        expect(sanitizeApiListFilters(filters, { includeFederated: false, tagKeys: undefined }).tags).toEqual(['eu-west']);
    });

    it('prunes tags once an empty catalog is known', () => {
        const filters = { ...EMPTY_API_LIST_FILTERS, tags: ['eu-west'] };

        expect(sanitizeApiListFilters(filters, { includeFederated: false, tagKeys: [] }).tags).toEqual([]);
    });
});
