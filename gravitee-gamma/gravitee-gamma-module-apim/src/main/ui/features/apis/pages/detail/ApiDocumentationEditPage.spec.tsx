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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiDocumentationEditPage } from './ApiDocumentationEditPage';
import { ApimApiError } from '../../../../shared/api/apimClient';
import { notify } from '../../../../shared/notify';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { getApiDocumentationPageContent, listApiDocumentation, saveApiDocumentationPageContent } from '../../services/apiDocumentation';
import type { ApiDocumentationItem, PortalPageContent, PortalPageContentType } from '../../types/apiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    useHasPermission: jest.fn(),
}));

jest.mock('../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../services/apiDocumentation');

jest.mock('../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

let mockLayoutConfig: Record<string, unknown> | undefined;
jest.mock('@gravitee/graphene-core', () => ({
    ...jest.requireActual<object>('@gravitee/graphene-core'),
    useLayoutConfig: (config: Record<string, unknown>) => {
        mockLayoutConfig = config;
    },
}));

// Monaco does not run in jsdom: a textarea stands in for the editor.
jest.mock('@gravitee/graphene-core/code-editor', () => ({
    CodeEditor: ({
        value,
        onChange,
        language,
        readOnly,
    }: {
        value?: string;
        onChange?: (next: string) => void;
        language?: string;
        readOnly?: boolean;
    }) => (
        <textarea
            aria-label="Page content"
            data-language={language}
            value={value}
            readOnly={readOnly}
            onChange={event => onChange?.(event.target.value)}
        />
    ),
}));

// The preview has its own spec; here only what it is given to render matters.
jest.mock('../../components/detail/documentation/GraviteeMarkdownPreview', () => ({
    GraviteeMarkdownPreview: ({ content }: { content: string }) => <div aria-label="Preview">{content}</div>,
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockListApiDocumentation = jest.mocked(listApiDocumentation);
const mockGetContent = jest.mocked(getApiDocumentationPageContent);
const mockSaveContent = jest.mocked(saveApiDocumentationPageContent);

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
const PETSTORE: ApiDocumentationItem = {
    ...BASE,
    id: 'petstore',
    rootId: 'petstore',
    title: 'Petstore',
    type: 'PAGE',
    portalPageContentId: 'content-2',
    order: 1,
    published: true,
    visibility: 'PRIVATE',
};

let grantedPermissions: string[];

function givenItems(items: ApiDocumentationItem[]) {
    mockListApiDocumentation.mockResolvedValue({ items, publications: [] });
}

function givenContent(content: string, type: PortalPageContentType = 'GRAVITEE_MARKDOWN') {
    const data: PortalPageContent = { id: 'content-1', type, content };
    mockGetContent.mockResolvedValue(data);
}

function renderPage(pageId = 'getting-started') {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`/apis/api-1/documentation/${pageId}/edit`]}>
                <Routes>
                    <Route path="apis/:apiId/documentation">
                        <Route index element={<p>Documentation list</p>} />
                        <Route path=":pageId/edit" element={<ApiDocumentationEditPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return queryClient;
}

async function editor() {
    return (await screen.findByRole('textbox', { name: 'Page content' })) as HTMLTextAreaElement;
}

function saveButton() {
    return screen.getByRole('button', { name: 'Save changes' }) as HTMLButtonElement;
}

function discardButton() {
    return screen.getByRole('button', { name: 'Discard' }) as HTMLButtonElement;
}

function leavePage() {
    const event = new Event('beforeunload', { cancelable: true });
    fireEvent(window, event);
    return event;
}

beforeEach(() => {
    jest.clearAllMocks();
    grantedPermissions = ['api-documentation-r', 'api-documentation-u'];
    mockUseHasPermission.mockImplementation(({ anyOf = [] }: { anyOf?: string[] }) => anyOf.some(p => grantedPermissions.includes(p)));
    jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    mockUseApiDetailContext.mockReturnValue({ api: { id: 'api-1' }, isLoading: false, permissionsReady: true });
    givenItems([GUIDES, GETTING_STARTED, PETSTORE]);
    givenContent('# Getting started');
    mockSaveContent.mockImplementation(async (_envId, _apiId, _navId, { content }) => ({
        id: 'content-1',
        type: 'GRAVITEE_MARKDOWN',
        content,
    }));
});

describe('ApiDocumentationEditPage', () => {
    // The shell's default content area is only as tall as the page, so the editor could not fill the window.
    it('asks the shell for a content area as tall as the window, for the editor to fill', async () => {
        mockLayoutConfig = undefined;
        renderPage();

        await editor();
        expect(mockLayoutConfig).toEqual({ contentVariant: 'full-bleed' });
    });

    it('renders nothing until the permission request has resolved', () => {
        mockUseApiDetailContext.mockReturnValue({ api: { id: 'api-1' }, isLoading: false, permissionsReady: false });
        renderPage();

        expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    });

    it('tells a user without api-documentation-r that they cannot view the page, and loads nothing', () => {
        grantedPermissions = [];
        renderPage();

        expect(screen.getByText(/don.t have permission to view/i)).toBeInTheDocument();
        expect(mockListApiDocumentation).not.toHaveBeenCalled();
        expect(mockGetContent).not.toHaveBeenCalled();
    });

    describe('showing the page', () => {
        it('shows the folders holding the page under its title, leaving the way back to the sidebar', async () => {
            const authentication: ApiDocumentationItem = {
                ...GUIDES,
                id: 'auth',
                rootId: 'guides',
                parentId: 'guides',
                title: 'Authentication',
            };
            givenItems([GUIDES, authentication, { ...GETTING_STARTED, parentId: 'auth' }]);
            renderPage();

            const title = await screen.findByRole('heading', { name: 'Getting started' });
            const folders = screen.getByText('Guides / Authentication');
            expect(title.compareDocumentPosition(folders) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
            expect(screen.queryByRole('link')).not.toBeInTheDocument();
        });

        it('shows no folders for a page at the top level', async () => {
            givenContent('openapi: 3.0.0\n', 'OPENAPI');
            renderPage('petstore');

            expect(await screen.findByRole('heading', { name: 'Petstore' })).toBeInTheDocument();
            expect(screen.queryByText('Guides')).not.toBeInTheDocument();
        });

        it('says what kind of page it is, whether it is published and who can view it', async () => {
            renderPage();

            expect(await screen.findByText('Gravitee Markdown')).toBeInTheDocument();
            expect(screen.getByText('Unpublished')).toBeInTheDocument();
            expect(screen.getByText('Public')).toBeInTheDocument();
        });

        it('edits Gravitee Markdown next to a preview of it', async () => {
            renderPage();

            const textbox = await editor();
            expect(mockGetContent).toHaveBeenCalledWith('env-1', 'api-1', 'getting-started');
            expect(textbox.value).toBe('# Getting started');
            expect(textbox.dataset.language).toBe('markdown');
            expect(screen.getByLabelText('Preview')).toHaveTextContent('# Getting started');
        });

        it('edits an OpenAPI document, saying its preview is not available yet', async () => {
            givenContent('openapi: 3.0.0\n', 'OPENAPI');
            renderPage('petstore');

            expect((await editor()).dataset.language).toBe('yaml');
            expect(screen.queryByLabelText('Preview')).not.toBeInTheDocument();
            expect(
                screen.getByText("OpenAPI preview isn't available here yet. The developer portal renders this page once it's published."),
            ).toBeInTheDocument();
            expect(screen.getByText('OpenAPI')).toBeInTheDocument();
            expect(screen.getByText('Published')).toBeInTheDocument();
            expect(screen.getByText('Private')).toBeInTheDocument();
        });
    });

    describe('editing', () => {
        it('keeps Save and Discard disabled until the content changes, and previews the change', async () => {
            renderPage();
            const textbox = await editor();
            expect(saveButton().disabled).toBe(true);
            expect(discardButton().disabled).toBe(true);

            await userEvent.type(textbox, '!');

            expect(saveButton().disabled).toBe(false);
            expect(discardButton().disabled).toBe(false);
            expect(screen.getByLabelText('Preview')).toHaveTextContent('# Getting started!');
        });

        it('saves the content and settles on what was saved', async () => {
            renderPage();

            await userEvent.type(await editor(), '!');
            await userEvent.click(saveButton());

            expect(mockSaveContent).toHaveBeenCalledWith('env-1', 'api-1', 'getting-started', { content: '# Getting started!' });
            expect(notify.success).toHaveBeenCalledWith("Page 'Getting started' saved");
            expect((await editor()).value).toBe('# Getting started!');
            expect(saveButton().disabled).toBe(true);
        });

        it('keeps the changes when the server refuses them', async () => {
            const serverError = new ApimApiError(400, 'Invalid Gravitee Markdown');
            mockSaveContent.mockRejectedValue(serverError);
            renderPage();

            await userEvent.type(await editor(), '!');
            await userEvent.click(saveButton());

            expect(notify.error).toHaveBeenCalledWith(serverError, "Failed to save 'Getting started'");
            expect((await editor()).value).toBe('# Getting started!');
            expect(saveButton().disabled).toBe(false);
        });

        it('keeps the editor and its unsaved changes when reloading the documentation or the content fails', async () => {
            const queryClient = renderPage();
            await userEvent.type(await editor(), '!');

            mockListApiDocumentation.mockRejectedValue(new ApimApiError(500, 'Internal error'));
            mockGetContent.mockRejectedValue(new ApimApiError(500, 'Internal error'));
            await act(() => queryClient.refetchQueries());
            // React Query tells the page about the failure on a later tick.
            await act(() => new Promise(resolve => setTimeout(resolve, 0)));

            expect(
                queryClient
                    .getQueryCache()
                    .getAll()
                    .map(query => query.state.status),
            ).toEqual(['error', 'error']);
            expect(screen.queryByText(/Failed to load/)).not.toBeInTheDocument();
            expect((await editor()).value).toBe('# Getting started!');
        });

        it('goes back to the saved content on Discard', async () => {
            renderPage();

            await userEvent.type(await editor(), '!');
            await userEvent.click(discardButton());

            expect((await editor()).value).toBe('# Getting started');
            expect(saveButton().disabled).toBe(true);
            expect(mockSaveContent).not.toHaveBeenCalled();
        });

        it('asks the browser to confirm leaving only while there are unsaved changes', async () => {
            renderPage();
            const textbox = await editor();
            expect(leavePage().defaultPrevented).toBe(false);

            await userEvent.type(textbox, '!');
            expect(leavePage().defaultPrevented).toBe(true);

            await userEvent.click(discardButton());
            expect(leavePage().defaultPrevented).toBe(false);
        });
    });

    describe('read only', () => {
        it('shows the content without letting a user without api-documentation-u change it', async () => {
            grantedPermissions = ['api-documentation-r'];
            renderPage();

            expect((await editor()).readOnly).toBe(true);
            expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Discard' })).not.toBeInTheDocument();
        });

        it('explains that a page synced from an external source cannot be edited', async () => {
            const synced: ApiDocumentationItem = { ...GUIDES, source: { type: 'github-fetcher', configuration: {} } };
            givenItems([synced, GETTING_STARTED]);
            renderPage();

            expect((await editor()).readOnly).toBe(true);
            expect(screen.getByText(/synced from an external source/i)).toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
        });
    });

    describe('when the page cannot be shown', () => {
        it.each([
            ['is not part of the API documentation', 'unknown'],
            ['is a folder', 'guides'],
        ])('says the page does not exist when its id %s, and loads no content', async (_case, pageId) => {
            renderPage(pageId);

            expect(await screen.findByText('This page does not exist in the documentation of this API.')).toBeInTheDocument();
            expect(mockGetContent).not.toHaveBeenCalled();
        });

        it('waits for the documentation and the content to load', async () => {
            mockGetContent.mockReturnValue(new Promise(() => undefined));
            renderPage();

            await waitFor(() => expect(mockGetContent).toHaveBeenCalled());
            expect(screen.getByRole('status', { name: 'Loading the page' })).toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        });

        it('reports documentation that failed to load', async () => {
            mockListApiDocumentation.mockRejectedValue(new ApimApiError(500, 'Internal error'));
            renderPage();

            expect(await screen.findByText('Failed to load the documentation. Refresh the page.')).toBeInTheDocument();
        });

        it('reports content that failed to load', async () => {
            mockGetContent.mockRejectedValue(new ApimApiError(500, 'Internal error'));
            renderPage();

            expect(await screen.findByText('Failed to load the content of this page. Refresh the page.')).toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        });
    });
});
