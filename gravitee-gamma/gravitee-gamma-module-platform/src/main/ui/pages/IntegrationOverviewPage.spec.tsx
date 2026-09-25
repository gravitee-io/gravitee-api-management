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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { IntegrationOverviewPage } from './IntegrationOverviewPage';
import { getIntegration } from '../features/integrations/services/integrationDetail';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../features/integrations/services/integrationDetail', () => ({ getIntegration: jest.fn() }));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetIntegration = jest.mocked(getIntegration);
const mockNotifyError = jest.mocked(notify.error);

function renderIntegrationOverviewPage(integrationId: string) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`/integrations/${integrationId}`]}>
                <Routes>
                    <Route path="/integrations/:integrationId" element={<IntegrationOverviewPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('IntegrationOverviewPage', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
    });

    afterEach(() => jest.clearAllMocks());

    it('raises a single error toast carrying the failure when the integration fails to load', async () => {
        const failure = new Error('integration unavailable');
        mockGetIntegration.mockRejectedValue(failure);

        renderIntegrationOverviewPage('integration-a2a');

        await waitFor(() => expect(mockNotifyError).toHaveBeenCalledWith(failure, expect.stringMatching(/\S/)));
        expect(mockNotifyError).toHaveBeenCalledTimes(1);
        expect(mockGetIntegration).toHaveBeenCalledWith('env-1', 'integration-a2a');
    });

    it('shows only the load failure message, with no integration details, when the integration fails to load', async () => {
        mockGetIntegration.mockRejectedValue(new Error('integration unavailable'));

        renderIntegrationOverviewPage('integration-a2a');

        const overview = screen.getByTestId('integration-overview-page');
        await waitFor(() => expect(overview.textContent).toBe('Integration could not be loaded. Please refresh and try again.'));
        expect(within(overview).queryByRole('heading')).toBeNull();
    });
});
