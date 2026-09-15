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

import { DashboardRecentActivity } from './DashboardRecentActivity';
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

const FEDERATED_API = {
    id: 'federated-1',
    name: 'Federated Orders',
    apiVersion: '1.0',
    type: 'PROXY',
    definitionVersion: 'FEDERATED',
    originContext: { origin: 'INTEGRATION', provider: 'solace' },
};

const EMPTY_RESULTS = { data: [], pagination: { page: 1, perPage: 6, pageCount: 0, totalCount: 0 } };
const EMPTY_MESSAGE = 'No APIs found.';

function renderWidget() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <DashboardRecentActivity onNavigateToApi={jest.fn()} onGoToApis={jest.fn()} />
        </QueryClientProvider>,
    );
}

function loadingPlaceholder(): Element | null {
    return document.body.querySelector('[data-slot="skeleton"]');
}

describe('DashboardRecentActivity', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockUseHasFeature.mockReturnValue(true);
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
