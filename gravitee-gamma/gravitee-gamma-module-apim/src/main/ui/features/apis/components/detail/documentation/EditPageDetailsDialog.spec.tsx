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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';

import { EditPageDetailsDialog } from './EditPageDetailsDialog';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { fetchApiDocumentationItem, listApiDocumentation, updateApiDocumentationItem } from '../../../services/apiDocumentation';
import { listFetchers } from '../../../services/fetchers';
import type { Fetcher, PortalNavigationFolder, PortalNavigationItemSource, PortalNavigationPage } from '../../../types/apiDocumentation';
import type { SyncState } from '../../../utils/documentationTree';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../../../services/apiDocumentation');
jest.mock('../../../services/fetchers');
jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
}));

const mockList = jest.mocked(listApiDocumentation);
const mockUpdate = jest.mocked(updateApiDocumentationItem);
const mockFetchItem = jest.mocked(fetchApiDocumentationItem);
const mockListFetchers = jest.mocked(listFetchers);

const GITHUB_FETCHER: Fetcher = {
    id: 'github-fetcher',
    name: 'GitHub',
    schema: {
        type: 'object',
        properties: { repository: { title: 'Repository', type: 'string' }, filepath: { title: 'File path', type: 'string' } },
        required: ['repository'],
    },
};

// The pointer-capture and scrolling APIs Radix's select calls when it opens are missing from jsdom.
Element.prototype.hasPointerCapture ??= () => false;
Element.prototype.setPointerCapture ??= () => {};
Element.prototype.releasePointerCapture ??= () => {};
Element.prototype.scrollIntoView ??= () => {};

const GUIDES: PortalNavigationFolder = {
    id: 'guides',
    organizationId: 'DEFAULT',
    environmentId: 'env-1',
    title: 'Guides',
    type: 'FOLDER',
    area: 'TOP_NAVBAR',
    rootId: 'guides',
    order: 0,
    published: true,
    visibility: 'PUBLIC',
};

const PAGE: PortalNavigationPage = {
    id: 'oauth',
    organizationId: 'DEFAULT',
    environmentId: 'env-1',
    title: 'OAuth setup',
    type: 'PAGE',
    area: 'TOP_NAVBAR',
    parentId: 'guides',
    rootId: 'guides',
    order: 2,
    published: true,
    visibility: 'PUBLIC',
    portalPageContentId: 'content-1',
};

const SOURCE: PortalNavigationItemSource = { type: 'github-fetcher', configuration: { repository: 'docs', filepath: 'oauth.md' } };

function renderDialog({
    page = PAGE,
    parent,
    syncState = 'NOT_SYNCED',
    hasUnsavedContent = false,
}: { page?: PortalNavigationPage; parent?: PortalNavigationFolder; syncState?: SyncState; hasUnsavedContent?: boolean } = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const onClose = jest.fn();
    render(
        <EditPageDetailsDialog
            open
            apiId="api-1"
            page={page}
            parent={parent}
            syncState={syncState}
            hasUnsavedContent={hasUnsavedContent}
            onClose={onClose}
        />,
        { wrapper },
    );
    return { onClose, user: userEvent.setup() };
}

function titleInput() {
    return screen.getByRole('textbox', { name: 'Title' }) as HTMLInputElement;
}

function privateSwitch() {
    return screen.getByRole('switch', { name: 'Authentication is required to view this page' }) as HTMLButtonElement;
}

function saveButton() {
    return screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement;
}

