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

import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { GatewayInstanceMonitoringPage } from './GatewayInstanceMonitoringPage';
import { useGatewayInstanceDetail } from '../features/gateway-instances/hooks/useGatewayInstanceDetail';
import { useGatewayInstanceMonitoring } from '../features/gateway-instances/hooks/useGatewayInstanceMonitoring';
import type { Instance, MonitoringData } from '../features/gateway-instances/types/instance';

jest.mock('../features/gateway-instances/hooks/useGatewayInstanceDetail');
jest.mock('../features/gateway-instances/hooks/useGatewayInstanceMonitoring');

jest.mock('../features/gateway-instances/components/GatewayInstanceMonitoringView', () => ({
    GatewayInstanceMonitoringView: () => <div data-testid="gateway-instance-monitoring-view">monitoring view</div>,
}));

const mockUseDetail = jest.mocked(useGatewayInstanceDetail);
const mockUseMonitoring = jest.mocked(useGatewayInstanceMonitoring);

const STARTED_INSTANCE: Instance = {
    id: 'gw-1',
    event: 'event-1',
    hostname: 'apim-gateway',
    ip: '10.0.0.22',
    port: '8082',
    version: '4.13.0',
    state: 'STARTED',
    started_at: 1,
    last_heartbeat_at: 2,
};

const STOPPED_INSTANCE: Instance = {
    ...STARTED_INSTANCE,
    state: 'STOPPED',
};

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

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/gateways/event-1/monitoring']}>
            <Routes>
                <Route path="/gateways/:instanceId/monitoring" element={<GatewayInstanceMonitoringPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('GatewayInstanceMonitoringPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseMonitoring.mockReturnValue({ data: MONITORING_DATA, isLoading: false, isError: false } as never);
    });

    it('shows a skeleton while the detail query is loading', () => {
        mockUseDetail.mockReturnValue({ data: undefined, isLoading: true, isError: false } as never);
        renderPage();
        expect(screen.getByTestId('gateway-instance-monitoring-skeleton')).not.toBeNull();
    });

    it('shows an error message when the detail query fails', () => {
        mockUseDetail.mockReturnValue({ data: undefined, isLoading: false, isError: true } as never);
        renderPage();
        expect(screen.getByText(/Failed to load monitoring details/i)).not.toBeNull();
    });

    it('shows a message when the gateway instance is not started', () => {
        mockUseDetail.mockReturnValue({ data: STOPPED_INSTANCE, isLoading: false, isError: false } as never);
        renderPage();
        expect(screen.getByTestId('gateway-instance-monitoring-no-data')).not.toBeNull();
        expect(screen.getByText(/There is no data for stopped gateway instance/i)).not.toBeNull();
        expect(mockUseMonitoring).toHaveBeenCalledWith({
            instanceId: 'event-1',
            gatewayId: 'gw-1',
            enabled: false,
        });
    });

    it('renders monitoring data for a started instance', () => {
        mockUseDetail.mockReturnValue({ data: STARTED_INSTANCE, isLoading: false, isError: false } as never);
        renderPage();
        expect(screen.getByTestId('gateway-instance-monitoring-view')).not.toBeNull();
        expect(mockUseMonitoring).toHaveBeenCalledWith({
            instanceId: 'event-1',
            gatewayId: 'gw-1',
            enabled: true,
        });
    });

    it('shows a skeleton while monitoring data is loading', () => {
        mockUseDetail.mockReturnValue({ data: STARTED_INSTANCE, isLoading: false, isError: false } as never);
        mockUseMonitoring.mockReturnValue({ data: undefined, isLoading: true, isError: false } as never);
        renderPage();
        expect(screen.getByTestId('gateway-instance-monitoring-skeleton')).not.toBeNull();
    });

    it('shows an error message when monitoring data fails to load', () => {
        mockUseDetail.mockReturnValue({ data: STARTED_INSTANCE, isLoading: false, isError: false } as never);
        mockUseMonitoring.mockReturnValue({ data: undefined, isLoading: false, isError: true } as never);
        renderPage();
        expect(screen.getByTestId('gateway-instance-monitoring-error')).not.toBeNull();
        expect(screen.getByText(/Failed to load monitoring data/i)).not.toBeNull();
    });

    it('shows an empty message when monitoring query settles with no payload', () => {
        mockUseDetail.mockReturnValue({ data: STARTED_INSTANCE, isLoading: false, isError: false } as never);
        mockUseMonitoring.mockReturnValue({ data: null, isLoading: false, isError: false } as never);
        renderPage();
        expect(screen.getByTestId('gateway-instance-monitoring-empty')).not.toBeNull();
        expect(screen.getByText(/There is no monitoring data for this gateway instance yet/i)).not.toBeNull();
        expect(screen.queryByTestId('gateway-instance-monitoring-skeleton')).toBeNull();
    });
});
