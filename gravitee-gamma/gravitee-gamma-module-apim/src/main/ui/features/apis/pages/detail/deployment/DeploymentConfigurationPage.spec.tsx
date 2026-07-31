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
import { permissionService, useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { DeploymentConfigurationPage } from './DeploymentConfigurationPage';
import { resetApimClientForTests } from '../../../../../shared/api/apimClient';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../../../testing/factories';
import { trackHandler } from '../../../../../testing/helpers';
import { server } from '../../../../../testing/server';

// Only the federation-provided environment context is stubbed; permissions use the REAL
// permissionService singleton and all data flows through real MSW HTTP.
jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));

const ORG_BASE = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}`;
const API_PATH = `${TEST_V2_BASE}/apis/:apiId`;

// id (UUID) deliberately differs from key (slug) — the backend stores/validates by key.
const ORG_TAGS = [
    { id: '258f7ecd-9f41-43e2-8f7e-cd9f4113e243', key: 'public', name: 'Public' },
    { id: 'b1d2c3e4-0000-0000-0000-000000000002', key: 'private', name: 'Private' },
];

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/apis/api-1/deployment/configuration']}>
                <Routes>
                    <Route path="/apis/:apiId/deployment/configuration" element={<DeploymentConfigurationPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('DeploymentConfigurationPage (sharding tags persist by key, not id)', () => {
    beforeEach(() => {
        resetApimClientForTests();
        (useEnvironment as jest.Mock).mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
        permissionService.load('api', ['api-definition-u']);
        server.use(
            http.get(`${ORG_BASE}/configuration/tags`, () => HttpResponse.json(ORG_TAGS)),
            // API already has the 'public' tag (stored as the key).
            http.get(API_PATH, () => HttpResponse.json({ id: 'api-1', name: 'My API', tags: ['public'] })),
        );
    });

    afterEach(() => {
        permissionService.clear('api');
        jest.clearAllMocks();
    });

    it('marks the existing tag (matched by key) as checked', async () => {
        renderPage();

        const publicCheckbox = await screen.findByRole('checkbox', { name: /public/i });
        await waitFor(() => expect(publicCheckbox).toBeChecked());
        expect(screen.getByRole('checkbox', { name: /private/i })).not.toBeChecked();
    });

    it('PUTs tag KEYS (not UUID ids) when saving', async () => {
        const putTracker = trackHandler('put', API_PATH, { id: 'api-1' });
        renderPage();

        const privateCheckbox = await screen.findByRole('checkbox', { name: /private/i });
        fireEvent.click(privateCheckbox);
        fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

        await waitFor(() => expect(putTracker.callCount).toBe(1));
        const body = putTracker.lastCall?.body as { tags: string[] };
        // Must be slugs the backend accepts — never the UUID that triggered tag.notAllowed (400).
        expect(body.tags).toEqual(['public', 'private']);
        expect(body.tags).not.toContain('258f7ecd-9f41-43e2-8f7e-cd9f4113e243');
    });
});
