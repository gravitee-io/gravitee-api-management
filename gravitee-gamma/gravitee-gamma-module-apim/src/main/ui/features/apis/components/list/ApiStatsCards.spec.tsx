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
import { http, HttpResponse } from 'msw';

import { ApiStatsCards } from './ApiStatsCards';
import { resetApimClientForTests } from '../../../../shared/api/apimClient';
import { ENTERPRISE_LICENSE, OSS_LICENSE, TEST_CONFIG, TEST_V2_BASE } from '../../../../testing/factories';
import { captureTimeoutSignals, settleOutstandingRequests, trackHandler } from '../../../../testing/helpers';
import { server } from '../../../../testing/server';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
    licenseService: { subscribe: () => () => {}, getSnapshot: jest.fn() },
}));

const mockLicenseSnapshot = jest.mocked(licenseService.getSnapshot);

const ORG_CONSOLE_PATH = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/console`;
const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const PROXY_TYPES = ['V4_HTTP_PROXY', 'V4_TCP_PROXY'];
const PROXY_AND_FEDERATED = ['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED'];

type StatsCard = 'total' | 'private' | 'published';

interface SearchBody {
    apiTypes: string[];
    visibilities?: string[];
    published?: string[];
}

type CardTotals = Record<StatsCard, number>;

const CARD_TOTALS: CardTotals = { total: 7, private: 3, published: 5 };
const PROXY_ONLY_TOTALS: CardTotals = { total: 4, private: 1, published: 2 };
const CARD_LABELS: Record<StatsCard, string> = { total: 'Total APIs', private: 'Private', published: 'Published' };
const ANY_RENDERED_COUNT = /^\d+$/;

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

function cardFor(body: SearchBody): StatsCard {
    if (body.visibilities) return 'private';
    if (body.published) return 'published';
    return 'total';
}

/** Gives a proxy-only search different totals than a federation-inclusive one, so a rendered number says which search produced it. */
function totalsByGate(body: SearchBody): CardTotals {
    return body.apiTypes.includes('FEDERATED') ? CARD_TOTALS : PROXY_ONLY_TOTALS;
}

/** Answers each of the three stats searches with a total of its own, so a rendered number traces back to one card. */
function stubCountSearches(failing?: StatsCard, totalsFor: (body: SearchBody) => CardTotals = () => CARD_TOTALS): SearchBody[] {
    const bodies: SearchBody[] = [];

    server.use(
        http.post(SEARCH_PATH, async ({ request }) => {
            const body = (await request.clone().json()) as SearchBody;
            bodies.push(body);
            const card = cardFor(body);
            return card === failing
                ? HttpResponse.json({ message: 'search failed' }, { status: 500 })
                : HttpResponse.json({ data: [], pagination: { page: 1, perPage: 1, pageCount: 1, totalCount: totalsFor(body)[card] } });
        }),
    );

    return bodies;
}

/** Narrows a query to one card, so a rendered number is pinned to the card it belongs to rather than to the page. */
function cardBody(card: StatsCard): HTMLElement {
    const body = screen.getByText(CARD_LABELS[card]).closest('[data-slot="card-content"]');
    if (!(body instanceof HTMLElement)) throw new Error(`No card rendered for "${CARD_LABELS[card]}"`);
    return body;
}

function loadingPlaceholderIn(card: StatsCard): Element | null {
    return cardBody(card).querySelector('[data-slot="skeleton"]');
}

function renderCards() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    // A fresh element each time, since React bails out of re-rendering the identical one; the client is kept so a
    // cache filled under one gate state is still there under the next.
    const tree = () => (
        <QueryClientProvider client={queryClient}>
            <ApiStatsCards />
        </QueryClientProvider>
    );
    const { rerender } = render(tree());
    return { rerenderCards: () => rerender(tree()) };
}

describe('ApiStatsCards', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
    });

    afterEach(() => jest.restoreAllMocks());

    it('asks the total, private and published searches for federated proxies once the federation gate resolves', async () => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        const bodies = stubCountSearches();

        renderCards();

        await waitFor(() => expect(bodies).toHaveLength(3));
        expect(bodies).toEqual(
            expect.arrayContaining([
                { apiTypes: PROXY_AND_FEDERATED },
                { visibilities: ['PRIVATE'], apiTypes: PROXY_AND_FEDERATED },
                { published: ['PUBLISHED'], apiTypes: PROXY_AND_FEDERATED },
            ]),
        );
    });

    it('shows each card the total its own search answered with', async () => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        stubCountSearches();

        renderCards();

        expect(await within(cardBody('total')).findByText('7')).not.toBeNull();
        expect(await within(cardBody('private')).findByText('3')).not.toBeNull();
        expect(await within(cardBody('published')).findByText('5')).not.toBeNull();
    });

    it.each(FEDERATION_OFF_CASES)('counts proxies alone in every card when %s', async (_reason, orgConsoleSettings, stubLicense) => {
        stubLicense();
        trackHandler('get', ORG_CONSOLE_PATH, orgConsoleSettings);
        const bodies = stubCountSearches();

        renderCards();

        expect(await within(cardBody('total')).findByText('7')).not.toBeNull();
        expect(await within(cardBody('private')).findByText('3')).not.toBeNull();
        expect(await within(cardBody('published')).findByText('5')).not.toBeNull();
        expect(bodies).toHaveLength(3);
        expect(bodies.map(body => body.apiTypes)).toEqual([PROXY_TYPES, PROXY_TYPES, PROXY_TYPES]);
    });

    it('re-counts with federated proxies rather than reusing the proxy-only totals when the federation gate turns on', async () => {
        mockLicenseSnapshot.mockReturnValue(OSS_LICENSE);
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        const bodies = stubCountSearches(undefined, totalsByGate);

        const { rerenderCards } = renderCards();
        await waitFor(() => expect(bodies).toHaveLength(3));

        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
        rerenderCards();

        await waitFor(() => expect(bodies).toHaveLength(6));
        expect(bodies.slice(3).map(body => body.apiTypes)).toEqual([PROXY_AND_FEDERATED, PROXY_AND_FEDERATED, PROXY_AND_FEDERATED]);
        expect(await within(cardBody('total')).findByText(String(CARD_TOTALS.total))).not.toBeNull();
        expect(await within(cardBody('private')).findByText(String(CARD_TOTALS.private))).not.toBeNull();
        expect(await within(cardBody('published')).findByText(String(CARD_TOTALS.published))).not.toBeNull();
    });

    it('issues no search while the federation gate is unresolved, then counts proxies alone once it fails closed', async () => {
        const timeoutControllers = captureTimeoutSignals();
        server.use(http.get(ORG_CONSOLE_PATH, () => new Promise(() => {})));
        const bodies = stubCountSearches();

        renderCards();

        await waitFor(() => expect(timeoutControllers.length).toBeGreaterThan(0));
        await settleOutstandingRequests();
        expect(bodies).toHaveLength(0);
        expect(screen.queryAllByText(ANY_RENDERED_COUNT)).toHaveLength(0);
        // Firing the bound the gate opened stands in for waiting out its wall-clock delay.
        timeoutControllers.forEach(controller => controller.abort(new DOMException('signal timed out', 'TimeoutError')));

        await waitFor(() => expect(bodies).toHaveLength(3));
        expect(bodies.map(body => body.apiTypes)).toEqual([PROXY_TYPES, PROXY_TYPES, PROXY_TYPES]);
        expect(await within(cardBody('total')).findByText('7')).not.toBeNull();
    });

    it('shows the totals that came back and holds the card whose search failed on its loading placeholder', async () => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        stubCountSearches('published');

        renderCards();

        expect(await within(cardBody('total')).findByText('7')).not.toBeNull();
        expect(await within(cardBody('private')).findByText('3')).not.toBeNull();
        await settleOutstandingRequests();
        expect(within(cardBody('published')).queryByText(ANY_RENDERED_COUNT)).toBeNull();
        // A placeholder, not an error indicator: surfacing count failures to the user is out of this deliverable's scope.
        expect(loadingPlaceholderIn('published')).not.toBeNull();
        expect(loadingPlaceholderIn('total')).toBeNull();
    });
});
