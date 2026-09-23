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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiDocumentationPagesPage } from './ApiDocumentationPagesPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import {
    useApiDocumentationTree,
    useCreateDocumentationPage,
    useDeleteDocumentationPage,
    usePortalDocumentationFolders,
    usePublishApiWithDefaultOverview,
    usePublishDocumentationPage,
    useSyncDocumentationToPortal,
    useUnpublishApiFromPortal,
    useUnpublishDocumentationFromPortal,
    useUnpublishDocumentationPage,
    useUpdateDocumentationPage,
} from '../../../hooks/useApiDocumentation';
import type { DocumentationPage } from '../../../types/documentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../hooks/useApiDocumentation', () => ({
    useApiDocumentationTree: jest.fn(),
    useCreateDocumentationPage: jest.fn(),
    useUpdateDocumentationPage: jest.fn(),
    usePublishDocumentationPage: jest.fn(),
    useUnpublishDocumentationPage: jest.fn(),
    useDeleteDocumentationPage: jest.fn(),
    usePortalDocumentationFolders: jest.fn(),
    useSyncDocumentationToPortal: jest.fn(),
    useUnpublishDocumentationFromPortal: jest.fn(),
    useUnpublishApiFromPortal: jest.fn(),
    usePublishApiWithDefaultOverview: jest.fn(),
}));

jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseTree = useApiDocumentationTree as jest.Mock;
const mockUseCreate = useCreateDocumentationPage as jest.Mock;
const mockUseUpdate = useUpdateDocumentationPage as jest.Mock;
const mockUsePublish = usePublishDocumentationPage as jest.Mock;
const mockUseUnpublish = useUnpublishDocumentationPage as jest.Mock;
const mockUseDelete = useDeleteDocumentationPage as jest.Mock;
const mockUsePortalFolders = usePortalDocumentationFolders as jest.Mock;
const mockUsePortalSync = useSyncDocumentationToPortal as jest.Mock;
const mockUsePortalUnpublish = useUnpublishDocumentationFromPortal as jest.Mock;
const mockUseUnpublishApi = useUnpublishApiFromPortal as jest.Mock;
const mockUsePublishEmptyApi = usePublishApiWithDefaultOverview as jest.Mock;

const FOLDER: DocumentationPage = { id: 'folder-1', name: 'Guides', type: 'FOLDER', visibility: 'PUBLIC', order: 0 };
const PAGE: DocumentationPage = {
    id: 'page-1',
    name: 'Getting started',
    type: 'MARKDOWN',
    visibility: 'PUBLIC',
    published: false,
    order: 1,
    updatedAt: '2026-01-01T00:00:00.000Z',
};
const NESTED: DocumentationPage = {
    id: 'page-2',
    name: 'Nested guide',
    type: 'MARKDOWN',
    parentId: 'folder-1',
    published: false,
    order: 0,
};

function mutationMock(mutate = jest.fn(), mutateAsync = jest.fn().mockResolvedValue({})) {
    return { mutate, mutateAsync, isPending: false };
}

