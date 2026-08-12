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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useGatewayInstanceMonitoring } from './useGatewayInstanceMonitoring';
import { getGatewayInstanceMonitoring } from '../services/instances';
import type { MonitoringData } from '../types/instance';
import { gatewayInstanceKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../services/instances', () => ({ getGatewayInstanceMonitoring: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetMonitoring = jest.mocked(getGatewayInstanceMonitoring);

const MONITORING_DATA = {
    cpu: { percent_use: 12, load_average: { '1m': 0.5 } },
    process: { cpu_percent: 10, open_file_descriptors: 100, max_file_descriptors: 1024 },
    jvm: {
        timestamp: 1_700_000_000_000,
        uptime_in_millis: 60_000,
        heap_used_in_bytes: 100,
        heap_used_percent: 50,
        heap_committed_in_bytes: 200,
        heap_max_in_bytes: 400,
        non_heap_used_in_bytes: 50,
        non_heap_committed_in_bytes: 75,
        young_pool_used_in_bytes: 10,
        young_pool_max_in_bytes: 20,
        young_pool_peak_used_in_bytes: 15,
        young_pool_peak_max_in_bytes: 20,
        survivor_pool_used_in_bytes: 5,
        survivor_pool_max_in_bytes: 10,
        survivor_pool_peak_used_in_bytes: 6,
        survivor_pool_peak_max_in_bytes: 10,
        old_pool_used_in_bytes: 30,
        old_pool_max_in_bytes: 60,
        old_pool_peak_used_in_bytes: 40,
        old_pool_peak_max_in_bytes: 60,
    },
    thread: { count: 42, peak_count: 55 },
    gc: {
        young_collection_count: 1,
        young_collection_time_in_millis: 2,
        old_collection_count: 3,
        old_collection_time_in_millis: 4,
    },
} satisfies MonitoringData;

function createTestContext() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { queryClient, Wrapper };
}

describe('useGatewayInstanceMonitoring', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockGetMonitoring.mockResolvedValue(MONITORING_DATA);
    });

    afterEach(() => jest.clearAllMocks());

    it('does not call the API when disabled', () => {
        const { Wrapper } = createTestContext();
        renderHook(
            () =>
                useGatewayInstanceMonitoring({
                    instanceId: 'event-1',
                    gatewayId: 'gw-1',
                    enabled: false,
                }),
            { wrapper: Wrapper },
        );

        expect(mockGetMonitoring).not.toHaveBeenCalled();
    });

    it('does not call the API when the environment is unavailable', () => {
        mockUseEnvironment.mockReturnValue(null);
        const { Wrapper } = createTestContext();

        renderHook(
            () =>
                useGatewayInstanceMonitoring({
                    instanceId: 'event-1',
                    gatewayId: 'gw-1',
                    enabled: true,
                }),
            { wrapper: Wrapper },
        );

        expect(mockGetMonitoring).not.toHaveBeenCalled();
    });

    it('calls getGatewayInstanceMonitoring when enabled with env, event id, and gateway id', async () => {
        const { Wrapper } = createTestContext();
        renderHook(
            () =>
                useGatewayInstanceMonitoring({
                    instanceId: 'event-1',
                    gatewayId: 'gw-1',
                    enabled: true,
                }),
            { wrapper: Wrapper },
        );

        await waitFor(() => expect(mockGetMonitoring).toHaveBeenCalledTimes(1));
        expect(mockGetMonitoring).toHaveBeenCalledWith('env-1', 'event-1', 'gw-1');
    });

    it('configures a 5s refetch interval when enabled', async () => {
        const { queryClient, Wrapper } = createTestContext();
        renderHook(
            () =>
                useGatewayInstanceMonitoring({
                    instanceId: 'event-1',
                    gatewayId: 'gw-1',
                    enabled: true,
                }),
            { wrapper: Wrapper },
        );
        await waitFor(() => expect(mockGetMonitoring).toHaveBeenCalled());
        const query = queryClient.getQueryCache().find({ queryKey: gatewayInstanceKeys.monitoring('env-1', 'event-1', 'gw-1') });
        expect(query?.options.refetchInterval).toBe(5_000);
    });

    it('uses a query key scoped by env id, event id, and gateway id', async () => {
        const { queryClient, Wrapper } = createTestContext();
        renderHook(
            () =>
                useGatewayInstanceMonitoring({
                    instanceId: 'event-1',
                    gatewayId: 'gw-1',
                    enabled: true,
                }),
            { wrapper: Wrapper },
        );

        await waitFor(() => expect(mockGetMonitoring).toHaveBeenCalledTimes(1));

        const expectedKey = gatewayInstanceKeys.monitoring('env-1', 'event-1', 'gw-1');
        expect(queryClient.getQueryCache().find({ queryKey: expectedKey })).toBeDefined();
    });
});
