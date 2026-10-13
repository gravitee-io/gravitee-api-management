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
import { listApiDocumentation, updateApiDocumentationItem } from '../../../services/apiDocumentation';
import type { PortalNavigationFolder, PortalNavigationItemSource, PortalNavigationPage } from '../../../types/apiDocumentation';
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
}: { page?: PortalNavigationPage; parent?: PortalNavigationFolder; syncState?: SyncState } = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const onClose = jest.fn();
    render(<EditPageDetailsDialog open apiId="api-1" page={page} parent={parent} syncState={syncState} onClose={onClose} />, { wrapper });
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

    it('closes without saving on Cancel', async () => {
        const { user, onClose } = renderDialog();

        await user.click(privateSwitch());
        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onClose).toHaveBeenCalled();
        expect(mockUpdate).not.toHaveBeenCalled();
    });
});
