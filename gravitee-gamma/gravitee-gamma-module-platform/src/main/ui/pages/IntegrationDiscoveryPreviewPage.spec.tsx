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
import { toast } from '@gravitee/graphene-core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { IntegrationDiscoveryPreviewPage } from './IntegrationDiscoveryPreviewPage';
import { getIntegration, previewIntegration } from '../features/integrations/services/integrationDetail';
import type { Integration, IntegrationPreview } from '../features/integrations/types/integration';
import { integrationKeys } from '../features/integrations/utils/queryKeys';
import { ApimApiError, resetApimClientForTests } from '../shared/api/apimClient';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../features/users/testing/buildModuleNavPathForTests'));
jest.mock('../features/integrations/services/integrationDetail', () => ({
    ...jest.requireActual('../features/integrations/services/integrationDetail'),
    getIntegration: jest.fn(),
    previewIntegration: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetIntegration = jest.mocked(getIntegration);
const mockPreviewIntegration = jest.mocked(previewIntegration);

const CONNECTED_INTEGRATION: Integration = {
    id: 'integration-1',
    name: 'Payments gateway',
    provider: 'aws-api-gateway',
    agentStatus: 'CONNECTED',
};
const DISCONNECTED_INTEGRATION: Integration = { ...CONNECTED_INTEGRATION, agentStatus: 'DISCONNECTED' };
const EMPTY_PREVIEW: IntegrationPreview = { isPartiallyDiscovered: false, totalCount: 0, newCount: 0, updateCount: 0, apis: [] };
const NEW_AND_UPDATE_PREVIEW: IntegrationPreview = {
    isPartiallyDiscovered: false,
    totalCount: 2,
    newCount: 1,
    updateCount: 1,
    apis: [
        { id: 'api-n', name: 'Inventory', state: 'NEW' },
        { id: 'api-u', name: 'Shipping', state: 'UPDATE' },
    ],
};
const INGEST_URL = 'https://apim.test/management/v2/environments/env-1/integrations/integration-1/_ingest';
const INGESTION_FAILED = 'Ingestion failed. Please check your settings and try again:';

function LocationProbe() {
    return <span data-testid="location">{useLocation().pathname}</span>;
}

function renderDiscoveryPreviewPage(
    integrationId: string,
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } }),
) {
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`/integrations/${integrationId}/discover`]}>
                <LocationProbe />
                <Routes>
                    <Route path="integrations">
                        <Route index element={<p>Integrations list</p>} />
                        <Route path=":integrationId">
                            <Route index element={<p>Integration overview</p>} />
                            <Route path="discover" element={<IntegrationDiscoveryPreviewPage />} />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

function jsonResponse(body: unknown, status = 200) {
    return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }));
}

// The API client resolves /constants.json then /ui/bootstrap before any environment-scoped call, so both answer
// here for the _ingest request to reach the network stub.
function spyOnIngestFetch(ingestResponse: () => Promise<Response>) {
    resetApimClientForTests();
    return jest.spyOn(global, 'fetch').mockImplementation(input => {
        const url = String(input);
        if (url.endsWith('/constants.json')) return jsonResponse({ gammaBaseURL: 'https://apim.test/gamma' });
        if (url.endsWith('/ui/bootstrap')) {
            return jsonResponse({
                managementBaseURL: 'https://apim.test/management',
                gammaBaseURL: 'https://apim.test/gamma',
                organizationId: 'org-1',
            });
        }
        if (url === INGEST_URL) return ingestResponse();
        return jsonResponse({ message: 'Not found' }, 404);
    });
}

function ingestRequestBodies(fetchSpy: jest.SpyInstance): unknown[] {
    return fetchSpy.mock.calls
        .filter(([input, init]) => String(input) === INGEST_URL && init?.method === 'POST')
        .map(([, init]) => JSON.parse(String(init?.body)));
}

function renderDiscoveredNewAndUpdateApis() {
    mockGetIntegration.mockResolvedValue(CONNECTED_INTEGRATION);
    mockPreviewIntegration.mockResolvedValue(NEW_AND_UPDATE_PREVIEW);
    renderDiscoveryPreviewPage(CONNECTED_INTEGRATION.id);
}

async function proceedWithTheNewApisOnly() {
    fireEvent.click(await screen.findByRole('switch', { name: 'APIs to update' }));
    fireEvent.click(screen.getByRole('button', { name: 'Proceed' }));
}

function errorNotificationTexts(toastError: jest.SpyInstance): string[] {
    return toastError.mock.calls.map(([message]) => String(message));
}

