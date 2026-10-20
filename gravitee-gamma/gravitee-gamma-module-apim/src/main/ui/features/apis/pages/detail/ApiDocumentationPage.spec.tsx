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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';

import { ApiDocumentationPage } from './ApiDocumentationPage';
import { ApimApiError } from '../../../../shared/api/apimClient';
import { notify } from '../../../../shared/notify';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { useApiDocumentation, useDeleteApiDocumentationItem, useUnpublishApiFromPortal } from '../../hooks/useApiDocumentation';
import { useFetchNow } from '../../hooks/useFetchNow';
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
    useUnpublishApiFromPortal: jest.fn(),
}));

jest.mock('../../hooks/useFetchNow', () => ({
    useFetchNow: jest.fn(),
}));

jest.mock('../../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
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

// The dialog has its own spec; here only which page it is opened for, and in which folder, matters.
jest.mock('../../components/detail/documentation/EditPageDetailsDialog', () => ({
    EditPageDetailsDialog: ({
        open,
        apiId,
        page,
        parent,
        syncState,
        onClose,
    }: {
        open: boolean;
        apiId: string;
        page: { title: string };
        parent?: { title: string };
        syncState: string;
        onClose: () => void;
    }) =>
        open ? (
            <div role="dialog" aria-label="Edit page details">
                Editing the details of {page.title} in {apiId}
                {parent ? `, inside ${parent.title}` : ', at the top level'}
                {` (${syncState})`}
                <button type="button" onClick={onClose}>
                    Close
                </button>
            </div>
        ) : null,
}));

// The dialog has its own spec; here only what it is opened for matters.
jest.mock('../../components/detail/documentation/FolderDialog', () => ({
    FolderDialog: ({
        open,
        apiId,
        folder,
        parent,
        syncState,
        itemsInside,
        onClose,
        onCreated,
    }: {
        open: boolean;
        apiId: string;
        folder?: { title: string };
        parent?: { title: string };
        syncState?: string;
        itemsInside?: number;
        onClose: () => void;
        onCreated?: () => void;
    }) =>
        open ? (
            <div role="dialog" aria-label={folder ? 'Edit folder details' : 'Add a folder'}>
                {folder
                    ? `Editing the details of ${folder.title} in ${apiId} (${syncState}), ${itemsInside} items inside`
                    : `Adding a folder to ${apiId}`}
                {parent ? `, inside ${parent.title}` : ', at the top level'}
                <button type="button" onClick={onClose}>
                    Close
                </button>
                <button type="button" onClick={onCreated}>
                    Created
                </button>
            </div>
        ) : null,
}));

// The dialog has its own spec; here only what it is opened for matters.
jest.mock('../../components/detail/documentation/LinkDialog', () => ({
    LinkDialog: ({
        open,
        apiId,
        link,
        parent,
        onClose,
        onCreated,
    }: {
        open: boolean;
        apiId: string;
        link?: { title: string };
        parent?: { title: string };
        onClose: () => void;
        onCreated?: () => void;
    }) =>
        open ? (
            <div role="dialog" aria-label={link ? 'Edit link details' : 'Add a link'}>
                {link ? `Editing the details of ${link.title} in ${apiId}` : `Adding a link to ${apiId}`}
                {parent ? `, inside ${parent.title}` : ', at the top level'}
                <button type="button" onClick={onClose}>
                    Close
                </button>
                <button type="button" onClick={onCreated}>
                    Created
                </button>
            </div>
        ) : null,
}));

// The dialog has its own spec; here only what it is opened for matters.
jest.mock('../../components/detail/documentation/PublicationDialog', () => ({
    PublicationDialog: ({
        open,
        apiId,
        item,
        itemsInside,
        onClose,
    }: {
        open: boolean;
        apiId: string;
        item: { title: string; published: boolean };
        itemsInside: number;
        onClose: () => void;
    }) =>
        open ? (
            <div role="dialog" aria-label={item.published ? 'Unpublish item' : 'Publish item'}>
                {`${item.published ? 'Unpublishing' : 'Publishing'} ${item.title} in ${apiId}, ${itemsInside} items inside`}
                <button type="button" onClick={onClose}>
                    Close
                </button>
            </div>
        ) : null,
}));

// The dialog has its own spec; here only whether the page opens it, for which API and listings, matters.
jest.mock('../../components/detail/documentation/PublishToPortalDialog', () => ({
    PublishToPortalDialog: ({
        open,
        apiId,
        publications,
        hasPage,
        canCreatePage,
        onOverviewPageCreated,
        onClose,
    }: {
        open: boolean;
        apiId: string;
        publications: { sectionName: string }[];
        hasPage: boolean;
        canCreatePage: boolean;
        onOverviewPageCreated: (pageId: string) => void;
        onClose: () => void;
    }) =>
        open ? (
            <div role="dialog" aria-label="Publish to the developer portal">
                Publishing {apiId}, listed in [{publications.map(publication => publication.sectionName).join(', ')}]
                {hasPage ? ', with a page' : ', with no page'}
                {canCreatePage ? ', can create one' : ', cannot create one'}
                <button type="button" onClick={onClose}>
                    Close
                </button>
                <button type="button" onClick={() => onOverviewPageCreated('overview')}>
                    Overview created
                </button>
            </div>
        ) : null,
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseApiDocumentation = useApiDocumentation as jest.Mock;
const mockUseDeleteApiDocumentationItem = useDeleteApiDocumentationItem as jest.Mock;
const mockDelete = jest.fn();
const mockUseUnpublishApiFromPortal = useUnpublishApiFromPortal as jest.Mock;
const mockUnpublish = jest.fn();
const mockUseFetchNow = useFetchNow as jest.Mock;
const mockFetchNow = jest.fn();

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
const STATUS_PAGE: ApiDocumentationItem = {
    ...BASE,
    id: 'status',
    rootId: 'guides',
    parentId: 'guides',
    title: 'Status page',
    type: 'LINK',
    url: 'https://status.acme.io',
    order: 1,
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
    return render(
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
    sessionStorage.clear();
    mockUseHasPermission.mockReturnValue(true);
    mockUseApiDetailContext.mockReturnValue({ api: { id: 'api-1' }, isLoading: false, permissionsReady: true });
    givenDocumentation({ items: [GUIDES, GETTING_STARTED, CHANGELOG], publications: [] });
    mockDelete.mockResolvedValue(undefined);
    mockUseDeleteApiDocumentationItem.mockReturnValue({ mutateAsync: mockDelete, isPending: false });
    mockUnpublish.mockResolvedValue(undefined);
    mockUseUnpublishApiFromPortal.mockReturnValue({ mutateAsync: mockUnpublish, isPending: false });
    mockUseFetchNow.mockReturnValue({ fetchNow: mockFetchNow, isFetching: false });
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

    describe('publishing the API', () => {
        it('offers to publish an API that is not listed, opening the dialog for this API', async () => {
            const user = userEvent.setup();
            renderPage();

            expect(screen.queryByRole('button', { name: 'Unpublish' })).not.toBeInTheDocument();
            await user.click(screen.getByRole('button', { name: 'Publish' }));

            expect(screen.getByRole('dialog', { name: 'Publish to the developer portal' })).toHaveTextContent(
                'Publishing api-1, listed in []',
            );
            await user.click(screen.getByRole('button', { name: 'Close' }));
            expect(screen.queryByRole('dialog', { name: 'Publish to the developer portal' })).not.toBeInTheDocument();
        });

        it('offers to publish an API whose listing is hidden, handing that listing to the dialog', async () => {
            givenDocumentation({ items: [GUIDES], publications: [publication(false)] });
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Publish' }));

            expect(screen.getByRole('dialog', { name: 'Publish to the developer portal' })).toHaveTextContent('listed in [APIs]');
        });

        it('offers to unpublish a published API, and not to publish it', () => {
            givenDocumentation({ items: [GUIDES], publications: [publication(true)] });
            renderPage();

            expect(screen.getByRole('button', { name: 'Unpublish' })).toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        });

        it('requires api-documentation-u to publish or unpublish', () => {
            mockUseHasPermission.mockImplementation((query: { anyOf?: string[] }) => !query.anyOf?.includes('api-documentation-u'));
            renderPage();

            expect(screen.getByText('Not published')).toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();

            givenDocumentation({ items: [GUIDES], publications: [publication(true)] });
            renderPage();
            expect(screen.queryByRole('button', { name: 'Unpublish' })).not.toBeInTheDocument();
        });

        it('tells the dialog the API has a page, and that the user can create one', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Publish' }));

            expect(screen.getByRole('dialog', { name: 'Publish to the developer portal' })).toHaveTextContent(
                'with a page, can create one',
            );
        });

        it('tells the dialog the API has no page when its documentation holds only folders and links', async () => {
            givenDocumentation({ items: [GUIDES, STATUS_PAGE], publications: [] });
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Publish' }));

            expect(screen.getByRole('dialog', { name: 'Publish to the developer portal' })).toHaveTextContent('with no page');
        });

        it('tells the dialog the user cannot create a page without api-documentation-c', async () => {
            mockUseHasPermission.mockImplementation(
                (query: { anyOf?: string[]; allOf?: string[] }) =>
                    ![...(query.anyOf ?? []), ...(query.allOf ?? [])].includes('api-documentation-c'),
            );
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Publish' }));

            expect(screen.getByRole('dialog', { name: 'Publish to the developer portal' })).toHaveTextContent('cannot create one');
        });

        it('opens the Overview page the dialog created in the editor', async () => {
            givenDocumentation({ items: [], publications: [] });
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await user.click(screen.getByRole('button', { name: 'Overview created' }));

            expect(screen.getByText('Editing overview')).toBeInTheDocument();
        });

        it('offers neither while the documentation is loading', () => {
            mockUseApiDocumentation.mockReturnValue({ data: undefined, isLoading: true, isError: false });
            renderPage();

            expect(screen.queryByRole('button', { name: /publish/i })).not.toBeInTheDocument();
        });

        it('offers neither when the documentation failed to load', () => {
            mockUseApiDocumentation.mockReturnValue({ data: undefined, isLoading: false, isError: true });
            renderPage();

            expect(screen.queryByRole('button', { name: /publish/i })).not.toBeInTheDocument();
        });
    });

    describe('unpublishing the API', () => {
        beforeEach(() => {
            mockUseApiDetailContext.mockReturnValue({
                api: { id: 'api-1', name: 'Payment API' },
                isLoading: false,
                permissionsReady: true,
            });
            givenDocumentation({ items: [GUIDES], publications: [publication(true)] });
        });

        it('asks first, naming the API and saying its documentation becomes invisible too', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Unpublish' }));

            const dialog = screen.getByRole('dialog', { name: 'Unpublish "Payment API"?' });
            expect(dialog).toHaveTextContent('The API and its documentation will become invisible in the developer portal.');
            expect(mockUnpublish).not.toHaveBeenCalled();
        });

        it('unpublishes once confirmed, then closes', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Unpublish' }));
            await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Unpublish' }));

            expect(mockUnpublish).toHaveBeenCalled();
            expect(notify.success).toHaveBeenCalledWith('Unpublished from the developer portal');
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('reports a refusal, keeping the confirmation open', async () => {
            const serverError = new ApimApiError(400, 'The API is not published');
            mockUnpublish.mockRejectedValue(serverError);
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Unpublish' }));
            await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Unpublish' }));

            expect(notify.error).toHaveBeenCalledWith(serverError, 'Failed to unpublish the API');
            expect(screen.getByRole('dialog', { name: 'Unpublish "Payment API"?' })).toBeInTheDocument();
        });

        it('does nothing when cancelled', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Unpublish' }));
            await user.click(screen.getByRole('button', { name: 'Cancel' }));

            expect(mockUnpublish).not.toHaveBeenCalled();
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });

    describe('open folders', () => {
        it('stay open when coming back to the documentation', async () => {
            const { unmount } = renderPage();
            await userEvent.click(screen.getByRole('button', { name: 'Expand Guides' }));
            unmount();

            renderPage();

            expect(screen.getByRole('link', { name: 'Getting started' })).toBeInTheDocument();
        });

        it('include the folder a page was just added to', async () => {
            const { unmount } = renderPage();
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Add page' }));
            await userEvent.click(screen.getByRole('button', { name: 'Created' }));
            unmount();

            renderPage();

            expect(screen.getByRole('link', { name: 'Getting started' })).toBeInTheDocument();
        });

        it('include the folder a folder was just added to', async () => {
            const { unmount } = renderPage();
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Add folder' }));
            await userEvent.click(screen.getByRole('button', { name: 'Created' }));
            unmount();

            renderPage();

            expect(screen.getByRole('link', { name: 'Getting started' })).toBeInTheDocument();
        });

        it('include the folder a link was just added to', async () => {
            const { unmount } = renderPage();
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Add link' }));
            await userEvent.click(screen.getByRole('button', { name: 'Created' }));
            unmount();

            renderPage();

            expect(screen.getByRole('link', { name: 'Getting started' })).toBeInTheDocument();
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

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Changelog' }));
            expect(screen.queryByRole('menuitem', { name: 'Delete' })).not.toBeInTheDocument();
            await userEvent.keyboard('{Escape}');
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            expect(screen.queryByRole('menuitem', { name: 'Delete' })).not.toBeInTheDocument();
        });
    });

    describe('fetching an item from its source', () => {
        const SOURCE = { type: 'github-fetcher', configuration: {} };
        const PETSTORE: ApiDocumentationItem = { ...CHANGELOG, id: 'petstore', title: 'Petstore API', source: SOURCE };
        const REFERENCE: ApiDocumentationItem = {
            ...GUIDES,
            id: 'reference',
            rootId: 'reference',
            title: 'Reference',
            source: { ...SOURCE, subtreeImport: true },
        };

        beforeEach(() => {
            givenDocumentation({ items: [PETSTORE, REFERENCE], publications: [] });
        });

        // How a fetch is reported is useFetchNow's; here only which item the tree asks to fetch, and when, matters.
        it('fetches the item chosen in the tree', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Actions for Petstore API' }));
            await user.click(screen.getByRole('menuitem', { name: 'Fetch now' }));

            expect(mockFetchNow).toHaveBeenCalledWith(PETSTORE);
            expect(mockUseFetchNow).toHaveBeenCalledWith('api-1');
        });

        it('offers no fetch while another one is running', async () => {
            mockUseFetchNow.mockReturnValue({ fetchNow: mockFetchNow, isFetching: true });
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: 'Actions for Reference' }));

            expect(screen.getByRole('menuitem', { name: 'Fetch now' })).toHaveAttribute('aria-disabled', 'true');
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

        it('opens the folder dialog for this API from the Add menu, and closes it', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Add documentation' }));
            expect(screen.getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Page', 'Folder', 'Link']);
            await userEvent.click(screen.getByRole('menuitem', { name: 'Folder' }));
            expect(screen.getByRole('dialog', { name: 'Add a folder' })).toHaveTextContent('Adding a folder to api-1, at the top level');

            await userEvent.click(screen.getByRole('button', { name: 'Close' }));
            expect(screen.queryByRole('dialog', { name: 'Add a folder' })).not.toBeInTheDocument();
        });

        it('opens the folder dialog for a folder from its row', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Add folder' }));

            expect(screen.getByRole('dialog', { name: 'Add a folder' })).toHaveTextContent('Adding a folder to api-1, inside Guides');
        });

        it('opens the link dialog for this API from the Add menu, and closes it', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Add documentation' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Link' }));
            expect(screen.getByRole('dialog', { name: 'Add a link' })).toHaveTextContent('Adding a link to api-1, at the top level');

            await userEvent.click(screen.getByRole('button', { name: 'Close' }));
            expect(screen.queryByRole('dialog', { name: 'Add a link' })).not.toBeInTheDocument();
        });

        it('opens the link dialog for a folder from its row', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Add link' }));

            expect(screen.getByRole('dialog', { name: 'Add a link' })).toHaveTextContent('Adding a link to api-1, inside Guides');
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
            expect(screen.queryByRole('menuitem', { name: 'Add folder' })).not.toBeInTheDocument();
            expect(screen.queryByRole('menuitem', { name: 'Add link' })).not.toBeInTheDocument();
        });
    });

    describe('publishing and unpublishing an item', () => {
        it('opens the dialog to unpublish a published item, and closes it', async () => {
            givenDocumentation({ items: [GUIDES, GETTING_STARTED, { ...CHANGELOG, published: true }], publications: [publication(true)] });
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Changelog' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Unpublish' }));
            expect(screen.getByRole('dialog', { name: 'Unpublish item' })).toHaveTextContent(
                'Unpublishing Changelog in api-1, 0 items inside',
            );

            await userEvent.click(screen.getByRole('button', { name: 'Close' }));
            expect(screen.queryByRole('dialog', { name: 'Unpublish item' })).not.toBeInTheDocument();
        });

        it('opens the dialog to publish a folder, counting every item inside', async () => {
            givenDocumentation({ items: [GUIDES, GETTING_STARTED, CHANGELOG], publications: [publication(true)] });
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Publish' }));

            expect(screen.getByRole('dialog', { name: 'Publish item' })).toHaveTextContent('Publishing Guides in api-1, 1 items inside');
        });

        it('keeps Publish disabled while the API is listed only in a hidden listing', async () => {
            givenDocumentation({ items: [GUIDES, GETTING_STARTED, CHANGELOG], publications: [publication(false)] });
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Changelog' }));

            expect(screen.getByRole('menuitem', { name: 'Publish' })).toHaveAttribute('aria-disabled', 'true');
        });
    });

    describe('editing the details of a link', () => {
        beforeEach(() => {
            givenDocumentation({ items: [GUIDES, GETTING_STARTED, STATUS_PAGE, CHANGELOG], publications: [] });
        });

        it('opens them from the link row, naming the folder holding it, and closes them', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Expand Guides' }));
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Status page' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Edit details' }));
            expect(screen.getByRole('dialog', { name: 'Edit link details' })).toHaveTextContent(
                'Editing the details of Status page in api-1, inside Guides',
            );
            expect(screen.queryByRole('dialog', { name: 'Edit folder details' })).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', { name: 'Close' }));
            expect(screen.queryByRole('dialog', { name: 'Edit link details' })).not.toBeInTheDocument();
        });

        it('opens them from the title of the link', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Expand Guides' }));
            await userEvent.click(screen.getByRole('button', { name: 'Status page' }));

            expect(screen.getByRole('dialog', { name: 'Edit link details' })).toBeInTheDocument();
        });
    });

    describe('editing the details of a folder', () => {
        const AUTH: ApiDocumentationItem = {
            ...BASE,
            id: 'auth',
            rootId: 'guides',
            parentId: 'guides',
            title: 'Auth',
            type: 'FOLDER',
            order: 1,
        };
        const OAUTH: ApiDocumentationItem = {
            ...BASE,
            id: 'oauth',
            rootId: 'guides',
            parentId: 'auth',
            title: 'OAuth setup',
            type: 'PAGE',
            portalPageContentId: 'content-3',
            order: 0,
        };

        it('opens them from the folder row, counting every item inside, and closes them', async () => {
            givenDocumentation({ items: [GUIDES, GETTING_STARTED, AUTH, OAUTH, CHANGELOG], publications: [] });
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Edit details' }));
            expect(screen.getByRole('dialog', { name: 'Edit folder details' })).toHaveTextContent(
                'Editing the details of Guides in api-1 (NOT_SYNCED), 3 items inside, at the top level',
            );
            expect(screen.queryByRole('dialog', { name: 'Edit page details' })).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', { name: 'Close' }));
            expect(screen.queryByRole('dialog', { name: 'Edit folder details' })).not.toBeInTheDocument();
        });

        it('names the folder holding the folder', async () => {
            givenDocumentation({ items: [GUIDES, GETTING_STARTED, AUTH, OAUTH, CHANGELOG], publications: [] });
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Expand Guides' }));
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Auth' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Edit details' }));

            expect(screen.getByRole('dialog', { name: 'Edit folder details' })).toHaveTextContent(
                'Editing the details of Auth in api-1 (NOT_SYNCED), 1 items inside, inside Guides',
            );
        });

        it('tells the dialog that a folder with its own source is synced', async () => {
            givenDocumentation({
                items: [{ ...GUIDES, source: { type: 'github-fetcher', configuration: {} } }, GETTING_STARTED, CHANGELOG],
                publications: [],
            });
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Guides' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Edit details' }));

            expect(screen.getByRole('dialog', { name: 'Edit folder details' })).toHaveTextContent('(OWN_SOURCE)');
        });
    });

    describe('editing the details of a page', () => {
        it('opens them from the page row, and closes them', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Changelog' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Edit details' }));
            expect(screen.getByRole('dialog', { name: 'Edit page details' })).toHaveTextContent(
                'Editing the details of Changelog in api-1, at the top level (NOT_SYNCED)',
            );

            await userEvent.click(screen.getByRole('button', { name: 'Close' }));
            expect(screen.queryByRole('dialog', { name: 'Edit page details' })).not.toBeInTheDocument();
        });

        it('names the folder holding the page', async () => {
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Expand Guides' }));
            await userEvent.click(screen.getByRole('button', { name: 'Actions for Getting started' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Edit details' }));

            expect(screen.getByRole('dialog', { name: 'Edit page details' })).toHaveTextContent(
                'Editing the details of Getting started in api-1, inside Guides (NOT_SYNCED)',
            );
        });

        it('tells the dialog that a page with its own source is synced', async () => {
            givenDocumentation({
                items: [GUIDES, GETTING_STARTED, { ...CHANGELOG, source: { type: 'github-fetcher', configuration: {} } }],
                publications: [],
            });
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Changelog' }));
            await userEvent.click(screen.getByRole('menuitem', { name: 'Edit details' }));

            expect(screen.getByRole('dialog', { name: 'Edit page details' })).toHaveTextContent('(OWN_SOURCE)');
        });

        it('offers them only with api-documentation-u', async () => {
            mockUseHasPermission.mockImplementation((query: { anyOf?: string[] }) => !query.anyOf?.includes('api-documentation-u'));
            renderPage();

            await userEvent.click(screen.getByRole('button', { name: 'Actions for Changelog' }));

            expect(screen.queryByRole('menuitem', { name: 'Edit details' })).not.toBeInTheDocument();
            expect(mockUseHasPermission).toHaveBeenCalledWith({ anyOf: ['api-documentation-u'] });
        });
    });
});
