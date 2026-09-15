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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiAlertsPage } from './ApiAlertsPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { deleteAlertTrigger, listAlerts, updateAlertTrigger } from '../../../services/alerts';
import type { AlertTrigger } from '../../../types';
import {
    API_ALERT_CREATE_PERMISSION,
    API_ALERT_DELETE_PERMISSION,
    API_ALERT_READ_PERMISSION,
    API_ALERT_UPDATE_PERMISSION,
} from '../../../utils/alertPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    useHasPermission: jest.fn(),
}));

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: jest.fn(),
}));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../services/alerts', () => ({
    listAlerts: jest.fn(),
    updateAlertTrigger: jest.fn(),
    deleteAlertTrigger: jest.fn(),
    alertTriggerToFormData: jest.fn((alert: AlertTrigger) => ({
        name: alert.name,
        description: alert.description ?? '',
        severity: alert.severity,
        enabled: alert.enabled,
        source: alert.source,
        type: alert.type,
        conditions: [],
        filters: [],
        notifications: [],
        timeframes: [],
        dampening: { mode: 'STRICT_COUNT', trueEvaluations: 1 },
    })),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseNavigate = jest.mocked(require('react-router-dom').useNavigate);
const mockUseApiDetailContext = jest.mocked(useApiDetailContext);
const mockListAlerts = jest.mocked(listAlerts);
const mockUpdateAlertTrigger = jest.mocked(updateAlertTrigger);
const mockDeleteAlertTrigger = jest.mocked(deleteAlertTrigger);
const mockNavigate = jest.fn();

const ALERT: AlertTrigger = {
    id: 'alert-1',
    name: 'High latency',
    description: 'When response time spikes',
    severity: 'WARNING',
    enabled: true,
    source: 'REQUEST',
    type: 'REQUEST_METRICS',
    counters: { '5m': 1, '1h': 2, '1d': 3, '1M': 4 },
    last_alert_at: '2026-08-12T10:00:00.000Z',
    last_alert_message: 'Threshold exceeded',
};

const ALL_API_ALERT_PERMISSIONS = [
    API_ALERT_READ_PERMISSION,
    API_ALERT_CREATE_PERMISSION,
    API_ALERT_UPDATE_PERMISSION,
    API_ALERT_DELETE_PERMISSION,
];

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={['/apis/api-1/alerts']}>
                <Routes>
                    <Route path="/apis/:apiId/alerts" element={<ApiAlertsPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiAlertsPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseNavigate.mockReturnValue(mockNavigate);
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockUseApiDetailContext.mockReturnValue({ permissionsReady: true, api: null, isLoading: false });
        const grantedPermissions = new Set<string>(ALL_API_ALERT_PERMISSIONS);
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: readonly string[] }) =>
            anyOf.some(permission => grantedPermissions.has(permission)),
        );
        mockListAlerts.mockResolvedValue([]);
        mockUpdateAlertTrigger.mockResolvedValue({ ...ALERT, enabled: false });
        mockDeleteAlertTrigger.mockResolvedValue(undefined);
    });

    it('renders the page header and loads alerts with event counts', async () => {
        renderPage();

        expect(screen.getByRole('heading', { name: /runtime alerts/i })).not.toBeNull();
        await waitFor(() => expect(mockListAlerts).toHaveBeenCalledWith('env-1', 'api-1', true));
    });

    it('shows the educational empty state when there are no alerts', async () => {
        renderPage();

        await waitFor(() => expect(screen.getByText(/why configure runtime alerts/i)).not.toBeNull());
        expect(screen.queryByRole('table')).toBeNull();
    });

    it('renders the alerts table with classic counter columns', async () => {
        mockListAlerts.mockResolvedValue([ALERT]);
        renderPage();

        await waitFor(() => expect(screen.getByText('High latency')).not.toBeNull());
        expect(screen.getByText('1 / 2 / 3 / 4')).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Last 5m / 1h / 1d / 1M' })).not.toBeNull();
    });

    it('shows a permission message when the user cannot read alerts', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();

        expect(screen.getByText(/don't have permission to view runtime alerts/i)).not.toBeNull();
        expect(mockListAlerts).not.toHaveBeenCalled();
    });

    it('toggles enabled state when the switch is clicked', async () => {
        mockListAlerts.mockResolvedValue([ALERT]);
        renderPage();

        await waitFor(() => expect(screen.getByText('High latency')).not.toBeNull());
        fireEvent.click(screen.getByRole('switch'));

        await waitFor(() =>
            expect(mockUpdateAlertTrigger).toHaveBeenCalledWith('env-1', 'api-1', 'alert-1', expect.objectContaining({ enabled: false })),
        );
    });

    it('deletes an alert from the actions menu', async () => {
        const user = userEvent.setup();
        mockListAlerts.mockResolvedValue([ALERT]);
        renderPage();

        await waitFor(() => expect(screen.getByText('High latency')).not.toBeNull());
        const row = screen.getByText('High latency').closest('tr');
        await user.click(within(row!).getByRole('button'));
        await user.click(await screen.findByRole('menuitem', { name: 'Delete' }));

        await waitFor(() => expect(mockDeleteAlertTrigger).toHaveBeenCalledWith('env-1', 'api-1', 'alert-1'));
    });
});
