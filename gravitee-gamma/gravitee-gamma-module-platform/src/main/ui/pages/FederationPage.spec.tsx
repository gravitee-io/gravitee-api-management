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
import { dataTableHarness } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { FederationPage } from './FederationPage';
import { listIntegrations } from '../features/integrations/services/integrationList';
import type { IntegrationsResponse } from '../features/integrations/types/integration';
import { DEFAULT_INTEGRATION_LIST_PAGE_SIZE, TABLE_PAGE_SIZE_OPTIONS } from '../features/integrations/utils/paginationConstants';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../features/integrations/services/integrationList', () => ({ listIntegrations: jest.fn() }));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockListIntegrations = jest.mocked(listIntegrations);
const mockNotifyError = jest.mocked(notify.error);

const EMPTY_RESPONSE: IntegrationsResponse = {
    data: [],
    pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
};

const SINGLE_PAGE_RESPONSE: IntegrationsResponse = {
    data: [
        { id: 'int-1', name: 'Acme Gateway', provider: 'aws-api-gateway' },
        { id: 'int-2', name: 'Broker North', provider: 'solace' },
        { id: 'int-3', name: 'Partner Apigee', provider: 'apigee' },
    ],
    pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 3, totalCount: 3 },
};

function pageOfIntegrations(page: number, names: string[]): IntegrationsResponse {
    return {
        data: names.map((name, index) => ({ id: `${name}-${index}`, name, provider: 'solace' })),
        pagination: { page, perPage: 10, pageCount: 3, pageItemsCount: names.length, totalCount: 23 },
    };
}

function integrationsTable() {
    return dataTableHarness({ within: screen.getByRole('region', { name: 'Integrations' }) });
}

function singlePageNamed(name: string): IntegrationsResponse {
    return {
        data: [{ id: `${name}-1`, name, provider: 'solace' }],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
    };
}

function renderFederationPage(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
    return render(<FederationPage />, {
        wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
    });
}