describe('IntegrationDiscoveryPreviewPage', () => {
    let toastError: jest.SpyInstance;

    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockPreviewIntegration.mockReturnValue(new Promise(() => undefined));
        toastError = jest.spyOn(toast, 'error').mockImplementation(() => '');
    });

    afterEach(() => {
        jest.restoreAllMocks();
        jest.clearAllMocks();
    });

    it('returns to the integration overview with an agent-disconnected error when the agent is disconnected', async () => {
        mockGetIntegration.mockResolvedValue(DISCONNECTED_INTEGRATION);

        renderDiscoveryPreviewPage(DISCONNECTED_INTEGRATION.id);

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations/integration-1'));
        expect(screen.getByText('Integration overview')).toBeInTheDocument();
        expect(errorNotificationTexts(toastError)).toEqual(['Agent is DISCONNECTED, make sure your Agent is CONNECTED']);
    });

    // A connected copy cached by an earlier overview visit must not let discovery start before the fresh status lands.
    it('requests no discovery for a disconnected agent even when a connected copy of the integration is cached', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        queryClient.setQueryData(integrationKeys.detail('env-1', CONNECTED_INTEGRATION.id), CONNECTED_INTEGRATION);
        mockGetIntegration.mockResolvedValue(DISCONNECTED_INTEGRATION);

        renderDiscoveryPreviewPage(DISCONNECTED_INTEGRATION.id, queryClient);

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations/integration-1'));
        expect(mockGetIntegration).toHaveBeenCalledWith('env-1', 'integration-1');
        expect(mockPreviewIntegration).not.toHaveBeenCalled();
    });

    it('returns to the integration overview without an error or a discovery request when the integration has no agent', async () => {
        jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const { agentStatus: _agentStatus, ...integrationWithoutAgent } = CONNECTED_INTEGRATION;
        mockGetIntegration.mockResolvedValue(integrationWithoutAgent);

        renderDiscoveryPreviewPage(CONNECTED_INTEGRATION.id);

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations/integration-1'));
        expect(errorNotificationTexts(toastError)).toEqual([]);
        expect(mockPreviewIntegration).not.toHaveBeenCalled();
    });

    it.each([
        ['403 to the Integrations list', new ApimApiError(403, 'Forbidden'), '/integrations'],
        ['500 to the integration overview', new ApimApiError(500, 'Internal Server Error'), '/integrations/integration-1'],
    ])('returns an integration load failure with %s without its own error notification', async (_case, loadError, expectedLocation) => {
        mockGetIntegration.mockRejectedValue(loadError);

        renderDiscoveryPreviewPage(CONNECTED_INTEGRATION.id);

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe(expectedLocation));
        expect(errorNotificationTexts(toastError)).toEqual([]);
        expect(mockPreviewIntegration).not.toHaveBeenCalled();
    });

    it('shows a gathering-data message while discovery runs', async () => {
        mockGetIntegration.mockResolvedValue(CONNECTED_INTEGRATION);

        renderDiscoveryPreviewPage(CONNECTED_INTEGRATION.id);

        await waitFor(() => expect(mockPreviewIntegration).toHaveBeenCalledWith('env-1', 'integration-1'));
        expect(screen.getByText("We're gathering your data")).toBeInTheDocument();
    });

    it('shows the discovered APIs of a connected integration', async () => {
        mockGetIntegration.mockResolvedValue(CONNECTED_INTEGRATION);
        mockPreviewIntegration.mockResolvedValue({ ...EMPTY_PREVIEW, isPartiallyDiscovered: true });

        renderDiscoveryPreviewPage(CONNECTED_INTEGRATION.id);

        expect(await screen.findByText('Partial API Discovery Warning')).toBeInTheDocument();
        expect(screen.queryByText("We're gathering your data")).not.toBeInTheDocument();
        expect(screen.getByTestId('location').textContent).toBe('/integrations/integration-1/discover');
    });

    it('runs discovery again when the user reopens the discovery preview', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockGetIntegration.mockResolvedValue(CONNECTED_INTEGRATION);
        mockPreviewIntegration.mockResolvedValue(EMPTY_PREVIEW);
        const firstVisit = renderDiscoveryPreviewPage(CONNECTED_INTEGRATION.id, queryClient);
        await screen.findByText('No assets found');
        firstVisit.unmount();
        await waitFor(() => expect(queryClient.getQueryData(integrationKeys.preview('env-1', CONNECTED_INTEGRATION.id))).toBeUndefined());

        renderDiscoveryPreviewPage(CONNECTED_INTEGRATION.id, queryClient);

        await waitFor(() => expect(mockPreviewIntegration).toHaveBeenCalledTimes(2));
    });

    it('starts ingesting the selected APIs, announces it and returns to the integration overview', async () => {
        const toastSuccess = jest.spyOn(toast, 'success').mockImplementation(() => '');
        const fetchSpy = spyOnIngestFetch(() => jsonResponse({ status: 'PENDING' }));
        renderDiscoveredNewAndUpdateApis();

        await proceedWithTheNewApisOnly();

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations/integration-1'));
        expect(ingestRequestBodies(fetchSpy)).toEqual([{ apiIds: ['api-n'] }]);
        expect(toastSuccess.mock.calls.map(([message]) => message)).toEqual([
            'API ingestion is in progress. The process should only take a few minute to complete. Come back shortly!',
        ]);
    });

    it('disables Proceed while the ingestion request is in flight', async () => {
        jest.spyOn(toast, 'success').mockImplementation(() => '');
        spyOnIngestFetch(() => new Promise(() => undefined));
        renderDiscoveredNewAndUpdateApis();

        await proceedWithTheNewApisOnly();

        await waitFor(() => expect(screen.getByRole('button', { name: 'Proceed' })).toHaveProperty('disabled', true));
    });

    it('shows the ingestion failure reason when _ingest fails with a 500 with a message', async () => {
        jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        spyOnIngestFetch(() => jsonResponse({ message: 'Provider unreachable' }, 500));
        renderDiscoveredNewAndUpdateApis();

        await proceedWithTheNewApisOnly();

        await waitFor(() => expect(errorNotificationTexts(toastError)).toEqual([`${INGESTION_FAILED} Provider unreachable`]));
    });

    it('returns to the integration overview when ingestion fails to start', async () => {
        jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        spyOnIngestFetch(() => Promise.reject(new TypeError('Failed to fetch')));
        renderDiscoveredNewAndUpdateApis();

        await proceedWithTheNewApisOnly();

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations/integration-1'));
        expect(screen.getByText('Integration overview')).toBeInTheDocument();
    });
});
