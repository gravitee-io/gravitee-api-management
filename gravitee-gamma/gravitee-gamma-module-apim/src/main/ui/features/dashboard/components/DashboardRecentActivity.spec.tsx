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
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { DashboardRecentActivity } from './DashboardRecentActivity';
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
    name: 'Orders Proxy',
    apiVersion: '2.0',
    type: 'PROXY',
    definitionVersion: 'V4',
    state: 'STARTED',
};

const PROXY_API = {
    id: 'proxy-2',
    name: 'Payments Proxy',
    apiVersion: '2.0',
    type: 'PROXY',
    definitionVersion: 'V4',
};

const EMPTY_RESULTS = { data: [], pagination: { page: 1, perPage: 6, pageCount: 0, totalCount: 0 } };
const EMPTY_MESSAGE = 'No APIs found.';
const PROXY_AND_FEDERATED = ['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED'];

interface SearchBody {
    apiTypes: string[];
}

/** Answers with the federated row only for a federation-inclusive search, so a rendered row says which search produced it. */
function stubSearchByGate(): SearchBody[] {
    const bodies: SearchBody[] = [];

    server.use(
        http.post(SEARCH_PATH, async ({ request }) => {
            const body = (await request.clone().json()) as SearchBody;
            bodies.push(body);
            const data = body.apiTypes.includes('FEDERATED') ? [PROXY_API, FEDERATED_API] : [PROXY_API];
            return HttpResponse.json({ data, pagination: { page: 1, perPage: 6, pageCount: 1, totalCount: data.length } });
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

/** Answers from the requested `apiTypes`, so a federated row the widget never shows reads as excluded rather than never stubbed. */
function stubSearchByRequestedApiTypes() {
    server.use(
        http.post(SEARCH_PATH, async ({ request }) => {
            const body = (await request.clone().json()) as { apiTypes: string[] };
            const data = body.apiTypes.includes('FEDERATED') ? [NATIVE_API, FEDERATED_API] : [NATIVE_API];
            return HttpResponse.json({ data, pagination: { page: 1, perPage: 6, pageCount: 1, totalCount: data.length } });
        }),
    );
}

function renderWidget() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    // A fresh element each time, since React bails out of re-rendering the identical one; the client is kept so a
    // cache filled under one gate state is still there under the next.
    const tree = () => (
        <QueryClientProvider client={queryClient}>
            <DashboardRecentActivity onNavigateToApi={jest.fn()} onGoToApis={jest.fn()} />
        </QueryClientProvider>
    );
    const { rerender } = render(tree());
    return { rerenderWidget: () => rerender(tree()) };
}

function loadingPlaceholder(): Element | null {
    return document.body.querySelector('[data-slot="skeleton"]');
}

describe('DashboardRecentActivity', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
    });

    afterEach(() => jest.restoreAllMocks());

    it('lists a federated API the search answered with once the federation gate resolves', async () => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        trackHandler('post', SEARCH_PATH, {
            data: [FEDERATED_API],
            pagination: { page: 1, perPage: 6, pageCount: 1, totalCount: 1 },
        });

        renderWidget();

        expect(await screen.findByText(FEDERATED_API.name)).not.toBeNull();
    });

    it.each(FEDERATION_OFF_CASES)(
        'lists the native API rows and no federated row when %s',
        async (_reason, orgConsoleSettings, stubLicense) => {
            stubLicense();
            trackHandler('get', ORG_CONSOLE_PATH, orgConsoleSettings);
            stubSearchByRequestedApiTypes();

            renderWidget();

            expect(await screen.findByText(NATIVE_API.name)).not.toBeNull();
            expect(screen.queryByText(FEDERATED_API.name)).toBeNull();
        },
    );

    it('re-lists with federated proxies rather than reusing the proxy-only rows when the federation gate turns on', async () => {
        mockLicenseSnapshot.mockReturnValue(OSS_LICENSE);
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } });
        const bodies = stubSearchByGate();

        const { rerenderWidget } = renderWidget();
        expect(await screen.findByText(PROXY_API.name)).not.toBeNull();
        expect(screen.queryByText(FEDERATED_API.name)).toBeNull();

        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
        rerenderWidget();

        expect(await screen.findByText(FEDERATED_API.name)).not.toBeNull();
        expect(bodies).toHaveLength(2);
        expect(bodies[1].apiTypes).toEqual(PROXY_AND_FEDERATED);
    });

    it('holds its loading placeholder rather than announcing no APIs while the federation gate is still resolving', async () => {
        const timeoutControllers = captureTimeoutSignals();
        server.use(http.get(ORG_CONSOLE_PATH, () => new Promise(() => {})));
        const searches = trackHandler('post', SEARCH_PATH, EMPTY_RESULTS);

        renderWidget();

        await waitFor(() => expect(timeoutControllers.length).toBeGreaterThan(0));
        await settleOutstandingRequests();
        expect(searches.callCount).toBe(0);
        expect(screen.queryByText(EMPTY_MESSAGE)).toBeNull();
        expect(loadingPlaceholder()).not.toBeNull();
        // Firing the bound the gate opened stands in for waiting out its wall-clock delay.
        timeoutControllers.forEach(controller => controller.abort(new DOMException('signal timed out', 'TimeoutError')));

        // Announcing the empty result only once the search has answered is what makes the assertion
        // above read as "still waiting" rather than "the widget never says this at all".
        expect(await screen.findByText(EMPTY_MESSAGE)).not.toBeNull();
        expect(loadingPlaceholder()).toBeNull();
    });
});
