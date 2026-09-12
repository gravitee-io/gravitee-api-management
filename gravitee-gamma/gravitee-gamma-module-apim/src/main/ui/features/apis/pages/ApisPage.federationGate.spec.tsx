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

import { ApisPage } from './ApisPage';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';
import type { OrgConsoleSettings } from '../../settings/services/orgConsoleSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
    useHasFeature: jest.fn(),
}));
jest.mock('@gravitee/gamma-lib-observability', () => ({
    DEFAULT_TIME_RANGE: { type: 'relative', period: '5m' },
    encodeObservabilityState: () => ({ q: 'ENCODED_Q', v: '1' }),
}));
// The stats cards issue their own searches; stubbing them leaves the list's search as the only one tracked.
jest.mock('../hooks/useApiStats', () => ({ useApiStats: () => ({ total: 1, private: 0, published: 0, isLoading: false }) }));

const mockUseHasFeature = jest.mocked(useHasFeature);

const ORG_CONSOLE_PATH = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/console`;
const SEARCH_PATH = `${TEST_V2_BASE}/apis/_search`;
const PROXY_TYPES = ['V4_HTTP_PROXY', 'V4_TCP_PROXY'];
const PROXY_AND_FEDERATED = ['V4_HTTP_PROXY', 'V4_TCP_PROXY', 'FEDERATED'];

const NATIVE_PROXY_NAME = 'Payments Proxy';
const SEARCH_RESPONSE = {
    data: [{ id: 'native-1', name: NATIVE_PROXY_NAME, apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4' }],
    pagination: { page: 1, perPage: 10, pageCount: 1, totalCount: 1 },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <ApisPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

/** Hands back every signal the code under test opened a time bound with, so the test can fire them itself. */
function captureTimeoutSignals(): AbortController[] {
    const controllers: AbortController[] = [];
    jest.spyOn(AbortSignal, 'timeout').mockImplementation(() => {
        const controller = new AbortController();
        controllers.push(controller);
        return controller.signal;
    });
    return controllers;
}

describe('ApisPage federation gate', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockUseHasFeature.mockReturnValue(true);
    });

    afterEach(() => jest.restoreAllMocks());

    it.each<[string, OrgConsoleSettings, boolean, string[]]>([
        ['the org setting is on and the license carries federation', { federation: { enabled: true } }, true, PROXY_AND_FEDERATED],
        ['the org setting is on but the license lacks federation', { federation: { enabled: true } }, false, PROXY_TYPES],
        ['the license carries federation but the org setting is off', { federation: { enabled: false } }, true, PROXY_TYPES],
        // `enabled` is a nullable Boolean on the backend, so both unset shapes reach the client.
        ['the console configuration carries no federation object', {}, true, PROXY_TYPES],
        ['the federation object carries no enabled flag', { federation: {} }, true, PROXY_TYPES],
    ])('asks the search for federated proxies only with both gates on — %s', async (_scenario, settings, isLicensed, expectedApiTypes) => {
        mockUseHasFeature.mockReturnValue(isLicensed);
        trackHandler('get', ORG_CONSOLE_PATH, settings);
        const tracker = trackHandler('post', SEARCH_PATH, SEARCH_RESPONSE);

        renderPage();

        expect(await screen.findByText(NATIVE_PROXY_NAME)).not.toBeNull();
        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.body).toEqual({ apiTypes: expectedApiTypes });
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
