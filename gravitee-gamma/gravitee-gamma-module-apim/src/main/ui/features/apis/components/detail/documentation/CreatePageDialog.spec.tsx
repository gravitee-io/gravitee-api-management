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

import { CreatePageDialog } from './CreatePageDialog';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { createApiDocumentationItem, saveApiDocumentationPageContent } from '../../../services/apiDocumentation';
import type { PortalNavigationFolder, PortalNavigationPage } from '../../../types/apiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../../../services/apiDocumentation');
jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
}));

const mockCreateItem = jest.mocked(createApiDocumentationItem);
const mockSaveContent = jest.mocked(saveApiDocumentationPageContent);

const CREATED_PAGE: PortalNavigationPage = {
    id: 'nav-1',
    organizationId: 'DEFAULT',
    environmentId: 'env-1',
    title: 'petstore',
    type: 'PAGE',
    area: 'TOP_NAVBAR',
    rootId: 'nav-1',
    order: 0,
    published: false,
    visibility: 'PUBLIC',
    portalPageContentId: 'content-1',
};

const OPENAPI_YAML = 'openapi: 3.0.0\ninfo:\n  title: Petstore\n';

type User = ReturnType<typeof userEvent.setup>;

const GUIDES: PortalNavigationFolder = {
    id: 'guides',
    organizationId: 'DEFAULT',
    environmentId: 'env-1',
    title: 'Guides',
    type: 'FOLDER',
    area: 'TOP_NAVBAR',
    rootId: 'guides',
    order: 0,
    published: false,
    visibility: 'PUBLIC',
};

function renderDialog({ parent }: { parent?: PortalNavigationFolder } = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const onClose = jest.fn();
    const onCreated = jest.fn();
    // The dropzone, not the browser, must judge the file type, so the test does not pre-filter on `accept`.
    const user = userEvent.setup({ applyAccept: false });
    render(<CreatePageDialog open apiId="api-1" parent={parent} onClose={onClose} onCreated={onCreated} />, { wrapper });
    return { onClose, onCreated, user };
}

async function chooseImport(user: User) {
    await user.click(screen.getByRole('radio', { name: 'Import from file' }));
}

async function uploadFile(user: User, file: File) {
    const input = document.querySelector<HTMLInputElement>('input[type="file"]');
    if (!input) throw new Error('No file input rendered');
    await user.upload(input, file);
}

async function importFile(user: User, file: File) {
    await chooseImport(user);
    await uploadFile(user, file);
}

function titleInput() {
    return screen.getByRole('textbox', { name: 'Title' }) as HTMLInputElement;
}

function createButton() {
    return screen.getByRole('button', { name: 'Create' }) as HTMLButtonElement;
}