describe('EditPageDetailsDialog', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockList.mockResolvedValue({ items: [GUIDES, PAGE], publications: [] });
        mockUpdate.mockResolvedValue(PAGE);
        mockListFetchers.mockResolvedValue([GITHUB_FETCHER]);
        mockFetchItem.mockResolvedValue({ item: PAGE });
    });

    it('starts from the title and access of the page, and saves nothing until one changes', async () => {
        const { user } = renderDialog();

        expect(titleInput().value).toBe('OAuth setup');
        expect(privateSwitch().getAttribute('aria-checked')).toBe('false');
        expect(saveButton().disabled).toBe(true);

        await user.click(privateSwitch());
        expect(saveButton().disabled).toBe(false);

        await user.click(privateSwitch());
        expect(saveButton().disabled).toBe(true);
    });

    it('needs a title', async () => {
        const { user } = renderDialog();

        await user.clear(titleInput());

        expect(saveButton().disabled).toBe(true);
    });

    it('saves the new title and access over the page as the server has it, then closes', async () => {
        // The server's copy has been moved and hidden since the screen loaded the page.
        mockList.mockResolvedValue({ items: [GUIDES, { ...PAGE, order: 0, published: false }], publications: [] });
        const { user, onClose } = renderDialog();

        await user.clear(titleInput());
        await user.type(titleInput(), '  OAuth 2.0 setup  ');
        await user.click(privateSwitch());
        await user.click(saveButton());

        await waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(mockUpdate).toHaveBeenCalledWith(
            'env-1',
            'api-1',
            'oauth',
            {
                type: 'PAGE',
                title: 'OAuth 2.0 setup',
                order: 0,
                published: false,
                visibility: 'PRIVATE',
                parentId: 'guides',
            },
            {},
        );
        expect(notify.success).toHaveBeenCalledWith("Page 'OAuth 2.0 setup' saved");
    });

    it('keeps a page inside a folder requiring authentication private, saying why', () => {
        renderDialog({ page: { ...PAGE, visibility: 'PRIVATE' }, parent: { ...GUIDES, visibility: 'PRIVATE' } });

        expect(privateSwitch().getAttribute('aria-checked')).toBe('true');
        expect(privateSwitch().disabled).toBe(true);
        expect(screen.getByText(/requires authentication, so this page does too/).textContent).toBe(
            'The parent folder Guides requires authentication, so this page does too.',
        );
    });

    describe('a synced page', () => {
        const SYNCED_PAGE = { ...PAGE, source: SOURCE };

        it('keeps its title, which the server refuses to change, saying why', () => {
            renderDialog({ page: SYNCED_PAGE, syncState: 'OWN_SOURCE' });

            expect(titleInput().disabled).toBe(true);
            expect(
                screen.getByText("This page is synced from an external source, so its title can't change here. Its access can."),
            ).not.toBeNull();
        });

        it('sends its title back as stored, spaces included, and does not count it as a change', async () => {
            const spacedTitle = { ...SYNCED_PAGE, title: '  OAuth setup ' };
            mockList.mockResolvedValue({ items: [GUIDES, spacedTitle], publications: [] });
            const { user, onClose } = renderDialog({ page: spacedTitle, syncState: 'OWN_SOURCE' });

            expect(saveButton().disabled).toBe(true);
            await user.click(privateSwitch());
            await user.click(saveButton());

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'oauth',
                expect.objectContaining({ title: '  OAuth setup ', visibility: 'PRIVATE' }),
                {},
            );
        });

        it('changes its access, sending its title and source back unchanged', async () => {
            mockList.mockResolvedValue({ items: [GUIDES, SYNCED_PAGE], publications: [] });
            const { user, onClose } = renderDialog({ page: SYNCED_PAGE, syncState: 'OWN_SOURCE' });

            await user.click(privateSwitch());
            await user.click(saveButton());

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'oauth',
                expect.objectContaining({ title: 'OAuth setup', visibility: 'PRIVATE', source: SOURCE }),
                {},
            );
        });
    });

    it('says nothing about syncing for a page without a source', () => {
        renderDialog();

        expect(screen.queryByText(/synced from an external source/)).toBeNull();
    });

    describe('when the server refuses', () => {
        it('shows why, keeping the dialog open with what was entered', async () => {
            mockUpdate.mockRejectedValue(new ApimApiError(400, 'A sibling item already uses the title "Getting started"'));
            const { user, onClose } = renderDialog();

            await user.clear(titleInput());
            await user.type(titleInput(), 'Getting started');
            await user.click(saveButton());

            expect(await screen.findByText('A sibling item already uses the title "Getting started"')).not.toBeNull();
            expect(titleInput().value).toBe('Getting started');
            expect(saveButton().disabled).toBe(false);
            expect(onClose).not.toHaveBeenCalled();
            expect(notify.success).not.toHaveBeenCalled();
        });

        it('clears the refusal once what was entered changes', async () => {
            mockUpdate.mockRejectedValue(new ApimApiError(400, 'A sibling item already uses the title "Getting started"'));
            const { user } = renderDialog();

            await user.clear(titleInput());
            await user.type(titleInput(), 'Getting started');
            await user.click(saveButton());
            await screen.findByText('A sibling item already uses the title "Getting started"');

            await user.type(titleInput(), ' again');

            expect(screen.queryByText('A sibling item already uses the title "Getting started"')).toBeNull();
        });
    });

    it('saves only once when Save is clicked twice', async () => {
        let resolveUpdate: (page: PortalNavigationPage) => void = () => {};
        mockUpdate.mockReturnValue(new Promise(resolve => (resolveUpdate = resolve)));
        const { user, onClose } = renderDialog();

        await user.click(privateSwitch());
        await user.dblClick(saveButton());
        resolveUpdate(PAGE);

        await waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(mockUpdate).toHaveBeenCalledTimes(1);
    });

    describe('the source of the page', () => {
        const SYNCED_PAGE = { ...PAGE, source: SOURCE };
        const sourceSwitch = () => screen.getByRole('switch', { name: 'Fetch this page from an external source' });

        it('shows the source of a synced page, ready to change', async () => {
            renderDialog({ page: SYNCED_PAGE, syncState: 'OWN_SOURCE' });

            expect(sourceSwitch()).toHaveAttribute('aria-checked', 'true');
            expect(await screen.findByRole('textbox', { name: /Repository/ })).toHaveValue('docs');
        });

        it('links a page to a source, keeping its title, then fetches its content', async () => {
            const { user, onClose } = renderDialog();

            expect(sourceSwitch()).toHaveAttribute('aria-checked', 'false');
            await user.click(sourceSwitch());
            expect(titleInput().disabled).toBe(true);
            await user.click(await screen.findByRole('combobox', { name: 'Source type' }));
            await user.click(await screen.findByRole('option', { name: 'GitHub' }));
            expect(saveButton().disabled).toBe(true);
            await user.type(screen.getByRole('textbox', { name: /Repository/ }), 'docs');
            await waitFor(() => expect(saveButton().disabled).toBe(false));
            await user.click(saveButton());

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'oauth',
                expect.objectContaining({
                    title: 'OAuth setup',
                    source: { type: 'github-fetcher', configuration: { repository: 'docs' }, useAutoFetch: false },
                }),
                expect.anything(),
            );
            await waitFor(() => expect(mockFetchItem).toHaveBeenCalledWith('env-1', 'api-1', 'oauth'));
        });

        it('saves a changed source, then fetches the page from it', async () => {
            mockList.mockResolvedValue({ items: [GUIDES, SYNCED_PAGE], publications: [] });
            const { user, onClose } = renderDialog({ page: SYNCED_PAGE, syncState: 'OWN_SOURCE' });
            const filepath = await screen.findByRole('textbox', { name: /File path/ });

            await user.clear(filepath);
            await user.type(filepath, 'oauth-v2.md');
            await waitFor(() => expect(saveButton().disabled).toBe(false));
            await user.click(saveButton());

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'oauth',
                expect.objectContaining({
                    source: expect.objectContaining({ configuration: { repository: 'docs', filepath: 'oauth-v2.md' } }),
                }),
                expect.anything(),
            );
            await waitFor(() => expect(mockFetchItem).toHaveBeenCalled());
        });

        it('removes the source, saying the page becomes editable, and lets it be renamed in the same save', async () => {
            mockList.mockResolvedValue({ items: [GUIDES, SYNCED_PAGE], publications: [] });
            const { user, onClose } = renderDialog({ page: SYNCED_PAGE, syncState: 'OWN_SOURCE' });
            expect(titleInput().disabled).toBe(true);

            await user.click(sourceSwitch());

            expect(
                screen.getByText(
                    'The source will be removed. The page stops being fetched and becomes editable. Its current content is kept.',
                ),
            ).toBeInTheDocument();
            expect(titleInput().disabled).toBe(false);
            await user.clear(titleInput());
            await user.type(titleInput(), 'OAuth');
            await user.click(saveButton());

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            const payload = mockUpdate.mock.calls[0][3];
            expect(payload).toMatchObject({ title: 'OAuth' });
            expect(payload).not.toHaveProperty('source');
            expect(mockFetchItem).not.toHaveBeenCalled();
        });

        it('puts the stored title back when linking a source locks it', async () => {
            const { user } = renderDialog();

            await user.clear(titleInput());
            await user.type(titleInput(), 'OAuth');
            await user.click(sourceSwitch());

            expect(titleInput().disabled).toBe(true);
            expect(titleInput().value).toBe('OAuth setup');
        });

        it('warns that unsaved changes to the content will be replaced by what the source holds', async () => {
            const { user } = renderDialog({ hasUnsavedContent: true });

            await user.click(sourceSwitch());

            expect(
                screen.getByText('The unsaved changes to the content will be lost: the content is fetched from the source instead.'),
            ).toBeInTheDocument();
        });

        it("does not count the defaults of its fetcher's settings as a change of source", async () => {
            mockListFetchers.mockResolvedValue([
                {
                    ...GITHUB_FETCHER,
                    schema: {
                        ...GITHUB_FETCHER.schema,
                        properties: {
                            ...GITHUB_FETCHER.schema.properties,
                            useSystemProxy: { title: 'Use system proxy', type: 'boolean', default: false },
                        },
                    },
                },
            ]);
            renderDialog({ page: SYNCED_PAGE, syncState: 'OWN_SOURCE' });

            await screen.findByRole('textbox', { name: /Repository/ });

            await waitFor(() => expect(saveButton().disabled).toBe(true));
            expect(screen.getByRole('switch', { name: 'Use system proxy' })).toHaveAttribute('aria-checked', 'false');
        });

        it('fetches nothing when only the access changes', async () => {
            mockList.mockResolvedValue({ items: [GUIDES, SYNCED_PAGE], publications: [] });
            const { user, onClose } = renderDialog({ page: SYNCED_PAGE, syncState: 'OWN_SOURCE' });
            await screen.findByRole('textbox', { name: /Repository/ });

            await user.click(privateSwitch());
            await user.click(saveButton());

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockFetchItem).not.toHaveBeenCalled();
        });
    });

    it('stays open while saving, so that a refusal is not lost', async () => {
        mockUpdate.mockReturnValue(new Promise(() => {}));
        const { user, onClose } = renderDialog();

        await user.click(privateSwitch());
        await user.click(saveButton());
        await user.keyboard('{Escape}');

        expect(onClose).not.toHaveBeenCalled();
        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('closes without saving on Cancel', async () => {
        const { user, onClose } = renderDialog();

        await user.click(privateSwitch());
        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onClose).toHaveBeenCalled();
        expect(mockUpdate).not.toHaveBeenCalled();
    });
});
