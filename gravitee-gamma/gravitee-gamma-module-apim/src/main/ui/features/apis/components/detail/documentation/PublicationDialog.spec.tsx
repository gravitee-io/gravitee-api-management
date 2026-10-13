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

import { PublicationDialog } from './PublicationDialog';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { listApiDocumentation, updateApiDocumentationItem } from '../../../services/apiDocumentation';
import type {
    ApiDocumentationItem,
    PortalNavigationFolder,
    PortalNavigationItemSource,
    PortalNavigationLink,
    PortalNavigationPage,
} from '../../../types/apiDocumentation';

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

const BASE = {
    organizationId: 'DEFAULT',
    environmentId: 'env-1',
    area: 'TOP_NAVBAR',
    order: 1,
    published: true,
    visibility: 'PUBLIC',
} as const;

const SOURCE: PortalNavigationItemSource = { type: 'github-fetcher', configuration: { repository: 'docs', filepath: 'start.md' } };

const GUIDES: PortalNavigationFolder = { ...BASE, id: 'guides', title: 'Guides', type: 'FOLDER', rootId: 'guides' };

const GETTING_STARTED: PortalNavigationPage = {
    ...BASE,
    id: 'getting-started',
    title: 'Getting started',
    type: 'PAGE',
    parentId: 'guides',
    rootId: 'guides',
    portalPageContentId: 'content-1',
    source: SOURCE,
};

const STATUS: PortalNavigationLink = {
    ...BASE,
    id: 'status',
    title: 'Status page',
    type: 'LINK',
    rootId: 'status',
    url: 'https://status.example.com',
};

function renderDialog(item: ApiDocumentationItem, itemsInside = 0) {
    mockList.mockResolvedValue({ items: [item], publications: [] });
    mockUpdate.mockResolvedValue(item);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const onClose = jest.fn();
    render(<PublicationDialog open apiId="api-1" item={item} itemsInside={itemsInside} onClose={onClose} />, { wrapper });
    return { onClose, user: userEvent.setup() };
}

function button(name: string) {
    return screen.getByRole('button', { name }) as HTMLButtonElement;
}

function contentsCheckbox() {
    return screen.queryByRole('checkbox', { name: /^Also publish/ });
}

describe('PublicationDialog', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    });

    describe('unpublishing', () => {
        it('unpublishes a page, keeping its external source, then closes', async () => {
            const { user, onClose } = renderDialog(GETTING_STARTED);

            expect(screen.getByRole('heading', { name: 'Unpublish "Getting started"?' })).not.toBeNull();
            expect(screen.getByText('The page will become invisible in the developer portal.')).not.toBeNull();
            await user.click(button('Unpublish'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'getting-started',
                {
                    type: 'PAGE',
                    title: 'Getting started',
                    order: 1,
                    published: false,
                    visibility: 'PUBLIC',
                    parentId: 'guides',
                    source: SOURCE,
                },
                {},
            );
            expect(notify.success).toHaveBeenCalledWith("Page 'Getting started' unpublished");
        });

        it('unpublishes a link, keeping its address', async () => {
            const { user, onClose } = renderDialog(STATUS);

            expect(screen.getByText(/^The link will become invisible in the developer portal/)).not.toBeNull();
            await user.click(button('Unpublish'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'status',
                expect.objectContaining({ type: 'LINK', published: false, url: 'https://status.example.com' }),
                {},
            );
        });

        it('says the items inside a folder are unpublished with it', () => {
            renderDialog(GUIDES, 4);

            expect(screen.getByText('The folder and the 4 items inside it will become invisible in the developer portal.')).not.toBeNull();
            expect(contentsCheckbox()).toBeNull();
        });

        it('counts a single item inside a folder in the singular', () => {
            renderDialog(GUIDES, 1);

            expect(screen.getByText(/^The folder and the 1 item inside it will become invisible/)).not.toBeNull();
        });

        it('says only the folder becomes invisible when it is empty', () => {
            renderDialog(GUIDES);

            expect(screen.getByText(/^The folder will become invisible in the developer portal/)).not.toBeNull();
        });
    });

    describe('publishing', () => {
        it('publishes a page, then closes', async () => {
            const { user, onClose } = renderDialog({ ...GETTING_STARTED, published: false });

            expect(screen.getByRole('heading', { name: 'Publish "Getting started"?' })).not.toBeNull();
            expect(screen.getByText('The page will become visible in the developer portal.')).not.toBeNull();
            expect(contentsCheckbox()).toBeNull();
            await user.click(button('Publish'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'getting-started',
                expect.objectContaining({ published: true, source: SOURCE }),
                {},
            );
            expect(notify.success).toHaveBeenCalledWith("Page 'Getting started' published");
        });

        it("publishes a folder's contents with it by default", async () => {
            const { user, onClose } = renderDialog({ ...GUIDES, published: false }, 4);

            expect(contentsCheckbox()?.getAttribute('aria-checked')).toBe('true');
            await user.click(button('Publish'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith('env-1', 'api-1', 'guides', expect.objectContaining({ published: true }), {
                propagatePublishToChildren: true,
            });
            expect(notify.success).toHaveBeenCalledWith("Folder 'Guides' published");
        });

        it('publishes only the folder when its contents are left out', async () => {
            const { user, onClose } = renderDialog({ ...GUIDES, published: false }, 1);

            await user.click(screen.getByRole('checkbox', { name: 'Also publish the 1 item inside' }));
            await user.click(button('Publish'));

            await waitFor(() => expect(onClose).toHaveBeenCalled());
            expect(mockUpdate).toHaveBeenCalledWith('env-1', 'api-1', 'guides', expect.objectContaining({ published: true }), {
                propagatePublishToChildren: false,
            });
        });

        it('offers no choice for an empty folder', () => {
            renderDialog({ ...GUIDES, published: false });

            expect(contentsCheckbox()).toBeNull();
        });
    });

    it('shows why the server refuses, keeping the dialog open', async () => {
        const { user, onClose } = renderDialog({ ...GETTING_STARTED, published: false });
        mockUpdate.mockRejectedValue(new ApimApiError(400, 'The parent item must be published'));

        await user.click(button('Publish'));

        expect(await screen.findByText('The parent item must be published')).not.toBeNull();
        expect(button('Publish').disabled).toBe(false);
        expect(onClose).not.toHaveBeenCalled();
        expect(notify.success).not.toHaveBeenCalled();
    });

    it('sends the change only once when the button is clicked twice', async () => {
        const { user, onClose } = renderDialog(GETTING_STARTED);
        let resolveUpdate: (item: ApiDocumentationItem) => void = () => {};
        mockUpdate.mockReturnValue(new Promise(resolve => (resolveUpdate = resolve)));

        await user.dblClick(button('Unpublish'));
        expect(button('Unpublishing…').disabled).toBe(true);
        expect(button('Cancel').disabled).toBe(true);
        resolveUpdate(GETTING_STARTED);

        await waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(mockUpdate).toHaveBeenCalledTimes(1);
    });

    it('closes without a change on Cancel', async () => {
        const { user, onClose } = renderDialog(GETTING_STARTED);

        await user.click(button('Cancel'));

        expect(onClose).toHaveBeenCalled();
        expect(mockUpdate).not.toHaveBeenCalled();
    });
});
