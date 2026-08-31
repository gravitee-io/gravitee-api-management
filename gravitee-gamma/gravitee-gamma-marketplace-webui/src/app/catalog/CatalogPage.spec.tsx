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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { CatalogPage } from './CatalogPage';
import { buildApi, buildApisResponse, buildCategory, TEST_PORTAL_API } from '../../testing/factories';
import { respondToPortalPath, respondToPortalPathError, respondWith, trackPortalPath } from '../../testing/helpers';

function renderCatalog(path = '/catalog') {
    return renderWithGraphene(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="/catalog" element={<CatalogPage />} />
                <Route path="/catalog/:apiId" element={<h1>Agent detail</h1>} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('CatalogPage', () => {
    it('should render agent cards from the portal search', async () => {
        respondToPortalPath(
            'post',
            '/apis/_search',
            buildApisResponse([
                buildApi(),
                buildApi({
                    id: 'api-slack',
                    name: 'Slack Ops Bot',
                    type: 'MCP_PROXY',
                    description: 'Runbooks via MCP tools.',
                    mcp: { mcpPath: '/mcp' },
                    labels: ['ops'],
                }),
            ]),
        );
        renderCatalog();

        expect(await screen.findByRole('link', { name: /IT Helpdesk Agent/ })).toBeTruthy();
        expect(screen.getByRole('link', { name: /Slack Ops Bot/ })).toBeTruthy();
        expect(screen.getByText('A2A')).toBeTruthy();
        expect(screen.getByText('Triage and route IT tickets to the right queue.')).toBeTruthy();
        expect(screen.getAllByText('MCP').length).toBeGreaterThan(0);
    });

    it('should send the URL query to the portal search', async () => {
        const tracker = trackPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderCatalog('/catalog?query=ticket%20triage');

        await screen.findByRole('link', { name: /IT Helpdesk Agent/ });

        expect(tracker.lastCall?.url).toContain('q=ticket');
    });

    it('should send the URL category to the portal search', async () => {
        const tracker = trackPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderCatalog('/catalog?category=it');

        await screen.findByRole('link', { name: /IT Helpdesk Agent/ });

        expect(tracker.lastCall?.url).toContain('category=it');
    });

    it('should keep only matching protocols when a protocol filter is set', async () => {
        respondToPortalPath(
            'post',
            '/apis/_search',
            buildApisResponse([
                buildApi(),
                buildApi({ id: 'api-slack', name: 'Slack Ops Bot', type: 'MCP_PROXY', mcp: { mcpPath: '/mcp' } }),
            ]),
        );
        renderCatalog('/catalog?protocol=A2A_PROXY');

        expect(await screen.findByRole('link', { name: /IT Helpdesk Agent/ })).toBeTruthy();
        expect(screen.queryByRole('link', { name: /Slack Ops Bot/ })).toBeNull();
    });

    it('should keep only matching labels when a label filter is set', async () => {
        respondToPortalPath(
            'post',
            '/apis/_search',
            buildApisResponse([
                buildApi(),
                buildApi({ id: 'api-slack', name: 'Slack Ops Bot', type: 'MCP_PROXY', labels: ['ops'] }),
            ]),
        );
        renderCatalog('/catalog?label=ops');

        expect(await screen.findByRole('link', { name: /Slack Ops Bot/ })).toBeTruthy();
        expect(screen.queryByRole('link', { name: /IT Helpdesk Agent/ })).toBeNull();
    });

    it('should switch to the list view', async () => {
        const user = userEvent.setup();
        respondToPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderCatalog();

        await screen.findByRole('link', { name: /IT Helpdesk Agent/ });
        await user.click(screen.getByRole('radio', { name: 'List' }));

        expect(await screen.findByRole('table')).toBeTruthy();
        expect(within(screen.getByRole('table')).getByRole('link', { name: 'IT Helpdesk Agent' })).toBeTruthy();
    });

    it('should request the next page', async () => {
        const user = userEvent.setup();
        const tracker = trackPortalPath('post', '/apis/_search', buildApisResponse([buildApi()], 30, 1, 12));
        renderCatalog();

        await screen.findByRole('link', { name: /IT Helpdesk Agent/ });
        await user.click(screen.getByRole('button', { name: 'Next page' }));

        await waitFor(() => expect(tracker.lastCall?.url).toContain('page=2'));
    });

    it('should show a first-use empty state when there are no agents', async () => {
        respondToPortalPath('post', '/apis/_search', buildApisResponse([], 0));
        renderCatalog();

        expect(await screen.findByText('No agents yet')).toBeTruthy();
        expect(screen.getByText('Published agents will appear here.')).toBeTruthy();
    });

    it('should show a no-results state when a query matches nothing', async () => {
        respondToPortalPath('post', '/apis/_search', buildApisResponse([], 0));
        renderCatalog('/catalog?query=unknown');

        expect(await screen.findByText('No agents match your search')).toBeTruthy();
    });

    it('should show an error when the catalog fails to load', async () => {
        respondToPortalPathError('post', '/apis/_search', 500);
        renderCatalog();

        expect(await screen.findByRole('alert')).toBeTruthy();
        expect(screen.getByText('Unable to load the catalog. Please try again.')).toBeTruthy();
    });

    it('should link an agent card to the agent detail page', async () => {
        const user = userEvent.setup();
        respondToPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderCatalog();

        await user.click(await screen.findByRole('link', { name: /IT Helpdesk Agent/ }));

        expect(await screen.findByRole('heading', { name: 'Agent detail' })).toBeTruthy();
    });

    it('should search from the catalog search field', async () => {
        const user = userEvent.setup();
        const tracker = trackPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderCatalog();

        await screen.findByRole('link', { name: /IT Helpdesk Agent/ });
        await user.type(screen.getByRole('searchbox', { name: 'Search catalog' }), 'helpdesk{enter}');

        await waitFor(() => expect(tracker.lastCall?.url).toContain('q=helpdesk'));
    });

    it('should populate category options from the portal categories API', async () => {
        respondWith('get', `${TEST_PORTAL_API}/apis/categories`, { data: [buildCategory(), buildCategory({ id: 'hr', name: 'HR' })] });
        respondToPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderCatalog();

        expect(await screen.findByRole('combobox', { name: 'Category' })).toBeTruthy();
        expect(screen.getByRole('combobox', { name: 'Protocol' })).toBeTruthy();
        expect(screen.getByRole('combobox', { name: 'Labels' })).toBeTruthy();
    });
});
