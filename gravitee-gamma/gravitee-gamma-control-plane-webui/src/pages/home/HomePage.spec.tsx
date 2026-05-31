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
import { buildUser, TEST_GAMMA_BASE, TEST_MANAGEMENT_BASE, TEST_MANAGEMENT_V2_ENVIRONMENT_BASE } from '../../testing/factories';
import { respondWith, seedEnvironments } from '../../testing/helpers';

const ALL_MODULES: readonly GammaModule[] = [
    { id: 'apim', name: 'APIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_apim', exposedModule: 'App' },
    { id: 'aim', name: 'AIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_aim', exposedModule: 'App' },
    { id: 'platform', name: 'Platform Management', version: '1.0.0', remoteName: 'gravitee_gamma_module_platform', exposedModule: 'App' },
    {
        id: 'authz',
        name: 'Authorization',
        version: '1.0.0',
        remoteName: 'gravitee_gamma_module_authz',
        exposedModule: 'App',
    },
];

function renderHome(modules: readonly GammaModule[]) {
    return render(
        <MemoryRouter initialEntries={['/environments/env-1/home']}>
            <Routes>
                <Route path="/environments/:envHrid/home" element={<HomePage modules={modules} loading={false} error={null} />} />
            </Routes>
        </MemoryRouter>,
    );
}

/** Seeds default MSW handlers for all metric endpoints so tests don't get unhandled-request errors. */
function seedMetricHandlers(overrides?: {
    apiCount?: number;
    agentCount?: number;
    appCount?: number;
    policyCount?: number;
    principalCount?: number;
    mcpServerCount?: number;
    requestsTotal?: number;
}) {
    const {
        apiCount = 0,
        agentCount = 0,
        appCount = 0,
        policyCount = 0,
        principalCount = 0,
        mcpServerCount = 0,
        requestsTotal = 0,
    } = overrides ?? {};

    respondWith('post', `${TEST_MANAGEMENT_V2_ENVIRONMENT_BASE}/env-1-id/apis/_search`, { pagination: { totalCount: apiCount } });
    respondWith('get', `${TEST_GAMMA_BASE}/environments/env-1-id/modules/aim/catalog/agents`, { pagination: { totalCount: agentCount } });
    respondWith('get', `${TEST_MANAGEMENT_BASE}/environments/env-1-id/applications/_paged`, { page: { total_elements: appCount } });
    respondWith('get', `${TEST_GAMMA_BASE}/environments/env-1-id/modules/authz/policies`, { total: policyCount });
    respondWith('get', `${TEST_GAMMA_BASE}/environments/env-1-id/modules/authz/entities`, { total: principalCount });
    respondWith('get', `${TEST_GAMMA_BASE}/environments/env-1-id/modules/aim/catalog/items`, { total: mcpServerCount });
    respondWith('get', `${TEST_MANAGEMENT_V2_ENVIRONMENT_BASE}/env-1-id/analytics/request-response-time`, {
        requestsTotal,
    });
}

describe('HomePage', () => {
    beforeEach(() => {
        seedEnvironments();
        useAuthStore.setState({ user: buildUser({ firstname: 'John', displayName: 'John Doe' }) });
        seedMetricHandlers();
    });

    it('should render all four module headings when all modules are present', async () => {
        renderHome(ALL_MODULES);

        await waitFor(() => {
            for (const name of ['Agent Management', 'API Management', 'Platform Management', 'Authorization Management']) {
                expect(screen.getByRole('heading', { level: 3, name })).toBeTruthy();
            }
        });

        expect(screen.queryByText('Coming soon')).toBeNull();
    });

    it('should render the Agent Management card without any CTA when aim is missing from /modules', async () => {
        renderHome(ALL_MODULES.filter(m => m.id !== 'aim'));

        const card = screen.getByRole('group', { name: 'Agent Management' });
        expect(within(card).queryByText('Open')).toBeNull();
        expect(within(card).queryByText('Add Integration')).toBeNull();
    });

    it('should greet the user by first name when available', () => {
        renderHome(ALL_MODULES);
        expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('Welcome back, John');
    });

    it('should render the live API count metric on the API Management card', async () => {
        seedMetricHandlers({ apiCount: 24 });
        renderHome(ALL_MODULES);
        expect(await screen.findByText((_content, el) => el?.tagName === 'P' && /24\s+APIs/.test(el.textContent ?? ''))).toBeTruthy();
    });

    it('should pluralize the API count correctly for a single API', async () => {
        seedMetricHandlers({ apiCount: 1 });
        renderHome(ALL_MODULES);
        expect(await screen.findByText((_content, el) => el?.tagName === 'P' && /\b1\s+API\b/.test(el.textContent ?? ''))).toBeTruthy();
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

    it('should show empty-state CTA when module is available but has zero data', async () => {
        seedMetricHandlers({ apiCount: 0, agentCount: 0, appCount: 0, policyCount: 0 });
        renderHome(ALL_MODULES);

        const appsSection = screen.getByRole('region', { name: /applications/i });

        await waitFor(() => {
            expect(within(appsSection).getAllByText('Add Integration')).toHaveLength(1);
            expect(within(appsSection).getByText('Create your first API')).toBeTruthy();
            expect(within(appsSection).getByText('Register an application')).toBeTruthy();
            expect(within(appsSection).getByText('Create your first policy')).toBeTruthy();
        });
    });

    it('should show metric view with Open CTA when module has data', async () => {
        seedMetricHandlers({ apiCount: 54, agentCount: 8, appCount: 12, policyCount: 5 });
        renderHome(ALL_MODULES);

        await waitFor(() => {
            expect(screen.getAllByText('Open')).toHaveLength(4);
        });

        expect(await screen.findByText(/54/)).toBeTruthy();
        expect(await screen.findByText(/8/)).toBeTruthy();
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