describe('CreatePageDialog', () => {
    // jsdom's File does not extend the global Blob here, so it has no text() to inherit.
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
        File.prototype.text = function (this: File) {
            return new Promise<string>((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result as string);
                reader.onerror = () => reject(reader.error);
                reader.readAsText(this);
            });
        };
    });

    afterAll(() => {
        delete (File.prototype as Partial<File>).text;
    });

    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockCreateItem.mockResolvedValue(CREATED_PAGE);
        mockSaveContent.mockResolvedValue({ id: 'content-1', type: 'OPENAPI', content: OPENAPI_YAML });
    });

    it('offers to fill in a Gravitee Markdown page by default, and needs a title', async () => {
        const { user } = renderDialog();

        expect(screen.getByRole('heading', { name: 'Add a page' })).not.toBeNull();
        expect(screen.getByRole('radio', { name: 'Fill in content' }).getAttribute('aria-checked')).toBe('true');
        expect(screen.getByRole('radio', { name: 'Gravitee Markdown' }).getAttribute('aria-checked')).toBe('true');
        expect(createButton().disabled).toBe(true);

        await user.type(titleInput(), '   ');
        expect(createButton().disabled).toBe(true);
    });

    it('does not offer to link an external source yet', () => {
        renderDialog();

        expect(screen.getAllByRole('radio').map(radio => radio.getAttribute('aria-label'))).not.toContain('Link to external source');
    });

    describe('filling in content', () => {
        it('creates an empty public page of the chosen type at the top level, then hands it over', async () => {
            const { user, onCreated } = renderDialog();

            await user.type(titleInput(), ' Petstore reference ');
            await user.click(screen.getByRole('radio', { name: 'OpenAPI' }));
            await user.click(createButton());

            await waitFor(() => expect(onCreated).toHaveBeenCalledWith('nav-1'));
            expect(mockCreateItem).toHaveBeenCalledWith('env-1', 'api-1', {
                type: 'PAGE',
                title: 'Petstore reference',
                contentType: 'OPENAPI',
                area: 'TOP_NAVBAR',
                visibility: 'PUBLIC',
            });
            expect(mockSaveContent).not.toHaveBeenCalled();
            expect(notify.success).toHaveBeenCalledWith("Page 'Petstore reference' created");
        });

        it('creates a private page when authentication is required to view it', async () => {
            const { user, onCreated } = renderDialog();

            await user.type(titleInput(), 'Internal notes');
            await user.click(screen.getByRole('switch', { name: 'Authentication is required to view this page' }));
            await user.click(createButton());

            await waitFor(() => expect(onCreated).toHaveBeenCalled());
            expect(mockCreateItem).toHaveBeenCalledWith('env-1', 'api-1', expect.objectContaining({ visibility: 'PRIVATE' }));
        });

        it('creates the page inside the folder the dialog was opened for', async () => {
            const { user, onCreated } = renderDialog({ parent: GUIDES });

            expect(screen.getByText('The page is created unpublished, inside Guides.')).not.toBeNull();
            await user.type(titleInput(), 'Getting started');
            await user.click(createButton());

            await waitFor(() => expect(onCreated).toHaveBeenCalled());
            expect(mockCreateItem).toHaveBeenCalledWith(
                'env-1',
                'api-1',
                expect.objectContaining({ parentId: 'guides', visibility: 'PUBLIC' }),
            );
        });

        it('requires authentication inside a folder that does, as the server refuses a public page there', async () => {
            const { user, onCreated } = renderDialog({ parent: { ...GUIDES, visibility: 'PRIVATE' } });

            const authenticationRequired = screen.getByRole('switch', { name: 'Authentication is required to view this page' });
            expect(authenticationRequired.getAttribute('aria-checked')).toBe('true');
            expect((authenticationRequired as HTMLButtonElement).disabled).toBe(true);
            expect(screen.getByText('Guides requires authentication, so this page does too.')).not.toBeNull();

            await user.type(titleInput(), 'Getting started');
            await user.click(createButton());

            await waitFor(() => expect(onCreated).toHaveBeenCalled());
            expect(mockCreateItem).toHaveBeenCalledWith('env-1', 'api-1', expect.objectContaining({ visibility: 'PRIVATE' }));
        });

        it('ignores a file chosen before switching back to filling in content', async () => {
            const { user, onCreated } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await user.click(screen.getByRole('radio', { name: 'Fill in content' }));
            await user.click(createButton());

            await waitFor(() => expect(onCreated).toHaveBeenCalled());
            expect(mockCreateItem).toHaveBeenCalledWith('env-1', 'api-1', expect.objectContaining({ contentType: 'GRAVITEE_MARKDOWN' }));
            expect(mockSaveContent).not.toHaveBeenCalled();
        });
    });

    describe('importing a file', () => {
        it('keeps Create disabled until a file is chosen', async () => {
            const { user } = renderDialog();

            await user.type(titleInput(), 'Petstore');
            await chooseImport(user);

            expect(createButton().disabled).toBe(true);
        });

        it('names the page after the file and says what it will be imported as', async () => {
            const { user } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));

            expect(titleInput().value).toBe('petstore');
            expect(screen.getByText('petstore.yaml will be imported as OpenAPI.')).not.toBeNull();
            expect(createButton().disabled).toBe(false);
        });

        it('imports a Markdown file as Gravitee Markdown', async () => {
            const { user } = renderDialog();

            await importFile(user, new File(['# Hello'], 'getting-started.md'));

            expect(screen.getByText('getting-started.md will be imported as Gravitee Markdown.')).not.toBeNull();
        });

        it('creates the page with the detected type, then saves the file as its content', async () => {
            const { user, onCreated } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await user.click(createButton());

            await waitFor(() => expect(onCreated).toHaveBeenCalledWith('nav-1'));
            expect(mockCreateItem).toHaveBeenCalledWith('env-1', 'api-1', {
                type: 'PAGE',
                title: 'petstore',
                contentType: 'OPENAPI',
                area: 'TOP_NAVBAR',
                visibility: 'PUBLIC',
            });
            expect(mockSaveContent).toHaveBeenCalledWith('env-1', 'api-1', 'nav-1', { content: OPENAPI_YAML });
            expect(notify.success).toHaveBeenCalledWith("Page 'petstore' created");
        });

        it('lets the chosen file be removed', async () => {
            const { user } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await user.click(screen.getByRole('button', { name: 'Remove petstore.yaml' }));

            expect(screen.queryByText(/will be imported as/)).toBeNull();
            expect(createButton().disabled).toBe(true);
            expect(mockCreateItem).not.toHaveBeenCalled();
        });

        it('keeps the chosen file when switching to filling in content and back', async () => {
            const { user } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await user.click(screen.getByRole('radio', { name: 'Fill in content' }));
            await chooseImport(user);

            expect(screen.getByText('petstore.yaml will be imported as OpenAPI.')).not.toBeNull();
            expect(createButton().disabled).toBe(false);
        });

        it('keeps a title that was already typed', async () => {
            const { user } = renderDialog();

            await user.type(titleInput(), 'Pet store reference');
            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));

            expect(titleInput().value).toBe('Pet store reference');
        });

        it('names the page after a newly chosen file, when its title came from the previous one', async () => {
            const { user } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await uploadFile(user, new File(['asyncapi: 3.0.0\n'], 'events.yaml'));

            expect(titleInput().value).toBe('events');
        });

        it('keeps a title edited after a file named the page, when another file is chosen', async () => {
            const { user } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await user.clear(titleInput());
            await user.type(titleInput(), 'Pet store reference');
            await uploadFile(user, new File(['asyncapi: 3.0.0\n'], 'events.yaml'));

            expect(titleInput().value).toBe('Pet store reference');
        });

        it('creates an empty file as an empty page, without saving content', async () => {
            const { user, onCreated } = renderDialog();

            await importFile(user, new File([''], 'notes.md'));
            await user.click(createButton());

            await waitFor(() => expect(onCreated).toHaveBeenCalled());
            expect(mockSaveContent).not.toHaveBeenCalled();
        });
    });

    describe('refusing a file', () => {
        it.each([
            [
                'of another type',
                new File(['hello'], 'notes.txt', { type: 'text/plain' }),
                'Only .md, .yaml, .yml and .json files can be imported.',
            ],
            [
                'whose API type cannot be told',
                new File(['info:\n  title: Petstore\n'], 'petstore.yaml'),
                "Cannot tell whether 'petstore.yaml' is OpenAPI or AsyncAPI: it needs a root openapi, swagger or asyncapi property.",
            ],
        ])('refuses a file %s', async (_case, file, message) => {
            const { user } = renderDialog();

            await importFile(user, file);

            expect(await screen.findByText(message)).not.toBeNull();
            expect(createButton().disabled).toBe(true);
        });

        it('refuses a file larger than 10 MB', async () => {
            const { user } = renderDialog();
            const file = new File(['# Big'], 'big.md');
            Object.defineProperty(file, 'size', { value: 11 * 1024 * 1024 });

            await importFile(user, file);

            expect(screen.getByText("'big.md' is larger than 10 MB.")).not.toBeNull();
        });

        it('says so when the file cannot be read', async () => {
            const { user } = renderDialog();
            const file = new File([OPENAPI_YAML], 'petstore.yaml');
            Object.defineProperty(file, 'text', { value: () => Promise.reject(new Error('NotReadableError')) });

            await importFile(user, file);

            expect(await screen.findByText("'petstore.yaml' could not be read.")).not.toBeNull();
            expect(createButton().disabled).toBe(true);
        });

        it('forgets a previously accepted file once a later one is refused', async () => {
            const { user } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await uploadFile(user, new File(['info: {}\n'], 'other.yaml'));

            expect(screen.queryByText(/will be imported as/)).toBeNull();
            expect(createButton().disabled).toBe(true);
        });
    });

    describe('when the server refuses', () => {
        it('keeps the dialog open with what was entered when the page cannot be created', async () => {
            const serverError = new ApimApiError(400, 'A sibling already uses this title');
            mockCreateItem.mockRejectedValue(serverError);
            const { user, onClose, onCreated } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await user.click(createButton());

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(serverError, 'Failed to create the page'));
            expect(onClose).not.toHaveBeenCalled();
            expect(onCreated).not.toHaveBeenCalled();
            expect(titleInput().value).toBe('petstore');
            expect(createButton().disabled).toBe(false);
            expect(mockSaveContent).not.toHaveBeenCalled();
        });

        it('still hands over the page, saying its content is missing, when only the content cannot be saved', async () => {
            mockSaveContent.mockRejectedValue(new ApimApiError(400, 'Invalid OpenAPI document'));
            const { user, onCreated } = renderDialog();

            await importFile(user, new File([OPENAPI_YAML], 'petstore.yaml'));
            await user.click(createButton());

            await waitFor(() => expect(onCreated).toHaveBeenCalledWith('nav-1'));
            expect(notify.warning).toHaveBeenCalledWith(
                "Page 'petstore' was created, but its content could not be saved: Invalid OpenAPI document",
            );
            expect(notify.success).not.toHaveBeenCalled();
        });

        it('creates the page only once when Create is clicked twice', async () => {
            let resolveCreate: (page: PortalNavigationPage) => void = () => undefined;
            mockCreateItem.mockReturnValue(new Promise(resolve => (resolveCreate = resolve)));
            const { user, onCreated } = renderDialog();

            await user.type(titleInput(), 'Getting started');
            await user.click(createButton());
            const pendingButton = screen.getByRole('button', { name: 'Creating…' }) as HTMLButtonElement;
            expect(pendingButton.disabled).toBe(true);
            await user.click(pendingButton);
            resolveCreate(CREATED_PAGE);

            await waitFor(() => expect(onCreated).toHaveBeenCalled());
            expect(mockCreateItem).toHaveBeenCalledTimes(1);
        });
    });

    it('closes without creating anything on Cancel', async () => {
        const { user, onClose } = renderDialog();

        await user.type(titleInput(), 'Getting started');
        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onClose).toHaveBeenCalled();
        expect(mockCreateItem).not.toHaveBeenCalled();
    });
});
