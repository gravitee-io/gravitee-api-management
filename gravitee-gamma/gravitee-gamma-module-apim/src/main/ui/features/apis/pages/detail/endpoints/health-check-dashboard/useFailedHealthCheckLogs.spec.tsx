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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useFailedHealthCheckLogs } from './useFailedHealthCheckLogs';
import { getHealthCheckLogs } from '../../../../services/healthCheck';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../../../../services/healthCheck', () => ({ getHealthCheckLogs: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetLogs = jest.mocked(getHealthCheckLogs);

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useFailedHealthCheckLogs', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' });
        mockGetLogs.mockResolvedValue({ data: [], pagination: { totalCount: 0 } });
    });

    afterEach(() => jest.clearAllMocks());

    it('always queries failures (success=false) with default pagination', async () => {
        renderHook(() => useFailedHealthCheckLogs('api-1', '1d'), { wrapper: createWrapper() });

        await waitFor(() => expect(mockGetLogs).toHaveBeenCalledTimes(1));
        const [envId, apiId, params] = mockGetLogs.mock.calls[0];
        expect(envId).toBe('DEFAULT');
        expect(apiId).toBe('api-1');
        expect(params.success).toBe(false);
        expect(params.page).toBe(1);
        expect(params.perPage).toBe(10);
        expect(params.from).toBeLessThan(params.to);
    });

    it('exposes returned logs and total count', async () => {
        mockGetLogs.mockResolvedValue({
            data: [{ id: 'l1', timestamp: 't', endpointName: 'ep', gatewayId: 'gw', responseTime: 5, success: false, steps: [] }],
            pagination: { totalCount: 42 },
        });

        const { result } = renderHook(() => useFailedHealthCheckLogs('api-1', '1d'), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.logs).toHaveLength(1));
        expect(result.current.totalCount).toBe(42);
    });

    it('resets to page 1 when the page size changes', async () => {
        const { result } = renderHook(() => useFailedHealthCheckLogs('api-1', '1d'), { wrapper: createWrapper() });

        await waitFor(() => expect(mockGetLogs).toHaveBeenCalled());
        act(() => result.current.setPage(3));
        await waitFor(() => expect(result.current.page).toBe(3));

        act(() => result.current.setPageSize(50));
        await waitFor(() => expect(result.current.pageSize).toBe(50));
        expect(result.current.page).toBe(1);
    });

    it('resets to page 1 when the timeframe changes', async () => {
        let timeframe: '1d' | '1h' = '1d';
        const { result, rerender } = renderHook(() => useFailedHealthCheckLogs('api-1', timeframe), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(mockGetLogs).toHaveBeenCalled());
        act(() => result.current.setPage(3));
        await waitFor(() => expect(result.current.page).toBe(3));

        timeframe = '1h';
        rerender();
        await waitFor(() => expect(result.current.page).toBe(1));
    });

    it('does not fire when the API id is missing (not applicable)', () => {
        renderHook(() => useFailedHealthCheckLogs(undefined, '1d'), { wrapper: createWrapper() });
        expect(mockGetLogs).not.toHaveBeenCalled();
    });
});
