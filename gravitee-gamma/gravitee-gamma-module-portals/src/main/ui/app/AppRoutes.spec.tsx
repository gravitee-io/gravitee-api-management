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
import { LayoutSlotsProvider } from '@gravitee/graphene-core';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';
import { LocalDevShell } from './LocalDevShell';
import { TEST_V2_BASE } from '../testing/factories';
import { server } from '../testing/server';

const PORTAL_ID = '00f8c9e7-78fc-4907-b8c9-e778fc790750';
const MODULE_ROOT = '/environments/dev/portals';

function LocationProbe() {
    return <output aria-label="Current path">{useLocation().pathname}</output>;
}

function renderModule(path: string, federated = false) {
    return render(
        <MemoryRouter basename={federated ? '/console' : '/'} initialEntries={[federated ? `/console${path}` : path]}>
            <LayoutSlotsProvider>
                <LocalDevShell>
                    <Routes>
                        <Route path={federated ? '/environments/:envHrid/portals/*' : '/*'} element={<AppRoutes />} />
                    </Routes>
                    <LocationProbe />
                </LocalDevShell>
            </LayoutSlotsProvider>
        </MemoryRouter>,
    );
}

describe('AppRoutes', () => {
    it.each(['portals', 'portals/_stats'])('does not retry a denied %s request with the production query client', async endpoint => {
        const request = jest.fn(() => HttpResponse.json({ message: 'Forbidden' }, { status: 403 }));
        server.use(http.get(`${TEST_V2_BASE}/${endpoint}`, request));

        renderModule('/');

        expect(await screen.findByText('Access denied')).toBeInTheDocument();
        expect(request).toHaveBeenCalledTimes(1);
    });

    it('opens the dashboard and provides working standalone sidebar links', async () => {
        const user = userEvent.setup();
        renderModule('/');

        expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
        const portalsLink = screen.getByRole('link', { name: /^Portals$/ });
        expect(portalsLink).toHaveAttribute('href', '/list');

        await user.click(portalsLink);

        expect(screen.getByRole('heading', { name: /^Portals$/ })).toBeInTheDocument();
        expect(portalsLink).toHaveAttribute('aria-current', 'page');
        expect(screen.getByLabelText('Current path')).toHaveTextContent('/list');
        await user.click(screen.getByRole('link', { name: /^Dashboard$/ }));
        expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    });

    it('preserves the host environment and basename when navigating', async () => {
        const user = userEvent.setup();
        renderModule(MODULE_ROOT, true);

        const portalsLink = screen.getByRole('link', { name: /^Portals$/ });
        expect(portalsLink).toHaveAttribute('href', `/console${MODULE_ROOT}/list`);
        await user.click(portalsLink);

        expect(screen.getByLabelText('Current path')).toHaveTextContent(`${MODULE_ROOT}/list`);
        expect(screen.getByRole('heading', { name: /^Portals$/ })).toBeInTheDocument();
    });

    it.each([false, true])('navigates Dashboard and Portals breadcrumbs with the keyboard (federated=%s)', async federated => {
        const user = userEvent.setup();
        const root = federated ? MODULE_ROOT : '';
        const hrefPrefix = federated ? `/console${root}` : '';
        renderModule(root || '/', federated);

        const dashboardBreadcrumbs = screen.getByRole('navigation', { name: /breadcrumb/i });
        expect(
            within(dashboardBreadcrumbs)
                .getAllByRole('listitem')
                .map(item => item.textContent),
        ).toEqual(['Developer Portals', 'Dashboard']);
        expect(within(dashboardBreadcrumbs).getByRole('link', { name: 'Developer Portals' })).toHaveAttribute('href', `${hrefPrefix}/`);

        await user.tab();
        await user.tab();
        expect(document.activeElement).toHaveAccessibleName('Dashboard');
        expect(document.activeElement).toHaveAttribute('href', `${hrefPrefix}/`);
        expect(document.activeElement).toHaveAttribute('aria-current', 'page');
        await user.tab();
        expect(screen.getByRole('link', { name: /^Portals$/ })).toHaveFocus();
        await user.keyboard('{Enter}');

        expect(screen.getByLabelText('Current path')).toHaveTextContent(`${root}/list`);
        expect(document.activeElement).toHaveAccessibleName('Portals');
        expect(document.activeElement).toHaveAttribute('aria-current', 'page');
        const listBreadcrumbs = screen.getByRole('navigation', { name: /breadcrumb/i });
        expect(
            within(listBreadcrumbs)
                .getAllByRole('listitem')
                .map(item => item.textContent),
        ).toEqual(['Developer Portals', 'Portals']);
        const dashboardLink = within(listBreadcrumbs).getByRole('link', { name: 'Developer Portals' });
        expect(dashboardLink).toHaveAttribute('href', `${hrefPrefix}/`);
        dashboardLink.focus();
        await user.keyboard('{Enter}');

        expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
        expect(screen.getByLabelText('Current path')).toHaveTextContent(`${root}/`);
    });

    it.each([false, true])('redirects a portal to its overview without a second portals segment (federated=%s)', async federated => {
        const root = federated ? MODULE_ROOT : '';
        renderModule(`${root}/${PORTAL_ID}`, federated);

        expect(await screen.findByRole('heading', { name: 'Portal overview' })).toBeInTheDocument();
        expect(screen.getByLabelText('Current path')).toHaveTextContent(`${root}/${PORTAL_ID}/overview`);
        expect(screen.getByText(PORTAL_ID, { selector: 'p' })).toBeInTheDocument();
    });

    it.each([
        [false, 'settings'],
        [true, 'settings'],
        [false, 'settings/details'],
        [true, 'settings/details'],
    ] as const)(
        'redirects an unknown portal path to overview while keeping the shell (federated=%s, suffix=%s)',
        async (federated, suffix) => {
            const root = federated ? MODULE_ROOT : '';
            const hrefPrefix = federated ? `/console${root}` : '';
            renderModule(`${root}/${PORTAL_ID}/${suffix}`, federated);

            expect(await screen.findByRole('heading', { name: 'Portal overview' })).toBeInTheDocument();
            expect(screen.getByLabelText('Current path')).toHaveTextContent(`${root}/${PORTAL_ID}/overview`);
            expect(screen.getByText(PORTAL_ID, { selector: 'p' })).toBeInTheDocument();
            expect(screen.getByRole('link', { name: /^Portals$/, current: 'page' })).toHaveAttribute('href', `${hrefPrefix}/list`);
            const breadcrumbs = screen.getByRole('navigation', { name: /breadcrumb/i });
            expect(within(breadcrumbs).getByText(PORTAL_ID)).toBeInTheDocument();
            expect(within(breadcrumbs).getByRole('link', { name: /^Portals$/ })).toHaveAttribute('href', `${hrefPrefix}/list`);
        },
    );

    it('opens an overview deep link and keeps Portals selected with navigable breadcrumbs', async () => {
        const user = userEvent.setup();
        renderModule(`${MODULE_ROOT}/${PORTAL_ID}/overview`, true);

        expect(screen.getByRole('heading', { name: 'Portal overview' })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /^Portals$/, current: 'page' })).toBeInTheDocument();
        const breadcrumbs = screen.getByRole('navigation', { name: /breadcrumb/i });
        expect(within(breadcrumbs).getByText(PORTAL_ID)).toBeInTheDocument();
        const portalsCrumb = within(breadcrumbs).getByRole('link', { name: /^Portals$/ });
        expect(portalsCrumb).toHaveAttribute('href', `/console${MODULE_ROOT}/list`);
        await user.click(portalsCrumb);
        expect(screen.getByRole('heading', { name: /^Portals$/ })).toBeInTheDocument();
    });
    it.each([false, true])('opens the full list and its row overview with complete breadcrumbs (federated=%s)', async federated => {
        const user = userEvent.setup();
        const root = federated ? MODULE_ROOT : '';
        renderModule(`${root}/list`, federated);
        expect(screen.getByRole('heading', { name: /^Portals$/ })).toBeVisible();
        expect(screen.getByRole('textbox', { name: 'Search portals' })).toBeVisible();
        const portal = await screen.findByText('Developer Portal');
        const listCrumbs = screen.getByRole('navigation', { name: /breadcrumb/i });
        expect(
            within(listCrumbs)
                .getAllByRole('listitem')
                .map(item => item.textContent),
        ).toEqual(['Developer Portals', 'Portals']);
        await user.click(portal);
        expect(screen.getByLabelText('Current path')).toHaveTextContent(`${root}/${PORTAL_ID}/overview`);
        const detailCrumbs = screen.getByRole('navigation', { name: /breadcrumb/i });
        expect(
            within(detailCrumbs)
                .getAllByRole('listitem')
                .map(item => item.textContent),
        ).toEqual(['Developer Portals', 'Portals', PORTAL_ID]);
        const listLink = within(detailCrumbs).getByRole('link', { name: 'Portals' });
        expect(listLink).toHaveAttribute('href', `${federated ? '/console' : ''}${root}/list`);
        await user.click(listLink);
        expect(screen.getByRole('textbox', { name: 'Search portals' })).toBeVisible();
    });
});
