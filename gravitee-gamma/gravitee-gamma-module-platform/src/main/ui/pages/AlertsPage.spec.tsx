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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useNavigate } from 'react-router-dom';

import { AlertsPage } from './AlertsPage';
import { deletePlatformAlert, listPlatformAlerts, updatePlatformAlert } from '../features/alerts/services/alerts';
import type { AlertTrigger } from '../features/alerts/types/alert';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    useHasPermission: jest.fn(),
}));

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: jest.fn(),
}));

jest.mock('../features/alerts/services/alerts', () => ({
    listPlatformAlerts: jest.fn(),
    updatePlatformAlert: jest.fn(),
    deletePlatformAlert: jest.fn(),
}));

jest.mock('../features/alerts/components/AlertsEducationalEmptyState', () => ({
    AlertsEducationalEmptyState: () => <div data-testid="alerts-educational-empty-state" />,
}));

jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseNavigate = jest.mocked(useNavigate);
const mockNavigate = jest.fn();
const mockListPlatformAlerts = jest.mocked(listPlatformAlerts);
const mockUpdatePlatformAlert = jest.mocked(updatePlatformAlert);
const mockDeletePlatformAlert = jest.mocked(deletePlatformAlert);

const ALERT: AlertTrigger = {
    id: 'alert-1',
    name: 'Node stopped',
    description: 'When a gateway node stops',
    severity: 'CRITICAL',
    enabled: true,
    source: 'NODE_LIFECYCLE',
    type: 'NODE_LIFECYCLE_CHANGED',
    counters: { '5m': 1, '1h': 2, '1d': 3, '1M': 4 },
    last_alert_at: '2026-08-12T10:00:00.000Z',
    last_alert_message: 'Node apim-gateway stopped',
};

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter>
                <AlertsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('AlertsPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseNavigate.mockReturnValue(mockNavigate);
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockUseHasPermission.mockReturnValue(true);
        mockListPlatformAlerts.mockResolvedValue([]);
        mockUpdatePlatformAlert.mockResolvedValue(ALERT);
        mockDeletePlatformAlert.mockResolvedValue(undefined);
    });

    it('renders the page header', async () => {
        renderPage();

        expect(screen.getByText('Alerts')).not.toBeNull();
        expect(screen.getByText('Get notified when your gateways or platform need attention.')).not.toBeNull();
        await waitFor(() => expect(mockListPlatformAlerts).toHaveBeenCalledWith('env-1'));
    });

    it('shows the educational empty state when there are no alerts', async () => {
        mockListPlatformAlerts.mockResolvedValue([]);
        renderPage();

        await waitFor(() => expect(screen.getByTestId('alerts-educational-empty-state')).not.toBeNull());
        expect(screen.queryByRole('table')).toBeNull();
    });

    it('shows Add alert when the user has create permission', async () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.includes('environment-alert-c'));
        renderPage();

        await waitFor(() => expect(screen.getByRole('button', { name: /Add alert/i })).not.toBeNull());
    });

    it('hides Add alert when the user lacks create permission', async () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-alert-c'));
        renderPage();

        await waitFor(() => expect(mockListPlatformAlerts).toHaveBeenCalled());
        expect(screen.queryByRole('button', { name: /Add alert/i })).toBeNull();
    });

    it('renders the alerts table when alerts exist', async () => {
        mockListPlatformAlerts.mockResolvedValue([ALERT]);
        renderPage();

        await waitFor(() => expect(screen.getByText('Node stopped')).not.toBeNull());
        expect(screen.getByText('Alert when the lifecycle status of a node has changed')).not.toBeNull();
        expect(screen.getByText('1 / 2 / 3 / 4')).not.toBeNull();
        expect(screen.getByText('Node apim-gateway stopped')).not.toBeNull();
        expect(screen.getByText('critical')).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Last 5m / 1h / 1d / 1M' })).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Last alert' })).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Last message' })).not.toBeNull();
        expect(screen.queryByTestId('alerts-educational-empty-state')).toBeNull();
        expect(screen.queryByText('When a gateway node stops')).toBeNull();
    });

    it('navigates to the alert id when Edit is clicked', async () => {
        const user = userEvent.setup();
        mockListPlatformAlerts.mockResolvedValue([ALERT]);
        renderPage();

        await waitFor(() => expect(screen.getByText('Node stopped')).not.toBeNull());
        await user.click(screen.getByRole('button', { name: 'Actions for Node stopped' }));
        await user.click(await screen.findByRole('menuitem', { name: 'Edit' }));

        expect(mockNavigate).toHaveBeenCalledWith('alert-1');
    });

    it('deletes an alert after ConfirmDialog confirmation', async () => {
        const user = userEvent.setup();
        mockListPlatformAlerts.mockResolvedValue([ALERT]);
        renderPage();

        await waitFor(() => expect(screen.getByText('Node stopped')).not.toBeNull());
        await user.click(screen.getByRole('button', { name: 'Actions for Node stopped' }));
        await user.click(await screen.findByRole('menuitem', { name: 'Delete alert' }));

        expect(screen.getByRole('heading', { name: 'Delete alert' })).not.toBeNull();
        await user.click(screen.getByRole('button', { name: /^Delete$/i }));

        await waitFor(() => expect(mockDeletePlatformAlert).toHaveBeenCalledWith('env-1', 'alert-1'));
        await waitFor(() => expect(notify.success).toHaveBeenCalledWith('Alert "Node stopped" deleted.'));
    });

    it('shows an error message when the list request fails', async () => {
        mockListPlatformAlerts.mockRejectedValue(new Error('boom'));
        renderPage();

        await waitFor(() => expect(screen.getByText('Failed to load alerts. Please try again.')).not.toBeNull());
    });

    it('toggles enabled state when the switch is clicked', async () => {
        mockListPlatformAlerts.mockResolvedValue([ALERT]);
        mockUpdatePlatformAlert.mockResolvedValue({ ...ALERT, enabled: false });
        renderPage();

        await waitFor(() => expect(screen.getByText('Node stopped')).not.toBeNull());
        const toggle = screen.getByRole('switch');
        fireEvent.click(toggle);

        await waitFor(() =>
            expect(mockUpdatePlatformAlert).toHaveBeenCalledWith('env-1', expect.objectContaining({ id: 'alert-1', enabled: false })),
        );
        await waitFor(() => expect(notify.success).toHaveBeenCalledWith('Alert "Node stopped" disabled.'));
    });

    it('does not allow enabling or disabling a template alert', async () => {
        mockListPlatformAlerts.mockResolvedValue([{ ...ALERT, id: 'tpl-1', name: 'API template', template: true }]);
        renderPage();

        await waitFor(() => expect(screen.getByText('API template')).not.toBeNull());
        const toggle = screen.getByRole('switch');
        expect(toggle).toHaveProperty('disabled', true);
        fireEvent.click(toggle);
        expect(mockUpdatePlatformAlert).not.toHaveBeenCalled();
    });

    it('shows an error toast when enable/disable fails', async () => {
        const failure = new Error('toggle failed');
        mockListPlatformAlerts.mockResolvedValue([ALERT]);
        mockUpdatePlatformAlert.mockRejectedValue(failure);
        renderPage();

        await waitFor(() => expect(screen.getByText('Node stopped')).not.toBeNull());
        fireEvent.click(screen.getByRole('switch'));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(failure, 'Failed to update alert.'));
        expect(notify.success).not.toHaveBeenCalled();
    });

    it('hides the actions column when the user cannot update or delete', async () => {
        mockUseHasPermission.mockImplementation(
            ({ anyOf }: { anyOf: string[] }) =>
                !anyOf.includes('environment-alert-u') && !anyOf.includes('environment-alert-d') && !anyOf.includes('environment-alert-c'),
        );
        mockListPlatformAlerts.mockResolvedValue([ALERT]);
        renderPage();

        await waitFor(() => expect(screen.getByText('Node stopped')).not.toBeNull());
        expect(screen.queryByRole('columnheader', { name: 'Actions' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Actions for Node stopped' })).toBeNull();
    });
});
