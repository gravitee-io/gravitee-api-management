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
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState, type ComponentProps } from 'react';
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
    source: { type: 'github-fetcher', configuration: {}, subtreeImport: true },
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

function renderTree(
    overrides: Partial<{
        items: ApiDocumentationItem[];
        canDelete: boolean;
        canAdd: boolean;
        canEdit: boolean;
        isLoading: boolean;
        isApiPublished: boolean;
        isFetching: boolean;
    }> = {},
) {
    const onDelete = jest.fn();
    const onAddPage = jest.fn();
    const onAddFolder = jest.fn();
    const onAddLink = jest.fn();
    const onEditDetails = jest.fn();
    const onChangePublication = jest.fn();
    const onFetch = jest.fn();
    render(
        <MemoryRouter initialEntries={['/apis/api-1/documentation']}>
            <Routes>
                <Route
                    path="apis/:apiId/documentation"
                    element={
                        <StatefulTree
                            items={overrides.items ?? ITEMS}
                            isLoading={overrides.isLoading ?? false}
                            canDelete={overrides.canDelete ?? true}
                            onDelete={onDelete}
                            canAdd={overrides.canAdd ?? true}
                            onAddPage={onAddPage}
                            onAddFolder={onAddFolder}
                            onAddLink={onAddLink}
                            canEdit={overrides.canEdit ?? true}
                            onEditDetails={onEditDetails}
                            isApiPublished={overrides.isApiPublished ?? true}
                            onChangePublication={onChangePublication}
                            isFetching={overrides.isFetching ?? false}
                            onFetch={onFetch}
                        />
                    }
                />
            </Routes>
        </MemoryRouter>,
    );
    return { onDelete, onAddPage, onAddFolder, onAddLink, onEditDetails, onChangePublication, onFetch };
}

