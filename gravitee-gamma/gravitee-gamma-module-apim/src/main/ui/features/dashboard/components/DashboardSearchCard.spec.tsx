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
import { licenseService } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { DashboardSearchCard } from './DashboardSearchCard';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { ENTERPRISE_LICENSE, OSS_LICENSE, TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { captureTimeoutSignals, settleOutstandingRequests, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
    licenseService: { subscribe: () => () => {}, getSnapshot: jest.fn() },
}));

const mockLicenseSnapshot = jest.mocked(licenseService.getSnapshot);

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

const NATIVE_API = {
    id: 'proxy-1',
    name: `${SEARCH_TERM} Proxy`,
    apiVersion: '2.0',
    type: 'PROXY',
    definitionVersion: 'V4',
};

const PROXY_API = {
    id: 'proxy-2',
    name: 'Orders Proxy',
    apiVersion: '2.0',
    type: 'PROXY',
    definitionVersion: 'V4',
};

const MATCHING_PRODUCT = { id: 'product-1', name: 'Orders Product' };

const EMPTY_RESULTS = { data: [], pagination: { page: 1, perPage: 5, pageCount: 0, totalCount: 0 } };
const NO_RESULTS_MESSAGE = /No results for/;
const SEARCHING_MESSAGE = 'Searching…';
const API_SECTION_LABEL = 'APIs';

function stubProductSearch(products: object[] = []) {
    return trackHandler('post', PRODUCT_SEARCH_PATH, {
        data: products,
        pagination: { page: 1, perPage: 5, pageCount: products.length, totalCount: products.length },
    });
}

/** Narrows a query to the card's APIs section, so a matched name is pinned to that section rather than to the whole card. */
function apisResultSection(): HTMLElement {
    const section = screen.getByText(API_SECTION_LABEL).closest('div');
    if (!(section instanceof HTMLElement)) throw new Error(`No result section rendered for "${API_SECTION_LABEL}"`);
    return section;
}

interface SearchBody {
    query: string;
    apiTypes: string[];
}

/** Answers with the federated match only for a federation-inclusive search, so a rendered row says which search produced it. */
function stubSearchByGate(): SearchBody[] {
    const bodies: SearchBody[] = [];

    server.use(
        http.post(SEARCH_PATH, async ({ request }) => {
            const body = (await request.clone().json()) as SearchBody;
            bodies.push(body);
            const data = body.apiTypes.includes('FEDERATED') ? [PROXY_API, FEDERATED_API] : [PROXY_API];
            return HttpResponse.json({ data, pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: data.length } });
        }),
    );

    return bodies;
}

type FederationOffCase = [string, { federation: { enabled: boolean } }, () => void];

const FEDERATION_OFF_CASES: FederationOffCase[] = [
    [
        'the federation setting is disabled',
        { federation: { enabled: false } },
        () => mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE),
    ],
    [
        'the license tier does not entitle federation',
        { federation: { enabled: true } },
        () => mockLicenseSnapshot.mockReturnValue(OSS_LICENSE),
    ],
];

/** Answers from the requested `apiTypes`, so a federated match the card never lists reads as excluded rather than never stubbed. */
function stubSearchByRequestedApiTypes(): SearchBody[] {
    const bodies: SearchBody[] = [];

    server.use(
        http.post(SEARCH_PATH, async ({ request }) => {
            const body = (await request.clone().json()) as SearchBody;
            bodies.push(body);
            const data = body.apiTypes.includes('FEDERATED') ? [NATIVE_API, FEDERATED_API] : [NATIVE_API];
            return HttpResponse.json({ data, pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: data.length } });
        }),
    );

    return bodies;
}

type NavigationCallback = 'onNavigateToApi' | 'onNavigateToProduct';

type NavigationCase = [string, string, NavigationCallback, string, NavigationCallback];

const NAVIGATION_CASES: NavigationCase[] = [
    ['API', 'Orders Proxy', 'onNavigateToApi', 'proxy-2', 'onNavigateToProduct'],
    ['API Product', 'Orders Product', 'onNavigateToProduct', 'product-1', 'onNavigateToApi'],
];

