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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { DocumentationTree } from './DocumentationTree';
import type { ApiDocumentationItem } from '../../../types/apiDocumentation';

const BASE = { organizationId: 'DEFAULT', environmentId: 'DEFAULT', area: 'TOP_NAVBAR', published: false, visibility: 'PUBLIC' } as const;

const GUIDES: ApiDocumentationItem = { ...BASE, id: 'guides', rootId: 'guides', title: 'Guides', type: 'FOLDER', order: 0 };
const GETTING_STARTED: ApiDocumentationItem = {
    ...BASE,
    id: 'getting-started',
    rootId: 'guides',
    parentId: 'guides',
    title: 'Getting started',
    type: 'PAGE',
    portalPageContentId: 'content-1',
    order: 0,
    published: true,
};
const OAUTH: ApiDocumentationItem = {
    ...BASE,
    id: 'oauth',
    rootId: 'guides',
    parentId: 'guides',
    title: 'OAuth setup',
    type: 'PAGE',
    portalPageContentId: 'content-2',
    order: 1,
    visibility: 'PRIVATE',
};
const REFERENCE: ApiDocumentationItem = {
    ...BASE,
    id: 'reference',
    rootId: 'reference',
    title: 'Reference',
    type: 'FOLDER',
    order: 1,
    source: { type: 'github-fetcher', configuration: {} },
};
const ENDPOINTS: ApiDocumentationItem = {
    ...BASE,
    id: 'endpoints',
    rootId: 'reference',
    parentId: 'reference',
    title: 'Endpoints',
    type: 'PAGE',
    portalPageContentId: 'content-3',
    order: 0,
};
const STATUS_PAGE: ApiDocumentationItem = {
    ...BASE,
    id: 'status',
    rootId: 'status',
    title: 'Status page',
    type: 'LINK',
    url: 'https://status.acme.io',
    order: 2,
};

const ITEMS = [GUIDES, GETTING_STARTED, OAUTH, REFERENCE, ENDPOINTS, STATUS_PAGE];

function renderTree(overrides: Partial<{ items: ApiDocumentationItem[]; canDelete: boolean; canAdd: boolean; isLoading: boolean }> = {}) {
    const onDelete = jest.fn();
    const onAddPage = jest.fn();
    render(
        <MemoryRouter initialEntries={['/apis/api-1/documentation']}>
            <Routes>
                <Route
                    path="apis/:apiId/documentation"
                    element={
                        <DocumentationTree
                            items={overrides.items ?? ITEMS}
                            isLoading={overrides.isLoading ?? false}
                            canDelete={overrides.canDelete ?? true}
                            onDelete={onDelete}
                            canAdd={overrides.canAdd ?? true}
                            onAddPage={onAddPage}
                        />
                    }
                />
            </Routes>
        </MemoryRouter>,
    );
    return { onDelete, onAddPage };
}

const rowOf = (title: string) => screen.getByText(title).closest('tr') as HTMLElement;
const visibleTitles = () =>
    screen
        .getAllByRole('row')
        .slice(1)
        .map(row => row.textContent?.match(/Guides|Getting started|OAuth setup|Reference|Endpoints|Status page/)?.[0]);

async function expand(user: ReturnType<typeof userEvent.setup>, folderTitle: string) {
    await user.click(screen.getByRole('button', { name: `Expand ${folderTitle}` }));
}

