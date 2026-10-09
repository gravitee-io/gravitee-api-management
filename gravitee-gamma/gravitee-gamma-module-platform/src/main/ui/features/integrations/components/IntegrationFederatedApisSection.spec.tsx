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
jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../../users/testing/buildModuleNavPathForTests'));

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { IntegrationFederatedApisSection } from './IntegrationFederatedApisSection';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { listFederatedApis } from '../services/integrationDetail';
import type { Integration } from '../types/integration';
import { ENVIRONMENT_INTEGRATION_CREATE_PERMISSION, ENVIRONMENT_INTEGRATION_UPDATE_PERMISSION } from '../utils/integrationPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../../../shared/hooks/useEnvironmentPermissions', () => ({
    useHasEnvironmentPermission: jest.fn(),
}));
jest.mock('../services/integrationDetail', () => ({ listFederatedApis: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseHasEnvironmentPermission = jest.mocked(useHasEnvironmentPermission);
const mockListFederatedApis = jest.mocked(listFederatedApis);

const INTEGRATION: Integration = {
    id: 'int-1',
    name: 'AWS integration',
    provider: 'aws-api-gateway',
    agentStatus: 'DISCONNECTED',
};

function renderSection(integration: Integration = INTEGRATION) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/environments/test/platform/integrations/int-1']}>
                <IntegrationFederatedApisSection integration={integration} />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('IntegrationFederatedApisSection', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockUseHasEnvironmentPermission.mockImplementation(
            anyOf => anyOf.includes(ENVIRONMENT_INTEGRATION_CREATE_PERMISSION) || anyOf.includes(ENVIRONMENT_INTEGRATION_UPDATE_PERMISSION),
        );
        mockListFederatedApis.mockResolvedValue({
            data: [],
            pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('shows the APIs section with a disabled Discover button when the agent is disconnected', async () => {
        renderSection();

        const section = await screen.findByTestId('integration-federated-apis');
        expect(within(section).getByText('APIs')).toBeInTheDocument();
        expect(within(section).getByTestId('discover-button')).toBeDisabled();
        expect(await screen.findByTestId('integration-federated-apis-empty')).toBeInTheDocument();
        expect(screen.getByText('No APIs created')).toBeInTheDocument();
    });

    it('lists federated APIs with links into the APIM general page', async () => {
        mockListFederatedApis.mockResolvedValue({
            data: [{ id: 'api-1', name: 'Parity-Federated-API', version: 'v1' }],
            pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
        });

        renderSection({ ...INTEGRATION, agentStatus: 'CONNECTED' });

        expect(await screen.findByRole('link', { name: 'Parity-Federated-API (v1)' })).toHaveAttribute(
            'href',
            '/environments/test/apim/apis/api-1/general',
        );
        expect(screen.getByRole('link', { name: 'Discover' })).toHaveAttribute(
            'href',
            '/environments/test/platform/integrations/int-1/discover',
        );
        await waitFor(() => expect(mockListFederatedApis).toHaveBeenCalledWith('env-1', 'int-1', 1, 10));
    });

    it('hides Discover when the user cannot create integrations', async () => {
        mockUseHasEnvironmentPermission.mockImplementation(anyOf => anyOf.includes(ENVIRONMENT_INTEGRATION_UPDATE_PERMISSION));

        renderSection({ ...INTEGRATION, agentStatus: 'CONNECTED' });

        await screen.findByTestId('integration-federated-apis');
        expect(screen.queryByTestId('discover-button')).toBeNull();
    });

    it('hides the configure action when the user lacks environment-integration-u', async () => {
        mockUseHasEnvironmentPermission.mockImplementation(anyOf => anyOf.includes(ENVIRONMENT_INTEGRATION_CREATE_PERMISSION));
        mockListFederatedApis.mockResolvedValue({
            data: [{ id: 'api-1', name: 'Parity-Federated-API', version: 'v1' }],
            pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
        });

        renderSection({ ...INTEGRATION, agentStatus: 'CONNECTED' });

        expect(await screen.findByRole('link', { name: 'Parity-Federated-API (v1)' })).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Configure federated API' })).toBeNull();
    });
});
