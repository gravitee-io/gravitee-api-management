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
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiDocumentationEditPage } from './ApiDocumentationEditPage';
import { ApimApiError } from '../../../../shared/api/apimClient';
import { notify } from '../../../../shared/notify';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import {
    fetchApiDocumentationItem,
    getApiDocumentationPageContent,
    listApiDocumentation,
    saveApiDocumentationPageConfiguration,
    saveApiDocumentationPageContent,
    updateApiDocumentationItem,
} from '../../services/apiDocumentation';
import type { ApiDocumentationItem, PortalPageContent, PortalPageContentType } from '../../types/apiDocumentation';
import { describeCronSchedule } from '../../utils/cronSchedule';
import { formatDateTime } from '../../utils/formatDate';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    useHasPermission: jest.fn(),
}));

jest.mock('../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../services/apiDocumentation');
// Linking a source lists the fetchers; none is needed by these tests.
jest.mock('../../services/fetchers', () => ({ listFetchers: jest.fn(async () => []) }));

jest.mock('../../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
}));

let mockLayoutConfig: Record<string, unknown> | undefined;
jest.mock('@gravitee/graphene-core', () => ({
    ...jest.requireActual<object>('@gravitee/graphene-core'),
    useLayoutConfig: (config: Record<string, unknown>) => {
        mockLayoutConfig = config;
    },
    // The resizable panels find their handle from the pointer's position and the elements' boxes, which jsdom leaves
    // empty, so every click would land on the handle: plain elements stand in for them.
    ResizablePanelGroup: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    ResizablePanel: ({ children }: { children: ReactNode }) => <div>{children}</div>,
    ResizableHandle: ({ 'aria-label': label }: { 'aria-label'?: string }) => <div role="separator" aria-label={label} />,
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

// The previews have their own specs; here only what they are given to render matters.
jest.mock('../../components/detail/documentation/GraviteeMarkdownPreview', () => ({
    GraviteeMarkdownPreview: ({ content }: { content: string }) => <div aria-label="Preview">{content}</div>,
}));
jest.mock('../../components/detail/documentation/OpenApiPreview', () => ({
    OpenApiPreview: ({ content, configuration }: { content: string; configuration?: { viewer: string } }) => (
        <div aria-label="Preview">
            OpenAPI preview in {configuration?.viewer ?? 'no configuration'}: {content}
        </div>
    ),
}));
jest.mock('../../components/detail/documentation/AsyncApiPreview', () => ({
    AsyncApiPreview: ({ content }: { content: string }) => <div aria-label="Preview">AsyncAPI preview: {content}</div>,
}));

// jsdom has none of the pointer-capture and scrolling APIs Radix's select calls when it opens.
Element.prototype.hasPointerCapture ??= () => false;
Element.prototype.setPointerCapture ??= () => {};
Element.prototype.releasePointerCapture ??= () => {};
Element.prototype.scrollIntoView ??= () => {};

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockListApiDocumentation = jest.mocked(listApiDocumentation);
const mockGetContent = jest.mocked(getApiDocumentationPageContent);
const mockSaveContent = jest.mocked(saveApiDocumentationPageContent);
const mockSaveConfiguration = jest.mocked(saveApiDocumentationPageConfiguration);
const mockUpdateItem = jest.mocked(updateApiDocumentationItem);
const mockFetchItem = jest.mocked(fetchApiDocumentationItem);

const SOURCE = { type: 'github-fetcher', configuration: {} };

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

function givenContent(
    content: string,
    type: PortalPageContentType = 'GRAVITEE_MARKDOWN',
    configuration?: PortalPageContent['configuration'],
) {
    const data: PortalPageContent = { id: 'content-1', type, content, configuration };
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

beforeAll(() => {
    // The tooltips on the badges measure their trigger, which jsdom cannot do.
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
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

        it('explains who can view the page', async () => {
            renderPage();

            await userEvent.hover(await screen.findByText('Public'));

            expect((await screen.findByRole('tooltip')).textContent).toBe("Users don't need to sign in to view this page.");
        });

        it('edits Gravitee Markdown next to a preview of it', async () => {
            renderPage();

            const textbox = await editor();
            expect(mockGetContent).toHaveBeenCalledWith('env-1', 'api-1', 'getting-started');
            expect(textbox.value).toBe('# Getting started');
            expect(textbox.dataset.language).toBe('markdown');
            expect(screen.getByLabelText('Preview')).toHaveTextContent('# Getting started');
        });

        it('puts a handle resizing the editor and the preview between them', async () => {
            renderPage();
            const textbox = await editor();

            const separator = screen.getByRole('separator', { name: 'Resize the editor and the preview' });
            expect(textbox.compareDocumentPosition(separator) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
            expect(separator.compareDocumentPosition(screen.getByLabelText('Preview')) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        });

        it('edits an OpenAPI document next to a preview in the viewer its configuration names', async () => {
            givenContent('openapi: 3.0.0\n', 'OPENAPI', { viewer: 'REDOC' });
            renderPage('petstore');

            expect((await editor()).dataset.language).toBe('yaml');
            expect(screen.getByLabelText('Preview')).toHaveTextContent('OpenAPI preview in REDOC: openapi: 3.0.0');
            expect(screen.getByText('OpenAPI')).toBeInTheDocument();
            expect(screen.getByText('Published')).toBeInTheDocument();
            expect(screen.getByText('Private')).toBeInTheDocument();
        });

        it('edits an AsyncAPI document next to a preview of it', async () => {
            givenContent('asyncapi: 3.0.0\n', 'ASYNCAPI');
            renderPage('petstore');

            expect((await editor()).dataset.language).toBe('yaml');
            expect(screen.getByLabelText('Preview')).toHaveTextContent('AsyncAPI preview: asyncapi: 3.0.0');
        });

        it('previews the OpenAPI document being typed', async () => {
            givenContent('openapi: 3.0.0\n', 'OPENAPI', { viewer: 'SWAGGER' });
            renderPage('petstore');

            fireEvent.change(await editor(), { target: { value: 'openapi: 3.1.0\n' } });

            expect(screen.getByLabelText('Preview')).toHaveTextContent('OpenAPI preview in SWAGGER: openapi: 3.1.0');
        });
    });

    describe('editing', () => {
        it('keeps Save disabled until the content changes, and previews the change', async () => {
            renderPage();
            const textbox = await editor();
            expect(saveButton().disabled).toBe(true);

            await userEvent.type(textbox, '!');

            expect(saveButton().disabled).toBe(false);
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

        it('asks the browser to confirm leaving only while there are unsaved changes', async () => {
            renderPage();
            const textbox = await editor();
            expect(leavePage().defaultPrevented).toBe(false);

            await userEvent.type(textbox, '!');
            expect(leavePage().defaultPrevented).toBe(true);
        });
    });

    describe('editing the details', () => {
        it('warns, when linking a source, that the unsaved content would be lost', async () => {
            const user = userEvent.setup();
            renderPage();
            await user.type(await editor(), ' today');

            await user.click(screen.getByRole('button', { name: 'Edit details' }));
            const dialog = screen.getByRole('dialog', { name: 'Edit page details' });
            await user.click(within(dialog).getByRole('switch', { name: 'Fetch this page from an external source' }));

            expect(within(dialog).getByText(/unsaved changes to the content will be lost/)).toBeInTheDocument();
        });

        it('opens the details of the page beside the content actions', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(await screen.findByRole('button', { name: 'Edit details' }));

            const dialog = screen.getByRole('dialog', { name: 'Edit page details' });
            expect((within(dialog).getByRole('textbox', { name: 'Title' }) as HTMLInputElement).value).toBe('Getting started');
        });

        it('shows the new title once saved, keeping the unsaved content', async () => {
            const user = userEvent.setup();
            mockUpdateItem.mockResolvedValue({ ...GETTING_STARTED, title: 'First steps' });
            renderPage();
            await user.type(await editor(), ' today');

            await user.click(screen.getByRole('button', { name: 'Edit details' }));
            const dialog = screen.getByRole('dialog', { name: 'Edit page details' });
            const title = within(dialog).getByRole('textbox', { name: 'Title' });
            await user.clear(title);
            await user.type(title, 'First steps');
            givenItems([GUIDES, { ...GETTING_STARTED, title: 'First steps' }, PETSTORE]);
            await user.click(within(dialog).getByRole('button', { name: 'Save' }));

            expect(await screen.findByRole('heading', { name: 'First steps' })).toBeInTheDocument();
            expect(screen.queryByRole('dialog', { name: 'Edit page details' })).not.toBeInTheDocument();
            expect((await editor()).value).toBe('# Getting started today');
            expect(saveButton().disabled).toBe(false);
        });

        it('keeps a page inside a folder requiring authentication private', async () => {
            const user = userEvent.setup();
            givenItems([
                { ...GUIDES, visibility: 'PRIVATE' },
                { ...GETTING_STARTED, visibility: 'PRIVATE' },
            ]);
            renderPage();

            await user.click(await screen.findByRole('button', { name: 'Edit details' }));

            const dialog = screen.getByRole('dialog', { name: 'Edit page details' });
            expect(
                (within(dialog).getByRole('switch', { name: 'Authentication is required to view this page' }) as HTMLButtonElement)
                    .disabled,
            ).toBe(true);
            expect(within(dialog).getByText(/requires authentication, so this page does too/).textContent).toBe(
                'The parent folder Guides requires authentication, so this page does too.',
            );
        });
    });

    describe('configuring the OpenAPI viewer', () => {
        it('opens the viewer configuration of an OpenAPI page beside the other actions', async () => {
            const user = userEvent.setup();
            givenContent('openapi: 3.0.0\n', 'OPENAPI', { viewer: 'REDOC', tryItURL: 'https://api.example.com' });
            renderPage();

            await user.click(await screen.findByRole('button', { name: 'Configure' }));

            const dialog = screen.getByRole('dialog', { name: 'Configure OpenAPI Viewer' });
            expect((within(dialog).getByRole('textbox', { name: 'Base URL' }) as HTMLInputElement).value).toBe('https://api.example.com');
        });

        it.each<PortalPageContentType>(['GRAVITEE_MARKDOWN', 'ASYNCAPI'])('offers no viewer configuration for a %s page', async type => {
            givenContent('content', type);
            renderPage();

            await editor();
            expect(screen.queryByRole('button', { name: 'Configure' })).not.toBeInTheDocument();
        });

        it('offers no viewer configuration without api-documentation-u', async () => {
            grantedPermissions = ['api-documentation-r'];
            givenContent('openapi: 3.0.0\n', 'OPENAPI', { viewer: 'REDOC' });
            renderPage();

            await editor();
            expect(screen.queryByRole('button', { name: 'Configure' })).not.toBeInTheDocument();
        });

        it('lets a synced page be configured, as in the portal editor', async () => {
            givenContent('openapi: 3.0.0\n', 'OPENAPI', { viewer: 'REDOC' });
            givenItems([GUIDES, { ...GETTING_STARTED, source: SOURCE }]);
            renderPage();

            expect(await screen.findByRole('button', { name: 'Configure' })).toBeInTheDocument();
        });

        it('offers no viewer configuration inside a synced folder, as in the portal editor', async () => {
            givenContent('openapi: 3.0.0\n', 'OPENAPI', { viewer: 'REDOC' });
            givenItems([{ ...GUIDES, source: SOURCE }, GETTING_STARTED]);
            renderPage();

            await editor();
            expect(screen.queryByRole('button', { name: 'Configure' })).not.toBeInTheDocument();
        });

        it('previews in the viewer just saved, keeping the unsaved content', async () => {
            const user = userEvent.setup();
            givenContent('openapi: 3.0.0\n', 'OPENAPI', { viewer: 'REDOC', tryItURL: '' });
            mockSaveConfiguration.mockImplementation(async (_envId, _apiId, _navId, configuration) => ({
                id: 'content-1',
                type: 'OPENAPI',
                content: 'openapi: 3.0.0\n',
                configuration,
            }));
            renderPage();
            fireEvent.change(await editor(), { target: { value: 'openapi: 3.1.0\n' } });

            await user.click(screen.getByRole('button', { name: 'Configure' }));
            const dialog = screen.getByRole('dialog', { name: 'Configure OpenAPI Viewer' });
            await user.click(within(dialog).getByRole('combobox', { name: 'OpenAPI Documentation Viewer' }));
            await user.click(await screen.findByRole('option', { name: 'SwaggerUI' }));
            await user.click(within(dialog).getByRole('button', { name: 'Save' }));

            await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Configure OpenAPI Viewer' })).not.toBeInTheDocument());
            expect(mockSaveConfiguration).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                'getting-started',
                expect.objectContaining({ viewer: 'SWAGGER' }),
            );
            expect(screen.getByLabelText('Preview')).toHaveTextContent('OpenAPI preview in SWAGGER: openapi: 3.1.0');
            expect((await editor()).value).toBe('openapi: 3.1.0\n');
            expect(saveButton().disabled).toBe(false);
        });
    });

    describe('read only', () => {
        it('shows the content without letting a user without api-documentation-u change it', async () => {
            grantedPermissions = ['api-documentation-r'];
            renderPage();

            expect((await editor()).readOnly).toBe(true);
            expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Edit details' })).not.toBeInTheDocument();
        });

        it('lets nothing be edited in a page inside a synced folder, which the server keeps read only', async () => {
            givenItems([{ ...GUIDES, source: SOURCE }, GETTING_STARTED]);
            renderPage();

            expect((await editor()).readOnly).toBe(true);
            expect(screen.getByRole('status', { name: 'External source' })).toHaveTextContent(
                /^Managed by the folder Guides, read only\.$/,
            );
            expect(screen.queryByRole('button', { name: 'Fetch now' })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Edit details' })).not.toBeInTheDocument();
        });

        it('sends the user to a synced folder that can be fetched', async () => {
            givenItems([{ ...GUIDES, source: { ...SOURCE, subtreeImport: true } }, GETTING_STARTED]);
            renderPage();
            await editor();

            expect(screen.getByText('Managed by the folder Guides, read only. Fetch it from that folder.')).toBeInTheDocument();
        });

        it('does not send a user without api-documentation-u to fetch the synced folder', async () => {
            grantedPermissions = ['api-documentation-r'];
            givenItems([{ ...GUIDES, source: { ...SOURCE, subtreeImport: true } }, GETTING_STARTED]);
            renderPage();
            await editor();

            expect(screen.getByRole('status', { name: 'External source' })).toHaveTextContent(
                /^Managed by the folder Guides, read only\.$/,
            );
        });

        it('keeps the content of a synced page read only, but lets its details be edited', async () => {
            givenItems([GUIDES, { ...GETTING_STARTED, source: SOURCE }]);
            renderPage();

            expect((await editor()).readOnly).toBe(true);
            expect(screen.getByText('Content from GitHub, read only')).toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Edit details' })).toBeInTheDocument();
        });
    });

    describe('a page synced from its own source', () => {
        const LAST_FETCHED_AT = '2026-10-12T10:42:00Z';
        const FAILED_AT = '2026-10-12T16:42:00Z';
        const SYNCED: ApiDocumentationItem = {
            ...GETTING_STARTED,
            source: { ...SOURCE, useAutoFetch: true, fetchCron: '0 0 * * * *', lastFetchedAt: LAST_FETCHED_AT },
        };

        it('names its source, when it was last fetched and how often it is fetched', async () => {
            givenItems([GUIDES, SYNCED]);
            renderPage();
            await editor();

            const status = screen.getByRole('status', { name: 'External source' });
            expect(status).toHaveTextContent('Content from GitHub, read only');
            expect(status).toHaveTextContent(`Last fetched ${formatDateTime(LAST_FETCHED_AT)}`);
            expect(status).toHaveTextContent(describeCronSchedule('0 0 * * * *')!);
        });

        it('says it has not been fetched yet', async () => {
            givenItems([GUIDES, { ...GETTING_STARTED, source: SOURCE }]);
            renderPage();
            await editor();

            expect(screen.getByRole('status', { name: 'External source' })).toHaveTextContent('Not fetched yet');
        });

        it('shows no schedule once automatic fetching is off', async () => {
            givenItems([GUIDES, { ...SYNCED, source: { ...SYNCED.source!, useAutoFetch: false } }]);
            renderPage();
            await editor();

            expect(screen.getByRole('status', { name: 'External source' })).not.toHaveTextContent(describeCronSchedule('0 0 * * * *')!);
        });

        it("does not add a period to the server's reason when it already ends with one", async () => {
            givenItems([
                GUIDES,
                { ...SYNCED, source: { ...SYNCED.source!, lastFetchAttemptAt: FAILED_AT, lastFetchError: 'Repository not found.' } },
            ]);
            renderPage();
            await editor();

            expect(screen.getByText(/Repository not found\. The content below/)).toBeInTheDocument();
        });

        it("explains a failed last fetch with the server's reason, keeping the content of the last successful one", async () => {
            givenItems([
                GUIDES,
                { ...SYNCED, source: { ...SYNCED.source!, lastFetchAttemptAt: FAILED_AT, lastFetchError: 'Unable to fetch content' } },
            ]);
            renderPage();
            await editor();

            expect(
                screen.getByText(
                    `Last fetch failed on ${formatDateTime(FAILED_AT)}. Unable to fetch content. The content below is from the last successful fetch.`,
                ),
            ).toBeInTheDocument();
        });

        it('shows no content before its first successful fetch, rather than the placeholder the page was created with', async () => {
            givenItems([
                GUIDES,
                { ...GETTING_STARTED, source: { ...SOURCE, lastFetchAttemptAt: FAILED_AT, lastFetchError: 'Unable to fetch content' } },
            ]);
            renderPage();

            expect((await editor()).value).toBe('');
            expect(
                screen.getByText(
                    /No content has been fetched from it yet\. Until then, the page holds the placeholder it was created with\./,
                ),
            ).toBeInTheDocument();
            expect(screen.getByLabelText('Preview')).not.toHaveTextContent('# Getting started');
        });

        it('fetches it on demand and shows the content fetched', async () => {
            givenItems([GUIDES, SYNCED]);
            mockFetchItem.mockImplementation(async () => {
                givenContent('# Getting started, fetched again');
                return { item: SYNCED };
            });
            const user = userEvent.setup();
            renderPage();
            await editor();

            await user.click(screen.getByRole('button', { name: 'Fetch now' }));

            expect(mockFetchItem).toHaveBeenCalledWith('env-1', 'api-1', 'getting-started');
            await waitFor(async () => expect((await editor()).value).toBe('# Getting started, fetched again'));
            expect(notify.success).toHaveBeenCalledWith("Content of 'Getting started' fetched");
        });

        it('starts editing from the content last fetched once the page no longer has a source', async () => {
            givenItems([GUIDES, SYNCED]);
            mockFetchItem.mockImplementation(async () => {
                givenContent('# Fetched again');
                return { item: SYNCED };
            });
            const user = userEvent.setup();
            const queryClient = renderPage();
            await editor();
            await user.click(screen.getByRole('button', { name: 'Fetch now' }));
            await waitFor(async () => expect((await editor()).value).toBe('# Fetched again'));

            givenItems([GUIDES, GETTING_STARTED]);
            await act(() => queryClient.invalidateQueries());

            await waitFor(async () => expect((await editor()).readOnly).toBe(false));
            expect((await editor()).value).toBe('# Fetched again');
            expect(saveButton().disabled).toBe(true);
        });

        it('offers no other fetch while one runs', async () => {
            givenItems([GUIDES, SYNCED]);
            mockFetchItem.mockReturnValue(new Promise(() => {}));
            const user = userEvent.setup();
            renderPage();
            await editor();

            await user.click(screen.getByRole('button', { name: 'Fetch now' }));

            await waitFor(() => expect(screen.getByRole('button', { name: 'Fetch now' })).toBeDisabled());
        });

        it('does not show the placeholder as fetched content while its first fetch reloads', async () => {
            const neverFetched = { ...GETTING_STARTED, source: SOURCE };
            givenItems([GUIDES, neverFetched]);
            mockFetchItem.mockImplementation(async () => {
                givenItems([GUIDES, { ...neverFetched, source: { ...SOURCE, lastFetchedAt: LAST_FETCHED_AT } }]);
                mockGetContent.mockReturnValue(new Promise(() => {}));
                return { item: SYNCED };
            });
            const user = userEvent.setup();
            renderPage();
            await editor();

            await user.click(screen.getByRole('button', { name: 'Fetch now' }));
            await waitFor(() => expect(mockGetContent).toHaveBeenCalledTimes(2));
            await act(async () => {});

            expect((await editor()).value).toBe('');
        });

        it('offers no fetch without api-documentation-u', async () => {
            grantedPermissions = ['api-documentation-r'];
            givenItems([GUIDES, SYNCED]);
            renderPage();
            await editor();

            expect(screen.queryByRole('button', { name: 'Fetch now' })).not.toBeInTheDocument();
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
