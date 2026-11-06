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
import { http, HttpResponse } from 'msw';

import { getPortalsStats, listPortals } from './portals';
import { createPortal, createPortalsResponse, createPortalsStats, TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { server } from '../../../testing/server';

describe('portals service', () => {
    it('requests the selected environment with encoded search and pagination', async () => {
        const response = createPortalsResponse({
            data: [createPortal({ name: 'Partners & developers' })],
            pagination: { page: 2, perPage: 25, pageCount: 2, pageItemsCount: 1, totalCount: 26 },
        });
        let receivedRequest: Request | undefined;
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                receivedRequest = request;
                return HttpResponse.json(response);
            }),
        );

        await expect(listPortals(TEST_CONFIG.environmentId, { query: 'Partners & developers', page: 2, perPage: 25 })).resolves.toEqual(
            response,
        );

        expect(Object.fromEntries(new URL(receivedRequest!.url).searchParams)).toEqual({
            q: 'Partners & developers',
            page: '2',
            perPage: '25',
        });
        expect(receivedRequest!.credentials).toBe('include');
        expect(receivedRequest!.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    });

    it.each([undefined, '', '   '])('omits an empty search (%s) and uses contract pagination defaults', async query => {
        let searchParams: URLSearchParams | undefined;
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                searchParams = new URL(request.url).searchParams;
                return HttpResponse.json(createPortalsResponse());
            }),
        );

        await listPortals(TEST_CONFIG.environmentId, { query });

        expect(Object.fromEntries(searchParams!)).toEqual({ page: '1', perPage: '10' });
    });

    it('preserves an empty list and its pagination metadata', async () => {
        const response = createPortalsResponse({
            data: [],
            pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
        });
        server.use(http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json(response)));

        await expect(listPortals(TEST_CONFIG.environmentId)).resolves.toEqual(response);
    });

    it('fetches environment-wide statistics without list parameters', async () => {
        const response = createPortalsStats({ quota: { used: 2, limit: 5 }, publishedApisCount: 7 });
        let search: string | undefined;
        server.use(
            http.get(`${TEST_V2_BASE}/portals/_stats`, ({ request }) => {
                search = new URL(request.url).search;
                return HttpResponse.json(response);
            }),
        );

        await expect(getPortalsStats(TEST_CONFIG.environmentId)).resolves.toEqual(response);
        expect(search).toBe('');
    });

    it.each([
        ['list', 403],
        ['list', 500],
        ['stats', 403],
        ['stats', 500],
    ] as const)('preserves the %s endpoint error status %s', async (endpoint, status) => {
        const path = endpoint === 'list' ? '/portals' : '/portals/_stats';
        server.use(http.get(`${TEST_V2_BASE}${path}`, () => HttpResponse.json({ message: 'Portal request failed' }, { status })));
        const request = endpoint === 'list' ? listPortals(TEST_CONFIG.environmentId) : getPortalsStats(TEST_CONFIG.environmentId);

        await expect(request).rejects.toMatchObject({ name: 'ApimApiError', status, message: 'Portal request failed' });
    });
});
