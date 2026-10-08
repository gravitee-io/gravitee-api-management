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
import { exportSubscriptionsCsv } from './subscriptions';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackBlobGet } from '../../../testing/helpers';

const EXPORT_PATH = `${TEST_V2_BASE}/apis/:apiId/subscriptions/_export`;

describe('exportSubscriptionsCsv', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('exports the subscriptions matching the list filters', async () => {
        const tracker = trackBlobGet(EXPORT_PATH, {});

        await exportSubscriptionsCsv('DEFAULT', 'api-1', {
            statuses: ['ACCEPTED', 'PAUSED'],
            planIds: ['plan-1'],
            applicationIds: ['app-1', 'app-2'],
            apiKey: 'abc',
            page: 2,
            perPage: 25,
        });

        const url = new URL(tracker.lastCall!.url);
        expect(url.pathname).toContain('/apis/api-1/subscriptions/_export');
        expect(url.searchParams.get('statuses')).toBe('ACCEPTED,PAUSED');
        expect(url.searchParams.get('planIds')).toBe('plan-1');
        expect(url.searchParams.get('applicationIds')).toBe('app-1,app-2');
        expect(url.searchParams.get('apiKey')).toBe('abc');
        expect(url.searchParams.get('page')).toBe('2');
        expect(url.searchParams.get('perPage')).toBe('25');
    });
});