describe('FederationPage', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;

        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
        Element.prototype.scrollIntoView = jest.fn();
    });

    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockListIntegrations.mockResolvedValue(EMPTY_RESPONSE);
    });

    afterEach(() => jest.clearAllMocks());

    it('requests the first page of integrations for the current environment', async () => {
        renderFederationPage();

        await waitFor(() => expect(mockListIntegrations).toHaveBeenCalledTimes(1));
        expect(mockListIntegrations).toHaveBeenCalledWith('env-1', { page: 1, perPage: 10 });
    });

    it('renders one row per integration returned for the page', async () => {
        mockListIntegrations.mockResolvedValue(SINGLE_PAGE_RESPONSE);

        renderFederationPage();

        await waitFor(() => expect(integrationsTable().getRowCount()).toBe(SINGLE_PAGE_RESPONSE.data.length));
    });

    it('replaces the rows with the next page when the user advances the page', async () => {
        mockListIntegrations.mockImplementation((_environmentId, { page }) =>
            Promise.resolve(page === 1 ? pageOfIntegrations(1, ['Alpha', 'Bravo']) : pageOfIntegrations(2, ['Charlie', 'Delta'])),
        );

        renderFederationPage();

        await waitFor(() => expect(integrationsTable().queryRow('Alpha')).not.toBeNull());

        await userEvent.click(screen.getByRole('button', { name: 'Next page' }));

        await waitFor(() => expect(mockListIntegrations).toHaveBeenCalledWith('env-1', { page: 2, perPage: 10 }));
        await waitFor(() => expect(integrationsTable().queryRow('Charlie')).not.toBeNull());
        expect(integrationsTable().queryRow('Delta')).not.toBeNull();
        expect(integrationsTable().queryRow('Alpha')).toBeNull();
        expect(integrationsTable().queryRow('Bravo')).toBeNull();
    });

    it('returns to the first page when the user changes the page size', async () => {
        mockListIntegrations.mockImplementation((_environmentId, { page }) =>
            Promise.resolve(pageOfIntegrations(page, page === 1 ? ['Alpha', 'Bravo'] : ['Charlie', 'Delta'])),
        );

        renderFederationPage();

        await waitFor(() => expect(integrationsTable().queryRow('Alpha')).not.toBeNull());
        await userEvent.click(screen.getByRole('button', { name: 'Next page' }));
        await waitFor(() => expect(mockListIntegrations).toHaveBeenCalledWith('env-1', { page: 2, perPage: 10 }));

        await userEvent.click(screen.getByRole('combobox', { name: 'Items per page' }));
        await userEvent.click(screen.getByRole('option', { name: '25' }));

        await waitFor(() => expect(mockListIntegrations).toHaveBeenCalledWith('env-1', { page: 1, perPage: 25 }));
        expect(mockListIntegrations).not.toHaveBeenCalledWith('env-1', { page: 2, perPage: 25 });
    });

    it('selects the default integration list page size in the items per page control', async () => {
        const totalCountExceedingEveryPageSize = Math.max(...TABLE_PAGE_SIZE_OPTIONS) + 1;
        mockListIntegrations.mockResolvedValue({
            data: [{ id: 'int-1', name: 'Acme Gateway', provider: 'solace' }],
            pagination: {
                page: 1,
                perPage: DEFAULT_INTEGRATION_LIST_PAGE_SIZE,
                pageCount: 2,
                pageItemsCount: 1,
                totalCount: totalCountExceedingEveryPageSize,
            },
        });

        renderFederationPage();

        await waitFor(() =>
            expect(screen.getByRole('combobox', { name: 'Items per page' }).textContent).toBe(String(DEFAULT_INTEGRATION_LIST_PAGE_SIZE)),
        );
    });

    it('issues no request until an environment is resolved', async () => {
        mockUseEnvironment.mockReturnValue(undefined);

        renderFederationPage();
        await act(async () => {});

        expect(mockListIntegrations).not.toHaveBeenCalled();
    });

    it('refetches instead of reusing the cached page when the environment changes', async () => {
        mockListIntegrations.mockImplementation(environmentId =>
            Promise.resolve(singlePageNamed(environmentId === 'env-1' ? 'Env One Gateway' : 'Env Two Gateway')),
        );
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

        const { rerender } = renderFederationPage(queryClient);

        await waitFor(() => expect(integrationsTable().queryRow('Env One Gateway')).not.toBeNull());

        mockUseEnvironment.mockReturnValue({ id: 'env-2' });
        rerender(<FederationPage />);

        await waitFor(() => expect(integrationsTable().queryRow('Env Two Gateway')).not.toBeNull());
        expect(integrationsTable().queryRow('Env One Gateway')).toBeNull();
        expect(mockListIntegrations).toHaveBeenCalledWith('env-2', { page: 1, perPage: 10 });
    });

    it('replaces the table with the empty state when the environment has no integrations', async () => {
        renderFederationPage();

        expect(await screen.findByText('No integrations yet')).not.toBeNull();
        expect(
            screen.getByText('Create an integration to start importing APIs and event streams from a 3rd-party provider.'),
        ).not.toBeNull();
        expect(screen.queryByRole('region', { name: 'Integrations' })).toBeNull();
        expect(screen.getByRole('heading', { name: 'Integrations' })).not.toBeNull();
        expect(screen.queryByText('Integrations could not be loaded. Please refresh and try again.')).toBeNull();
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('keeps the empty state off the screen while the first page is still loading', () => {
        mockListIntegrations.mockReturnValue(new Promise(() => {}));

        renderFederationPage();

        expect(screen.queryByText('No integrations yet')).toBeNull();
        expect(screen.getByRole('region', { name: 'Integrations' })).not.toBeNull();
    });

    it('raises a single error toast carrying the failure when the integrations request fails', async () => {
        const failure = new Error('integrations unavailable');
        mockListIntegrations.mockRejectedValue(failure);

        renderFederationPage();

        await waitFor(() => expect(mockNotifyError).toHaveBeenCalledWith(failure, expect.stringMatching(/\S/)));
        expect(mockNotifyError).toHaveBeenCalledTimes(1);
    });

    it('replaces the table with an inline error region when the integrations request fails', async () => {
        mockListIntegrations.mockRejectedValue(new Error('integrations unavailable'));

        renderFederationPage();

        expect(await screen.findByText('Integrations could not be loaded. Please refresh and try again.')).not.toBeNull();
        expect(screen.queryByRole('region', { name: 'Integrations' })).toBeNull();
    });

    it('keeps the page header on screen when the integrations request fails', async () => {
        mockListIntegrations.mockRejectedValue(new Error('integrations unavailable'));

        renderFederationPage();

        expect(await screen.findByText('Integrations could not be loaded. Please refresh and try again.')).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Integrations' })).not.toBeNull();
        expect(
            screen.getByText(
                'Connect to third-party API gateways and event brokers to create a unified control plane and API portal with Gravitee.',
            ),
        ).not.toBeNull();
    });

    it('keeps the empty state off the screen when the integrations request fails', async () => {
        mockListIntegrations.mockRejectedValue(new Error('integrations unavailable'));

        renderFederationPage();

        await waitFor(() => expect(mockNotifyError).toHaveBeenCalled());
        expect(screen.queryByText('No integrations yet')).toBeNull();
        expect(screen.queryByText('Create an integration to start importing APIs and event streams from a 3rd-party provider.')).toBeNull();
    });
});
