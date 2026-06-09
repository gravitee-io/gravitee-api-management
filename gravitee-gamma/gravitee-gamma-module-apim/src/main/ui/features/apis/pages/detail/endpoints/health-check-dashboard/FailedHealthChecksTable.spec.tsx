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
import userEvent from '@testing-library/user-event';

import { FailedHealthChecksTable } from './FailedHealthChecksTable';
import * as logsHook from './useFailedHealthCheckLogs';
import type { HealthCheckLog } from '../../../../types/healthCheck';

const useFailedHealthCheckLogsSpy = jest.spyOn(logsHook, 'useFailedHealthCheckLogs');

const LOG: HealthCheckLog = {
    id: 'log-1',
    timestamp: '2026-04-13T12:30:00Z',
    endpointName: 'endpoint-1',
    gatewayId: 'gateway-a',
    responseTime: 250,
    success: false,
    steps: [
        {
            name: 'default-step',
            success: false,
            message: 'connection refused',
            request: { uri: 'https://api/health', method: 'GET', headers: { Accept: 'application/json' } },
            response: { status: 503, body: 'Service Unavailable', headers: { 'content-type': 'text/plain' } },
        },
    ],
};

function buildData(overrides: Partial<logsHook.FailedHealthCheckLogsData> = {}): logsHook.FailedHealthCheckLogsData {
    return {
        logs: [LOG],
        totalCount: 1,
        page: 1,
        pageSize: 10,
        isLoading: false,
        isError: false,
        setPage: jest.fn(),
        setPageSize: jest.fn(),
        ...overrides,
    };
}

describe('FailedHealthChecksTable', () => {
    beforeEach(() => {
        useFailedHealthCheckLogsSpy.mockReturnValue(buildData());
    });

    afterEach(() => jest.clearAllMocks());

    it('renders failed log rows', () => {
        render(<FailedHealthChecksTable apiId="api-1" timeframe="1d" />);

        expect(screen.getByText('endpoint-1')).toBeInTheDocument();
        expect(screen.getByText('gateway-a')).toBeInTheDocument();
    });

    it('shows an empty state when there are no failures', () => {
        useFailedHealthCheckLogsSpy.mockReturnValue(buildData({ logs: [], totalCount: 0 }));

        render(<FailedHealthChecksTable apiId="api-1" timeframe="1d" />);

        expect(screen.getByText(/no failed health checks/i)).toBeInTheDocument();
    });

    it('opens the log detail sheet with request/response steps on row click', async () => {
        render(<FailedHealthChecksTable apiId="api-1" timeframe="1d" />);

        await userEvent.click(screen.getByRole('button', { name: /view health check detail for endpoint-1/i }));

        expect(screen.getByText('Health check detail')).toBeInTheDocument();
        expect(screen.getByText(/GET https:\/\/api\/health/)).toBeInTheDocument();
        expect(screen.getByText(/Status: 503/)).toBeInTheDocument();
    });
});
