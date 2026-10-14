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
import { createApiDocumentationItem, listApiDocumentation, updateApiDocumentationItem } from '../../../services/apiDocumentation';
import type { PortalNavigationFolder, PortalNavigationItemSource } from '../../../types/apiDocumentation';
import type { SyncState } from '../../../utils/documentationTree';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../../../services/apiDocumentation');
jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
}));

const mockList = jest.mocked(listApiDocumentation);
const mockCreate = jest.mocked(createApiDocumentationItem);
const mockUpdate = jest.mocked(updateApiDocumentationItem);

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

// jsdom ships no scrollIntoView, which a refusal shown above a scrolled form is brought into view with.
Element.prototype.scrollIntoView ??= () => {};

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

    it('closes without saving on Cancel', async () => {
        const { user, onClose } = renderDialog({ folder: AUTH });

        await user.click(privateSwitch());
        await user.click(button('Cancel'));

        expect(onClose).toHaveBeenCalled();
        expect(mockUpdate).not.toHaveBeenCalled();
    });
});
