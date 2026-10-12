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
import { listApiSubscribers } from './subscriptions';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';

const SUBSCRIBERS_PATH = `${TEST_V2_BASE}/apis/:apiId/subscribers`;

describe('listApiSubscribers', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('searches subscribers by name', async () => {
        const tracker = trackHandler('get', SUBSCRIBERS_PATH, { data: [{ id: 'app-1', name: 'Checkout' }] });

        await listApiSubscribers('DEFAULT', 'api-1', { name: 'che', perPage: 20 });

        const url = new URL(tracker.lastCall!.url);
        expect(url.searchParams.get('name')).toBe('che');
        expect(url.searchParams.get('perPage')).toBe('20');
    });

    it('stops the request when the caller aborts it', async () => {
        trackHandler('get', SUBSCRIBERS_PATH, { data: [] });
        const controller = new AbortController();
        controller.abort();

        await expect(listApiSubscribers('DEFAULT', 'api-1', { name: 'che', signal: controller.signal })).rejects.toThrow();
    });
});
