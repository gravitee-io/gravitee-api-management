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
import { searchApis } from './apiList';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';
import type { ApiSearchQuery } from '../types';

const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const EMPTY_RESPONSE = { data: [], pagination: { page: 1, perPage: 10, pageCount: 0, totalCount: 0 } };

const PROXY_TYPES = ['V4_HTTP_PROXY', 'V4_TCP_PROXY'];
const PROXY_AND_FEDERATED = ['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED'];

describe('searchApis', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it.each<[string, ApiSearchQuery, boolean, object]>([
        ['a plain query, gate off', { query: 'my-api' }, false, { query: 'my-api', apiTypes: PROXY_TYPES }],
        ['a plain query, gate on', { query: 'my-api' }, true, { query: 'my-api', apiTypes: PROXY_AND_FEDERATED }],
        ['only types this page does not list, gate off', { apiTypes: ['V4_KAFKA', 'V2'] }, false, { apiTypes: [] }],
        ['only types this page does not list, gate on', { apiTypes: ['V4_KAFKA', 'V2'] }, true, { apiTypes: [] }],
        ['an agent type alone, gate off', { apiTypes: ['FEDERATED_AGENT'] }, false, { apiTypes: [] }],
        ['an agent type alone, gate on', { apiTypes: ['FEDERATED_AGENT'] }, true, { apiTypes: [] }],
        ['federated alone while the gate is off', { apiTypes: ['FEDERATED'] }, false, { apiTypes: [] }],
    ])(
        'returns no matching types when the selection is outside the module set — %s',
        async (_scenario, query, includeFederated, expectedBody) => {
            const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

            await searchApis('DEFAULT', query, 1, 10, undefined, includeFederated);

            expect(tracker.callCount).toBe(1);
            expect(tracker.lastCall?.body).toEqual(expectedBody);
        },
    );

    it.each<[string, ApiSearchQuery, boolean, object]>([
        ['tcp only', { apiTypes: ['V4_TCP_PROXY'] }, false, { apiTypes: ['V4_TCP_PROXY'] }],
        [
            'tcp together with a search term',
            { query: 'orders', apiTypes: ['V4_TCP_PROXY'] },
            false,
            { query: 'orders', apiTypes: ['V4_TCP_PROXY'] },
        ],
        [
            'an allowed type kept and a disallowed type dropped',
            { apiTypes: ['V4_HTTP_PROXY', 'V4_KAFKA'] },
            false,
            { apiTypes: ['V4_HTTP_PROXY'] },
        ],
        ['federated alone while the gate is on', { apiTypes: ['FEDERATED'] }, true, { apiTypes: ['FEDERATED'] }],
        [
            'status, tags, and categories with the type and the search term',
            {
                query: 'orders',
                apiTypes: ['V4_TCP_PROXY'],
                statuses: ['STARTED'],
                tags: ['eu-west'],
                categories: ['partners'],
            },
            false,
            {
                query: 'orders',
                apiTypes: ['V4_TCP_PROXY'],
                statuses: ['STARTED'],
                tags: ['eu-west'],
                categories: ['partners'],
            },
        ],
    ])('narrows one search to the selected filters — %s', async (_scenario, query, includeFederated, expectedBody) => {
        const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

        await searchApis('DEFAULT', query, 1, 10, undefined, includeFederated);

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.body).toEqual(expectedBody);
    });

    it('omits empty filter lists so an untouched filter is not sent as a match-nothing constraint', async () => {
        const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

        await searchApis('DEFAULT', { query: 'orders', statuses: [], tags: [], categories: [], apiTypes: [] }, 1, 10);

        expect(tracker.lastCall?.body).toEqual({ query: 'orders', apiTypes: PROXY_TYPES });
    });

    it('sends the proxy-only filter when the caller omits the federation gate argument', async () => {
        const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

        await searchApis('DEFAULT', { query: 'my-api' }, 1, 10);

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.body).toEqual({ query: 'my-api', apiTypes: PROXY_TYPES });
    });

    it('passes pagination and sort as query params', async () => {
        const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

        await searchApis('DEFAULT', {}, 2, 25, 'name');

        const url = new URL(tracker.lastCall!.url);
        expect(url.searchParams.get('page')).toBe('2');
        expect(url.searchParams.get('perPage')).toBe('25');
        expect(url.searchParams.get('sortBy')).toBe('name');
    });

    it('omits sortBy and still expands deploymentState when no sort is given', async () => {
        const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

        await searchApis('DEFAULT', {}, 1, 10);

        const url = new URL(tracker.lastCall!.url);
        expect(url.searchParams.has('sortBy')).toBe(false);
        expect(url.searchParams.get('expands')).toBe('deploymentState');
    });

    it.each(['status', '-status', 'paths', '-paths', 'tags_asc', '-tags_desc'])(
        'sends sortBy=%s verbatim while the federation gate widens the type filter',
        async sortBy => {
            const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

            await searchApis('DEFAULT', {}, 1, 10, sortBy, true);

            const url = new URL(tracker.lastCall!.url);
            expect(url.searchParams.get('sortBy')).toBe(sortBy);
            expect(tracker.lastCall?.body).toEqual({ apiTypes: PROXY_AND_FEDERATED });
        },
    );
});
