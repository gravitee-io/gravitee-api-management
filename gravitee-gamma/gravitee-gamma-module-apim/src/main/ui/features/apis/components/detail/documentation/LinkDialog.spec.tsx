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

import { LinkDialog } from './LinkDialog';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { createApiDocumentationItem, listApiDocumentation, updateApiDocumentationItem } from '../../../services/apiDocumentation';
import type { PortalNavigationFolder, PortalNavigationLink } from '../../../types/apiDocumentation';

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

const STATUS: PortalNavigationLink = {
    id: 'status',
    organizationId: 'DEFAULT',
    environmentId: 'env-1',
    title: 'Status page',
    type: 'LINK',
    area: 'TOP_NAVBAR',
    parentId: 'guides',
    rootId: 'guides',
    order: 2,
    published: true,
    visibility: 'PUBLIC',
    url: 'https://status.example.com',
};

function renderDialog({ link, parent }: { link?: PortalNavigationLink; parent?: PortalNavigationFolder } = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const onClose = jest.fn();
    const onCreated = jest.fn();
    render(<LinkDialog open apiId="api-1" link={link} parent={parent} onClose={onClose} onCreated={onCreated} />, { wrapper });
    return { onClose, onCreated, user: userEvent.setup() };
}

function titleInput() {
    return screen.getByRole('textbox', { name: 'Title' }) as HTMLInputElement;
}

function urlInput() {
    return screen.getByRole('textbox', { name: 'URL' }) as HTMLInputElement;
}

function privateSwitch() {
    return screen.getByRole('switch', { name: 'Authentication is required to view this link' }) as HTMLButtonElement;
}

function button(name: string) {
    return screen.getByRole('button', { name }) as HTMLButtonElement;
}

const INVALID_URL = 'Enter a valid URL, such as https://status.example.com';

