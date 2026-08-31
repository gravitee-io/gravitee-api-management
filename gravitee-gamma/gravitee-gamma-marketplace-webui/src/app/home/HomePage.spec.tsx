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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes, useParams, useSearchParams } from 'react-router-dom';

import { HomePage } from './HomePage';
import { buildApi, buildApisResponse, buildCategory, TEST_PORTAL_API } from '../../testing/factories';
import { respondToPortalPath, respondToPortalPathError, respondWith, trackPortalPath } from '../../testing/helpers';
import { server } from '../../testing/server';

function CatalogStub() {
    const [searchParams] = useSearchParams();
    const query = searchParams.get('query');
    const category = searchParams.get('category');
    return <h1>{category ? `Catalog · ${category}` : query ? `Catalog · ${query}` : 'Catalog'}</h1>;
}

function AgentStub() {
    const { apiId, tab } = useParams();
    return <h1>{tab ? `${apiId} ${tab}` : apiId}</h1>;
}

function renderHome() {
    return renderWithGraphene(
        <MemoryRouter initialEntries={['/']}>
            <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/catalog" element={<CatalogStub />} />
                <Route path="/catalog/:apiId/:tab?" element={<AgentStub />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('HomePage', () => {
    it('should render the hero tagline and search', () => {
        renderHome();

        expect(
            screen.getByRole('heading', {
                name: 'Discover agents',
            }),
        ).toBeTruthy();
        expect(screen.getByRole('searchbox', { name: 'Find an agent' })).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Search' })).toBeTruthy();
    });

    it('should search the catalog from the hero', async () => {
        const user = userEvent.setup();
        renderHome();

        await user.type(screen.getByRole('searchbox', { name: 'Find an agent' }), 'ticket triage');
        await user.click(screen.getByRole('button', { name: 'Search' }));

        expect(await screen.findByRole('heading', { name: 'Catalog · ticket triage' })).toBeTruthy();
    });

    it('should open the catalog when the hero search is empty', async () => {
        const user = userEvent.setup();
        renderHome();

        await user.click(screen.getByRole('button', { name: 'Search' }));

        expect(await screen.findByRole('heading', { name: 'Catalog' })).toBeTruthy();
    });

    it('should request a small featured page from the portal search', async () => {
        const tracker = trackPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderHome();

        expect(await screen.findByRole('link', { name: 'IT Helpdesk Agent' })).toBeTruthy();
        expect(tracker.lastCall?.url).toContain('size=3');
        expect(tracker.lastCall?.url).toContain('page=1');
        expect(tracker.lastCall?.url).not.toContain('q=');
        expect(tracker.lastCall?.url).not.toContain('category=');
    });

    it('should render featured agents from the catalog', async () => {
        respondToPortalPath(
            'post',
            '/apis/_search',
            buildApisResponse([
                buildApi(),
                buildApi({
                    id: 'api-hr',
                    name: 'HR Onboarding',
                    type: 'MCP_PROXY',
                    description: 'New-hire setup.',
                    mcp: { mcpPath: '/mcp' },
                }),
                buildApi({
                    id: 'api-contracts',
                    name: 'Contract QA',
                    type: 'A2A_PROXY',
                    description: 'Clause check.',
                    version: '0.9',
                }),
            ]),
        );
        renderHome();

        expect(await screen.findByRole('link', { name: 'IT Helpdesk Agent' })).toBeTruthy();
        expect(screen.getByRole('link', { name: 'HR Onboarding' })).toBeTruthy();
        expect(screen.getByRole('link', { name: 'Contract QA' })).toBeTruthy();
        expect(screen.getByRole('heading', { name: 'Featured' })).toBeTruthy();
    });

    it('should open agent detail from a featured card', async () => {
        const user = userEvent.setup();
        respondToPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderHome();

        await user.click(await screen.findByRole('link', { name: 'IT Helpdesk Agent' }));

        expect(await screen.findByRole('heading', { name: 'api-helpdesk' })).toBeTruthy();
    });

    it('should open subscribe from a featured agent', async () => {
        const user = userEvent.setup();
        respondToPortalPath('post', '/apis/_search', buildApisResponse([buildApi()]));
        renderHome();

        await user.click(await screen.findByRole('link', { name: 'Subscribe to IT Helpdesk Agent' }));

        expect(await screen.findByRole('heading', { name: 'api-helpdesk subscribe' })).toBeTruthy();
    });

    it('should render category tiles that filter the catalog', async () => {
        const user = userEvent.setup();
        respondWith('get', `${TEST_PORTAL_API}/apis/categories`, {
            data: [buildCategory(), buildCategory({ id: 'hr', name: 'HR', total_apis: 8 })],
        });
        renderHome();

        expect(await screen.findByRole('link', { name: 'IT, 12 agents' })).toBeTruthy();
        expect(screen.getByText('12 agents')).toBeTruthy();
        expect(screen.getByRole('link', { name: 'HR, 8 agents' })).toBeTruthy();
        expect(screen.getByText('8 agents')).toBeTruthy();

        await user.click(screen.getByRole('link', { name: 'IT, 12 agents' }));

        expect(await screen.findByRole('heading', { name: 'Catalog · it' })).toBeTruthy();
    });

    it('should show an empty state when there are no featured agents', async () => {
        respondToPortalPath('post', '/apis/_search', buildApisResponse([], 0));
        renderHome();

        expect(await screen.findByText('No agents yet')).toBeTruthy();
        expect(screen.getByRole('link', { name: 'Browse catalog' })).toBeTruthy();
    });

    it('should show an error when featured agents fail to load', async () => {
        respondToPortalPathError('post', '/apis/_search', 500);
        renderHome();

        expect(await screen.findByRole('alert')).toBeTruthy();
        expect(screen.getByText('Unable to load featured agents. Please try again.')).toBeTruthy();
    });

    it('should show a loading state while featured agents are fetching', () => {
        server.use(
            http.post(
                ({ request }) => {
                    const url = new URL(request.url);
                    return `${url.origin}${url.pathname}` === `${TEST_PORTAL_API}/apis/_search`;
                },
                () => new Promise(() => undefined),
            ),
        );
        renderHome();

        expect(screen.getByRole('status', { name: 'Loading featured agents' })).toBeTruthy();
    });
});
