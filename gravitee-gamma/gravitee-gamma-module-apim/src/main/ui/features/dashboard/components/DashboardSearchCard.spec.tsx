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
import userEvent from '@testing-library/user-event';
import { http } from 'msw';

import { DashboardSearchCard } from './DashboardSearchCard';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { captureTimeoutSignals, settleOutstandingRequests, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
    useHasFeature: jest.fn(),
}));

const mockUseHasFeature = jest.mocked(useHasFeature);

const ORG_CONSOLE_PATH = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/console`;
const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const PRODUCT_SEARCH_PATH = `${TEST_V2_BASE}/api-products/_search`;
const PROXY_TYPES = ['V4_HTTP_PROXY', 'V4_TCP_PROXY'];
const PROXY_AND_FEDERATED = ['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED'];

const SEARCH_TERM = 'Orders';
const SEARCH_PLACEHOLDER = 'Search APIs, API Products…';
const FEDERATED_API = {
    id: 'federated-1',
    name: 'Federated Orders',
    apiVersion: '1.0',
    type: 'PROXY',
    definitionVersion: 'FEDERATED',
    originContext: { origin: 'INTEGRATION', provider: 'solace' },
};

const EMPTY_RESULTS = { data: [], pagination: { page: 1, perPage: 5, pageCount: 0, totalCount: 0 } };
const NO_RESULTS_MESSAGE = /No results for/;
const SEARCHING_MESSAGE = 'Searching…';

function stubProductSearch() {
    return trackHandler('post', PRODUCT_SEARCH_PATH, EMPTY_RESULTS);
}

function renderCard() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <DashboardSearchCard onNavigateToApi={jest.fn()} onNavigateToProduct={jest.fn()} />
        </QueryClientProvider>,
    );
}

describe('DashboardSearchCard', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockUseHasFeature.mockReturnValue(true);
    });

    afterEach(() => jest.restoreAllMocks());

    it('asks the search for federated proxies and lists the federated match once the federation gate resolves', async () => {
        const user = userEvent.setup();
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        const apiSearches = trackHandler('post', SEARCH_PATH, {
            data: [FEDERATED_API],
            pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: 1 },
        });
        stubProductSearch();

        renderCard();
        await user.type(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), SEARCH_TERM);

        // The term passes through useDeferredValue, so the row is awaited rather than read synchronously.
        expect(await screen.findByText(FEDERATED_API.name)).not.toBeNull();
        expect(apiSearches.lastCall?.body).toEqual({ query: SEARCH_TERM, apiTypes: PROXY_AND_FEDERATED });
    });

    it('asks the search for proxies alone when the federation gate has resolved off', async () => {
        const user = userEvent.setup();
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: false } });
        const apiSearches = trackHandler('post', SEARCH_PATH, EMPTY_RESULTS);
        stubProductSearch();

        renderCard();
        await user.type(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), SEARCH_TERM);

        // Each keystroke issues its own search, so the assertion waits for the one carrying the whole term.
        await waitFor(() => expect(apiSearches.lastCall?.body).toEqual({ query: SEARCH_TERM, apiTypes: PROXY_TYPES }));
    });

    it('keeps saying it is searching rather than announcing no results while the federation gate is still resolving', async () => {
        const user = userEvent.setup();
        const timeoutControllers = captureTimeoutSignals();
        server.use(http.get(ORG_CONSOLE_PATH, () => new Promise(() => {})));
        const apiSearches = trackHandler('post', SEARCH_PATH, EMPTY_RESULTS);
        const productSearches = stubProductSearch();

        renderCard();
        await user.type(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), SEARCH_TERM);

        await waitFor(() => expect(productSearches.callCount).toBeGreaterThan(0));
        await waitFor(() => expect(timeoutControllers.length).toBeGreaterThan(0));
        await settleOutstandingRequests();
        expect(apiSearches.callCount).toBe(0);
        expect(screen.queryByText(NO_RESULTS_MESSAGE)).toBeNull();
        expect(screen.getByText(SEARCHING_MESSAGE)).not.toBeNull();
        // Firing the bound the gate opened stands in for waiting out its wall-clock delay.
        timeoutControllers.forEach(controller => controller.abort(new DOMException('signal timed out', 'TimeoutError')));

        // Announcing the empty result only once the API search has answered is what makes the assertion
        // above read as "still waiting" rather than "the card never says this at all".
        expect(await screen.findByText(NO_RESULTS_MESSAGE)).not.toBeNull();
        expect(apiSearches.callCount).toBe(1);
    });
});
