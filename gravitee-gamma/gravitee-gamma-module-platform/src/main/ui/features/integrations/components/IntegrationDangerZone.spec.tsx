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
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { IntegrationDangerZone } from './IntegrationDangerZone';
import { ApimApiError } from '../../../shared/api/apimClient';
import { notify } from '../../../shared/notify/notify';
import { getEnvironmentPermissions } from '../../../shared/services/environmentPermissions';
import { deleteFederatedApis, deleteIntegration, getIntegration, hasFederatedApis } from '../services/integrationDetail';
import type { Integration } from '../types/integration';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../services/integrationDetail', () => ({
    getIntegration: jest.fn(),
    hasFederatedApis: jest.fn(),
    deleteIntegration: jest.fn(),
    deleteFederatedApis: jest.fn(),
}));
jest.mock('../../../shared/services/environmentPermissions', () => ({ getEnvironmentPermissions: jest.fn() }));
jest.mock('../../../shared/notify/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetIntegration = jest.mocked(getIntegration);
const mockHasFederatedApis = jest.mocked(hasFederatedApis);
const mockDeleteIntegration = jest.mocked(deleteIntegration);
const mockDeleteFederatedApis = jest.mocked(deleteFederatedApis);
const mockGetEnvironmentPermissions = jest.mocked(getEnvironmentPermissions);
const mockNotify = jest.mocked(notify);

const INTEGRATION: Integration = { id: 'int-1', name: 'Payments gateway', provider: 'aws-api-gateway', agentStatus: 'CONNECTED' };
const CONFIGURATION_PATH = `/integrations/${INTEGRATION.id}/configuration`;
const ENVIRONMENT_API_DELETE = 'environment-api-d';

function neverResolve<T>(): Promise<T> {
    return new Promise(() => undefined);
}

function LocationProbe() {
    return <span data-testid="location">{useLocation().pathname}</span>;
}

function renderDangerZone() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[CONFIGURATION_PATH]}>
                <LocationProbe />
                <Routes>
                    <Route path="/integrations" element={<p>Integrations list</p>} />
                    <Route
                        path="/integrations/:integrationId/configuration"
                        element={<IntegrationDangerZone integrationId={INTEGRATION.id} />}
                    />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function openDeleteDialog(user: ReturnType<typeof userEvent.setup>) {
    const deleteButton = screen.getByRole('button', { name: 'Delete Integration' });
    await waitFor(() => expect(deleteButton).toBeEnabled());
    await user.click(deleteButton);
    return screen.getByRole('dialog', { name: 'Delete integration' });
}

async function openDeleteApisDialog(user: ReturnType<typeof userEvent.setup>) {
    const deleteApisButton = await screen.findByRole('button', { name: 'Delete APIs' });
    await waitFor(() => expect(deleteApisButton).toBeEnabled());
    await user.click(deleteApisButton);
    return screen.getByRole('dialog', { name: 'Delete APIs' });
}

async function confirmDeleteApis(user: ReturnType<typeof userEvent.setup>) {
    const dialog = await openDeleteApisDialog(user);
    await user.click(within(dialog).getByRole('button', { name: 'Delete APIs' }));
}

async function confirmDelete(user: ReturnType<typeof userEvent.setup>) {
    const dialog = await openDeleteDialog(user);
    await user.type(within(dialog).getByRole('textbox'), INTEGRATION.name);
    await user.click(within(dialog).getByRole('button', { name: 'Delete permanently' }));
}

