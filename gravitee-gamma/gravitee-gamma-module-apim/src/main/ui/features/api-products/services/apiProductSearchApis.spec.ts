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
import { searchApisAllowedInProducts } from './apiProduct';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';

const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;

describe('searchApisAllowedInProducts', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('asks only for HTTP proxies, so an agent asset is never offered for an API Product', async () => {
        const tracker = trackHandler('post', SEARCH_PATH, { data: [], pagination: {} });

        await searchApisAllowedInProducts('DEFAULT', 'llm', 1, 50);

        expect(tracker.callCount).toBe(1);
        // An LLM, MCP or A2A proxy belongs to the AI Workspace that provisioned it; the API refuses it in any
        // other product, so the picker must not list one. Asking for the type we want keeps a future proxy
        // type out without anyone remembering to exclude it.
        expect(tracker.lastCall?.body).toEqual({
            query: 'llm',
            apiTypes: ['V4_HTTP_PROXY'],
            allowedInApiProducts: true,
        });
    });
});
