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

import { PublishToPortalDialog } from './PublishToPortalDialog';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { createApiOverviewPage, listApiPublishLocations, publishApiToPortal } from '../../../services/apiDocumentation';
import type { ApiPortalPublication, ApiPortalPublishLocation, PortalNavigationPage } from '../../../types/apiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../../../services/apiDocumentation');
jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
}));

// jsdom ships no ResizeObserver, which the dialog and its select mount.
globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};

// ...nor the pointer-capture and scrolling APIs Radix's select calls when it opens.
Element.prototype.hasPointerCapture ??= () => false;
Element.prototype.setPointerCapture ??= () => {};
Element.prototype.releasePointerCapture ??= () => {};
Element.prototype.scrollIntoView ??= () => {};

const mockLocations = jest.mocked(listApiPublishLocations);
const mockPublish = jest.mocked(publishApiToPortal);
const mockCreateOverviewPage = jest.mocked(createApiOverviewPage);

const GUIDES: ApiPortalPublishLocation = { id: 'guides', name: 'Guides' };
const APIS: ApiPortalPublishLocation = { id: 'apis', name: 'APIs' };
const PARTNERS: ApiPortalPublishLocation = { id: 'partners', name: 'Partner APIs' };

function publication(sectionId: string, sectionName: string, published: boolean): ApiPortalPublication {
    return {
        portalId: 'DEFAULT',
        sectionName,
        portalNavigationItem: {
            id: 'listing',
            organizationId: 'DEFAULT',
            environmentId: 'env-1',
            title: 'Payment API',
            type: 'API',
            apiId: 'api-1',
            area: 'TOP_NAVBAR',
            parentId: sectionId,
            rootId: sectionId,
            order: 0,
            published,
            visibility: 'PUBLIC',
        },
    };
}

function renderDialog(
    publications: ApiPortalPublication[] = [],
    { hasPage = true, canCreatePage = true }: { hasPage?: boolean; canCreatePage?: boolean } = {},
) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const onClose = jest.fn();
    const onOverviewPageCreated = jest.fn();
    render(
        <PublishToPortalDialog
            open
            apiId="api-1"
            publications={publications}
            hasPage={hasPage}
            canCreatePage={canCreatePage}
            onOverviewPageCreated={onOverviewPageCreated}
            onClose={onClose}
        />,
        { wrapper },
    );
    return { onClose, onOverviewPageCreated, user: userEvent.setup() };
}

const OVERVIEW_PAGE = { id: 'overview', type: 'PAGE', title: 'Overview' } as PortalNavigationPage;

function createOverviewPageButton() {
    return screen.getByRole('button', { name: /^(Create Overview page|Creating)/ }) as HTMLButtonElement;
}

function sectionPicker() {
    return screen.getByRole('combobox', { name: 'Section' }) as HTMLButtonElement;
}

function publishButton() {
    return screen.getByRole('button', { name: /^Publish/ }) as HTMLButtonElement;
}