beforeAll(() => {
    // The tooltips on the badges measure their trigger, which jsdom cannot do.
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

describe('DocumentationTree', () => {
    it('starts with every folder collapsed, showing only the top-level items', () => {
        renderTree();

        expect(visibleTitles()).toEqual(['Guides', 'Reference', 'Status page']);
        expect(screen.getByRole('button', { name: 'Expand Guides' })).toHaveAttribute('aria-expanded', 'false');
    });

    it('shows each item with its status and access, children indented below their folder', async () => {
        const user = userEvent.setup();
        renderTree();

        await expand(user, 'Guides');

        expect(visibleTitles()).toEqual(['Guides', 'Getting started', 'OAuth setup', 'Reference', 'Status page']);
        expect(within(rowOf('Getting started')).getByText('Published')).toHaveAttribute('data-variant', 'success');
        expect(within(rowOf('Guides')).getByText('Unpublished')).toBeInTheDocument();
        expect(within(rowOf('OAuth setup')).getByText('Private')).toBeInTheDocument();
        // Both access values are badges, so their text lines up in the column
        expect(within(rowOf('Guides')).getByText('Public')).toHaveAttribute('data-variant', 'outline');
    });

    it('explains what the status and access of an item mean, naming the kind of item', async () => {
        const user = userEvent.setup();
        renderTree();

        await user.hover(within(rowOf('Guides')).getByText('Unpublished'));

        expect((await screen.findByRole('tooltip')).textContent).toBe('Not shown in the developer portal until the folder is published.');
    });

    it('collapses an expanded folder to hide its contents again', async () => {
        const user = userEvent.setup();
        renderTree();

        await expand(user, 'Guides');
        const toggle = screen.getByRole('button', { name: 'Collapse Guides' });
        expect(toggle).toHaveAttribute('aria-expanded', 'true');

        await user.click(toggle);

        expect(screen.queryByText('Getting started')).not.toBeInTheDocument();
        expect(screen.queryByText('OAuth setup')).not.toBeInTheDocument();
    });

    it('links a page to its edit screen', async () => {
        const user = userEvent.setup();
        renderTree();

        await expand(user, 'Guides');

        expect(screen.getByRole('link', { name: 'Getting started' })).toHaveAttribute(
            'href',
            '/apis/api-1/documentation/getting-started/edit',
        );
        expect(screen.queryByRole('link', { name: 'Guides' })).not.toBeInTheDocument();
    });

    it('shows the address of a link', () => {
        renderTree();

        expect(within(rowOf('Status page')).getByText('https://status.acme.io')).toBeInTheDocument();
    });

    it('marks an item fed by an external source as synced', () => {
        renderTree();

        expect(within(rowOf('Reference')).getByText('Synced')).toBeInTheDocument();
        expect(within(rowOf('Guides')).queryByText('Synced')).not.toBeInTheDocument();
    });

    it('offers to delete an item and reports which one', async () => {
        const user = userEvent.setup();
        const { onDelete } = renderTree();

        await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));
        await user.click(screen.getByRole('menuitem', { name: 'Delete' }));

        expect(onDelete).toHaveBeenCalledWith(expect.objectContaining({ item: GUIDES, descendantCount: 2 }));
    });

    it('offers no delete on a synced item or anything below it, which the server refuses', async () => {
        const user = userEvent.setup();
        renderTree();

        await expand(user, 'Reference');

        expect(screen.getByText('Endpoints')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Actions for Reference' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Actions for Endpoints' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Actions for Status page' })).toBeInTheDocument();
    });

    it('offers no delete without the permission', async () => {
        const user = userEvent.setup();
        renderTree({ canDelete: false });

        expect(screen.queryByRole('button', { name: 'Actions for Status page' })).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));
        expect(screen.queryByRole('menuitem', { name: 'Delete' })).not.toBeInTheDocument();
    });

    describe('adding a page to a folder', () => {
        it('offers it from the folder and reports which one', async () => {
            const user = userEvent.setup();
            const { onAddPage } = renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await user.click(screen.getByRole('menuitem', { name: 'Add page' }));

            expect(onAddPage).toHaveBeenCalledWith(GUIDES);
        });

        it('offers it only on a folder', async () => {
            const user = userEvent.setup();
            renderTree();

            await expand(user, 'Guides');
            await user.click(screen.getByRole('button', { name: 'Actions for Getting started' }));

            expect(screen.queryByRole('menuitem', { name: 'Add page' })).not.toBeInTheDocument();
        });

        it('does not offer it on a synced folder, which the server refuses', () => {
            renderTree({ canDelete: false });

            expect(screen.queryByRole('button', { name: 'Actions for Reference' })).not.toBeInTheDocument();
        });

        it('does not offer it without the permission', async () => {
            const user = userEvent.setup();
            renderTree({ canAdd: false });

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));

            expect(screen.queryByRole('menuitem', { name: 'Add page' })).not.toBeInTheDocument();
        });
    });
});
