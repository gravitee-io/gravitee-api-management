/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { HomePage } from './HomePage';
import { useAuthStore } from '../../features/auth/auth.store';
import type { GammaModule } from '../../features/modules';
import { buildUser, TEST_GAMMA_BASE, TEST_MANAGEMENT_V2_ENVIRONMENT_BASE } from '../../testing/factories';
import { respondWith, seedEnvironments } from '../../testing/helpers';

const ALL_MODULES: readonly GammaModule[] = [
    { id: 'apim', name: 'APIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_apim', exposedModule: 'App' },
    { id: 'aim', name: 'AIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_aim', exposedModule: 'App' },
    { id: 'platform', name: 'Platform', version: '1.0.0', remoteName: 'gravitee_gamma_module_platform', exposedModule: 'App' },
    {
        id: 'authz',
        name: 'Authorization',
        version: '1.0.0',
        remoteName: 'gravitee_gamma_module_authz',
        exposedModule: 'App',
    },
];

/** Renders HomePage under the same MemoryRouter shape AppRoutes uses (so `useEnvHrid`
 *  resolves to the env path segment). */
function renderHome(modules: readonly GammaModule[]) {
    return render(
        <MemoryRouter initialEntries={['/environments/env-1/home']}>
            <Routes>
                <Route path="/environments/:envHrid/home" element={<HomePage modules={modules} loading={false} error={null} />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('HomePage', () => {
    beforeEach(() => {
        // First env from TEST_ENVIRONMENTS has id `env-1-id` / hrid `env-1`.
        seedEnvironments();
        useAuthStore.setState({ user: buildUser({ firstname: 'John', displayName: 'John Doe' }) });
        // `useApiCount` always fetches when apim is in the modules list — return 0 by
        // default so we don't get an unhandled-request error from MSW. Tests that care
        // about the badge override this with their own handler.
        respondWith('post', `${TEST_MANAGEMENT_V2_ENVIRONMENT_BASE}/env-1-id/apis/_search`, { pagination: { totalCount: 0 } });
        // `useAgentCount` hits the catalog endpoint — return 0 by default so we don't get
        // an unhandled-request error from MSW.
        respondWith('get', `${TEST_GAMMA_BASE}/environments/env-1-id/modules/aim/catalog/agents`, { pagination: { totalCount: 0 } });
    });

    it('should render one Open link per active module when all modules are present', async () => {
        renderHome(ALL_MODULES);

        await waitFor(() => {
            expect(screen.getAllByText('Open')).toHaveLength(4);
        });

        expect(screen.queryByText('Coming soon')).toBeNull();

        for (const name of ['Agent Management', 'API Management', 'Platform', 'Authorization']) {
            expect(screen.getByRole('heading', { level: 3, name })).toBeTruthy();
        }
    });

    it('should render the Agent Management card without an Open CTA when aim is missing from /modules', async () => {
        renderHome(ALL_MODULES.filter(m => m.id !== 'aim'));

        const card = screen.getByRole('group', { name: 'Agent Management' });
        expect(within(card).queryByText('Open')).toBeNull();

        await waitFor(() => {
            expect(screen.getAllByText('Open')).toHaveLength(3);
        });
    });

    it('should greet the user by first name when available', () => {
        renderHome(ALL_MODULES);
        expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('Welcome back, John');
    });

    it('should render the live API count badge on the API Management card', async () => {
        respondWith('post', `${TEST_MANAGEMENT_V2_ENVIRONMENT_BASE}/env-1-id/apis/_search`, {
            pagination: { totalCount: 24 },
        });

        renderHome(ALL_MODULES);

        expect(await screen.findByText('24 APIs')).toBeTruthy();
    });

    it('should pluralize the API count badge correctly for a single API', async () => {
        respondWith('post', `${TEST_MANAGEMENT_V2_ENVIRONMENT_BASE}/env-1-id/apis/_search`, {
            pagination: { totalCount: 1 },
        });

        renderHome(ALL_MODULES);

        expect(await screen.findByText('1 API')).toBeTruthy();
    });

    it('should render an error alert when the modules fetch failed upstream', () => {
        render(
            <MemoryRouter initialEntries={['/environments/env-1/home']}>
                <Routes>
                    <Route
                        path="/environments/:envHrid/home"
                        element={<HomePage modules={[]} loading={false} error={new Error('boom')} />}
                    />
                </Routes>
            </MemoryRouter>,
        );

        const alert = screen.getByRole('alert');
        expect(alert.textContent).toContain('Failed to load modules: boom');
    });

    describe('Get Started section', () => {
        it('should render the three Get Started cards with linked CTAs', async () => {
            renderHome(ALL_MODULES);

            await waitFor(() => {
                expect(screen.getAllByText('Get started')).toHaveLength(3);
            });

            for (const name of ['Add Integration', 'Create MCP Proxy', 'Protect with FGA']) {
                expect(screen.getByRole('heading', { level: 3, name })).toBeTruthy();
            }
        });
    });
});
