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
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { IntegrationDiscoveryPage } from './IntegrationDiscoveryPage';
import { getIntegration, ingestIntegration, previewIntegration } from '../features/integrations/services/integrationDetail';
import { integrationKeys } from '../features/integrations/utils/queryKeys';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../features/integrations/services/integrationDetail', () => ({
    getIntegration: jest.fn(),
    previewIntegration: jest.fn(),
    ingestIntegration: jest.fn(),
}));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetIntegration = jest.mocked(getIntegration);
const mockPreviewIntegration = jest.mocked(previewIntegration);
const mockIngestIntegration = jest.mocked(ingestIntegration);
const mockNotifySuccess = jest.mocked(notify.success);

function LocationProbe() {
    return <span data-testid="location">{useLocation().pathname}</span>;
}

function renderDiscoveryPage(integrationId: string) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return {
        queryClient,
        ...render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[`/integrations/${integrationId}/discover`]}>
                    <LocationProbe />
                    <Routes>
                        <Route path="/integrations/:integrationId" element={<span>Overview</span>} />
                        <Route path="/integrations/:integrationId/discover" element={<IntegrationDiscoveryPage />} />
                        <Route path="/integrations" element={<span>Integrations list</span>} />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>,
        ),
    };
}

describe('IntegrationDiscoveryPage', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockGetIntegration.mockResolvedValue({
            id: 'int-1',
            name: 'AWS integration',
            provider: 'aws-api-gateway',
            agentStatus: 'CONNECTED',
        });
        mockPreviewIntegration.mockResolvedValue({
            totalCount: 1,
            newCount: 1,
            updateCount: 0,
            apis: [{ id: 'api-1', name: 'Orders', version: 'v1', state: 'NEW' }],
            isPartiallyDiscovered: false,
        });
        mockIngestIntegration.mockResolvedValue({ status: 'PENDING' });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('lists previewed APIs and ingests the selected ones', async () => {
        renderDiscoveryPage('int-1');

        expect(await screen.findByText('Orders (v1)')).toBeInTheDocument();
        fireEvent.click(screen.getByTestId('discover-proceed-button'));

        await waitFor(() => expect(mockIngestIntegration).toHaveBeenCalledWith('env-1', 'int-1', ['api-1']));
        expect(mockNotifySuccess).toHaveBeenCalled();
        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations/int-1'));
    });

    it('redirects to the overview when the agent is disconnected', async () => {
        mockGetIntegration.mockResolvedValue({
            id: 'int-1',
            name: 'AWS integration',
            provider: 'aws-api-gateway',
            agentStatus: 'DISCONNECTED',
        });

        renderDiscoveryPage('int-1');

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations/int-1'));
        expect(mockPreviewIntegration).not.toHaveBeenCalled();
    });

    it('keeps New/Update checkbox choices after a preview refetch', async () => {
        mockPreviewIntegration.mockResolvedValue({
            totalCount: 2,
            newCount: 1,
            updateCount: 1,
            apis: [
                { id: 'api-1', name: 'Orders', version: 'v1', state: 'NEW' },
                { id: 'api-2', name: 'Catalog', version: 'v1', state: 'UPDATE' },
            ],
            isPartiallyDiscovered: false,
        });

        const { queryClient } = renderDiscoveryPage('int-1');

        expect(await screen.findByText('Orders (v1)')).toBeInTheDocument();
        expect(screen.getByText('Catalog (v1)')).toBeInTheDocument();

        fireEvent.click(screen.getByLabelText(/New/));
        expect(screen.queryByText('Orders (v1)')).not.toBeInTheDocument();
        expect(screen.getByText('Catalog (v1)')).toBeInTheDocument();

        mockPreviewIntegration.mockResolvedValue({
            totalCount: 2,
            newCount: 1,
            updateCount: 1,
            apis: [
                { id: 'api-1', name: 'Orders', version: 'v1', state: 'NEW' },
                { id: 'api-2', name: 'Catalog', version: 'v1', state: 'UPDATE' },
            ],
            isPartiallyDiscovered: false,
        });

        await act(async () => {
            await queryClient.invalidateQueries({ queryKey: integrationKeys.preview('env-1', 'int-1') });
        });

        expect(await screen.findByText('Catalog (v1)')).toBeInTheDocument();
        expect(screen.queryByText('Orders (v1)')).not.toBeInTheDocument();
    });
});
