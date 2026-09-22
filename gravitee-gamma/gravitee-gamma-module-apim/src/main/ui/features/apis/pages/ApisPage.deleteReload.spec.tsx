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
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
}));
jest.mock('@gravitee/gamma-lib-observability', () => ({
    DEFAULT_TIME_RANGE: { type: 'relative', period: '5m' },
    encodeObservabilityState: () => ({ q: 'ENCODED_Q', v: '1' }),
}));
// The stats cards issue their own searches; stubbing them leaves the list's search as the only one counted.
jest.mock('../hooks/useApiStats', () => ({
    useApiStats: () => ({
        total: 2,
        private: 0,
        published: 0,
        isLoading: false,
        failed: { total: false, private: false, published: false },
        isError: false,
    }),
}));
jest.mock('../hooks/useEnvCategories', () => ({ useEnvCategories: () => ({ data: [], isLoading: false }) }));
jest.mock('../context/ApiDetailContext', () => ({ useApiDetailContext: jest.fn() }));

import { ApisPage } from './ApisPage';
import { ApiGeneralPage } from './detail/general/ApiGeneralPage';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { server } from '../../../testing/server';
import { useApiDetailContext } from '../context/ApiDetailContext';

const mockUseApiDetailContext = useApiDetailContext as jest.Mock;

const ORG_CONSOLE_PATH = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/console`;
const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const API_PATH = `${TEST_V2_BASE}/apis/:apiId`;

// `lifecycleState: 'CREATED'` and no runtime `state` keep the General page's delete control unblocked.
const FEDERATED_API = {
    id: 'federated-1',
    name: 'Federated Orders API',
    apiVersion: '1.0',
    type: 'PROXY',
    definitionVersion: 'FEDERATED',
    lifecycleState: 'CREATED',
    originContext: { origin: 'INTEGRATION', provider: 'solace' },
};

// A second, natively-managed row keeps the reloaded list off the empty landing, so "appears in no
// table row" is read off a table that is actually rendered.
const NATIVE_API = {
    id: 'native-1',
    name: 'Payments Proxy',
    apiVersion: '1.0',
    type: 'PROXY',
    definitionVersion: 'V4',
    state: 'STOPPED',
};

function stubApisBackend(rows: (typeof FEDERATED_API | typeof NATIVE_API)[]) {
    let remaining = rows;
    let searchCount = 0;

    server.use(
        http.get(ORG_CONSOLE_PATH, () => HttpResponse.json({ federation: { enabled: true } })),
        http.post(SEARCH_PATH, () => {
            searchCount += 1;
            return HttpResponse.json({
                data: remaining,
                pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: remaining.length },
            });
        }),
        http.delete(API_PATH, ({ params }) => {
            remaining = remaining.filter(api => api.id !== params.apiId);
            return new HttpResponse(null, { status: 204 });
        }),
    );

    return {
        get searchCount() {
            return searchCount;
        },
    };
}

function renderApisApp() {
    // A never-stale cache makes the delete's own invalidation — not react-query's refetch-on-mount
    // default — the only thing that can send the remounted list back to the search endpoint.
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false, staleTime: Infinity }, mutations: { retry: false } },
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/apis']}>
                <Routes>
                    <Route path="apis">
                        <Route index element={<ApisPage />} />
                        <Route path=":apiId">
                            <Route path="general" element={<ApiGeneralPage />} />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('Deleting an API from its General page', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockUseApiDetailContext.mockReturnValue({ api: FEDERATED_API, isLoading: false, permissionsReady: true });
    });

    afterEach(() => jest.clearAllMocks());

    it('leaves a deleted federated API in no row of the reloaded API Proxies list', async () => {
        const backend = stubApisBackend([FEDERATED_API, NATIVE_API]);
        renderApisApp();

        fireEvent.click(await screen.findByRole('button', { name: FEDERATED_API.name }));

        fireEvent.click(await screen.findByRole('button', { name: /delete this api/i }));
        fireEvent.change(await screen.findByPlaceholderText(FEDERATED_API.name), { target: { value: FEDERATED_API.name } });
        fireEvent.click(screen.getByRole('button', { name: /delete permanently/i }));

        const table = await screen.findByRole('table');
        await waitFor(() => expect(within(table).queryByText(FEDERATED_API.name)).toBeNull());
        expect(within(table).getByText(NATIVE_API.name)).toBeInTheDocument();
        expect(backend.searchCount).toBe(2);
    });
});
