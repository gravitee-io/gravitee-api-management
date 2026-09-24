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
import type { License } from '@gravitee/gamma-modules-sdk/types';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router-dom';

import { ApisPage } from './ApisPage';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { ENTERPRISE_LICENSE, OSS_LICENSE, TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { captureTimeoutSignals, respondWith, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';
import type { OrgConsoleSettings } from '../../settings/services/orgConsoleSettings';
import { apiListKeys, orgConsoleKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
    licenseService: { subscribe: jest.fn(() => () => {}), getSnapshot: jest.fn() },
}));
jest.mock('@gravitee/gamma-lib-observability', () => ({
    DEFAULT_TIME_RANGE: { type: 'relative', period: '5m' },
    encodeObservabilityState: () => ({ q: 'ENCODED_Q', v: '1' }),
}));
// The stats cards issue their own searches; stubbing them leaves the list's search as the only one tracked.
jest.mock('../hooks/useApiStats', () => ({
    useApiStats: () => ({
        total: 1,
        private: 0,
        published: 0,
        isLoading: false,
        failed: { total: false, private: false, published: false },
        isError: false,
    }),
}));

const mockLicenseSnapshot = jest.mocked(licenseService.getSnapshot);
const mockLicenseSubscribe = jest.mocked(licenseService.subscribe);

