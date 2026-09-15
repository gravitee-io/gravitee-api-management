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
import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { http } from 'msw';
import { MemoryRouter } from 'react-router-dom';

import { QuickStartPage } from './QuickStartPage';
import { OnboardingProvider } from '../../../app/onboarding';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { captureTimeoutSignals, settleOutstandingRequests, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
    useHasFeature: jest.fn(),
}));
jest.mock('@gravitee/gamma-modules-sdk/routing', () => ({
    useModuleRouting: () => ({ navigateToKey: jest.fn(), modulePrefix: 'apim' }),
}));

const mockUseHasFeature = jest.mocked(useHasFeature);

const ORG_CONSOLE_PATH = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/console`;
const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const PRODUCT_SEARCH_PATH = `${TEST_V2_BASE}/api-products/_search`;
const PROXY_AND_FEDERATED = ['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED'];

const FEDERATION_INCLUSIVE_TOTAL = 12;
const CREATE_PROXY_BUTTON = /create api proxy/i;

function countOf(totalCount: number) {
    return { data: [], pagination: { page: 1, perPage: 1, pageCount: 1, totalCount } };
}

function loadingPlaceholder(): Element | null {
    return document.body.querySelector('[data-slot="skeleton"]');
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <OnboardingProvider>
                    <QuickStartPage />
                </OnboardingProvider>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('QuickStartPage', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockUseHasFeature.mockReturnValue(true);
    });

    afterEach(() => jest.restoreAllMocks());

    it('shows the API count the federated search answered with once the federation gate resolves', async () => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        const apiSearches = trackHandler('post', SEARCH_PATH, countOf(FEDERATION_INCLUSIVE_TOTAL));
        trackHandler('post', PRODUCT_SEARCH_PATH, countOf(0));

        renderPage();

        expect(await screen.findByText(String(FEDERATION_INCLUSIVE_TOTAL))).not.toBeNull();
        expect(apiSearches.lastCall?.body).toEqual({ apiTypes: PROXY_AND_FEDERATED });
    });

    it('holds its loading placeholder rather than the empty landing while the federation gate is still resolving', async () => {
        const timeoutControllers = captureTimeoutSignals();
        server.use(http.get(ORG_CONSOLE_PATH, () => new Promise(() => {})));
        const apiSearches = trackHandler('post', SEARCH_PATH, countOf(0));
        const productSearches = trackHandler('post', PRODUCT_SEARCH_PATH, countOf(0));

        renderPage();

        // The product count settling first is what would let a page reading the API count as "nothing yet"
        // hand the user the empty landing before the API search has been allowed to run.
        await waitFor(() => expect(productSearches.callCount).toBeGreaterThan(0));
        await waitFor(() => expect(timeoutControllers.length).toBeGreaterThan(0));
        await settleOutstandingRequests();
        expect(apiSearches.callCount).toBe(0);
        expect(screen.queryByRole('button', { name: CREATE_PROXY_BUTTON })).toBeNull();
        expect(loadingPlaceholder()).not.toBeNull();
        // Firing the bound the gate opened stands in for waiting out its wall-clock delay.
        timeoutControllers.forEach(controller => controller.abort(new DOMException('signal timed out', 'TimeoutError')));

        // Landing on the empty state only once the search has answered is what makes the assertion
        // above read as "still waiting" rather than "the page never lands there at all".
        expect(await screen.findByRole('button', { name: CREATE_PROXY_BUTTON })).not.toBeNull();
        expect(apiSearches.callCount).toBe(1);
        expect(loadingPlaceholder()).toBeNull();
    });
});