describe('PublishToPortalDialog', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockLocations.mockResolvedValue({ data: [GUIDES, APIS, PARTNERS] });
        mockPublish.mockImplementation(async (_envId, _apiId, { sectionId }) =>
            publication(sectionId, [GUIDES, APIS, PARTNERS].find(location => location.id === sectionId)!.name, true),
        );
    });

    it('says the API and its documentation become visible, including the items unpublished one by one', () => {
        renderDialog();

        expect(
            screen.getByText(
                'The API and its documentation will become visible in the developer portal. Every page, folder and link will be published, including those unpublished individually.',
            ),
        ).not.toBeNull();
    });

    it('offers the section named APIs first, and publishes the API there', async () => {
        const { user, onClose } = renderDialog();

        await waitFor(() => expect(sectionPicker().textContent).toBe('APIs'));
        await user.click(publishButton());

        await waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(mockPublish).toHaveBeenCalledWith('env-1', 'api-1', { sectionId: 'apis' });
        expect(notify.success).toHaveBeenCalledWith("Published in 'APIs'");
    });

    it('publishes the API in the section chosen instead', async () => {
        const { user, onClose } = renderDialog();

        await waitFor(() => expect(sectionPicker().textContent).toBe('APIs'));
        await user.click(sectionPicker());
        await user.click(await screen.findByRole('option', { name: 'Guides' }));
        await user.click(publishButton());

        await waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(mockPublish).toHaveBeenCalledWith('env-1', 'api-1', { sectionId: 'guides' });
        expect(notify.success).toHaveBeenCalledWith("Published in 'Guides'");
    });

    describe('when the API already has a hidden listing', () => {
        it('offers the section the listing is in, without announcing a move', async () => {
            renderDialog([publication('partners', 'Partner APIs', false)]);

            await waitFor(() => expect(sectionPicker().textContent).toBe('Partner APIs'));
            expect(screen.queryByText(/Publishing moves it/)).toBeNull();
        });

        it('says where the listing is and that publishing moves it, when it is somewhere the API cannot be published to', async () => {
            renderDialog([publication('nested', 'Internal', false)]);

            expect(
                await screen.findByText(
                    'This API is already in the portal menu, hidden, under "Internal". Publishing moves it to the section you choose.',
                ),
            ).not.toBeNull();
            expect(sectionPicker().textContent).toBe('APIs');
        });
    });

    it('shows an empty section picker, saying why, when the portal has no section to publish to', async () => {
        mockLocations.mockResolvedValue({ data: [] });
        renderDialog();

        expect(
            await screen.findByText(
                "The portal's top navigation has no published section to list this API in. Ask a portal administrator to add one.",
            ),
        ).not.toBeNull();
        expect(sectionPicker().textContent).toBe('No sections available');
        expect(sectionPicker().disabled).toBe(true);
        expect(publishButton().disabled).toBe(true);
    });

    it('cannot publish while the sections are loading', () => {
        mockLocations.mockReturnValue(new Promise(() => {}));
        renderDialog();

        expect(sectionPicker().textContent).toBe('Loading sections…');
        expect(publishButton().disabled).toBe(true);
    });

    it('reports sections that failed to load', async () => {
        mockLocations.mockRejectedValue(new ApimApiError(500, 'Internal error'));
        renderDialog();

        expect(await screen.findByText('Failed to load the portal sections. Close and try again.')).not.toBeNull();
        expect(publishButton().disabled).toBe(true);
    });

    it('cannot be sent twice while publishing', async () => {
        mockPublish.mockReturnValue(new Promise(() => {}));
        const { user } = renderDialog();

        await waitFor(() => expect(sectionPicker().textContent).toBe('APIs'));
        await user.click(publishButton());

        expect(publishButton().textContent).toBe('Publishing…');
        expect(publishButton().disabled).toBe(true);
    });

    it('cannot be dismissed while publishing, so a refusal that comes afterwards is still shown', async () => {
        let refuse: (error: Error) => void = () => {};
        mockPublish.mockReturnValue(new Promise((_resolve, reject) => (refuse = reject)));
        const { user, onClose } = renderDialog();

        await waitFor(() => expect(sectionPicker().textContent).toBe('APIs'));
        await user.click(publishButton());
        await user.keyboard('{Escape}');

        expect(onClose).not.toHaveBeenCalled();
        refuse(new ApimApiError(400, 'The API is already published'));
        expect(await screen.findByText('The API is already published')).not.toBeNull();
    });

    it('shows why the server refused, keeping the dialog open with the chosen section', async () => {
        mockPublish.mockRejectedValue(new ApimApiError(400, 'The API is already published'));
        const { user, onClose } = renderDialog();

        await waitFor(() => expect(sectionPicker().textContent).toBe('APIs'));
        await user.click(sectionPicker());
        await user.click(await screen.findByRole('option', { name: 'Guides' }));
        await user.click(publishButton());

        expect(await screen.findByText('The API is already published')).not.toBeNull();
        expect(sectionPicker().textContent).toBe('Guides');
        expect(publishButton().disabled).toBe(false);
        expect(onClose).not.toHaveBeenCalled();
        expect(notify.success).not.toHaveBeenCalled();
    });

    describe('when the API has no page', () => {
        it('asks for a page first, without offering a section', () => {
            renderDialog([], { hasPage: false });

            expect(screen.getByRole('heading', { name: 'Add a page before publishing' })).not.toBeNull();
            expect(
                screen.getByText(
                    "An API needs at least one page to be shown in the developer portal. Start from an Overview page built from the API's details.",
                ),
            ).not.toBeNull();
            expect(screen.queryByRole('combobox', { name: 'Section' })).toBeNull();
            expect(screen.queryByRole('button', { name: /^Publish/ })).toBeNull();
            expect(mockLocations).not.toHaveBeenCalled();
        });

        it('creates the Overview page and hands it over', async () => {
            mockCreateOverviewPage.mockResolvedValue(OVERVIEW_PAGE);
            const { user, onOverviewPageCreated } = renderDialog([], { hasPage: false });

            await user.click(createOverviewPageButton());

            await waitFor(() => expect(onOverviewPageCreated).toHaveBeenCalledWith('overview'));
            expect(mockCreateOverviewPage).toHaveBeenCalledWith('env-1', 'api-1');
            expect(notify.success).toHaveBeenCalledWith("Page 'Overview' created");
            expect(mockPublish).not.toHaveBeenCalled();
        });

        it('cannot be sent twice or dismissed while creating the page', async () => {
            mockCreateOverviewPage.mockReturnValue(new Promise(() => {}));
            const { user, onClose } = renderDialog([], { hasPage: false });

            await user.click(createOverviewPageButton());
            await user.keyboard('{Escape}');

            expect(createOverviewPageButton().textContent).toBe('Creating…');
            expect(createOverviewPageButton().disabled).toBe(true);
            expect(onClose).not.toHaveBeenCalled();
        });

        it('shows why the server refused, keeping the dialog open', async () => {
            mockCreateOverviewPage.mockRejectedValue(new ApimApiError(500, 'The page could not be created'));
            const { user, onClose, onOverviewPageCreated } = renderDialog([], { hasPage: false });

            await user.click(createOverviewPageButton());

            expect(await screen.findByText('The page could not be created')).not.toBeNull();
            expect(createOverviewPageButton().disabled).toBe(false);
            expect(onOverviewPageCreated).not.toHaveBeenCalled();
            expect(onClose).not.toHaveBeenCalled();
        });

        it('only explains, without permission to add documentation', async () => {
            const { user, onClose } = renderDialog([], { hasPage: false, canCreatePage: false });

            expect(
                screen.getByText(
                    'An API needs at least one page to be shown in the developer portal. Ask someone who can add documentation to this API.',
                ),
            ).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Create Overview page' })).toBeNull();

            // The footer's button, not the dialog's own close icon, which bears the same name
            await user.click(screen.getByText('Close', { selector: 'button' }));

            expect(onClose).toHaveBeenCalled();
        });
    });
});
