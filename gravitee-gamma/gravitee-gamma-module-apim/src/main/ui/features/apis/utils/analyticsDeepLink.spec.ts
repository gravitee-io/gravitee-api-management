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
import { encodeObservabilityState } from '@gravitee/gamma-lib-observability';

import { buildApiAnalyticsPath } from './analyticsDeepLink';

const DEFAULT_TIME_RANGE = { type: 'relative', period: '5m' };
jest.mock('@gravitee/gamma-lib-observability', () => ({
    DEFAULT_TIME_RANGE: { type: 'relative', period: '5m' },
    encodeObservabilityState: jest.fn(() => ({ q: 'ENCODED_Q', v: '1' })),
}));

const mockEncode = encodeObservabilityState as jest.Mock;

describe('buildApiAnalyticsPath', () => {
    afterEach(() => jest.clearAllMocks());

    it('filters the dashboard to the single API over the default time range', () => {
        buildApiAnalyticsPath('api-123');
        expect(mockEncode).toHaveBeenCalledWith({
            conditions: [{ field: 'API', label: 'API', operator: 'in', value: ['api-123'] }],
            timeRange: DEFAULT_TIME_RANGE,
        });
    });

    it('targets the HTTP proxy dashboard one level above the API list, carrying q/v params', () => {
        const path = buildApiAnalyticsPath('api-123');
        const url = new URL(path, 'http://x/');
        expect(path.startsWith('../observe/dashboards/http-proxy-overview?')).toBe(true);
        expect(url.searchParams.get('q')).toBe('ENCODED_Q');
        expect(url.searchParams.get('v')).toBe('1');
    });

    it('falls back to the bare dashboard path when encoding yields nothing', () => {
        mockEncode.mockReturnValueOnce(null);
        expect(buildApiAnalyticsPath('api-123')).toBe('../observe/dashboards/http-proxy-overview');
    });
});
