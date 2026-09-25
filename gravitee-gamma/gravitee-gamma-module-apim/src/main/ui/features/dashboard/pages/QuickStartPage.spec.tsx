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
import { licenseService, useHasFeature } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, useLocation } from 'react-router-dom';

import { QuickStartPage } from './QuickStartPage';
import { OnboardingProvider } from '../../../app/onboarding';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { ENTERPRISE_LICENSE, OSS_LICENSE, TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { captureTimeoutSignals, settleOutstandingRequests, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
    useHasFeature: jest.fn(),
    licenseService: { subscribe: () => () => {}, getSnapshot: jest.fn() },
}));
jest.mock('@gravitee/gamma-modules-sdk/routing', () => ({
    useModuleRouting: () => ({ navigateToKey: jest.fn(), modulePrefix: 'apim' }),
}));

const mockUseHasFeature = jest.mocked(useHasFeature);
const mockLicenseSnapshot = jest.mocked(licenseService.getSnapshot);

const ORG_CONSOLE_PATH = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/console`;
const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const PRODUCT_SEARCH_PATH = `${TEST_V2_BASE}/api-products/_search`;
const PROXY_TYPES = ['V4_HTTP_PROXY', 'V4_TCP_PROXY'];
const PROXY_AND_FEDERATED = ['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED'];

const FEDERATION_INCLUSIVE_TOTAL = 12;
const FEDERATION_FREE_TOTAL = 4;
const CREATE_PROXY_BUTTON = /create api proxy/i;
const CREATE_PRODUCT_BUTTON = /create api product/i;
const UPGRADE_BUTTON = /upgrade to access/i;
const TOTAL_APIS_CARD = 'Total APIs';
const CURRENT_PATH_TEST_ID = 'current-path';
const LOAD_ERROR_MESSAGE = 'Failed to load dashboard data. Please refresh the page.';

type FederationOffCase = [string, { federation: { enabled: boolean } }, () => void];

const FEDERATION_OFF_CASES: FederationOffCase[] = [
    [
        'the federation setting is disabled',
        { federation: { enabled: false } },
        () => mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE),
    ],
    // Only the tier is downgraded, so the API Products licence — a `features` entry — stays on.
    [
        'the license tier does not entitle federation',
        { federation: { enabled: true } },
        () => mockLicenseSnapshot.mockReturnValue(OSS_LICENSE),
    ],
];

function countOf(totalCount: number) {
    return { data: [], pagination: { page: 1, perPage: 1, pageCount: 1, totalCount } };
}

function CurrentPath() {
    return <span data-testid={CURRENT_PATH_TEST_ID}>{useLocation().pathname}</span>;
}

function currentPath(): string {
    return screen.getByTestId(CURRENT_PATH_TEST_ID).textContent ?? '';
}

function loadingPlaceholder(): Element | null {
    return document.body.querySelector('[data-slot="skeleton"]');
}

/** Narrows a query to the Total APIs card, so a rendered number is pinned to it rather than to the page. */
function totalApisCardBody(): HTMLElement {
    const body = screen.getByText(TOTAL_APIS_CARD).closest('[data-slot="card-content"]');
    if (!(body instanceof HTMLElement)) throw new Error(`No card rendered for "${TOTAL_APIS_CARD}"`);
    return body;
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <OnboardingProvider>
                    <QuickStartPage />
                </OnboardingProvider>
                <CurrentPath />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('QuickStartPage', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockUseHasFeature.mockReturnValue(true);
        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
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

    it('shows the load error instead of the dashboard or the empty landing when the API count search fails', async () => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        server.use(http.post(SEARCH_PATH, () => HttpResponse.json({}, { status: 500 })));
        trackHandler('post', PRODUCT_SEARCH_PATH, countOf(0));

        renderPage();

        expect(await screen.findByText(LOAD_ERROR_MESSAGE)).not.toBeNull();
        expect(screen.queryByText(TOTAL_APIS_CARD)).toBeNull();
        expect(screen.queryByRole('button', { name: CREATE_PROXY_BUTTON })).toBeNull();
        expect(loadingPlaceholder()).toBeNull();
    });

    it.each(FEDERATION_OFF_CASES)(
        'shows the API count the proxy-only search answered with when %s',
        async (_reason, orgConsoleSettings, stubLicense) => {
            stubLicense();
            trackHandler('get', ORG_CONSOLE_PATH, orgConsoleSettings);
            const apiSearches = trackHandler('post', SEARCH_PATH, countOf(FEDERATION_FREE_TOTAL));
            trackHandler('post', PRODUCT_SEARCH_PATH, countOf(0));

            renderPage();

            expect(await screen.findByText(TOTAL_APIS_CARD)).not.toBeNull();
            expect(within(totalApisCardBody()).getByText(String(FEDERATION_FREE_TOTAL))).not.toBeNull();
            expect(apiSearches.lastCall?.body).toEqual({ apiTypes: PROXY_TYPES });
        },
    );

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

    it('offers upgrade prompts instead of API Product creation when API Products are not licensed', async () => {
        mockUseHasFeature.mockReturnValue(false);
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        trackHandler('post', SEARCH_PATH, countOf(FEDERATION_INCLUSIVE_TOTAL));
        trackHandler('post', PRODUCT_SEARCH_PATH, countOf(0));

        renderPage();

        expect(await screen.findByText(TOTAL_APIS_CARD)).not.toBeNull();
        expect(screen.getAllByRole('button', { name: UPGRADE_BUTTON }).length).toBeGreaterThan(0);
        expect(screen.queryByRole('button', { name: CREATE_PRODUCT_BUTTON })).toBeNull();
    });

    it('opens API proxy creation from the empty landing', async () => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        trackHandler('post', SEARCH_PATH, countOf(0));
        trackHandler('post', PRODUCT_SEARCH_PATH, countOf(0));

        renderPage();

        fireEvent.click(await screen.findByRole('button', { name: CREATE_PROXY_BUTTON }));
        expect(currentPath()).toBe('/apim/apis/new');
    });

    it.each([
        [CREATE_PROXY_BUTTON, '/apim/apis/new'],
        [CREATE_PRODUCT_BUTTON, '/apim/api-products/new'],
    ])('opens the matching creation page when %s is clicked on the dashboard', async (button, expectedPath) => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        trackHandler('post', SEARCH_PATH, countOf(FEDERATION_INCLUSIVE_TOTAL));
        trackHandler('post', PRODUCT_SEARCH_PATH, countOf(0));

        renderPage();

        expect(await screen.findByText(TOTAL_APIS_CARD)).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: button }));
        expect(currentPath()).toBe(expectedPath);
    });
});