describe('IntegrationDangerZone', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockGetIntegration.mockResolvedValue(INTEGRATION);
        mockHasFederatedApis.mockResolvedValue(false);
        mockDeleteIntegration.mockResolvedValue(undefined);
        mockGetEnvironmentPermissions.mockResolvedValue([]);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('titles its card Danger Zone', () => {
        renderDangerZone();

        expect(screen.getByText('Danger Zone')).toBeInTheDocument();
    });

    it('disables Delete Integration and explains why when the integration has a federated API', async () => {
        mockHasFederatedApis.mockResolvedValue(true);

        renderDangerZone();

        const deleteButton = screen.getByRole('button', { name: 'Delete Integration' });
        await waitFor(() =>
            expect(deleteButton).toHaveAccessibleDescription(
                'An integration with federated APIs cannot be deleted. Delete its federated APIs first.',
            ),
        );
        expect(deleteButton).toBeDisabled();
    });

    it('enables Delete Integration when the integration has no federated APIs', async () => {
        renderDangerZone();

        const deleteButton = screen.getByRole('button', { name: 'Delete Integration' });
        await waitFor(() => expect(deleteButton).toBeEnabled());
        expect(deleteButton).toHaveAccessibleDescription('Permanently deletes the integration. This action cannot be undone.');
    });

    it.each([
        {
            scenario: 'the federated-APIs check is still pending',
            arrange: () => mockHasFederatedApis.mockReturnValue(neverResolve()),
            description: 'Checking whether this integration has federated APIs…',
        },
        {
            scenario: 'the federated-APIs check fails',
            arrange: () => mockHasFederatedApis.mockRejectedValue(new ApimApiError(500, 'Internal error')),
            description: 'Could not check whether this integration has federated APIs. Reload the page to try again.',
        },
        {
            scenario: 'the integration has not loaded yet',
            arrange: () => mockGetIntegration.mockReturnValue(neverResolve()),
            description: 'Permanently deletes the integration. This action cannot be undone.',
        },
    ])('keeps Delete Integration disabled while $scenario', async ({ arrange, description }) => {
        arrange();

        renderDangerZone();

        const deleteButton = screen.getByRole('button', { name: 'Delete Integration' });
        await waitFor(() => expect(deleteButton).toHaveAccessibleDescription(description));
        expect(deleteButton).toBeDisabled();
    });

    it('opens the Delete integration confirmation dialog when Delete Integration is selected', async () => {
        const user = userEvent.setup();
        renderDangerZone();

        const dialog = await openDeleteDialog(user);

        expect(within(dialog).getByRole('button', { name: 'Delete permanently' })).toBeDisabled();
        expect(mockDeleteIntegration).not.toHaveBeenCalled();
    });

    it('shows a success notification when the confirmed delete succeeds', async () => {
        const user = userEvent.setup();
        renderDangerZone();

        await confirmDelete(user);

        await waitFor(() => expect(mockNotify.success).toHaveBeenCalledWith('Integration successfully deleted!'));
        expect(mockDeleteIntegration).toHaveBeenCalledWith('env-1', INTEGRATION.id);
    });

    it('returns to the Integrations list when the confirmed delete succeeds', async () => {
        const user = userEvent.setup();
        renderDangerZone();

        await confirmDelete(user);

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(screen.getByText('Integrations list')).toBeInTheDocument();
    });

    it('shows an error notification carrying the API error message when the confirmed delete fails', async () => {
        const user = userEvent.setup();
        mockDeleteIntegration.mockRejectedValue(new ApimApiError(400, 'Boom', { httpStatus: 400, message: 'Boom' }));
        renderDangerZone();

        await confirmDelete(user);

        await waitFor(() => expect(mockNotify.error).toHaveBeenCalledWith('Something went wrong! Boom'));
        expect(mockNotify.success).not.toHaveBeenCalled();
        expect(screen.getByTestId('location').textContent).toBe(CONFIGURATION_PATH);
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    });

    it('shows Deleting… and blocks a second confirm while the Delete integration request is in flight', async () => {
        const user = userEvent.setup();
        mockDeleteIntegration.mockReturnValue(neverResolve());
        renderDangerZone();
        const dialog = await openDeleteDialog(user);
        await user.type(within(dialog).getByRole('textbox'), INTEGRATION.name);

        await user.click(within(dialog).getByRole('button', { name: 'Delete permanently' }));

        const pendingConfirmButton = await within(dialog).findByRole('button', { name: 'Deleting…' });
        expect(pendingConfirmButton).toBeDisabled();
        expect(within(dialog).getByRole('button', { name: 'Cancel' })).toBeDisabled();
        expect(screen.getByRole('dialog', { name: 'Delete integration' })).toBeInTheDocument();
        expect(mockDeleteIntegration).toHaveBeenCalledTimes(1);
        expect(mockDeleteIntegration).toHaveBeenCalledWith('env-1', INTEGRATION.id);
        expect(screen.getByTestId('location').textContent).toBe(CONFIGURATION_PATH);
    });

    it('sends no delete request when the confirmation dialog is cancelled after typing the name', async () => {
        const user = userEvent.setup();
        renderDangerZone();
        const dialog = await openDeleteDialog(user);
        await user.type(within(dialog).getByRole('textbox'), INTEGRATION.name);

        await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(mockDeleteIntegration).not.toHaveBeenCalled();
        expect(screen.getByTestId('location').textContent).toBe(CONFIGURATION_PATH);
    });

    describe('Delete APIs', () => {
        beforeEach(() => {
            mockGetEnvironmentPermissions.mockResolvedValue([ENVIRONMENT_API_DELETE]);
            mockHasFederatedApis.mockResolvedValue(true);
            mockDeleteFederatedApis.mockResolvedValue({ deleted: 1, skipped: 0, errors: 0 });
        });

        it('offers Delete APIs to a user who can delete APIs', async () => {
            renderDangerZone();

            expect(await screen.findByRole('button', { name: 'Delete APIs' })).toBeInTheDocument();
        });

        it('offers no Delete APIs to a user who cannot delete APIs', async () => {
            mockGetEnvironmentPermissions.mockResolvedValue(['environment-api-r']);
            mockHasFederatedApis.mockResolvedValue(false);

            renderDangerZone();

            await waitFor(() => expect(screen.getByRole('button', { name: 'Delete Integration' })).toBeEnabled());
            expect(mockGetEnvironmentPermissions).toHaveBeenCalledWith('env-1');
            expect(screen.queryByRole('button', { name: 'Delete APIs' })).toBeNull();
        });

        it('enables Delete APIs and describes what it deletes when the integration has a federated API', async () => {
            renderDangerZone();

            const deleteApisButton = await screen.findByRole('button', { name: 'Delete APIs' });
            await waitFor(() => expect(deleteApisButton).toBeEnabled());
            expect(deleteApisButton).toHaveAccessibleDescription(
                'Deletes the federated APIs of this integration. Published APIs are not deleted.',
            );
        });

        it.each([
            {
                scenario: 'the integration has no federated APIs',
                arrange: () => mockHasFederatedApis.mockResolvedValue(false),
                description: 'This integration has no federated APIs to delete.',
            },
            {
                scenario: 'the federated-APIs check is still pending',
                arrange: () => mockHasFederatedApis.mockReturnValue(neverResolve()),
                description: 'Checking whether this integration has federated APIs…',
            },
            {
                scenario: 'the federated-APIs check fails',
                arrange: () => mockHasFederatedApis.mockRejectedValue(new ApimApiError(500, 'Internal error')),
                description: 'Could not check whether this integration has federated APIs. Reload the page to try again.',
            },
        ])('keeps Delete APIs disabled while $scenario', async ({ arrange, description }) => {
            const user = userEvent.setup();
            arrange();

            renderDangerZone();

            const deleteApisButton = await screen.findByRole('button', { name: 'Delete APIs' });
            await waitFor(() => expect(deleteApisButton).toHaveAccessibleDescription(description));
            expect(deleteApisButton).toBeDisabled();
            await user.click(deleteApisButton);
            expect(screen.queryByRole('dialog')).toBeNull();
        });

        it('opens the Delete APIs confirmation dialog when Delete APIs is selected', async () => {
            const user = userEvent.setup();
            renderDangerZone();

            await openDeleteApisDialog(user);

            expect(mockDeleteFederatedApis).not.toHaveBeenCalled();
        });

        it('shows the deleted, not deleted and error counts when the confirmed delete succeeds', async () => {
            const user = userEvent.setup();
            mockDeleteFederatedApis.mockResolvedValue({ deleted: 2, skipped: 1, errors: 0 });
            renderDangerZone();

            await confirmDeleteApis(user);

            await waitFor(() =>
                expect(mockNotify.success).toHaveBeenCalledWith(
                    'Federated APIs have been deleted.\n• Deleted: 2\n• Not deleted: 1\n• Errors: 0',
                ),
            );
            expect(mockDeleteFederatedApis).toHaveBeenCalledWith('env-1', INTEGRATION.id);
        });

        it('enables Delete Integration once the confirmed delete leaves no federated APIs', async () => {
            const user = userEvent.setup();
            mockHasFederatedApis.mockResolvedValueOnce(true).mockResolvedValue(false);
            renderDangerZone();

            await confirmDeleteApis(user);

            await waitFor(() => expect(screen.getByRole('button', { name: 'Delete Integration' })).toBeEnabled());
            expect(mockHasFederatedApis).toHaveBeenCalledTimes(2);
        });

        it('shows an error notification carrying the API error message when the confirmed delete fails', async () => {
            const user = userEvent.setup();
            mockDeleteFederatedApis.mockRejectedValue(new ApimApiError(500, 'Boom', { httpStatus: 500, message: 'Boom' }));
            renderDangerZone();

            await confirmDeleteApis(user);

            await waitFor(() => expect(mockNotify.error).toHaveBeenCalledWith('Something went wrong! Boom'));
            expect(mockNotify.success).not.toHaveBeenCalled();
        });

        it.each([
            { outcome: 'succeeds', arrange: () => mockDeleteFederatedApis.mockResolvedValue({ deleted: 1, skipped: 0, errors: 0 }) },
            {
                outcome: 'fails',
                arrange: () =>
                    mockDeleteFederatedApis.mockRejectedValue(new ApimApiError(500, 'Boom', { httpStatus: 500, message: 'Boom' })),
            },
        ])('closes the Delete APIs dialog and stays on the page when the confirmed delete $outcome', async ({ arrange }) => {
            const user = userEvent.setup();
            arrange();
            renderDangerZone();

            await confirmDeleteApis(user);

            await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
            expect(mockDeleteFederatedApis).toHaveBeenCalledWith('env-1', INTEGRATION.id);
            expect(screen.getByTestId('location').textContent).toBe(CONFIGURATION_PATH);
        });

        it('shows Deleting… and blocks a second confirm while the Delete APIs request is in flight', async () => {
            const user = userEvent.setup();
            mockDeleteFederatedApis.mockReturnValue(neverResolve());
            renderDangerZone();
            const dialog = await openDeleteApisDialog(user);

            await user.click(within(dialog).getByRole('button', { name: 'Delete APIs' }));

            const pendingConfirmButton = await within(dialog).findByRole('button', { name: 'Deleting…' });
            expect(pendingConfirmButton).toBeDisabled();
            expect(within(dialog).getByRole('button', { name: 'Cancel' })).toBeDisabled();
            expect(screen.getByRole('dialog', { name: 'Delete APIs' })).toBeInTheDocument();
            expect(mockDeleteFederatedApis).toHaveBeenCalledTimes(1);
            expect(mockDeleteFederatedApis).toHaveBeenCalledWith('env-1', INTEGRATION.id);
        });

        it('sends no delete request when the Delete APIs confirmation dialog is cancelled', async () => {
            const user = userEvent.setup();
            renderDangerZone();
            const dialog = await openDeleteApisDialog(user);

            await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
            expect(mockDeleteFederatedApis).not.toHaveBeenCalled();
        });
    });
});
