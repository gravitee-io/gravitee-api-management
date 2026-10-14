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

import { FolderDialog } from './FolderDialog';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import {
    createApiDocumentationItem,
    importApiDocumentation,
    listApiDocumentation,
    updateApiDocumentationItem,
    fetchApiDocumentationItem,
} from '../../../services/apiDocumentation';
import { listFetchers } from '../../../services/fetchers';
import type { Fetcher, PortalNavigationFolder, PortalNavigationItemSource } from '../../../types/apiDocumentation';
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
const mockCreate = jest.mocked(createApiDocumentationItem);
const mockUpdate = jest.mocked(updateApiDocumentationItem);
const mockImport = jest.mocked(importApiDocumentation);
const mockListFetchers = jest.mocked(listFetchers);
const mockFetchItem = jest.mocked(fetchApiDocumentationItem);

const GITLAB_FETCHER: Fetcher = {
    id: 'gitlab-fetcher',
    name: 'GitLab',
    schema: { type: 'object', properties: { project: { title: 'Project', type: 'string' } }, required: ['project'] },
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

const AUTH: PortalNavigationFolder = {
    ...GUIDES,
    id: 'auth',
    title: 'Authentication',
    parentId: 'guides',
    order: 3,
};

const SOURCE: PortalNavigationItemSource = { type: 'github-fetcher', configuration: { repository: 'docs', filepath: 'sdk' } };

function renderDialog({
    folder,
    parent,
    syncState,
    itemsInside,
}: { folder?: PortalNavigationFolder; parent?: PortalNavigationFolder; syncState?: SyncState; itemsInside?: number } = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const onClose = jest.fn();
    const onCreated = jest.fn();
    render(
        <FolderDialog
            open
            apiId="api-1"
            folder={folder}
            parent={parent}
            syncState={syncState}
            itemsInside={itemsInside}
            onClose={onClose}
            onCreated={onCreated}
        />,
        { wrapper },
    );
    return { onClose, onCreated, user: userEvent.setup() };
}

function titleInput() {
    return screen.getByRole('textbox', { name: 'Title' }) as HTMLInputElement;
}

function privateSwitch() {
    return screen.getByRole('switch', { name: 'Authentication is required to view this folder' }) as HTMLButtonElement;
}

function button(name: string) {
    return screen.getByRole('button', { name }) as HTMLButtonElement;
}

describe('FolderDialog', () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });

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
        mockList.mockResolvedValue({ items: [GUIDES, AUTH], publications: [] });
        mockCreate.mockResolvedValue({ ...GUIDES, id: 'new-folder', title: 'Tutorials' });
        mockUpdate.mockResolvedValue(AUTH);
        mockListFetchers.mockResolvedValue([GITLAB_FETCHER]);
    });

    describe('adding a folder', () => {
        it('says it is created unpublished, at the top level', () => {
            renderDialog();

            expect(screen.getByRole('heading', { name: 'Add a folder' })).not.toBeNull();
            expect(screen.getByText('The folder is created unpublished.')).not.toBeNull();
        });

        it('needs a title', async () => {
            const { user } = renderDialog();

            expect(button('Create').disabled).toBe(true);
            await user.type(titleInput(), '   ');
            expect(button('Create').disabled).toBe(true);
        });

        it('creates a folder at the top level, then closes', async () => {
            const { user, onClose, onCreated } = renderDialog();

            await user.type(titleInput(), '  Tutorials  ');
            await user.click(privateSwitch());
            await user.click(button('Create'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockCreate).toHaveBeenCalledWith('env-1', 'api-1', {
                type: 'FOLDER',
                title: 'Tutorials',
                area: 'TOP_NAVBAR',
                visibility: 'PRIVATE',
                parentId: undefined,
            });
            expect(onCreated).toHaveBeenCalled();
            expect(notify.success).toHaveBeenCalledWith("Folder 'Tutorials' created");
        });

        it('creates a folder inside another one', async () => {
            const { user, onClose } = renderDialog({ parent: GUIDES });

            expect(screen.getByText('The folder is created unpublished, inside Guides.')).not.toBeNull();
            await user.type(titleInput(), 'Tutorials');
            await user.click(button('Create'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockCreate).toHaveBeenCalledWith('env-1', 'api-1', expect.objectContaining({ type: 'FOLDER', parentId: 'guides' }));
        });

        it('keeps a folder inside a folder requiring authentication private, saying why', () => {
            renderDialog({ parent: { ...GUIDES, visibility: 'PRIVATE' } });

            expect(privateSwitch().getAttribute('aria-checked')).toBe('true');
            expect(privateSwitch().disabled).toBe(true);
            expect(screen.getByText(/requires authentication, so this folder does too/).textContent).toBe(
                'The parent folder Guides requires authentication, so this folder does too.',
            );
        });

        it('shows why the server refuses, keeping the dialog open with what was entered', async () => {
            mockCreate.mockRejectedValue(new ApimApiError(400, 'A sibling item already uses the title "Guides"'));
            const { user, onClose, onCreated } = renderDialog();

            await user.type(titleInput(), 'Guides');
            await user.click(button('Create'));

            expect(await screen.findByText('A sibling item already uses the title "Guides"')).not.toBeNull();
            expect(titleInput().value).toBe('Guides');
            expect(button('Create').disabled).toBe(false);
            expect(onClose).not.toHaveBeenCalled();
            expect(onCreated).not.toHaveBeenCalled();
        });

        it('brings the refusal into view, as a long dialog scrolls it out of sight', async () => {
            const scrollIntoView = jest.spyOn(Element.prototype, 'scrollIntoView').mockImplementation(() => {});
            mockCreate.mockRejectedValue(new ApimApiError(400, 'A sibling item already uses the title "Guides"'));
            const { user } = renderDialog();

            await user.type(titleInput(), 'Guides');
            await user.click(button('Create'));

            const refusal = (await screen.findByText('A sibling item already uses the title "Guides"')).closest('[role="alert"]');
            expect(scrollIntoView.mock.contexts).toContain(refusal);
        });

        it('brings the same refusal into view again when it is given again', async () => {
            const scrollIntoView = jest.spyOn(Element.prototype, 'scrollIntoView').mockImplementation(() => {});
            mockCreate.mockRejectedValue(new ApimApiError(400, 'A sibling item already uses the title "Guides"'));
            const { user } = renderDialog();
            await user.type(titleInput(), 'Guides');

            await user.click(button('Create'));
            await screen.findByText('A sibling item already uses the title "Guides"');
            await user.click(button('Create'));

            await waitFor(() => expect(scrollIntoView).toHaveBeenCalledTimes(2));
        });

        it('creates only once when Create is clicked twice', async () => {
            let resolveCreate: (folder: PortalNavigationFolder) => void = () => {};
            mockCreate.mockReturnValue(new Promise(resolve => (resolveCreate = resolve)));
            const { user, onClose } = renderDialog();

            await user.type(titleInput(), 'Tutorials');
            await user.dblClick(button('Create'));
            resolveCreate(GUIDES);

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockCreate).toHaveBeenCalledTimes(1);
        });
    });

    describe('editing a folder', () => {
        it('starts from the title and access of the folder, and saves nothing until one changes', async () => {
            const { user } = renderDialog({ folder: AUTH, parent: GUIDES });

            expect(screen.getByRole('heading', { name: 'Edit folder details' })).not.toBeNull();
            expect(titleInput().value).toBe('Authentication');
            expect(privateSwitch().getAttribute('aria-checked')).toBe('false');
            expect(button('Save').disabled).toBe(true);

            await user.click(privateSwitch());
            expect(button('Save').disabled).toBe(false);

            await user.click(privateSwitch());
            expect(button('Save').disabled).toBe(true);
        });

        it('saves the new title and access over the folder as the server has it, then closes', async () => {
            // The server's copy has been moved and hidden since the screen loaded the folder.
            mockList.mockResolvedValue({ items: [GUIDES, { ...AUTH, order: 0, published: false }], publications: [] });
            const { user, onClose, onCreated } = renderDialog({ folder: AUTH, parent: GUIDES });

            await user.clear(titleInput());
            await user.type(titleInput(), '  Auth  ');
            await user.click(privateSwitch());
            await user.click(button('Save'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'auth',
                {
                    type: 'FOLDER',
                    title: 'Auth',
                    order: 0,
                    published: false,
                    visibility: 'PRIVATE',
                    parentId: 'guides',
                },
                {},
            );
            expect(mockCreate).not.toHaveBeenCalled();
            expect(onCreated).not.toHaveBeenCalled();
            expect(notify.success).toHaveBeenCalledWith("Folder 'Auth' saved");
        });

        it('says the items inside become private too when the folder does', async () => {
            const { user } = renderDialog({ folder: AUTH, itemsInside: 3 });

            expect(screen.queryByText(/inside become/)).toBeNull();
            await user.click(privateSwitch());

            expect(screen.getByText('The 3 items inside become private too.')).not.toBeNull();
        });

        it('says it of a single item inside', async () => {
            const { user } = renderDialog({ folder: AUTH, itemsInside: 1 });

            await user.click(privateSwitch());

            expect(screen.getByText('The 1 item inside becomes private too.')).not.toBeNull();
        });

        it('says nothing about the items inside of an empty folder', async () => {
            const { user } = renderDialog({ folder: AUTH, itemsInside: 0 });

            await user.click(privateSwitch());

            expect(screen.queryByText(/inside become/)).toBeNull();
        });

        it('says nothing about the items inside when a private folder becomes public', async () => {
            const { user } = renderDialog({ folder: { ...AUTH, visibility: 'PRIVATE' }, itemsInside: 3 });

            await user.click(privateSwitch());

            expect(screen.queryByText(/inside become/)).toBeNull();
        });

        it('shows why the server refuses, keeping the dialog open with what was entered', async () => {
            mockUpdate.mockRejectedValue(new ApimApiError(400, 'A sibling item already uses the title "Getting started"'));
            const { user, onClose } = renderDialog({ folder: AUTH });

            await user.clear(titleInput());
            await user.type(titleInput(), 'Getting started');
            await user.click(button('Save'));

            expect(await screen.findByText('A sibling item already uses the title "Getting started"')).not.toBeNull();
            expect(titleInput().value).toBe('Getting started');
            expect(onClose).not.toHaveBeenCalled();

            await user.type(titleInput(), ' again');
            expect(screen.queryByText('A sibling item already uses the title "Getting started"')).toBeNull();
        });

        describe('a synced folder', () => {
            const SYNCED = { ...AUTH, source: SOURCE };

            it('keeps its title, which the server refuses to change, saying why', () => {
                renderDialog({ folder: SYNCED, syncState: 'OWN_SOURCE' });

                expect(titleInput().disabled).toBe(true);
                expect(
                    screen.getByText("This folder is synced from an external source, so its title can't change here. Its access can."),
                ).not.toBeNull();
            });

            it('offers no source to change when it was not imported, as the server refuses one', () => {
                renderDialog({ folder: AUTH });

                expect(screen.queryByRole('switch', { name: 'Fetch this folder from an external source' })).not.toBeInTheDocument();
            });

            describe('imported from a repository', () => {
                const IMPORTED = {
                    ...AUTH,
                    source: { type: 'gitlab-fetcher', configuration: { project: 'acme/sdk' }, useAutoFetch: false, subtreeImport: true },
                };
                const sourceSwitch = () => screen.getByRole('switch', { name: 'Fetch this folder from an external source' });

                beforeEach(() => {
                    mockList.mockResolvedValue({ items: [GUIDES, IMPORTED], publications: [] });
                    mockUpdate.mockResolvedValue(IMPORTED);
                    mockFetchItem.mockResolvedValue({ summary: { succeeded: 3, failed: 0, results: [] } });
                });

                it('shows its source, among the fetchers that can import a folder', async () => {
                    renderDialog({ folder: IMPORTED, syncState: 'OWN_SOURCE' });

                    expect(sourceSwitch()).toHaveAttribute('aria-checked', 'true');
                    expect(await screen.findByRole('textbox', { name: /Project/ })).toHaveValue('acme/sdk');
                    expect(mockListFetchers).toHaveBeenCalledWith('env-1', { forImport: true });
                });

                it('saves nothing while it is left as it is', async () => {
                    renderDialog({ folder: IMPORTED, syncState: 'OWN_SOURCE' });

                    await screen.findByRole('textbox', { name: /Project/ });

                    await waitFor(() => expect(button('Save').disabled).toBe(true));
                });

                it('fetches nothing when only the access changes', async () => {
                    const { user, onClose } = renderDialog({ folder: IMPORTED, syncState: 'OWN_SOURCE' });
                    await screen.findByRole('textbox', { name: /Project/ });

                    await user.click(privateSwitch());
                    await user.click(button('Save'));

                    await waitFor(() => expect(onClose).toHaveBeenCalled());
                    expect(mockUpdate.mock.calls[0][3]).toMatchObject({ source: IMPORTED.source });
                    expect(mockFetchItem).not.toHaveBeenCalled();
                });

                it('cannot be saved once a required setting is cleared', async () => {
                    const { user } = renderDialog({ folder: IMPORTED, syncState: 'OWN_SOURCE' });
                    const project = await screen.findByRole('textbox', { name: /Project/ });

                    await user.clear(project);

                    await waitFor(() => expect(button('Save').disabled).toBe(true));
                });

                it('saves a changed source, then fetches the folder from it', async () => {
                    const { user, onClose } = renderDialog({ folder: IMPORTED, syncState: 'OWN_SOURCE' });
                    const project = await screen.findByRole('textbox', { name: /Project/ });

                    await user.clear(project);
                    await user.type(project, 'acme/sdk-v2');
                    await waitFor(() => expect(button('Save').disabled).toBe(false));
                    await user.click(button('Save'));

                    await waitFor(() => expect(onClose).toHaveBeenCalled());
                    expect(mockUpdate).toHaveBeenCalledWith(
                        'env-1',
                        'api-1',
                        'auth',
                        expect.objectContaining({
                            source: { type: 'gitlab-fetcher', configuration: { project: 'acme/sdk-v2' }, useAutoFetch: false },
                        }),
                        {},
                    );
                    await waitFor(() => expect(mockFetchItem).toHaveBeenCalledWith('env-1', 'api-1', 'auth'));
                });

                it('removes the source, saying the folder and what it holds become editable, and lets it be renamed', async () => {
                    const { user, onClose } = renderDialog({ folder: IMPORTED, syncState: 'OWN_SOURCE' });
                    expect(titleInput().disabled).toBe(true);

                    await user.click(sourceSwitch());

                    expect(
                        screen.getByText(/The source will be removed\. The folder and the items in it stop being fetched/),
                    ).toBeInTheDocument();
                    expect(titleInput().disabled).toBe(false);
                    await user.clear(titleInput());
                    await user.type(titleInput(), 'SDK');
                    await user.click(button('Save'));

                    await waitFor(() => expect(onClose).toHaveBeenCalled());
                    const payload = mockUpdate.mock.calls[0][3];
                    expect(payload).toMatchObject({ title: 'SDK' });
                    expect(payload).not.toHaveProperty('source');
                    expect(mockFetchItem).not.toHaveBeenCalled();
                });
            });

            it('offers only to remove a source it was not imported with, which the server refuses to change', async () => {
                const legacy = { ...AUTH, source: SOURCE };
                mockList.mockResolvedValue({ items: [GUIDES, legacy], publications: [] });
                const { user, onClose } = renderDialog({ folder: legacy, syncState: 'OWN_SOURCE' });

                expect(screen.getByText(/can only be removed here/)).toBeInTheDocument();
                expect(screen.queryByRole('combobox', { name: 'Source type' })).not.toBeInTheDocument();
                expect(button('Save').disabled).toBe(true);

                await user.click(screen.getByRole('switch', { name: 'Fetch this folder from an external source' }));
                await user.click(button('Save'));

                await waitFor(() => expect(onClose).toHaveBeenCalled());
                expect(mockUpdate.mock.calls[0][3]).not.toHaveProperty('source');
            });

            it('changes its access, sending its title, spaces included, and its source back unchanged', async () => {
                const spacedTitle = { ...SYNCED, title: ' SDK reference ' };
                mockList.mockResolvedValue({ items: [GUIDES, spacedTitle], publications: [] });
                const { user, onClose } = renderDialog({ folder: spacedTitle, syncState: 'OWN_SOURCE' });

                expect(button('Save').disabled).toBe(true);
                await user.click(privateSwitch());
                await user.click(button('Save'));

                await waitFor(() => expect(onClose).toHaveBeenCalled());
                expect(mockUpdate).toHaveBeenCalledWith(
                    'env-1',
                    'api-1',
                    'auth',
                    expect.objectContaining({ title: ' SDK reference ', visibility: 'PRIVATE', source: SOURCE }),
                    {},
                );
            });
        });

        it('says nothing about syncing for a folder without a source', () => {
            renderDialog({ folder: AUTH });

            expect(screen.queryByText(/synced from an external source/)).toBeNull();
        });
    });

    describe('importing a folder from a repository', () => {
        const SUMMARY = { succeeded: 14, failed: 0, results: [] };

        async function fillImport(user: ReturnType<typeof userEvent.setup>) {
            await user.click(screen.getByRole('radio', { name: 'Import from a repository' }));
            await user.type(titleInput(), 'SDK reference');
            await user.click(await screen.findByRole('combobox', { name: 'Source type' }));
            await user.click(await screen.findByRole('option', { name: 'GitLab' }));
            await user.type(screen.getByRole('textbox', { name: /Project/ }), 'acme/sdk');
        }

        async function submitImport(user: ReturnType<typeof userEvent.setup>) {
            await waitFor(() => expect(button('Import').disabled).toBe(false));
            await user.click(button('Import'));
        }

        beforeEach(() => {
            mockImport.mockResolvedValue({ rootFolder: { ...GUIDES, id: 'sdk', title: 'SDK reference' }, summary: SUMMARY });
        });

        it('creates an empty folder by default, and offers an import instead', () => {
            renderDialog();

            expect(screen.getByRole('radio', { name: 'Empty folder' })).toHaveAttribute('aria-checked', 'true');
            expect(screen.getByRole('radio', { name: 'Import from a repository' })).toHaveAttribute('aria-checked', 'false');
        });

        it('offers only the fetchers that can list a folder, and explains how the pages are laid out', async () => {
            const { user } = renderDialog();

            await user.click(screen.getByRole('radio', { name: 'Import from a repository' }));

            await screen.findByRole('combobox', { name: 'Source type' });
            expect(mockListFetchers).toHaveBeenCalledWith('env-1', { forImport: true });
            expect(screen.getByText(/A \.gravitee\.json manifest decides the hierarchy/)).toBeInTheDocument();
        });

        it('imports the folder with its source, then says how many pages were fetched', async () => {
            const { user, onClose, onCreated } = renderDialog({ parent: GUIDES });

            await fillImport(user);
            await submitImport(user);

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockImport).toHaveBeenCalledWith('env-1', 'api-1', {
                title: 'SDK reference',
                parentId: 'guides',
                visibility: 'PUBLIC',
                source: { type: 'gitlab-fetcher', configuration: { project: 'acme/sdk' }, useAutoFetch: false },
            });
            expect(mockCreate).not.toHaveBeenCalled();
            expect(onCreated).toHaveBeenCalled();
            expect(notify.success).toHaveBeenCalledWith("Folder 'SDK reference' imported. 14 pages fetched");
        });

        it('creates an empty folder after an import was started then given up', async () => {
            const { user, onClose } = renderDialog();

            await user.click(screen.getByRole('radio', { name: 'Import from a repository' }));
            await user.click(await screen.findByRole('combobox', { name: 'Source type' }));
            await user.click(await screen.findByRole('option', { name: 'GitLab' }));
            await user.click(screen.getByRole('radio', { name: 'Empty folder' }));
            await user.type(titleInput(), 'Tutorials');

            expect(button('Create').disabled).toBe(false);
            await user.click(button('Create'));
            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockCreate).toHaveBeenCalled();
            expect(mockImport).not.toHaveBeenCalled();
        });

        it('locks the choice between an empty folder and an import while importing', async () => {
            mockImport.mockReturnValue(new Promise(() => {}));
            const { user } = renderDialog();
            await fillImport(user);

            await submitImport(user);

            await waitFor(() => expect(screen.getByRole('radio', { name: 'Empty folder' })).toBeDisabled());
            expect(screen.getByRole('radio', { name: 'Import from a repository' })).toBeDisabled();
            expect(screen.getByRole('textbox', { name: /Project/ })).toHaveValue('acme/sdk');
        });

        it('cannot be closed while importing', async () => {
            mockImport.mockReturnValue(new Promise(() => {}));
            const { user, onClose } = renderDialog();
            await fillImport(user);
            await submitImport(user);
            await waitFor(() => expect(button('Cancel').disabled).toBe(true));

            await user.keyboard('{Escape}');

            expect(onClose).not.toHaveBeenCalled();
            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });

        it('keeps Import disabled until the source is complete', async () => {
            const { user } = renderDialog();

            await user.click(screen.getByRole('radio', { name: 'Import from a repository' }));
            await user.type(titleInput(), 'SDK reference');

            expect(button('Import').disabled).toBe(true);
        });

        it('names the files that could not be fetched, the folder being created all the same', async () => {
            mockImport.mockResolvedValue({
                rootFolder: { ...GUIDES, id: 'sdk', title: 'SDK reference' },
                summary: {
                    succeeded: 12,
                    failed: 2,
                    results: [
                        { title: 'errors.md', success: false },
                        { title: 'webhooks.md', success: false },
                    ],
                },
            });
            const { user, onCreated } = renderDialog();

            await fillImport(user);
            await submitImport(user);

            await waitFor(() => expect(onCreated).toHaveBeenCalled());
            expect(notify.warning).toHaveBeenCalledWith(
                "Folder 'SDK reference' imported. 12 of 14 pages fetched. Failed: errors.md, webhooks.md.",
            );
        });

        it('shows why the server refuses, keeping the dialog open with what was entered', async () => {
            mockImport.mockRejectedValue(new ApimApiError(400, 'The source address gitlab.internal is not allowed'));
            const { user, onClose } = renderDialog();

            await fillImport(user);
            await submitImport(user);

            expect(await screen.findByText('The source address gitlab.internal is not allowed')).toBeInTheDocument();
            expect(onClose).not.toHaveBeenCalled();
            expect(screen.getByRole('textbox', { name: /Project/ })).toHaveValue('acme/sdk');
        });

        it('clears the refusal once the source changes', async () => {
            mockImport.mockRejectedValue(new ApimApiError(400, 'The source address gitlab.internal is not allowed'));
            const { user } = renderDialog();
            await fillImport(user);
            await submitImport(user);
            await screen.findByText('The source address gitlab.internal is not allowed');

            await user.type(screen.getByRole('textbox', { name: /Project/ }), '-v2');

            await waitFor(() => expect(screen.queryByText('The source address gitlab.internal is not allowed')).not.toBeInTheDocument());
        });
    });

    it('closes without saving on Cancel', async () => {
        const { user, onClose } = renderDialog({ folder: AUTH });

        await user.click(privateSwitch());
        await user.click(button('Cancel'));

        expect(onClose).toHaveBeenCalled();
        expect(mockUpdate).not.toHaveBeenCalled();
    });
});
