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
import { act, fireEvent, render, screen } from '@testing-library/react';
import { createMemoryRouter, MemoryRouter, RouterProvider } from 'react-router-dom';

import { API_PROXY_NAV_GROUPS, ApiDetailSidebarNav } from './ApiDetailSidebarNav';

const GROUPS = API_PROXY_NAV_GROUPS;
const BASE = '/env/apis/abc-123';

function renderNav(currentPath: string) {
    return render(
        <MemoryRouter initialEntries={[currentPath]}>
            <ApiDetailSidebarNav groups={GROUPS} basePath={BASE} />
        </MemoryRouter>,
    );
}

// ─── Nav structure ────────────────────────────────────────────────────────────

describe('API_PROXY_NAV_GROUPS', () => {
    it('returns 7 groups with the expected labels', () => {
        expect(GROUPS).toHaveLength(7);
        expect(GROUPS.map(g => g.label)).toEqual([
            'General',
            'Gateway',
            'Design',
            'Consumer Access',
            'Security',
            'Observability',
            'Operations',
        ]);
    });

    it('Endpoints item has 3 children (Endpoints, Failover, Health Check Dashboard)', () => {
        const endpoints = GROUPS.find(g => g.label === 'Gateway')!.items.find(i => i.path === 'endpoints')!;
        expect(endpoints.children).toHaveLength(3);
        expect(endpoints.children!.map(c => c.path)).toEqual(['list', 'failover', 'health-check-dashboard']);
    });

    it('Deployment item has 2 children (Configuration, History)', () => {
        const deployment = GROUPS.find(g => g.label === 'Operations')!.items.find(i => i.path === 'deployment')!;
        expect(deployment.children).toHaveLength(2);
        expect(deployment.children!.map(c => c.path)).toEqual(['configuration', 'history']);
    });
});

// ─── Flat nav links ───────────────────────────────────────────────────────────

describe('ApiDetailSidebarNav — flat links', () => {
    it('renders a leaf link with the correct href', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.getByRole('link', { name: /^overview$/i })).toHaveAttribute('href', `${BASE}/overview`);
    });

    it('renders all group section headings', () => {
        renderNav(`${BASE}/overview`);
        // Most group labels are unique in the DOM; "General" also appears as a nav item label so use getAllByText.
        for (const group of GROUPS) {
            expect(screen.getAllByText(group.label).length).toBeGreaterThanOrEqual(1);
        }
    });
});

// ─── Collapsible items ────────────────────────────────────────────────────────

describe('ApiDetailSidebarNav — collapsible items', () => {
    it('is closed by default when current URL does not match any child path', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.queryByRole('link', { name: /failover/i })).not.toBeInTheDocument();
    });

    it('is open by default when current URL matches a child path', () => {
        renderNav(`${BASE}/endpoints/list`);
        expect(screen.getByRole('link', { name: /failover/i })).toBeInTheDocument();
        expect(screen.getByText(/health check dashboard/i)).toBeInTheDocument();
    });

    it('is open by default when current URL matches the parent path exactly', () => {
        renderNav(`${BASE}/endpoints`);
        expect(screen.getByRole('link', { name: /failover/i })).toBeInTheDocument();
    });

    it('clicking a closed collapsible item opens it and shows its children', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.queryByRole('link', { name: /failover/i })).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /endpoints/i }));

        expect(screen.getByRole('link', { name: /failover/i })).toBeInTheDocument();
    });

    it('clicking an open collapsible item closes it and hides its children', () => {
        renderNav(`${BASE}/endpoints/list`);
        expect(screen.getByRole('link', { name: /failover/i })).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /endpoints/i }));

        expect(screen.queryByRole('link', { name: /failover/i })).not.toBeInTheDocument();
    });

    it('child links have the correct href composed from basePath + parent + child', () => {
        renderNav(`${BASE}/endpoints/list`);
        expect(screen.getByRole('link', { name: /failover/i })).toHaveAttribute('href', `${BASE}/endpoints/failover`);
    });

    it('auto-expands when route changes to match a child path', async () => {
        const router = createMemoryRouter([{ path: '*', element: <ApiDetailSidebarNav groups={GROUPS} basePath={BASE} /> }], {
            initialEntries: [`${BASE}/overview`],
        });
        render(<RouterProvider router={router} />);
        expect(screen.queryByRole('link', { name: /failover/i })).not.toBeInTheDocument();

        await act(async () => {
            router.navigate(`${BASE}/endpoints/list`);
        });

        expect(screen.getByRole('link', { name: /failover/i })).toBeInTheDocument();
    });
});