function renderPage(path = '/apis/api-1/documentation') {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="/apis/:apiId/documentation" element={<ApiDocumentationPagesPage />} />
                <Route path="/apis/:apiId/documentation/new" element={<div>new page</div>} />
                <Route path="/apis/:apiId/documentation/:pageId" element={<div>edit page</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApiDocumentationPagesPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseHasPermission.mockReturnValue(true);
        mockUseApiDetailContext.mockReturnValue({
            api: { id: 'api-1', name: 'Orders', type: 'PROXY', definitionVersion: 'V4' },
            isLoading: false,
            permissionsReady: true,
        });
        mockUseTree.mockReturnValue({ pages: [FOLDER, PAGE, NESTED], isLoading: false, isError: false, refetch: jest.fn() });
        mockUseCreate.mockReturnValue(mutationMock());
        mockUseUpdate.mockReturnValue(mutationMock());
        mockUsePublish.mockReturnValue(mutationMock());
        mockUseUnpublish.mockReturnValue(mutationMock());
        mockUseDelete.mockReturnValue(mutationMock());
        mockUsePortalFolders.mockReturnValue({
            data: { folders: [{ id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' }], placement: null },
            isFetching: false,
            refetch: jest.fn().mockResolvedValue({
                data: { folders: [{ id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' }], placement: null },
            }),
        });
        mockUsePortalSync.mockReturnValue(mutationMock());
        mockUsePortalUnpublish.mockReturnValue(mutationMock());
        mockUseUnpublishApi.mockReturnValue(mutationMock());
        mockUsePublishEmptyApi.mockReturnValue(mutationMock());
    });

    it('lists the nested documentation tree', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'Documentation' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Sync with Portal' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Publish API' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Unpublish API' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Getting started' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Nested guide' })).toBeInTheDocument();
        expect(screen.getAllByText('Unpublished').length).toBeGreaterThan(0);
    });

    it('prompts for a Navigation folder then publishes the API and selected docs under it', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue({});
        const syncAsync = jest.fn().mockResolvedValue({});
        mockUsePublish.mockReturnValue(mutationMock(jest.fn(), mutateAsync));
        mockUsePortalSync.mockReturnValue(mutationMock(jest.fn(), syncAsync));
        renderPage();

        await user.click(screen.getByRole('checkbox', { name: 'Select Getting started' }));
        expect(screen.getByRole('button', { name: 'Publish' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Publish' }));
        expect(screen.getByText('Publish to Next Gen Portal')).toBeInTheDocument();

        const dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: 'Publish' }));

        expect(mutateAsync).toHaveBeenCalledWith('page-1');
        expect(syncAsync).toHaveBeenCalledWith({
            title: 'Orders',
            folder: { id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' },
            placement: null,
            pages: [FOLDER, PAGE, NESTED],
            pageIds: ['page-1'],
        });
    });

    it('publishes in place when the API is already published and move is not requested', async () => {
        const user = userEvent.setup();
        const syncAsync = jest.fn().mockResolvedValue({});
        mockUsePortalSync.mockReturnValue(mutationMock(jest.fn(), syncAsync));
        const placement = {
            folderId: 'nav-1',
            folderPath: 'Docs',
            itemId: 'api-nav',
            area: 'TOP_NAVBAR',
            published: true,
        };
        mockUsePortalFolders.mockReturnValue({
            data: {
                folders: [
                    { id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' },
                    { id: 'nav-2', path: 'Other', area: 'TOP_NAVBAR' },
                ],
                placement,
            },
            isFetching: false,
            refetch: jest.fn().mockResolvedValue({
                data: {
                    folders: [
                        { id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' },
                        { id: 'nav-2', path: 'Other', area: 'TOP_NAVBAR' },
                    ],
                    placement,
                },
            }),
        });
        renderPage();

        await user.click(screen.getByRole('checkbox', { name: 'Select Getting started' }));
        await user.click(screen.getByRole('button', { name: 'Publish' }));

        expect(screen.getByText(/already published in “Docs”/)).toBeInTheDocument();
        expect(screen.getByText(/Publish the whole API documentation to another folder/)).toBeInTheDocument();
        const dialog = screen.getByRole('dialog');
        expect(within(dialog).queryByRole('combobox')).not.toBeInTheDocument();
        await user.click(within(dialog).getByRole('button', { name: 'Publish' }));

        expect(syncAsync).toHaveBeenCalledWith(
            expect.objectContaining({
                folder: { id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' },
                placement,
            }),
        );
    });

    it('moves the whole API documentation when republishing to another folder', async () => {
        const user = userEvent.setup();
        const syncAsync = jest.fn().mockResolvedValue({});
        mockUsePortalSync.mockReturnValue(mutationMock(jest.fn(), syncAsync));
        const placement = {
            folderId: 'nav-1',
            folderPath: 'Docs',
            itemId: 'api-nav',
            area: 'TOP_NAVBAR',
            published: true,
        };
        mockUsePortalFolders.mockReturnValue({
            data: {
                folders: [
                    { id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' },
                    { id: 'nav-2', path: 'Other', area: 'TOP_NAVBAR' },
                ],
                placement,
            },
            isFetching: false,
            refetch: jest.fn().mockResolvedValue({
                data: {
                    folders: [
                        { id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' },
                        { id: 'nav-2', path: 'Other', area: 'TOP_NAVBAR' },
                    ],
                    placement,
                },
            }),
        });
        renderPage();

        await user.click(screen.getByRole('checkbox', { name: 'Select Getting started' }));
        await user.click(screen.getByRole('button', { name: 'Publish' }));

        const dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('checkbox', { name: /Publish the whole API documentation/i }));
        expect(within(dialog).getByRole('combobox')).toBeInTheDocument();
        // Only one destination folder remains after excluding the current one — auto-selected.
        expect(within(dialog).getByRole('button', { name: 'Publish' })).toBeEnabled();
        await user.click(within(dialog).getByRole('button', { name: 'Publish' }));

        expect(syncAsync).toHaveBeenCalledWith(
            expect.objectContaining({
                folder: { id: 'nav-2', path: 'Other', area: 'TOP_NAVBAR' },
                placement,
            }),
        );
    });

    it('shows Unpublish for selected published pages and unpublishes them from the portal', async () => {
        const user = userEvent.setup();
        const unpublishAsync = jest.fn().mockResolvedValue({});
        const portalUnpublishAsync = jest.fn().mockResolvedValue({});
        mockUseTree.mockReturnValue({
            pages: [{ ...PAGE, published: true }, FOLDER, NESTED],
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        });
        mockUseUnpublish.mockReturnValue(mutationMock(jest.fn(), unpublishAsync));
        mockUsePortalUnpublish.mockReturnValue(mutationMock(jest.fn(), portalUnpublishAsync));
        mockUsePortalFolders.mockReturnValue({
            data: {
                folders: [{ id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' }],
                placement: { folderId: 'nav-1', folderPath: 'Docs', itemId: 'api-nav', area: 'TOP_NAVBAR', published: true },
            },
            isFetching: false,
            refetch: jest.fn().mockResolvedValue({
                data: {
                    folders: [{ id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' }],
                    placement: { folderId: 'nav-1', folderPath: 'Docs', itemId: 'api-nav', area: 'TOP_NAVBAR', published: true },
                },
            }),
        });
        renderPage();

        await user.click(screen.getByRole('checkbox', { name: 'Select Getting started' }));
        expect(screen.getByRole('button', { name: 'Unpublish' })).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Unpublish' }));

        const dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: /unpublish/i }));

        expect(unpublishAsync).toHaveBeenCalledWith('page-1');
        expect(portalUnpublishAsync).toHaveBeenCalledWith({
            placement: { folderId: 'nav-1', folderPath: 'Docs', itemId: 'api-nav', area: 'TOP_NAVBAR', published: true },
            pages: [{ ...PAGE, published: true }, FOLDER, NESTED],
            pageIds: ['page-1'],
        });
    });

    it('shows the educational empty landing when there are no pages', () => {
        mockUseTree.mockReturnValue({ pages: [], isLoading: false, isError: false, refetch: jest.fn() });
        renderPage();
        expect(screen.getByText('Why add documentation?')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Publish API' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Add folder' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /add page/i })).toBeInTheDocument();
    });

    it('creates a default Overview then publishes the API when documentation is empty', async () => {
        const user = userEvent.setup();
        const publishEmptyAsync = jest.fn().mockResolvedValue({ id: 'overview-1', name: 'Overview' });
        mockUseTree.mockReturnValue({ pages: [], isLoading: false, isError: false, refetch: jest.fn() });
        mockUsePublishEmptyApi.mockReturnValue(mutationMock(jest.fn(), publishEmptyAsync));
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Publish API' }));
        expect(screen.getByText('Publish API to Next Gen Portal')).toBeInTheDocument();
        expect(screen.getByText(/No documentation exists for this API yet/)).toBeInTheDocument();

        const dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: 'Publish' }));

        expect(publishEmptyAsync).toHaveBeenCalledWith({
            title: 'Orders',
            folder: { id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' },
            placement: null,
        });
    });

    it('publishes selected documentation with the API under a Navigation folder', async () => {
        const user = userEvent.setup();
        const publishAsync = jest.fn().mockResolvedValue({});
        const syncAsync = jest.fn().mockResolvedValue({});
        mockUsePublish.mockReturnValue(mutationMock(jest.fn(), publishAsync));
        mockUsePortalSync.mockReturnValue(mutationMock(jest.fn(), syncAsync));
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Publish API' }));
        expect(screen.getByText('Publish API to Next Gen Portal')).toBeInTheDocument();

        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByText('Getting started')).toBeInTheDocument();
        expect(within(dialog).getByText('Guides')).toBeInTheDocument();
        expect(within(dialog).getByRole('checkbox', { name: /select all documentation/i })).toBeInTheDocument();
        await user.click(within(dialog).getByRole('button', { name: 'Publish' }));

        expect(publishAsync).toHaveBeenCalledWith('page-1');
        expect(publishAsync).toHaveBeenCalledWith('folder-1');
        expect(publishAsync).toHaveBeenCalledWith('page-2');
        expect(syncAsync).toHaveBeenCalledWith(
            expect.objectContaining({
                pageIds: expect.arrayContaining(['page-1', 'folder-1', 'page-2']),
            }),
        );
    });

    it('unpublishes the whole API from the Next Gen Portal', async () => {
        const user = userEvent.setup();
        const unpublishApiAsync = jest.fn().mockResolvedValue({});
        const pages = [{ ...PAGE, published: true }, FOLDER, NESTED];
        const placement = {
            folderId: 'nav-1',
            folderPath: 'Docs',
            itemId: 'api-nav',
            area: 'TOP_NAVBAR',
            published: true,
        };
        mockUseTree.mockReturnValue({ pages, isLoading: false, isError: false, refetch: jest.fn() });
        mockUseUnpublishApi.mockReturnValue(mutationMock(jest.fn(), unpublishApiAsync));
        mockUsePortalFolders.mockReturnValue({
            data: { folders: [{ id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' }], placement },
            isFetching: false,
            refetch: jest.fn().mockResolvedValue({
                data: { folders: [{ id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' }], placement },
            }),
        });
        renderPage();

        expect(screen.getByRole('button', { name: 'Unpublish API' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Publish API' })).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Unpublish API' }));

        const dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: 'Unpublish API' }));

        expect(unpublishApiAsync).toHaveBeenCalledWith({ placement, pages });
    });

    it('shows Publish for a newly selected unpublished page when the API is already published', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue({});
        const syncAsync = jest.fn().mockResolvedValue({});
        const placement = {
            folderId: 'nav-1',
            folderPath: 'Docs',
            itemId: 'api-nav',
            area: 'TOP_NAVBAR',
            published: true,
        };
        mockUseTree.mockReturnValue({
            pages: [FOLDER, PAGE, NESTED],
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        });
        mockUsePublish.mockReturnValue(mutationMock(jest.fn(), mutateAsync));
        mockUsePortalSync.mockReturnValue(mutationMock(jest.fn(), syncAsync));
        mockUsePortalFolders.mockReturnValue({
            data: { folders: [{ id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' }], placement },
            isFetching: false,
            refetch: jest.fn().mockResolvedValue({
                data: { folders: [{ id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' }], placement },
            }),
        });
        renderPage();

        expect(screen.getByRole('button', { name: 'Unpublish API' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Publish API' })).not.toBeInTheDocument();

        await user.click(screen.getByRole('checkbox', { name: 'Select Getting started' }));
        expect(screen.getByRole('button', { name: 'Publish' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Publish' }));
        expect(screen.getByText('Publish to Next Gen Portal')).toBeInTheDocument();

        const dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: 'Publish' }));

        expect(mutateAsync).toHaveBeenCalledWith('page-1');
        expect(syncAsync).toHaveBeenCalledWith(
            expect.objectContaining({
                pageIds: ['page-1'],
                placement,
            }),
        );
    });

    it('navigates to create a Markdown page', async () => {
        const user = userEvent.setup();
        renderPage();
        await user.click(screen.getByRole('button', { name: /add page/i }));
        await user.click(screen.getByRole('menuitem', { name: 'Markdown' }));
        expect(screen.getByText('new page')).toBeInTheDocument();
    });

    it('collapses and expands a folder without leaving the tree', async () => {
        const user = userEvent.setup();
        renderPage();
        expect(screen.getByRole('button', { name: 'Nested guide' })).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Collapse Guides' }));
        expect(screen.queryByRole('button', { name: 'Nested guide' })).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Expand Guides' }));
        expect(screen.getByRole('button', { name: 'Nested guide' })).toBeInTheDocument();
    });

    it('publishes a root page after choosing a Next Gen Portal Navigation folder', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue({});
        const syncAsync = jest.fn().mockResolvedValue({});
        mockUsePublish.mockReturnValue(mutationMock(jest.fn(), mutateAsync));
        mockUsePortalSync.mockReturnValue(mutationMock(jest.fn(), syncAsync));
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Actions for Getting started' }));
        await user.click(screen.getByRole('menuitem', { name: 'Publish' }));
        expect(screen.getByText('Publish to Next Gen Portal')).toBeInTheDocument();

        const dialog = screen.getByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: 'Publish' }));

        expect(mutateAsync).toHaveBeenCalledWith('page-1');
        expect(syncAsync).toHaveBeenCalledWith({
            title: 'Orders',
            folder: { id: 'nav-1', path: 'Docs', area: 'TOP_NAVBAR' },
            placement: null,
            pages: [FOLDER, PAGE, NESTED],
            pageIds: ['page-1'],
        });
    });

    it('hides mutating actions when kubernetes origin', () => {
        mockUseApiDetailContext.mockReturnValue({
            api: { id: 'api-1', name: 'Orders', definitionContext: { origin: 'KUBERNETES' } },
            isLoading: false,
            permissionsReady: true,
        });
        renderPage();
        expect(screen.queryByRole('button', { name: /add folder/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /add page/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Publish API' })).not.toBeInTheDocument();
        expect(screen.getByText('This API is managed by the Kubernetes operator. Documentation is read-only.')).toBeInTheDocument();
    });
});
