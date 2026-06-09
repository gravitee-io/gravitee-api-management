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
import { getAvailability, getAverageResponseTime, getHealthCheckLogs, getResponseTimeOvertime } from './healthCheck';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';

const HEALTH = `${TEST_V2_BASE}/apis/api-1/health`;

describe('healthCheck service', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('requests the response-time over-time series with from/to', async () => {
        const tracker = trackHandler('get', `${HEALTH}/average-response-time-overtime`, {
            timeRange: { from: 1, to: 2, interval: 1 },
            data: [],
        });

        await getResponseTimeOvertime('DEFAULT', 'api-1', 1000, 2000);

        const url = new URL(tracker.lastCall!.url);
        expect(url.searchParams.get('from')).toBe('1000');
        expect(url.searchParams.get('to')).toBe('2000');
    });

    it('requests availability scoped by field', async () => {
        const tracker = trackHandler('get', `${HEALTH}/availability`, { global: 1, group: {} });

        await getAvailability('DEFAULT', 'api-1', 1000, 2000, 'gateway');

        const url = new URL(tracker.lastCall!.url);
        expect(url.searchParams.get('field')).toBe('gateway');
        expect(url.searchParams.get('from')).toBe('1000');
        expect(url.searchParams.get('to')).toBe('2000');
    });

    it('requests average response time scoped by field', async () => {
        const tracker = trackHandler('get', `${HEALTH}/average-response-time`, { global: 1, group: {} });

        await getAverageResponseTime('DEFAULT', 'api-1', 1000, 2000, 'endpoint');

        expect(new URL(tracker.lastCall!.url).searchParams.get('field')).toBe('endpoint');
    });

    it('requests logs with pagination and success filter', async () => {
        const tracker = trackHandler('get', `${HEALTH}/logs`, { data: [], pagination: {} });

        await getHealthCheckLogs('DEFAULT', 'api-1', { from: 1000, to: 2000, page: 2, perPage: 25, success: false });

        const url = new URL(tracker.lastCall!.url);
        expect(url.searchParams.get('page')).toBe('2');
        expect(url.searchParams.get('perPage')).toBe('25');
        expect(url.searchParams.get('success')).toBe('false');
    });
});
