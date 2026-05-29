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

const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const EMPTY_RESPONSE = { data: [], pagination: { page: 1, perPage: 10, pageCount: 0, totalCount: 0 } };

describe('searchApis', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('always enforces the V4 HTTP proxy filter server-side', async () => {
        const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

        await searchApis('DEFAULT', { query: 'my-api' }, 1, 10);

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.body).toEqual({ query: 'my-api', apiTypes: ['V4_HTTP_PROXY'] });
    });

    it('overrides any caller-supplied apiTypes so the filter cannot be widened', async () => {
        const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

        await searchApis('DEFAULT', { apiTypes: ['V4_KAFKA', 'V2'] }, 1, 10);

        expect(tracker.lastCall?.body).toEqual({ apiTypes: ['V4_HTTP_PROXY'] });
    });

    it('passes pagination and sort as query params', async () => {
        const tracker = trackHandler('post', SEARCH_PATH, EMPTY_RESPONSE);

        await searchApis('DEFAULT', {}, 2, 25, 'name');

        const url = new URL(tracker.lastCall!.url);
        expect(url.searchParams.get('page')).toBe('2');
        expect(url.searchParams.get('perPage')).toBe('25');
        expect(url.searchParams.get('sortBy')).toBe('name');
    });
});
