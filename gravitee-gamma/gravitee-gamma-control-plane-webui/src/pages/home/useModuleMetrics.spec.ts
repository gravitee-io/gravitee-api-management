/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { licenseService } from '@gravitee/gamma-modules-sdk';

import { type CountResult, useApiCount } from './useModuleMetrics';
import { TEST_MANAGEMENT_BASE, TEST_MANAGEMENT_V2_ENVIRONMENT_BASE } from '../../testing/factories';
import { respondWith, respondWithError, seedEnvironments } from '../../testing/helpers';
import { server } from '../../testing/server';

const ENTERPRISE_LICENSE = { tier: 'enterprise', packs: [], features: [], isExpired: false };
const NATIVE_API_COUNT = 24;
const NATIVE_AND_FEDERATED_API_COUNT = 26;

function recordApiSearchRequests(): string[][] {
    const requestedApiTypes: string[][] = [];
    server.use(
        http.post(`${TEST_MANAGEMENT_V2_ENVIRONMENT_BASE}/env-1-id/apis/_search`, async ({ request }) => {
            const apiTypes = ((await request.json()) as { apiTypes?: string[] }).apiTypes ?? [];
            requestedApiTypes.push(apiTypes);
            return HttpResponse.json({
                pagination: { totalCount: apiTypes.includes('FEDERATED') ? NATIVE_AND_FEDERATED_API_COUNT : NATIVE_API_COUNT },
            });
        }),
    );
    return requestedApiTypes;
}

describe('useApiCount', () => {
    beforeEach(() => {
        seedEnvironments();
        licenseService.setLicense(null);
    });

    afterEach(() => {
        licenseService.setLicense(null);
        jest.restoreAllMocks();
    });

    it('should request native and federated APIs once and never show a native-only count when federation is on and licensed', async () => {
        respondWith('get', `${TEST_MANAGEMENT_BASE}/console`, { federation: { enabled: true } });
        licenseService.setLicense(ENTERPRISE_LICENSE);
        const requestedApiTypes = recordApiSearchRequests();
        const observedResults: CountResult[] = [];

        const { result } = renderHook(() => {
            const count = useApiCount();
            observedResults.push(count);
            return count;
        });

        await waitFor(() => expect(result.current).toEqual({ value: NATIVE_AND_FEDERATED_API_COUNT, loading: false }));
        expect(requestedApiTypes).toEqual([['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED']]);
        expect(observedResults.filter(observed => observed.value === NATIVE_API_COUNT)).toEqual([]);
    });

    it.each([
        { scenario: 'the federation setting is absent', respondToConsole: () => respondWith('get', `${TEST_MANAGEMENT_BASE}/console`, {}) },
        {
            scenario: 'the federation setting is disabled',
            respondToConsole: () => respondWith('get', `${TEST_MANAGEMENT_BASE}/console`, { federation: { enabled: false } }),
        },
        {
            scenario: 'the console settings read fails',
            respondToConsole: () => respondWithError('get', `${TEST_MANAGEMENT_BASE}/console`, 500),
        },
    ])('should count only native APIs when $scenario', async ({ respondToConsole }) => {
        jest.spyOn(console, 'warn').mockImplementation(() => {});
        respondToConsole();
        licenseService.setLicense(ENTERPRISE_LICENSE);
        const requestedApiTypes = recordApiSearchRequests();

        const { result } = renderHook(() => useApiCount());

        await waitFor(() => expect(result.current).toEqual({ value: NATIVE_API_COUNT, loading: false }));
        expect(requestedApiTypes).toEqual([['V4_HTTP_PROXY', 'V4_TCP_PROXY']]);
    });

    describe('when federation is on and the license is not reported', () => {
        it('should count only native APIs at once', async () => {
            respondWith('get', `${TEST_MANAGEMENT_BASE}/console`, { federation: { enabled: true } });
            const requestedApiTypes = recordApiSearchRequests();

            const { result } = renderHook(() => useApiCount());

            await waitFor(() => expect(result.current).toEqual({ value: NATIVE_API_COUNT, loading: false }));
            expect(requestedApiTypes).toEqual([['V4_HTTP_PROXY', 'V4_TCP_PROXY']]);
        });

        it('should keep the native-only count when the license is reported afterwards', async () => {
            respondWith('get', `${TEST_MANAGEMENT_BASE}/console`, { federation: { enabled: true } });
            const requestedApiTypes = recordApiSearchRequests();
            const { result } = renderHook(() => useApiCount());
            await waitFor(() => expect(result.current).toEqual({ value: NATIVE_API_COUNT, loading: false }));

            act(() => {
                licenseService.setLicense(ENTERPRISE_LICENSE);
            });
            await act(async () => {});

            expect(result.current).toEqual({ value: NATIVE_API_COUNT, loading: false });
            expect(requestedApiTypes).toEqual([['V4_HTTP_PROXY', 'V4_TCP_PROXY']]);
        });
    });
});