function renderCard() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const navigation = { onNavigateToApi: jest.fn(), onNavigateToProduct: jest.fn() };
    // A fresh element each time, since React bails out of re-rendering the identical one; the client is kept so a
    // cache filled under one gate state is still there under the next.
    const tree = () => (
        <QueryClientProvider client={queryClient}>
            <DashboardSearchCard onNavigateToApi={navigation.onNavigateToApi} onNavigateToProduct={navigation.onNavigateToProduct} />
        </QueryClientProvider>
    );
    const { rerender } = render(tree());
    return { navigation, rerenderCard: () => rerender(tree()) };
}

describe('DashboardSearchCard', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
    });

    afterEach(() => jest.restoreAllMocks());

    it('asks the search for federated proxies and lists the federated match under APIs once the federation gate resolves', async () => {
        const user = userEvent.setup();
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        const apiSearches = trackHandler('post', SEARCH_PATH, {
            data: [FEDERATED_API],
            pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: 1 },
        });
        stubProductSearch([MATCHING_PRODUCT]);

        renderCard();
        await user.type(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), SEARCH_TERM);

        // The term passes through useDeferredValue, so the section is awaited rather than read synchronously.
        const apisSection = await waitFor(apisResultSection);
        expect(within(apisSection).getByText(FEDERATED_API.name)).not.toBeNull();
        expect(apiSearches.lastCall?.body).toEqual({ query: SEARCH_TERM, apiTypes: PROXY_AND_FEDERATED });
    });

    it.each(FEDERATION_OFF_CASES)(
        'lists the native match and no federated match for the same term when %s',
        async (_reason, orgConsoleSettings, stubLicense) => {
            const user = userEvent.setup();
            stubLicense();
            trackHandler('get', ORG_CONSOLE_PATH, orgConsoleSettings);
            const bodies = stubSearchByRequestedApiTypes();
            stubProductSearch();

            renderCard();
            await user.type(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), SEARCH_TERM);

            // Each keystroke issues its own search, so the assertion waits for the one carrying the whole term.
            await waitFor(() => expect(bodies[bodies.length - 1]).toEqual({ query: SEARCH_TERM, apiTypes: PROXY_TYPES }));
            expect(await screen.findByText(NATIVE_API.name)).not.toBeNull();
            expect(screen.queryByText(FEDERATED_API.name)).toBeNull();
        },
    );

    it('re-searches with federated proxies rather than reusing the proxy-only matches when the federation gate turns on', async () => {
        const user = userEvent.setup();
        mockLicenseSnapshot.mockReturnValue(OSS_LICENSE);
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        const bodies = stubSearchByGate();
        stubProductSearch();

        const { rerenderCard } = renderCard();
        await user.type(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), SEARCH_TERM);

        // The term passes through useDeferredValue, so the section is awaited rather than read synchronously.
        await waitFor(() => expect(within(apisResultSection()).getByText(PROXY_API.name)).not.toBeNull());
        expect(screen.queryByText(FEDERATED_API.name)).toBeNull();

        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
        rerenderCard();

        await waitFor(() => expect(within(apisResultSection()).getByText(FEDERATED_API.name)).not.toBeNull());
        expect(bodies[bodies.length - 1]).toEqual({ query: SEARCH_TERM, apiTypes: PROXY_AND_FEDERATED });
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

    it.each(NAVIGATION_CASES)(
        'navigates to the clicked %s result by its own id and not to the other kind',
        async (_kind, rowName, expectedCallback, expectedId, otherCallback) => {
            const user = userEvent.setup();
            trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: false } });
            trackHandler('post', SEARCH_PATH, { data: [PROXY_API], pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: 1 } });
            stubProductSearch([MATCHING_PRODUCT]);

            const { navigation } = renderCard();
            await user.type(screen.getByPlaceholderText(SEARCH_PLACEHOLDER), SEARCH_TERM);
            await user.click(await screen.findByRole('button', { name: new RegExp(rowName) }));

            expect(navigation[expectedCallback]).toHaveBeenCalledTimes(1);
            expect(navigation[expectedCallback]).toHaveBeenCalledWith(expectedId);
            expect(navigation[otherCallback]).not.toHaveBeenCalled();
        },
    );

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
