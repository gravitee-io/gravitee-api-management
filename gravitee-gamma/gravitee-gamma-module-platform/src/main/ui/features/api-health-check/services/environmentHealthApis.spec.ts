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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import { HEALTH_CHECK_FILTER_QUERY } from '../utils/healthCheckQuery';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

function searchBody(callIndex = 0): Record<string, unknown> {
    const init = mockApimFetchJsonV2.mock.calls[callIndex]?.[2] as { body: string };
    return JSON.parse(init.body) as Record<string, unknown>;
}

describe('environmentHealthApis', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue({ data: [], pagination: { page: 1, perPage: 10, pageCount: 0, totalCount: 0 } });
    });

    it('always searches V4 HTTP Proxy APIs and never requests V2', async () => {
        await searchEnvironmentHealthApis('env-1', { query: 'name:Petstore', page: 2, perPage: 25, sortBy: 'name' });

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith(
            'env-1',
            '/apis/_search?page=2&perPage=25&sortBy=name',
            expect.objectContaining({ method: 'POST' }),
        );
        expect(searchBody().apiTypes).toEqual(['V4_HTTP_PROXY']);
        expect(searchBody()).not.toHaveProperty('definitionVersions');
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
        expect(searchBody().apiTypes).toEqual(['V4_HTTP_PROXY']);
    });

    it('requests v2 availability by endpoint for the selected window', async () => {
        mockApimFetchJsonV2.mockResolvedValue({ global: 0.99, group: {} });
        const signal = new AbortController().signal;

        await getApiAvailability('env-1', 'api/1', 1000, 2000, signal);

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api%2F1/health/availability?from=1000&to=2000&field=endpoint', {
            signal,
        });
    });

    it('passes an undefined signal through instead of omitting the init object', async () => {
        mockApimFetchJsonV2.mockResolvedValue({ global: 0.99, group: {} });

        await getApiAvailability('env-1', 'api-1', 1000, 2000);

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api-1/health/availability?from=1000&to=2000&field=endpoint', {
            signal: undefined,
        });
    });

    it('loads the report from has_health_check:true pages and ignores the table search', async () => {
        mockApimFetchJsonV2
            .mockResolvedValueOnce({
                data: [{ id: 'api-ok', name: 'Petstore', apiVersion: '1.0.0' }],
                pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 1 },
            })
            .mockResolvedValueOnce({ global: 0.99, group: { default: 0.99 } });

        const report = await fetchEnvironmentHealthReport('env-1', 1, 2);

        expect(searchBody(0).query).toBe(HEALTH_CHECK_FILTER_QUERY);
        expect(searchBody(0).apiTypes).toEqual(['V4_HTTP_PROXY']);
        expect(mockApimFetchJsonV2.mock.calls[0]?.[1]).toContain('perPage=100');
        expect(report).toEqual({ operational: 1, inWarning: 0, inError: 0 });
    });

    it('does not count a v2 empty group as an error, matching Classic no-data', async () => {
        mockApimFetchJsonV2
            .mockResolvedValueOnce({
                data: [
                    { id: 'api-empty', name: 'oas-repro-ui', apiVersion: '1.0' },
                    { id: 'api-down', name: 'Alert Test API', apiVersion: '1.0.0' },
                ],
                pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 2 },
            })
            .mockResolvedValueOnce({ global: 0, group: {} })
            .mockResolvedValueOnce({ global: 0.027, group: { default: 0.027 } });

        await expect(fetchEnvironmentHealthReport('env-1', 1, 2)).resolves.toEqual({
            operational: 0,
            inWarning: 0,
            inError: 1,
        });
    });

    it('counts a 0% populated group as an error so fully-down APIs appear on the report cards', async () => {
        mockApimFetchJsonV2
            .mockResolvedValueOnce({
                data: [{ id: 'api-fail', name: 'HC Probe Unhealthy', apiVersion: '1.0.0' }],
                pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 1 },
            })
            .mockResolvedValueOnce({ global: 0, group: { default: 0 } });

        await expect(fetchEnvironmentHealthReport('env-1', 1, 2)).resolves.toEqual({
            operational: 0,
            inWarning: 0,
            inError: 1,
        });
    });

    it('fetches availability for APIs on a page concurrently', async () => {
        let inFlight = 0;
        let maxInFlight = 0;
        const release: Array<() => void> = [];

        mockApimFetchJsonV2.mockImplementation((_env, path) => {
            if (String(path).includes('/apis/_search')) {
                return Promise.resolve({
                    data: [
                        { id: 'api-a', name: 'A', apiVersion: '1' },
                        { id: 'api-b', name: 'B', apiVersion: '1' },
                    ],
                    pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 2 },
                });
            }
            inFlight += 1;
            maxInFlight = Math.max(maxInFlight, inFlight);
            return new Promise(resolve => {
                release.push(() => {
                    inFlight -= 1;
                    resolve({ global: 0.99, group: { default: 0.99 } });
                });
            });
        });

        const pending = fetchEnvironmentHealthReport('env-1', 1, 2);
        await waitFor(() => expect(release).toHaveLength(2));
        expect(maxInFlight).toBe(2);
        release.forEach(done => done());
        await expect(pending).resolves.toEqual({ operational: 2, inWarning: 0, inError: 0 });
    });

    it('fails the report when every availability request fails', async () => {
        mockApimFetchJsonV2.mockImplementation((_env, path) => {
            if (String(path).includes('/apis/_search')) {
                return Promise.resolve({
                    data: [
                        { id: 'api-a', name: 'A', apiVersion: '1' },
                        { id: 'api-b', name: 'B', apiVersion: '1' },
                    ],
                    pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 2 },
                });
            }
            return Promise.reject(new Error('health backend down'));
        });

        await expect(fetchEnvironmentHealthReport('env-1', 1, 2)).rejects.toThrow('Failed to load availability for all health-check APIs');
    });

    it('still returns a partial report when some availability requests fail', async () => {
        mockApimFetchJsonV2.mockImplementation((_env, path) => {
            if (String(path).includes('/apis/_search')) {
                return Promise.resolve({
                    data: [
                        { id: 'api-a', name: 'A', apiVersion: '1' },
                        { id: 'api-b', name: 'B', apiVersion: '1' },
                    ],
                    pagination: { page: 1, perPage: 100, pageCount: 1, totalCount: 2 },
                });
            }
            if (String(path).includes('/apis/api-a/')) {
                return Promise.reject(new Error('timeout'));
            }
            return Promise.resolve({ global: 0.99, group: { default: 0.99 } });
        });

        await expect(fetchEnvironmentHealthReport('env-1', 1, 2)).resolves.toEqual({
            operational: 1,
            inWarning: 0,
            inError: 0,
        });
    });
});