describe('LinkDialog', () => {
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
        mockList.mockResolvedValue({ items: [GUIDES, STATUS], publications: [] });
        mockCreate.mockResolvedValue({ ...STATUS, id: 'new-link' });
        mockUpdate.mockResolvedValue(STATUS);
    });

    describe('adding a link', () => {
        it('says it is created unpublished, at the top level', () => {
            renderDialog();

            expect(screen.getByRole('heading', { name: 'Add a link' })).not.toBeNull();
            expect(screen.getByText('The link is created unpublished.')).not.toBeNull();
        });

        it('needs a title and a URL', async () => {
            const { user } = renderDialog();

            expect(button('Create').disabled).toBe(true);
            await user.type(titleInput(), 'Status page');
            expect(button('Create').disabled).toBe(true);
            await user.type(urlInput(), '   ');
            expect(button('Create').disabled).toBe(true);
            await user.clear(urlInput());
            await user.type(urlInput(), 'https://status.example.com');
            expect(button('Create').disabled).toBe(false);
        });

        it('refuses a URL that is not a full address, once the field is left', async () => {
            const { user } = renderDialog();

            await user.type(titleInput(), 'Status page');
            await user.type(urlInput(), 'status.example.com');
            expect(screen.queryByText(INVALID_URL)).toBeNull();

            await user.tab();

            expect(screen.getByText(INVALID_URL)).not.toBeNull();
            expect(urlInput().getAttribute('aria-invalid')).toBe('true');
            expect(button('Create').disabled).toBe(true);

            await user.clear(urlInput());
            await user.type(urlInput(), 'https://status.example.com');
            expect(screen.queryByText(INVALID_URL)).toBeNull();
        });

        it.each(['mailto:support@example.com', 'ftp://files.example.com/docs'])('accepts %s, as the portal editor does', async url => {
            const { user } = renderDialog();

            await user.type(titleInput(), 'Contact');
            await user.type(urlInput(), url);
            await user.tab();

            expect(screen.queryByText(INVALID_URL)).toBeNull();
            expect(button('Create').disabled).toBe(false);
        });

        it('creates a link at the top level, then closes', async () => {
            const { user, onClose, onCreated } = renderDialog();

            await user.type(titleInput(), '  Status page  ');
            await user.type(urlInput(), '  https://status.example.com  ');
            await user.click(privateSwitch());
            await user.click(button('Create'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockCreate).toHaveBeenCalledWith('env-1', 'api-1', {
                type: 'LINK',
                title: 'Status page',
                url: 'https://status.example.com',
                area: 'TOP_NAVBAR',
                visibility: 'PRIVATE',
                parentId: undefined,
            });
            expect(onCreated).toHaveBeenCalled();
            expect(notify.success).toHaveBeenCalledWith("Link 'Status page' created");
        });

        it('creates a link inside a folder', async () => {
            const { user, onClose } = renderDialog({ parent: GUIDES });

            expect(screen.getByText('The link is created unpublished, inside Guides.')).not.toBeNull();
            await user.type(titleInput(), 'Status page');
            await user.type(urlInput(), 'https://status.example.com');
            await user.click(button('Create'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockCreate).toHaveBeenCalledWith('env-1', 'api-1', expect.objectContaining({ type: 'LINK', parentId: 'guides' }));
        });

        it('keeps a link inside a folder requiring authentication private, saying why', () => {
            renderDialog({ parent: { ...GUIDES, visibility: 'PRIVATE' } });

            expect(privateSwitch().getAttribute('aria-checked')).toBe('true');
            expect(privateSwitch().disabled).toBe(true);
            expect(screen.getByText(/requires authentication, so this link does too/).textContent).toBe(
                'The parent folder Guides requires authentication, so this link does too.',
            );
        });

        it('shows why the server refuses, keeping the dialog open with what was entered', async () => {
            mockCreate.mockRejectedValue(new ApimApiError(400, 'Invalid URL format'));
            const { user, onClose, onCreated } = renderDialog();

            await user.type(titleInput(), 'Status page');
            await user.type(urlInput(), 'javascript:alert(1)');
            await user.click(button('Create'));

            expect(await screen.findByText('Invalid URL format')).not.toBeNull();
            expect(titleInput().value).toBe('Status page');
            expect(urlInput().value).toBe('javascript:alert(1)');
            expect(onClose).not.toHaveBeenCalled();
            expect(onCreated).not.toHaveBeenCalled();

            await user.type(urlInput(), 'x');
            expect(screen.queryByText('Invalid URL format')).toBeNull();
        });

        it('creates only once when Create is clicked twice', async () => {
            let resolveCreate: (link: PortalNavigationLink) => void = () => {};
            mockCreate.mockReturnValue(new Promise(resolve => (resolveCreate = resolve)));
            const { user, onClose } = renderDialog();

            await user.type(titleInput(), 'Status page');
            await user.type(urlInput(), 'https://status.example.com');
            await user.dblClick(button('Create'));
            resolveCreate(STATUS);

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockCreate).toHaveBeenCalledTimes(1);
        });
    });

    describe('editing a link', () => {
        it('starts from the title, URL and access of the link, and saves nothing until one changes', async () => {
            const { user } = renderDialog({ link: STATUS, parent: GUIDES });

            expect(screen.getByRole('heading', { name: 'Edit link details' })).not.toBeNull();
            expect(screen.getByText('Changes apply in every portal that lists this API.')).not.toBeNull();
            expect(titleInput().value).toBe('Status page');
            expect(urlInput().value).toBe('https://status.example.com');
            expect(privateSwitch().getAttribute('aria-checked')).toBe('false');
            expect(button('Save').disabled).toBe(true);

            await user.type(urlInput(), '/incidents');
            expect(button('Save').disabled).toBe(false);

            await user.clear(urlInput());
            await user.type(urlInput(), ' https://status.example.com ');
            expect(button('Save').disabled).toBe(true);
        });

        it('saves the new title, URL and access over the link as the server has it, then closes', async () => {
            // The server's copy has been moved and hidden since the screen loaded the link.
            mockList.mockResolvedValue({ items: [GUIDES, { ...STATUS, order: 0, published: false }], publications: [] });
            const { user, onClose, onCreated } = renderDialog({ link: STATUS, parent: GUIDES });

            await user.clear(titleInput());
            await user.type(titleInput(), '  Status  ');
            await user.clear(urlInput());
            await user.type(urlInput(), ' https://status.example.org ');
            await user.click(privateSwitch());
            await user.click(button('Save'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'status',
                {
                    type: 'LINK',
                    title: 'Status',
                    order: 0,
                    published: false,
                    visibility: 'PRIVATE',
                    parentId: 'guides',
                    url: 'https://status.example.org',
                },
                {},
            );
            expect(mockCreate).not.toHaveBeenCalled();
            expect(onCreated).not.toHaveBeenCalled();
            expect(notify.success).toHaveBeenCalledWith("Link 'Status' saved");
        });

        it('keeps the URL when only the title changes', async () => {
            const { user, onClose } = renderDialog({ link: STATUS });

            await user.type(titleInput(), ' (live)');
            await user.click(button('Save'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'status',
                expect.objectContaining({ title: 'Status page (live)', url: 'https://status.example.com' }),
                {},
            );
        });

        it('shows why the server refuses, keeping the dialog open with what was entered', async () => {
            mockUpdate.mockRejectedValue(new ApimApiError(400, 'A sibling item already uses the title "Getting started"'));
            const { user, onClose } = renderDialog({ link: STATUS });

            await user.clear(titleInput());
            await user.type(titleInput(), 'Getting started');
            await user.click(button('Save'));

            expect(await screen.findByText('A sibling item already uses the title "Getting started"')).not.toBeNull();
            expect(titleInput().value).toBe('Getting started');
            expect(onClose).not.toHaveBeenCalled();
        });
    });

    it('closes without saving on Cancel', async () => {
        const { user, onClose } = renderDialog({ link: STATUS });

        await user.click(privateSwitch());
        await user.click(button('Cancel'));

        expect(onClose).toHaveBeenCalled();
        expect(mockUpdate).not.toHaveBeenCalled();
    });
});