const ORG_CONSOLE_PATH = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/console`;
const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const PROXY_TYPES = ['V4_HTTP_PROXY', 'V4_TCP_PROXY'];
const PROXY_AND_FEDERATED = ['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED'];

const NATIVE_PROXY_NAME = 'Payments Proxy';
const FEDERATED_API_NAME = 'Federated Orders';
const SEARCH_RESPONSE = {
    data: [{ id: 'native-1', name: NATIVE_PROXY_NAME, apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4' }],
    pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: 1 },
};

const AGENT_NAME = 'Fraud Detection Agent';
const FEDERATED_NAME = 'Federated Orders';
const FIRST_PROXY_LANDING_HEADING = 'Why add an API proxy?';

// Both APIs really live in the seeded environment; only the search body decides which come back.
const ENVIRONMENT_APIS = [
    { id: 'agent-1', name: AGENT_NAME, apiVersion: '1.0', type: 'PROXY', definitionVersion: 'FEDERATED_AGENT' },
    {
        id: 'federated-1',
        name: FEDERATED_NAME,
        apiVersion: '1.0',
        type: 'PROXY',
        definitionVersion: 'FEDERATED',
        originContext: { origin: 'INTEGRATION', provider: 'solace' },
    },
];

function respondWithEnvironmentApisMatchingRequestedTypes() {
    const searchBodies: { apiTypes: string[] }[] = [];
    server.use(
        http.post(SEARCH_PATH, async ({ request }) => {
            const body = (await request.json()) as { apiTypes: string[] };
            searchBodies.push(body);
            const { apiTypes } = body;
            const data = ENVIRONMENT_APIS.filter(api => apiTypes.includes(api.definitionVersion));
            return HttpResponse.json({ data, pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: data.length } });
        }),
    );
    return searchBodies;
}

function tableRowsContaining(name: string) {
    return screen.getAllByRole('row').filter(row => within(row).queryByText(name) !== null);
}

const TYPE_GATED_APIS = [
    {
        apiType: 'V4_HTTP_PROXY',
        api: { id: 'native-1', name: NATIVE_PROXY_NAME, apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4' },
    },
    {
        apiType: 'FEDERATED',
        api: {
            id: 'federated-1',
            name: FEDERATED_API_NAME,
            apiVersion: '1.0',
            type: 'PROXY',
            definitionVersion: 'FEDERATED',
            originContext: { origin: 'INTEGRATION', provider: 'solace' },
        },
    },
];

const TYPE_GATED_API_NAMES = TYPE_GATED_APIS.map(entry => entry.api.name);

// Answering from one fixed environment that does hold a federated API is what makes a missing federated
// row attributable to the gate, rather than to a fixture hand-trimmed to match the assertion.
function serveApisMatchingRequestedTypes() {
    server.use(
        http.post(SEARCH_PATH, async ({ request }) => {
            const { apiTypes } = (await request.json()) as { apiTypes: string[] };
            const data = TYPE_GATED_APIS.filter(entry => apiTypes.includes(entry.apiType)).map(entry => entry.api);
            return HttpResponse.json({ data, pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: data.length } });
        }),
    );
}

function renderedApiNames() {
    return TYPE_GATED_API_NAMES.filter(name => screen.queryByText(name) !== null);
}

function newQueryClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

/** A query that has started a fetch is 'fetching' and one that finished has left 'pending', so pending-and-idle means no list search was ever started. */
function listSearchQueryStates(queryClient: QueryClient) {
    return queryClient
        .getQueryCache()
        .findAll({ queryKey: [...apiListKeys.all, 'search'] })
        .map(({ state }) => ({ status: state.status, fetchStatus: state.fetchStatus }));
}

function renderPage(queryClient = newQueryClient()) {
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <ApisPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApisPage federation gate', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
    });

    afterEach(() => jest.restoreAllMocks());

    it.each<[string, OrgConsoleSettings, License, string[]]>([
        [
            'the org setting is on and the license tier entitles federation',
            { federation: { enabled: true } },
            ENTERPRISE_LICENSE,
            PROXY_AND_FEDERATED,
        ],
        [
            'the org setting is on but the license tier does not entitle federation',
            { federation: { enabled: true } },
            OSS_LICENSE,
            PROXY_TYPES,
        ],
        [
            'the license tier entitles federation but the org setting is off',
            { federation: { enabled: false } },
            ENTERPRISE_LICENSE,
            PROXY_TYPES,
        ],
        // `enabled` is a nullable Boolean on the backend, so both unset shapes reach the client.
        ['the console configuration carries no federation object', {}, ENTERPRISE_LICENSE, PROXY_TYPES],
        ['the federation object carries no enabled flag', { federation: {} }, ENTERPRISE_LICENSE, PROXY_TYPES],
    ])('asks the search for federated proxies only with both gates on — %s', async (_scenario, settings, license, expectedApiTypes) => {
        mockLicenseSnapshot.mockReturnValue(license);
        trackHandler('get', ORG_CONSOLE_PATH, settings);
        const tracker = trackHandler('post', SEARCH_PATH, SEARCH_RESPONSE);

        renderPage();

        expect(await screen.findByText(NATIVE_PROXY_NAME)).not.toBeNull();
        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.body).toEqual({ apiTypes: expectedApiTypes });
    });

    it.each<[string, OrgConsoleSettings, License, string[]]>([
        [
            'both the org setting and the license tier entitle federation',
            { federation: { enabled: true } },
            ENTERPRISE_LICENSE,
            TYPE_GATED_API_NAMES,
        ],
        [
            'the org setting is off while the license tier entitles federation',
            { federation: { enabled: false } },
            ENTERPRISE_LICENSE,
            [NATIVE_PROXY_NAME],
        ],
        [
            'the org setting is on while the license tier does not entitle federation',
            { federation: { enabled: true } },
            OSS_LICENSE,
            [NATIVE_PROXY_NAME],
        ],
    ])('renders a table row for the federated API only when %s', async (_scenario, settings, license, expectedNames) => {
        mockLicenseSnapshot.mockReturnValue(license);
        respondWith('get', ORG_CONSOLE_PATH, settings);
        serveApisMatchingRequestedTypes();

        renderPage();

        // The native row is what proves the page finished loading rather than failing before rendering anything.
        expect(await screen.findByText(NATIVE_PROXY_NAME)).not.toBeNull();
        expect(renderedApiNames()).toEqual(expectedNames);
    });

    it('lists the natively-managed proxies and asks for proxies alone when the console settings never answer', async () => {
        const timeoutControllers = captureTimeoutSignals();
        server.use(http.get(ORG_CONSOLE_PATH, () => new Promise(() => {})));
        const tracker = trackHandler('post', SEARCH_PATH, SEARCH_RESPONSE);

        const { container } = renderPage();
        // Firing the bound the gate opened stands in for waiting out its wall-clock delay.
        await waitFor(() => expect(timeoutControllers.length).toBeGreaterThan(0));
        // Pinning the wait before releasing it is what makes the assertion below read as "stopped
        // waiting" rather than "never waited at all".
        expect(container.querySelector('[aria-busy="true"]')).not.toBeNull();
        timeoutControllers.forEach(controller => controller.abort(new DOMException('signal timed out', 'TimeoutError')));

        expect(await screen.findByText(NATIVE_PROXY_NAME)).not.toBeNull();
        expect(container.querySelector('[aria-busy="true"]')).toBeNull();
        expect(tracker.lastCall?.body).toEqual({ apiTypes: PROXY_TYPES });
    });
});

describe('ApisPage federated agent exclusion', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
    });

    afterEach(() => jest.restoreAllMocks());

    it('lists the federated API in exactly one row and the federated agent in none', async () => {
        trackHandler('get', ORG_CONSOLE_PATH, { federation: { enabled: true } } satisfies OrgConsoleSettings);
        respondWithEnvironmentApisMatchingRequestedTypes();

        renderPage();

        expect(await screen.findByText(FEDERATED_NAME)).not.toBeNull();
        expect(tableRowsContaining(FEDERATED_NAME)).toHaveLength(1);
        expect(tableRowsContaining(AGENT_NAME)).toHaveLength(0);
    });

    it('does not show the first-proxy landing while the host license is still unreported in an environment holding only federated APIs', async () => {
        mockLicenseSnapshot.mockReturnValue(null);
        const searchBodies = respondWithEnvironmentApisMatchingRequestedTypes();
        // Settings already in the cache leave the host license as the only thing the gate can be waiting on
        // from the first render, so an idle search below is decided by the license and not by response timing.
        const queryClient = newQueryClient();
        queryClient.setQueryData(orgConsoleKeys.settings(), { federation: { enabled: true } } satisfies OrgConsoleSettings);

        renderPage(queryClient);

        expect(listSearchQueryStates(queryClient)).toEqual([{ status: 'pending', fetchStatus: 'idle' }]);
        expect(searchBodies).toEqual([]);
        expect(screen.queryByText(FIRST_PROXY_LANDING_HEADING)).toBeNull();

        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
        const notifyLicenseChanged = mockLicenseSubscribe.mock.calls.at(-1)![0];
        act(() => notifyLicenseChanged());

        expect(await screen.findByText(FEDERATED_NAME)).not.toBeNull();
        expect(searchBodies.at(-1)).toEqual({ apiTypes: PROXY_AND_FEDERATED });
    });
});