// The page owns which folders are open; this stands in for it.
function StatefulTree(props: Omit<ComponentProps<typeof DocumentationTree>, 'expandedIds' | 'onToggle'>) {
    const [expandedIds, setExpandedIds] = useState<ReadonlySet<string>>(new Set());
    const toggle = (id: string) =>
        setExpandedIds(previous => {
            const next = new Set(previous);
            if (!next.delete(id)) next.add(id);
            return next;
        });
    return <DocumentationTree {...props} expandedIds={expandedIds} onToggle={toggle} />;
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

    it('opens the address of a link in a new tab', () => {
        renderTree();

        const address = within(rowOf('Status page')).getByRole('link', { name: 'https://status.acme.io (opens in a new tab)' });
        expect(address).toHaveAttribute('href', 'https://status.acme.io');
        expect(address).toHaveAttribute('target', '_blank');
        expect(address).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('shows an address other than http or https as text only', () => {
        renderTree({ items: [{ ...STATUS_PAGE, url: 'mailto:support@acme.io' }] });

        expect(within(rowOf('Status page')).getByText('mailto:support@acme.io')).toBeInTheDocument();
        expect(within(rowOf('Status page')).queryByRole('link')).not.toBeInTheDocument();
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

    it('offers no delete on anything below a synced item, which the server refuses', async () => {
        const user = userEvent.setup();
        renderTree();

        await expand(user, 'Reference');

        expect(screen.getByText('Endpoints')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Actions for Endpoints' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Actions for Status page' })).toBeInTheDocument();
    });

    it('offers to delete a synced item directly, the server deleting what it holds with it', async () => {
        const user = userEvent.setup();
        const { onDelete } = renderTree();

        await user.click(screen.getByRole('button', { name: 'Actions for Reference' }));
        const deleteItem = screen.getByRole('menuitem', { name: 'Delete' });
        expect(deleteItem).not.toHaveAttribute('aria-disabled');

        await user.click(deleteItem);
        expect(onDelete).toHaveBeenCalledWith(expect.objectContaining({ item: REFERENCE }));
    });

    it('offers no delete without the permission', async () => {
        const user = userEvent.setup();
        renderTree({ canDelete: false });

        await user.click(screen.getByRole('button', { name: 'Actions for Status page' }));
        expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Edit details', 'Publish']);
        await user.keyboard('{Escape}');
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

        it('does not offer it on a synced folder, which the server refuses', async () => {
            const user = userEvent.setup();
            renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Reference' }));

            expect(screen.queryByRole('menuitem', { name: 'Add page' })).not.toBeInTheDocument();
        });

        it('does not offer it without the permission', async () => {
            const user = userEvent.setup();
            renderTree({ canAdd: false });

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));

            expect(screen.queryByRole('menuitem', { name: 'Add page' })).not.toBeInTheDocument();
        });
    });

    describe('adding a folder to a folder', () => {
        it('offers it from the folder, after adding a page, and reports which one', async () => {
            const user = userEvent.setup();
            const { onAddFolder } = renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));

            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual([
                'Add page',
                'Add folder',
                'Add link',
                'Edit details',
                'Publish',
                'Delete',
            ]);
            await user.click(screen.getByRole('menuitem', { name: 'Add folder' }));
            expect(onAddFolder).toHaveBeenCalledWith(GUIDES);
        });

        it('offers it only on a folder', async () => {
            const user = userEvent.setup();
            renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Status page' }));

            expect(screen.queryByRole('menuitem', { name: 'Add folder' })).not.toBeInTheDocument();
        });

        it('does not offer it on a synced folder, which the server refuses', async () => {
            const user = userEvent.setup();
            renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Reference' }));

            expect(screen.queryByRole('menuitem', { name: 'Add folder' })).not.toBeInTheDocument();
        });

        it('does not offer it without the permission', async () => {
            const user = userEvent.setup();
            renderTree({ canAdd: false });

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));

            expect(screen.queryByRole('menuitem', { name: 'Add folder' })).not.toBeInTheDocument();
        });
    });

    describe('adding a link to a folder', () => {
        it('offers it from the folder and reports which one', async () => {
            const user = userEvent.setup();
            const { onAddLink } = renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await user.click(screen.getByRole('menuitem', { name: 'Add link' }));

            expect(onAddLink).toHaveBeenCalledWith(GUIDES);
        });

        it('offers it only on a folder', async () => {
            const user = userEvent.setup();
            renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Status page' }));

            expect(screen.queryByRole('menuitem', { name: 'Add link' })).not.toBeInTheDocument();
        });

        it('does not offer it on a synced folder, which the server refuses', async () => {
            const user = userEvent.setup();
            renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Reference' }));

            expect(screen.queryByRole('menuitem', { name: 'Add link' })).not.toBeInTheDocument();
        });

        it('does not offer it without the permission', async () => {
            const user = userEvent.setup();
            renderTree({ canAdd: false });

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));

            expect(screen.queryByRole('menuitem', { name: 'Add link' })).not.toBeInTheDocument();
        });
    });

    describe('editing the details of a page', () => {
        it('offers it from the page, before delete, and reports which one', async () => {
            const user = userEvent.setup();
            const { onEditDetails } = renderTree();

            await expand(user, 'Guides');
            await user.click(screen.getByRole('button', { name: 'Actions for Getting started' }));

            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Edit details', 'Unpublish', 'Delete']);
            await user.click(screen.getByRole('menuitem', { name: 'Edit details' }));
            expect(onEditDetails).toHaveBeenCalledWith(GETTING_STARTED);
        });

        it('offers it on a page synced from its own source, whose access can still change', async () => {
            const user = userEvent.setup();
            const syncedPage: ApiDocumentationItem = { ...ENDPOINTS, parentId: undefined, rootId: 'endpoints', source: REFERENCE.source };
            renderTree({ items: [syncedPage] });

            await user.click(screen.getByRole('button', { name: 'Actions for Endpoints' }));

            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual([
                'Fetch now',
                'Edit details',
                'Publish',
                'Delete',
            ]);
        });

        it('does not offer it on a page with its own source inside a synced folder, as the editor does not either', async () => {
            const user = userEvent.setup();
            renderTree({ items: [REFERENCE, { ...ENDPOINTS, source: REFERENCE.source }] });

            await expand(user, 'Reference');

            expect(screen.queryByRole('button', { name: 'Actions for Endpoints' })).not.toBeInTheDocument();
        });

        it('does not offer it on a page inside a synced folder, which the server keeps read only', async () => {
            const user = userEvent.setup();
            renderTree();

            await expand(user, 'Reference');

            expect(screen.queryByRole('button', { name: 'Actions for Endpoints' })).not.toBeInTheDocument();
        });

        it('does not offer it without the permission', async () => {
            const user = userEvent.setup();
            renderTree({ canEdit: false });

            await expand(user, 'Guides');
            await user.click(screen.getByRole('button', { name: 'Actions for Getting started' }));

            expect(screen.queryByRole('menuitem', { name: 'Edit details' })).not.toBeInTheDocument();
        });
    });

    describe('editing the details of a folder', () => {
        it('offers it from the folder and reports which one', async () => {
            const user = userEvent.setup();
            const { onEditDetails } = renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await user.click(screen.getByRole('menuitem', { name: 'Edit details' }));

            expect(onEditDetails).toHaveBeenCalledWith(GUIDES);
        });

        it('offers it, besides fetching, on a folder synced from its own source, whose access can still change', async () => {
            const user = userEvent.setup();
            renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Reference' }));

            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual([
                'Fetch now',
                'Edit details',
                'Publish',
                'Delete',
            ]);
        });

        it('does not offer it on a folder inside a synced folder, which the server keeps read only', async () => {
            const user = userEvent.setup();
            const nested: ApiDocumentationItem = {
                ...BASE,
                id: 'v1',
                rootId: 'reference',
                parentId: 'reference',
                title: 'v1',
                type: 'FOLDER',
                order: 1,
            };
            renderTree({ items: [REFERENCE, nested] });

            await expand(user, 'Reference');

            expect(screen.queryByRole('button', { name: 'Actions for v1' })).not.toBeInTheDocument();
        });

        it('does not offer it without the permission', async () => {
            const user = userEvent.setup();
            renderTree({ canEdit: false });

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));

            expect(screen.queryByRole('menuitem', { name: 'Edit details' })).not.toBeInTheDocument();
        });
    });

    describe('editing the details of a link', () => {
        it('offers it from the link, before delete, and reports which one', async () => {
            const user = userEvent.setup();
            const { onEditDetails } = renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Status page' }));

            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Edit details', 'Publish', 'Delete']);
            await user.click(screen.getByRole('menuitem', { name: 'Edit details' }));
            expect(onEditDetails).toHaveBeenCalledWith(STATUS_PAGE);
        });

        it('opens it from the title of the link, which has no content to edit', async () => {
            const user = userEvent.setup();
            const { onEditDetails } = renderTree();

            await user.click(screen.getByRole('button', { name: 'Status page' }));

            expect(onEditDetails).toHaveBeenCalledWith(STATUS_PAGE);
        });

        it('does not offer it on a link inside a synced folder, which the server keeps read only', async () => {
            const user = userEvent.setup();
            const syncedLink: ApiDocumentationItem = { ...STATUS_PAGE, rootId: 'reference', parentId: 'reference' };
            renderTree({ items: [REFERENCE, syncedLink] });

            await expand(user, 'Reference');

            expect(screen.queryByRole('button', { name: 'Actions for Status page' })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Status page' })).not.toBeInTheDocument();
            expect(screen.getByText('Status page')).toBeInTheDocument();
        });

        it('does not offer it without the permission', async () => {
            const user = userEvent.setup();
            renderTree({ canEdit: false });

            await user.click(screen.getByRole('button', { name: 'Actions for Status page' }));

            expect(screen.queryByRole('menuitem', { name: 'Edit details' })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Status page' })).not.toBeInTheDocument();
        });
    });

    describe('publishing and unpublishing an item', () => {
        it('offers to unpublish a published item, after editing its details, and reports which one', async () => {
            const user = userEvent.setup();
            const { onChangePublication } = renderTree();

            await expand(user, 'Guides');
            await user.click(screen.getByRole('button', { name: 'Actions for Getting started' }));
            await user.click(screen.getByRole('menuitem', { name: 'Unpublish' }));

            expect(onChangePublication).toHaveBeenCalledWith(expect.objectContaining({ item: GETTING_STARTED }));
        });

        it('offers to publish an unpublished item and reports which one', async () => {
            const user = userEvent.setup();
            const { onChangePublication } = renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Status page' }));
            await user.click(screen.getByRole('menuitem', { name: 'Publish' }));

            expect(onChangePublication).toHaveBeenCalledWith(expect.objectContaining({ item: STATUS_PAGE }));
        });

        it('does not offer it without the permission', async () => {
            const user = userEvent.setup();
            renderTree({ canEdit: false });

            await user.click(screen.getByRole('button', { name: 'Actions for Status page' }));

            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Delete']);
        });

        it('keeps Publish disabled while the API is unpublished, saying why on hover', async () => {
            const user = userEvent.setup();
            const { onChangePublication } = renderTree({ isApiPublished: false });

            await user.click(screen.getByRole('button', { name: 'Actions for Status page' }));
            const publish = screen.getByRole('menuitem', { name: 'Publish' });
            expect(publish.getAttribute('aria-disabled')).toBe('true');
            await user.hover(publish);

            expect((await screen.findByRole('tooltip')).textContent).toBe("A link can't be published while the API is unpublished.");
            fireEvent.click(publish);
            expect(onChangePublication).not.toHaveBeenCalled();
        });

        it('keeps Publish disabled inside an unpublished folder, which the server refuses, saying why to the keyboard too', async () => {
            const user = userEvent.setup();
            const { onChangePublication } = renderTree();

            await expand(user, 'Guides');
            await user.click(screen.getByRole('button', { name: 'Actions for OAuth setup' }));
            const publish = screen.getByRole('menuitem', { name: 'Publish' });
            expect(publish.getAttribute('aria-disabled')).toBe('true');
            await user.keyboard('{ArrowDown}{ArrowDown}');
            expect(document.activeElement).toBe(publish);

            expect((await screen.findByRole('tooltip')).textContent).toBe("A page can't be published inside an unpublished folder.");
            await user.keyboard('{Enter}');
            expect(onChangePublication).not.toHaveBeenCalled();
        });
    });

    describe('fetching an item from its source', () => {
        const SYNCED_PAGE: ApiDocumentationItem = {
            ...OAUTH,
            id: 'petstore',
            title: 'Petstore API',
            order: 2,
            source: { type: 'http-fetcher', configuration: {} },
        };

        it('offers it first on a folder synced from its own source, and reports which one', async () => {
            const user = userEvent.setup();
            const { onFetch } = renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Reference' }));
            expect(screen.getAllByRole('menuitem')[0]).toHaveTextContent('Fetch now');
            await user.click(screen.getByRole('menuitem', { name: 'Fetch now' }));

            expect(onFetch).toHaveBeenCalledWith(REFERENCE);
        });

        it('offers it on a plain folder holding a synced page, to fetch every such page', async () => {
            const user = userEvent.setup();
            const { onFetch } = renderTree({ items: [GUIDES, GETTING_STARTED, SYNCED_PAGE] });

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await user.click(screen.getByRole('menuitem', { name: 'Fetch now' }));

            expect(onFetch).toHaveBeenCalledWith(GUIDES);
        });

        it('does not offer it on a plain folder holding no synced page', async () => {
            const user = userEvent.setup();
            renderTree();

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));

            expect(screen.queryByRole('menuitem', { name: 'Fetch now' })).not.toBeInTheDocument();
        });

        it('does not offer it without the permission to update the documentation', async () => {
            const user = userEvent.setup();
            renderTree({ canEdit: false, items: [GUIDES, SYNCED_PAGE] });

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));

            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Add page', 'Add folder', 'Add link', 'Delete']);
        });

        it('keeps it disabled while a fetch is running', async () => {
            const user = userEvent.setup();
            const { onFetch } = renderTree({ isFetching: true });

            await user.click(screen.getByRole('button', { name: 'Actions for Reference' }));
            const fetchNow = screen.getByRole('menuitem', { name: 'Fetch now' });
            expect(fetchNow).toHaveAttribute('aria-disabled', 'true');
            await user.click(fetchNow);

            expect(onFetch).not.toHaveBeenCalled();
        });
    });

    describe('right-clicking a row', () => {
        it('opens the same menu as the row button, anywhere in the row', async () => {
            const user = userEvent.setup();
            const { onDelete } = renderTree();

            const browserMenuShown = fireEvent.contextMenu(within(rowOf('Guides')).getByText('Unpublished'));

            expect(browserMenuShown).toBe(false);
            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual([
                'Add page',
                'Add folder',
                'Add link',
                'Edit details',
                'Publish',
                'Delete',
            ]);
            await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
            expect(onDelete).toHaveBeenCalledWith(expect.objectContaining({ item: GUIDES }));
        });

        it('offers only what the row button offers for that row', () => {
            renderTree();

            fireEvent.contextMenu(screen.getByText('Status page'));

            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Edit details', 'Publish', 'Delete']);
        });

        it('leaves the browser menu alone on an item inside a synced folder, which has no actions', async () => {
            const user = userEvent.setup();
            renderTree();
            await expand(user, 'Reference');

            const browserMenuShown = fireEvent.contextMenu(screen.getByText('Endpoints'));

            expect(browserMenuShown).toBe(true);
            expect(screen.queryByRole('menu')).not.toBeInTheDocument();
        });

        it('leaves the browser menu alone on the column headers', () => {
            renderTree();

            const browserMenuShown = fireEvent.contextMenu(screen.getByText('Status'));

            expect(browserMenuShown).toBe(true);
            expect(screen.queryByRole('menu')).not.toBeInTheDocument();
        });

        it('leaves the browser menu alone without the permissions', () => {
            renderTree({ canDelete: false, canAdd: false, canEdit: false });

            expect(fireEvent.contextMenu(screen.getByText('Guides'))).toBe(true);
            expect(screen.queryByRole('menu')).not.toBeInTheDocument();
        });
    });
});
