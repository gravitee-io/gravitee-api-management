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
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { AgentDetailPage } from './AgentDetailPage';
import { buildApi, buildPage, buildPagesResponse } from '../../testing/factories';
import { respondToPortalPath, respondToPortalPathError } from '../../testing/helpers';
import { PortalLayout } from '../layout/PortalLayout';

function renderDetail(path = '/catalog/api-helpdesk') {
    return renderWithGraphene(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route element={<PortalLayout />}>
                    <Route path="/catalog/:apiId/:tab?" element={<AgentDetailPage />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('AgentDetailPage', () => {
    it('should render overview metadata from the portal API', async () => {
        respondToPortalPath('get', '/apis/api-helpdesk', buildApi());
        renderDetail();

        expect(await screen.findByRole('heading', { name: 'IT Helpdesk Agent' })).toBeTruthy();
        expect(screen.getByText('Triage and route IT tickets to the right queue.')).toBeTruthy();
        expect(screen.getByText('v1.2')).toBeTruthy();
        expect(screen.getByText('A2A')).toBeTruthy();
        expect(screen.getByText('Publisher: Acme Platform')).toBeTruthy();
        expect(screen.getByText('ticketing')).toBeTruthy();
        expect(screen.getByText('https://gw.example/a2a/it-helpdesk')).toBeTruthy();
        expect(screen.getByText('No tools published for this agent.')).toBeTruthy();
    });

    it('should list MCP tools and the MCP server URL', async () => {
        respondToPortalPath(
            'get',
            '/apis/api-helpdesk',
            buildApi({
                type: 'MCP_PROXY',
                entrypoints: ['https://gw.example/mcp/helpdesk'],
                mcp: {
                    mcpPath: '/mcp',
                    tools: [
                        { toolDefinition: { name: 'create_ticket', description: 'Open a ticket in the ITSM system' } },
                        { toolDefinition: { name: 'list_queues', description: 'List available assignment queues' } },
                    ],
                },
            }),
        );
        renderDetail();

        expect(await screen.findByText('create_ticket')).toBeTruthy();
        expect(screen.getByText('Open a ticket in the ITSM system')).toBeTruthy();
        expect(screen.getByText('list_queues')).toBeTruthy();
        expect(screen.getByText('https://gw.example/mcp/helpdesk/mcp')).toBeTruthy();
    });

    it('should show an error when the agent is missing', async () => {
        respondToPortalPathError('get', '/apis/api-helpdesk', 404);
        renderDetail();

        expect(await screen.findByRole('alert')).toBeTruthy();
        expect(screen.getByText('This agent could not be found.')).toBeTruthy();
    });

    it('should show an error when the agent fails to load', async () => {
        respondToPortalPathError('get', '/apis/api-helpdesk', 500);
        renderDetail();

        expect(await screen.findByRole('alert')).toBeTruthy();
        expect(screen.getByText('Unable to load this agent. Please try again.')).toBeTruthy();
    });

    it('should open the subscribe tab from the overview CTA', async () => {
        const user = userEvent.setup();
        respondToPortalPath('get', '/apis/api-helpdesk', buildApi());
        renderDetail();

        await user.click(await screen.findByRole('button', { name: 'Subscribe to this agent' }));

        expect(await screen.findByRole('heading', { name: 'Subscribe to IT Helpdesk Agent' })).toBeTruthy();
        expect(screen.getByText(/Choose a plan, accept terms if required/)).toBeTruthy();
    });

    it('should tell the user to subscribe before chatting', async () => {
        const user = userEvent.setup();
        respondToPortalPath('get', '/apis/api-helpdesk', buildApi());
        renderDetail();

        await user.click(await screen.findByRole('tab', { name: 'Chat' }));

        expect(await screen.findByText('Chat is available after you subscribe.')).toBeTruthy();
    });

    it('should render documentation pages and load selected content', async () => {
        const user = userEvent.setup();
        respondToPortalPath('get', '/apis/api-helpdesk', buildApi());
        respondToPortalPath(
            'get',
            '/apis/api-helpdesk/pages',
            buildPagesResponse([
                buildPage({ id: 'folder-guides', name: 'Guides', type: 'FOLDER', order: 0 }),
                buildPage({ id: 'page-overview', name: 'Overview', parent: 'folder-guides', type: 'MARKDOWN', order: 0 }),
                buildPage({
                    id: 'page-skills',
                    name: 'Skills',
                    parent: 'folder-guides',
                    type: 'MARKDOWN',
                    order: 1,
                    content: '# Skills\n\nTicketing and triage.',
                }),
            ]),
        );
        respondToPortalPath(
            'get',
            '/apis/api-helpdesk/pages/page-overview',
            buildPage({ id: 'page-overview', name: 'Overview', content: '# Getting started\n\nSubscribe to a plan.' }),
        );
        respondToPortalPath(
            'get',
            '/apis/api-helpdesk/pages/page-skills',
            buildPage({ id: 'page-skills', name: 'Skills', content: '# Skills\n\nTicketing and triage.' }),
        );
        renderDetail();

        await user.click(await screen.findByRole('tab', { name: 'Documentation' }));

        const tree = await screen.findByRole('navigation', { name: 'Documentation' });
        expect(within(tree).getByRole('button', { name: 'Overview' })).toBeTruthy();
        expect(await screen.findByRole('heading', { name: 'Getting started' })).toBeTruthy();

        await user.click(within(tree).getByRole('button', { name: 'Skills' }));

        expect(await screen.findByRole('heading', { name: 'Skills' })).toBeTruthy();
        expect(screen.getByText('Ticketing and triage.')).toBeTruthy();
    });

    it('should show an empty state when there are no documentation pages', async () => {
        const user = userEvent.setup();
        respondToPortalPath('get', '/apis/api-helpdesk', buildApi());
        respondToPortalPath('get', '/apis/api-helpdesk/pages', buildPagesResponse([]));
        renderDetail();

        await user.click(await screen.findByRole('tab', { name: 'Documentation' }));

        expect(await screen.findByText('No documentation has been published for this agent.')).toBeTruthy();
    });

    it('should redirect an unknown tab to overview', async () => {
        respondToPortalPath('get', '/apis/api-helpdesk', buildApi());
        renderDetail('/catalog/api-helpdesk/unknown');

        expect(await screen.findByRole('heading', { name: 'IT Helpdesk Agent' })).toBeTruthy();
        expect(await screen.findByText('Skills / labels')).toBeTruthy();
    });
});
