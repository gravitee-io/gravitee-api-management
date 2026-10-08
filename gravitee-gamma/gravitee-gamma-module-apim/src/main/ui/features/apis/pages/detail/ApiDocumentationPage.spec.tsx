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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';

import { ApiDocumentationPage } from './ApiDocumentationPage';
import { ApimApiError } from '../../../../shared/api/apimClient';
import { notify } from '../../../../shared/notify';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { useApiDocumentation, useDeleteApiDocumentationItem } from '../../hooks/useApiDocumentation';
import type { ApiDocumentationItem, ApiPortalNavigationItemsResponse, ApiPortalPublication } from '../../types/apiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(() => true),
}));

jest.mock('../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../hooks/useApiDocumentation', () => ({
    useApiDocumentation: jest.fn(),
    useDeleteApiDocumentationItem: jest.fn(),
}));

jest.mock('../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

// The dialog has its own spec; here only whether the page opens it, for which API, and what follows, matters.
jest.mock('../../components/detail/documentation/CreatePageDialog', () => ({
    CreatePageDialog: ({
        open,
        apiId,
        parent,
        onClose,
        onCreated,
    }: {
        open: boolean;
        apiId: string;
        parent?: { title: string };
        onClose: () => void;
        onCreated: (pageId: string) => void;
    }) =>
        open ? (
            <div role="dialog" aria-label="Add a page">
                Adding a page to {apiId}
                {parent ? ` in ${parent.title}` : ' at the top level'}
                <button type="button" onClick={onClose}>
                    Close
                </button>
                <button type="button" onClick={() => onCreated('new-page')}>
                    Created
                </button>
            </div>
        ) : null,
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseApiDocumentation = useApiDocumentation as jest.Mock;
const mockUseDeleteApiDocumentationItem = useDeleteApiDocumentationItem as jest.Mock;
const mockDelete = jest.fn();

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
};
const CHANGELOG: ApiDocumentationItem = {
    ...BASE,
    id: 'changelog',
    rootId: 'changelog',
    title: 'Changelog',
    type: 'PAGE',
    portalPageContentId: 'content-2',
    order: 1,
};

function publication(published: boolean): ApiPortalPublication {
    return {
        portalId: 'DEFAULT',
        sectionName: 'APIs',
        portalNavigationItem: {
            ...BASE,
            id: 'listing',
            rootId: 'apis',
            parentId: 'apis',
            title: 'Payment API',
            order: 0,
            published,
            type: 'API',
            apiId: 'api-1',
        },
    };
}

function givenDocumentation(data: ApiPortalNavigationItemsResponse) {
    mockUseApiDocumentation.mockReturnValue({ data, isLoading: false, isError: false });
}

function EditRoute() {
    const { pageId } = useParams<{ pageId: string }>();
    return <p>Editing {pageId}</p>;
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/apis/api-1/documentation']}>
            <Routes>
                <Route path="apis/:apiId/documentation">
                    <Route index element={<ApiDocumentationPage />} />
                    <Route path=":pageId/edit" element={<EditRoute />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUseApiDetailContext.mockReturnValue({ api: { id: 'api-1' }, isLoading: false, permissionsReady: true });
    givenDocumentation({ items: [GUIDES, GETTING_STARTED, CHANGELOG], publications: [] });
    mockDelete.mockResolvedValue(undefined);
    mockUseDeleteApiDocumentationItem.mockReturnValue({ mutateAsync: mockDelete, isPending: false });
});

describe('ApiDocumentationPage', () => {
    it('renders the documentation heading for a user who can read it', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'Documentation' })).toBeInTheDocument();
    });

    it('renders nothing until the permission request has resolved', () => {
        mockUseApiDetailContext.mockReturnValue({ api: { id: 'api-1' }, isLoading: false, permissionsReady: false });
        renderPage();

        expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    });

    // The nav entry is hidden without the permission, but the route stays reachable by URL.
    it('tells a user without api-documentation-r that they cannot view the documentation', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();

        expect(screen.getByText(/don.t have permission to view/i)).toBeInTheDocument();
        expect(mockUseApiDocumentation).not.toHaveBeenCalled();
    });

    it('asks for api-documentation-r, the same permission the nav entry is gated on', () => {
        renderPage();

        expect(mockUseHasPermission).toHaveBeenCalledWith({ anyOf: ['api-documentation-r'] });
    });

    it("lists the API's documentation", () => {
        renderPage();

        expect(mockUseApiDocumentation).toHaveBeenCalledWith('api-1');
        expect(screen.getByText('Guides')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Changelog' })).toBeInTheDocument();
    });

    describe('publication status', () => {
        it('names the section the API is published in', () => {
            givenDocumentation({ items: [GUIDES], publications: [publication(true)] });
            renderPage();

            expect(screen.getByText('Published in APIs')).toHaveAttribute('data-variant', 'success');
        });

        it('says the API is not published when it is not listed', () => {
            renderPage();

            expect(screen.getByText('Not published')).toBeInTheDocument();
        });

        it('says the API is not published when its listing is hidden', () => {
            givenDocumentation({ items: [GUIDES], publications: [publication(false)] });
            renderPage();

            expect(screen.getByText('Not published')).toBeInTheDocument();
        });

        it('claims no publication status while the documentation is loading', () => {
            mockUseApiDocumentation.mockReturnValue({ data: undefined, isLoading: true, isError: false });
            renderPage();

            expect(screen.queryByText(/published/i)).not.toBeInTheDocument();
        });

        it('claims no publication status when the documentation failed to load', () => {
            mockUseApiDocumentation.mockReturnValue({ data: undefined, isLoading: false, isError: true });
            renderPage();

            expect(screen.queryByText(/published/i)).not.toBeInTheDocument();
        });
    });

    it('invites to write documentation when the API has none', () => {
        givenDocumentation({ items: [], publications: [] });
        renderPage();

        expect(screen.getByText('No documentation yet')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('reports a documentation list that failed to load', () => {
        mockUseApiDocumentation.mockReturnValue({ data: undefined, isLoading: false, isError: true });
        renderPage();

        expect(screen.getByText(/failed to load the documentation/i)).toBeInTheDocument();
    });

    describe('deleting an item', () => {
        it('asks for confirmation, warning about the contents of a folder, then deletes it', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await user.click(screen.getByRole('menuitem', { name: 'Delete' }));

            const dialog = screen.getByRole('dialog');
            expect(dialog).toHaveTextContent('Delete "Guides"?');
            expect(dialog).toHaveTextContent('The 1 item inside this folder is deleted too.');

            await user.click(screen.getByRole('button', { name: 'Delete' }));

            expect(mockDelete).toHaveBeenCalledWith('guides');
            expect(notify.success).toHaveBeenCalledWith("'Guides' deleted");
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('does not warn about contents when deleting a page', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Actions for Changelog' }));
            await user.click(screen.getByRole('menuitem', { name: 'Delete' }));

            expect(screen.getByRole('dialog')).not.toHaveTextContent(/inside this folder/);
        });

        it('deletes nothing when cancelled', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Actions for Changelog' }));
            await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
            await user.click(screen.getByRole('button', { name: 'Cancel' }));

            expect(mockDelete).not.toHaveBeenCalled();
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('reports why the server refused the delete', async () => {
            const serverError = new ApimApiError(400, 'The item is fetched from an external source and cannot be deleted');
            mockDelete.mockRejectedValue(serverError);
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Actions for Changelog' }));
            await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
            await user.click(screen.getByRole('button', { name: 'Delete' }));

            expect(notify.error).toHaveBeenCalledWith(serverError, "Failed to delete 'Changelog'");
        });

        it('offers no delete without api-documentation-d', async () => {
            mockUseHasPermission.mockImplementation(({ anyOf = [] }: { anyOf?: string[] }) => !anyOf.includes('api-documentation-d'));
            renderPage();

            expect(screen.queryByRole('button', { name: 'Actions for Changelog' })).not.toBeInTheDocument();
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            expect(screen.queryByRole('menuitem', { name: 'Delete' })).not.toBeInTheDocument();
        });
    });

    describe('adding documentation', () => {
        it('opens the page dialog for this API from the Add menu, and closes it', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Add documentation' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Page' }));
            expect(screen.getByRole('dialog', { name: 'Add a page' })).toHaveTextContent('Adding a page to api-1 at the top level');

            await userEvent.click(screen.getByRole('button', { name: 'Close' }));
            expect(screen.queryByRole('dialog', { name: 'Add a page' })).not.toBeInTheDocument();
        });

        it('opens the page dialog for a folder from its row', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Add page' }));

            expect(screen.getByRole('dialog', { name: 'Add a page' })).toHaveTextContent('Adding a page to api-1 in Guides');
        });

        it('opens the new page once it is created', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Add documentation' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Page' }));
            await userEvent.click(screen.getByRole('button', { name: 'Created' }));

            expect(screen.getByText('Editing new-page')).toBeInTheDocument();
        });

        it('offers the same Add menu from the empty state', async () => {
            givenDocumentation({ items: [], publications: [] });
            renderPage();

            const addButtons = screen.getAllByRole('button', { name: 'Add documentation' });
            await userEvent.click(addButtons[addButtons.length - 1]!);
            await userEvent.click(screen.getByRole('menuitem', { name: 'Page' }));

            expect(screen.getByRole('dialog', { name: 'Add a page' })).toBeInTheDocument();
        });

        // A page is created, then its content is saved: with create alone it would stay empty.
        it('requires both api-documentation-c and api-documentation-u', () => {
            renderPage();

            expect(mockUseHasPermission).toHaveBeenCalledWith({ allOf: ['api-documentation-c', 'api-documentation-u'] });
        });

        it('offers no add without those permissions', async () => {
            mockUseHasPermission.mockImplementation((query: { allOf?: string[] }) => query.allOf === undefined);
            renderPage();

            expect(screen.queryByRole('button', { name: 'Add documentation' })).not.toBeInTheDocument();
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            expect(screen.queryByRole('menuitem', { name: 'Add page' })).not.toBeInTheDocument();
        });
    });
});
