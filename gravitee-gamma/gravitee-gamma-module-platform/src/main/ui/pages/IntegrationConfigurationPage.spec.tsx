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

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { IntegrationConfigurationPage } from './IntegrationConfigurationPage';
import { getIntegration } from '../features/integrations/services/integrationDetail';
import { getIntegrationPermissions } from '../features/integrations/services/integrationPermissions';
import {
    INTEGRATION_DEFINITION_DELETE_PERMISSION,
    INTEGRATION_DEFINITION_READ_PERMISSION,
    INTEGRATION_DEFINITION_UPDATE_PERMISSION,
} from '../features/integrations/utils/integrationPermissions';
import { ApimApiError } from '../shared/api/apimClient';
import { notify } from '../shared/notify';

jest.mock('../features/integrations/services/integrationDetail', () => ({ getIntegration: jest.fn() }));
jest.mock('../features/integrations/services/integrationPermissions', () => ({ getIntegrationPermissions: jest.fn() }));
jest.mock('../features/integrations/components/IntegrationDangerZone', () => ({
    IntegrationDangerZone: ({ integrationId }: { integrationId: string }) => <section>Danger Zone of {integrationId}</section>,
}));
jest.mock('../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() } }));

const mockGetIntegration = jest.mocked(getIntegration);
const mockGetIntegrationPermissions = jest.mocked(getIntegrationPermissions);
const mockNotifyError = jest.mocked(notify.error);

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
});

function LocationProbe() {
    return <span data-testid="location">{useLocation().pathname}</span>;
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/integrations/int-1/configuration']}>
                <LocationProbe />
                <Routes>
                    <Route path="/integrations">
                        <Route index element={<p>Integrations list</p>} />
                        <Route path=":integrationId/configuration" element={<IntegrationConfigurationPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function waitForIntegrationAndPermissionsToLoad() {
    await waitFor(() => expect(mockGetIntegration).toHaveBeenCalledWith('DEFAULT', 'int-1'));
    await waitFor(() => expect(mockGetIntegrationPermissions).toHaveBeenCalledWith('DEFAULT', 'int-1'));
    await act(() => new Promise(resolve => setTimeout(resolve, 0)));
}

describe('IntegrationConfigurationPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGetIntegration.mockResolvedValue({ id: 'int-1', name: 'Old name', provider: 'solace' });
    });

    it('shows the Name field on the General tab to a user who can update the integration', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_UPDATE_PERMISSION]);

        renderPage();

        expect(await screen.findByRole('textbox', { name: /^Name/ })).toHaveValue('Old name');
    });

    it('shows no Name field on the General tab to a user who can delete but not update the integration', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_DELETE_PERMISSION]);

        renderPage();
        await waitForIntegrationAndPermissionsToLoad();

        expect(screen.getByRole('tabpanel', { name: 'General' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).not.toBeInTheDocument();
    });

    it('shows the Danger Zone of the integration on the General tab when the user can delete the integration', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_DELETE_PERMISSION]);

        renderPage();

        expect(screen.getByRole('tab', { name: 'General' })).toBeInTheDocument();
        expect(await screen.findByText('Danger Zone of int-1')).toBeInTheDocument();
        expect(mockGetIntegrationPermissions).toHaveBeenCalledWith('DEFAULT', 'int-1');
    });

    it('shows no Danger Zone on the General tab when the user can update but not delete the integration', async () => {
        mockGetIntegrationPermissions.mockResolvedValue([INTEGRATION_DEFINITION_READ_PERMISSION, INTEGRATION_DEFINITION_UPDATE_PERMISSION]);

        renderPage();
        await waitForIntegrationAndPermissionsToLoad();

        expect(screen.getByRole('tab', { name: 'General' })).toBeInTheDocument();
        expect(screen.queryByText(/Danger Zone/)).toBeNull();
    });

    it('redirects to the Integrations list without a toast when the integration is an A2A one', async () => {
        mockGetIntegration.mockResolvedValue({ id: 'int-1', name: 'Agent Bridge', provider: 'A2A' });
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_DEFINITION_DELETE_PERMISSION,
        ]);

        renderPage();

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(screen.getByText('Integrations list')).toBeInTheDocument();
        expect(screen.queryByTestId('integration-configuration-page')).toBeNull();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).toBeNull();
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('shows only the load failure message, with no Name field or Danger Zone, when the integration fails to load', async () => {
        const failure = new Error('integration unavailable');
        mockGetIntegration.mockRejectedValue(failure);
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_DEFINITION_DELETE_PERMISSION,
        ]);

        renderPage();

        expect(await screen.findByText('Integration could not be loaded. Please refresh and try again.')).toBeInTheDocument();
        expect(mockNotifyError).toHaveBeenCalledWith(failure, 'Integration could not be loaded. Please refresh and try again.');
        await waitForIntegrationAndPermissionsToLoad();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).toBeNull();
        expect(screen.queryByText(/Danger Zone/)).toBeNull();
    });

    it('redirects to the Integrations list without a toast when the integration request is forbidden', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        mockGetIntegration.mockRejectedValue(new ApimApiError(403, 'Forbidden'));
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_DEFINITION_DELETE_PERMISSION,
        ]);

        renderPage();

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(screen.getByText('Integrations list')).toBeInTheDocument();
        expect(screen.queryByTestId('integration-configuration-page')).toBeNull();
        expect(mockNotifyError).not.toHaveBeenCalled();
        warn.mockRestore();
    });

    it('shows a loading skeleton and no Name field or Danger Zone while the integration is loading', async () => {
        mockGetIntegration.mockReturnValue(new Promise(() => {}));
        mockGetIntegrationPermissions.mockResolvedValue([
            INTEGRATION_DEFINITION_READ_PERMISSION,
            INTEGRATION_DEFINITION_UPDATE_PERMISSION,
            INTEGRATION_DEFINITION_DELETE_PERMISSION,
        ]);

        renderPage();
        await waitFor(() => expect(mockGetIntegrationPermissions).toHaveBeenCalledWith('DEFAULT', 'int-1'));
        await act(() => new Promise(resolve => setTimeout(resolve, 0)));

        expect(document.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
        expect(screen.queryByRole('textbox', { name: /^Name/ })).toBeNull();
        expect(screen.queryByText(/Danger Zone/)).toBeNull();
    });
});
