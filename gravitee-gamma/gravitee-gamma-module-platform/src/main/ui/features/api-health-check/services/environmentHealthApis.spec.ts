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
import { waitFor } from '@testing-library/react';

import { fetchEnvironmentHealthReport, getApiAvailability, searchEnvironmentHealthApis } from './environmentHealthApis';
import { apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import { HEALTH_CHECK_FILTER_QUERY } from '../utils/healthCheckQuery';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);
const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

/** A v1 availability payload: every timeframe in one response. */
function v1Availability(pct: number) {
    return { global: { '1m': pct, '1h': pct, '1d': pct, '1w': pct, '1M': pct } };
}

function searchBody(callIndex = 0): Record<string, unknown> {
    const init = mockApimFetchJsonV2.mock.calls[callIndex]?.[2] as { body: string };
    return JSON.parse(init.body) as Record<string, unknown>;
}

describe('environmentHealthApis', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue({ data: [], pagination: { page: 1, perPage: 10, pageCount: 0, totalCount: 0 } });
        mockApimFetchJsonV1Env.mockResolvedValue(v1Availability(99));
    });

    it('scopes the search by definition version like Classic, and never requests V2', async () => {
        await searchEnvironmentHealthApis('env-1', { query: 'name:Petstore', page: 2, perPage: 25, sortBy: 'name' });

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith(
            'env-1',
            '/apis/_search?page=2&perPage=25&sortBy=name',
            expect.objectContaining({ method: 'POST' }),
        );
        // Classic sends definitionVersions because it still serves V2; Gamma sends the same field with V4 only.
        // Scoping by apiTypes instead would silently drop V4 APIs that are not HTTP proxies.
        expect(searchBody().definitionVersions).toEqual(['V4']);
        expect(searchBody()).not.toHaveProperty('apiTypes');
        expect(JSON.stringify(searchBody())).not.toContain('V2');
        expect(searchBody().query).toBe('name:Petstore');
    });

    it('omits sortBy when none is provided, matching Classic default search', async () => {
        await searchEnvironmentHealthApis('env-1', { page: 1, perPage: 10 });

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith(
            'env-1',
            '/apis/_search?page=1&perPage=10',
            expect.objectContaining({ method: 'POST' }),
        );
    });

    it('forwards a free-text Lucene query unchanged', async () => {
        await searchEnvironmentHealthApis('env-1', { query: 'ownerName:admin AND name:"My api *"', page: 1, perPage: 10 });

        expect(searchBody().query).toBe('ownerName:admin AND name:"My api *"');
        expect(searchBody().definitionVersions).toEqual(['V4']);
    });

    it('requests availability from the v1 endpoint Classic uses, with no time window', async () => {
        const signal = new AbortController().signal;

        await getApiAvailability('env-1', 'api/1', signal);

        // Classic calls /apis/{id}/health?type=availability. One response carries every timeframe, so the
        // timeframe select re-reads it instead of re-fetching every row.
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/apis/api%2F1/health?type=availability', { signal });
    });

    it('passes an undefined signal through instead of omitting the init object', async () => {
        await getApiAvailability('env-1', 'api-1');

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/apis/api-1/health?type=availability', { signal: undefined });
    });

    it('loads the report from has_health_check:true pages and ignores the table search', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({
            data: [{ id: 'api-ok', name: 'Petstore', apiVersion: '1.0.0' }],
            pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 1 },
        });
        mockApimFetchJsonV1Env.mockResolvedValue(v1Availability(99));

        const report = await fetchEnvironmentHealthReport('env-1', '1m');

        expect(searchBody(0).query).toBe(HEALTH_CHECK_FILTER_QUERY);
        expect(searchBody(0).definitionVersions).toEqual(['V4']);
        expect(mockApimFetchJsonV2.mock.calls[0]?.[1]).toContain('perPage=100');
        expect(report).toEqual({ inWarning: 0, inError: 0 });
    });

    it('does not count an API that never reported as an error, matching Classic no-data', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({
            data: [
                { id: 'api-empty', name: 'oas-repro-ui', apiVersion: '1.0' },
                { id: 'api-down', name: 'Alert Test API', apiVersion: '1.0.0' },
            ],
            pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 2 },
        });
        mockApimFetchJsonV1Env.mockResolvedValueOnce({ global: null }).mockResolvedValueOnce(v1Availability(2.7));

        await expect(fetchEnvironmentHealthReport('env-1', '1m')).resolves.toEqual({
            inWarning: 0,
            inError: 1,
        });
    });

    it('counts a fully-down API at 0% as an error, so it is not silently dropped', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({
            data: [{ id: 'api-fail', name: 'HC Probe Unhealthy', apiVersion: '1.0.0' }],
            pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 1 },
        });
        mockApimFetchJsonV1Env.mockResolvedValue(v1Availability(0));

        await expect(fetchEnvironmentHealthReport('env-1', '1m')).resolves.toEqual({
            inWarning: 0,
            inError: 1,
        });
    });

    it('fetches availability for APIs on a page concurrently', async () => {
        let inFlight = 0;
        let maxInFlight = 0;
        const release: Array<() => void> = [];

        mockApimFetchJsonV2.mockResolvedValue({
            data: [
                { id: 'api-a', name: 'A', apiVersion: '1' },
                { id: 'api-b', name: 'B', apiVersion: '1' },
            ],
            pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 2 },
        });
        mockApimFetchJsonV1Env.mockImplementation(() => {
            inFlight += 1;
            maxInFlight = Math.max(maxInFlight, inFlight);
            return new Promise(resolve => {
                release.push(() => {
                    inFlight -= 1;
                    resolve(v1Availability(99));
                });
            });
        });

        const pending = fetchEnvironmentHealthReport('env-1', '1m');
        await waitFor(() => expect(release).toHaveLength(2));
        expect(maxInFlight).toBe(2);
        release.forEach(done => done());
        await expect(pending).resolves.toEqual({ inWarning: 0, inError: 0 });
    });

    it('fails the report when every availability request fails', async () => {
        mockApimFetchJsonV2.mockResolvedValue({
            data: [
                { id: 'api-a', name: 'A', apiVersion: '1' },
                { id: 'api-b', name: 'B', apiVersion: '1' },
            ],
            pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 2 },
        });
        mockApimFetchJsonV1Env.mockRejectedValue(new Error('health backend down'));

        await expect(fetchEnvironmentHealthReport('env-1', '1m')).rejects.toThrow('Failed to load availability for all health-check APIs');
    });

    it('still returns a partial report when some availability requests fail', async () => {
        mockApimFetchJsonV2.mockResolvedValue({
            data: [
                { id: 'api-a', name: 'A', apiVersion: '1' },
                { id: 'api-b', name: 'B', apiVersion: '1' },
            ],
            pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 2 },
        });
        mockApimFetchJsonV1Env.mockImplementation((_env, path) =>
            String(path).includes('api-a') ? Promise.reject(new Error('timeout')) : Promise.resolve(v1Availability(20)),
        );

        // api-a is skipped, api-b is counted: 20% availability is an error.
        await expect(fetchEnvironmentHealthReport('env-1', '1m')).resolves.toEqual({
            inWarning: 0,
            inError: 1,
        });
    });
});
