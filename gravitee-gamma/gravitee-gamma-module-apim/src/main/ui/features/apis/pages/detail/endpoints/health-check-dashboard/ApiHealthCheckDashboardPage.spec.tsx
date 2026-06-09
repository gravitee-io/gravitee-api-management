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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiHealthCheckDashboardPage } from './ApiHealthCheckDashboardPage';
import * as dashboardHook from './useHealthCheckDashboard';
import { notify } from '../../../../../../shared/notify';
import { ApiDetailContext } from '../../../../context/ApiDetailContext';
import type { ApiDetailDto } from '../../../../types/api';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-charts', () => ({
    ChartContainer: ({ children }: { children: React.ReactNode }) => <div data-testid="chart-container">{children}</div>,
    DoughnutChart: ({ centerContent }: { centerContent?: React.ReactNode }) => <div data-testid="doughnut-chart">{centerContent}</div>,
    LineChart: () => <div data-testid="line-chart" />,
    AreaChart: () => <div data-testid="area-chart" />,
    BarChart: () => <div data-testid="bar-chart" />,
}));

jest.mock('./useFailedHealthCheckLogs', () => ({
    FAILED_LOGS_PAGE_SIZES: [10, 25, 50, 100],
    useFailedHealthCheckLogs: jest.fn(() => ({
        logs: [],
        totalCount: 0,
        page: 1,
        pageSize: 10,
        isLoading: false,
        isError: false,
        setPage: jest.fn(),
        setPageSize: jest.fn(),
    })),
}));

jest.mock('../../../../../../shared/notify', () => ({ notify: { error: jest.fn(), success: jest.fn() } }));

const mockNotifyError = jest.mocked(notify.error);
const useHealthCheckDashboardSpy = jest.spyOn(dashboardHook, 'useHealthCheckDashboard');

function buildDashboard(overrides: Partial<dashboardHook.HealthCheckDashboardData> = {}): dashboardHook.HealthCheckDashboardData {
    return {
        canRead: true,
        availability: { value: 92.68, isLoading: false, isError: false },
        responseTime: { value: 87, isLoading: false, isError: false },
        trend: { points: [], isLoading: false, isError: false },
        endpoint: { rows: [{ key: 'ep-1', name: 'ep-1', availabilityPct: 99, avgResponseTimeMs: 42 }], isLoading: false, isError: false },
        gateway: { rows: [], isLoading: false, isError: false },
        anyError: false,
        refresh: jest.fn(),
        ...overrides,
    };
}

function renderPage(api: Partial<ApiDetailDto> | null) {
    return render(
        <ApiDetailContext.Provider value={{ api: api as ApiDetailDto, isLoading: false, permissionsReady: true }}>
            <MemoryRouter initialEntries={['/apis/api-1/endpoints/health-check-dashboard']}>
                <Routes>
                    <Route path="apis/:apiId/endpoints/health-check-dashboard" element={<ApiHealthCheckDashboardPage />} />
                </Routes>
            </MemoryRouter>
        </ApiDetailContext.Provider>,
    );
}

const PROXY_API: Partial<ApiDetailDto> = { id: 'api-1', name: 'My API', type: 'PROXY', definitionVersion: 'V4' };

describe('ApiHealthCheckDashboardPage', () => {
    beforeEach(() => {
        mockNotifyError.mockClear();
        useHealthCheckDashboardSpy.mockReturnValue(buildDashboard());
    });

    it('renders metrics, trend chart, and availability tables for a V4 proxy API', () => {
        renderPage(PROXY_API);

        expect(screen.getByText('Health Check Dashboard')).toBeInTheDocument();
        expect(screen.getByText('92.68 %')).toBeInTheDocument();
        expect(screen.getByText('87 ms')).toBeInTheDocument();
        expect(screen.getByTestId('line-chart')).toBeInTheDocument();
        expect(screen.getByText('Availability per endpoint')).toBeInTheDocument();
        expect(screen.getByText('Availability per gateway')).toBeInTheDocument();
        expect(screen.getByText('ep-1')).toBeInTheDocument();
    });

    it('shows a "proxy APIs only" state for TCP proxy APIs and fires no queries', () => {
        renderPage({ ...PROXY_API, listeners: [{ type: 'TCP' } as never] });

        expect(screen.getByText('Health checks are available for proxy APIs only')).toBeInTheDocument();
        expect(useHealthCheckDashboardSpy).toHaveBeenCalledWith(undefined, '1d');
    });

    it('shows a "proxy APIs only" state for non-proxy APIs and fires no queries', () => {
        renderPage({ ...PROXY_API, type: 'NATIVE' });

        expect(screen.getByText('Health checks are available for proxy APIs only')).toBeInTheDocument();
        // Not applicable → hook called with undefined apiId so queries stay disabled.
        expect(useHealthCheckDashboardSpy).toHaveBeenCalledWith(undefined, '1d');
    });

    it('shows a permission state when the user cannot read health checks', () => {
        useHealthCheckDashboardSpy.mockReturnValue(buildDashboard({ canRead: false }));

        renderPage(PROXY_API);

        expect(screen.getByText("You don't have permission to view health checks")).toBeInTheDocument();
    });

    it('invalidates dashboard data when Refresh is clicked', async () => {
        const refresh = jest.fn();
        useHealthCheckDashboardSpy.mockReturnValue(buildDashboard({ refresh }));

        renderPage(PROXY_API);
        await userEvent.click(screen.getByRole('button', { name: /refresh data/i }));

        expect(refresh).toHaveBeenCalledTimes(1);
    });

    it('degrades silently on load failure: keeps widgets, no banner, no error toast (classic parity)', () => {
        useHealthCheckDashboardSpy.mockReturnValue(buildDashboard({ anyError: true }));

        renderPage(PROXY_API);

        // Dashboard still renders its widgets (parity with the classic console).
        expect(screen.getByText('Health Check Dashboard')).toBeInTheDocument();
        expect(screen.getByTestId('line-chart')).toBeInTheDocument();
        // No blocking error banner…
        expect(screen.queryByText(/failed to load health-check data/i)).not.toBeInTheDocument();
        // …and no error toast — widgets just show their own empty/0 states.
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('still shows global availability when only average-response-time fails (per-widget independence)', () => {
        // Availability resolves; response time fails — mirrors the classic console.
        useHealthCheckDashboardSpy.mockReturnValue(
            buildDashboard({
                availability: { value: 92.68, isLoading: false, isError: false },
                responseTime: { value: undefined, isLoading: false, isError: true },
                anyError: true,
            }),
        );

        renderPage(PROXY_API);

        expect(screen.getByText('92.68 %')).toBeInTheDocument();
    });
});
