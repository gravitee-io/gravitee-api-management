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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useEnvironmentHealthAvailability } from './useEnvironmentHealthAvailability';
import { getApiAvailability, getApiAvailabilityAverage } from '../services/environmentHealthApis';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../services/environmentHealthApis', () => ({ getApiAvailability: jest.fn(), getApiAvailabilityAverage: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetApiAvailability = jest.mocked(getApiAvailability);
const mockGetApiAvailabilityAverage = jest.mocked(getApiAvailabilityAverage);

const RANGE = { from: 1000, to: 2000, interval: 33 };

function createTestContext() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe('useEnvironmentHealthAvailability', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockGetApiAvailability.mockResolvedValue({ global: { '1m': 88, '1h': 70, '1d': 99 } });
        mockGetApiAvailabilityAverage.mockResolvedValue({ values: [{ buckets: [{ name: 'default', data: [1, 1] }] }] });
    });

    afterEach(() => jest.clearAllMocks());

    it('skips the availability GET when health check is not configured', () => {
        const { Wrapper } = createTestContext();
        renderHook(() => useEnvironmentHealthAvailability({ apiId: 'api-1', timeframe: '1m', range: RANGE, enabled: false }), {
            wrapper: Wrapper,
        });

        expect(mockGetApiAvailability).not.toHaveBeenCalled();
        expect(mockGetApiAvailabilityAverage).not.toHaveBeenCalled();
    });

    it('loads configured availability when health check is enabled', async () => {
        const { Wrapper } = createTestContext();
        const { result } = renderHook(
            () => useEnvironmentHealthAvailability({ apiId: 'api-1', timeframe: '1m', range: RANGE, enabled: true }),
            {
                wrapper: Wrapper,
            },
        );

        await waitFor(() => expect(mockGetApiAvailability).toHaveBeenCalledWith('env-1', 'api-1', expect.anything()));
        // Classic's second per-row call.
        await waitFor(() => expect(mockGetApiAvailabilityAverage).toHaveBeenCalledWith('env-1', 'api-1', RANGE, expect.anything()));
        await waitFor(() => expect(result.current.availability).toEqual({ type: 'configured', availabilityPct: 88 }));
    });

    it('reuses the cached health payload on a timeframe change and re-requests only the average', async () => {
        const { Wrapper } = createTestContext();
        const { result, rerender } = renderHook(
            ({ timeframe }: { timeframe: '1m' | '1h' }) =>
                useEnvironmentHealthAvailability({ apiId: 'api-1', timeframe, range: RANGE, enabled: true }),
            { wrapper: Wrapper, initialProps: { timeframe: '1m' as const } },
        );

        await waitFor(() => expect(result.current.availability).toEqual({ type: 'configured', availabilityPct: 88 }));
        expect(mockGetApiAvailability).toHaveBeenCalledTimes(1);
        expect(mockGetApiAvailabilityAverage).toHaveBeenCalledTimes(1);

        rerender({ timeframe: '1h' });

        // Classic keeps the health payload (it carries every timeframe) and re-requests only the window average.
        await waitFor(() => expect(result.current.availability).toEqual({ type: 'configured', availabilityPct: 70 }));
        expect(mockGetApiAvailability).toHaveBeenCalledTimes(1);
        expect(mockGetApiAvailabilityAverage).toHaveBeenCalledTimes(2);
    });

    it('shows no-data when the window carries no buckets, even with a lifetime percentage', async () => {
        mockGetApiAvailabilityAverage.mockResolvedValue({ values: [] });
        const { Wrapper } = createTestContext();
        const { result } = renderHook(
            () => useEnvironmentHealthAvailability({ apiId: 'api-1', timeframe: '1m', range: RANGE, enabled: true }),
            { wrapper: Wrapper },
        );

        await waitFor(() => expect(result.current.availability).toEqual({ type: 'no-data' }));
    });
});
