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

import { OpenApiViewerSettingsDialog } from './OpenApiViewerSettingsDialog';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { saveApiDocumentationPageConfiguration } from '../../../services/apiDocumentation';
import type { PortalPageOpenApiConfiguration, PortalPageSwaggerConfiguration } from '../../../types/apiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../../../services/apiDocumentation');
jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
}));

// jsdom has none of the pointer-capture and scrolling APIs Radix's select calls when it opens.
Element.prototype.hasPointerCapture ??= () => false;
Element.prototype.setPointerCapture ??= () => {};
Element.prototype.releasePointerCapture ??= () => {};
Element.prototype.scrollIntoView ??= () => {};

const mockSave = jest.mocked(saveApiDocumentationPageConfiguration);

const SWAGGER_ONLY_SWITCHES = [
    'Use API entrypoints as server URLs',
    'Use API context-path as server URL path',
    'Enable "Try It!" mode',
    'Disable response body styling for large JSON payloads',
    'Enable "Try It!" mode for anonymous users',
    'Show URL to download content',
    'Display the operationId in the operation list',
    'Use PKCE with OAuth',
    'Add top bar to filter content',
    'Display vendor extensions',
    'Display extension fields for Parameters',
];

const SWAGGER: PortalPageSwaggerConfiguration = {
    viewer: 'SWAGGER',
    tryItURL: 'https://api.example.com',
    tryIt: true,
    tryItAnonymous: false,
    disableSyntaxHighlight: false,
    showURL: false,
    entrypointsAsServers: false,
    contextPathAsServerPath: false,
    displayOperationId: true,
    usePkce: false,
    docExpansion: 'list',
    enableFiltering: false,
    showExtensions: false,
    showCommonExtensions: false,
    maxDisplayedTags: 10,
};

function renderDialog(configuration: PortalPageOpenApiConfiguration = { viewer: 'REDOC', tryItURL: '' }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const onClose = jest.fn();
    render(<OpenApiViewerSettingsDialog open apiId="api-1" pageId="nav-1" configuration={configuration} onClose={onClose} />, {
        wrapper,
    });
    return { onClose, user: userEvent.setup() };
}

function viewerPicker() {
    return screen.getByRole('combobox', { name: 'OpenAPI Documentation Viewer' });
}

function baseUrlInput() {
    return screen.getByRole('textbox', { name: 'Base URL' }) as HTMLInputElement;
}

function toggle(name: string) {
    return screen.getByRole('switch', { name }) as HTMLButtonElement;
}

function saveButton() {
    return screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement;
}

async function chooseViewer(user: ReturnType<typeof userEvent.setup>, name: 'SwaggerUI' | 'Redoc') {
    await user.click(viewerPicker());
    await user.click(await screen.findByRole('option', { name }));
}

describe('OpenApiViewerSettingsDialog', () => {
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
        mockSave.mockImplementation(async (_envId, _apiId, _navId, configuration) => ({
            id: 'content-1',
            type: 'OPENAPI',
            content: 'openapi: 3.0.3',
            configuration,
        }));
    });

    it('opens on the saved Redoc configuration, with only the base URL and the viewer', () => {
        renderDialog({ viewer: 'REDOC', tryItURL: 'https://redoc.example.com' });

        expect(viewerPicker().textContent).toBe('Redoc');
        expect(baseUrlInput().value).toBe('https://redoc.example.com');
        for (const name of SWAGGER_ONLY_SWITCHES) {
            expect(screen.queryByRole('switch', { name })).toBeNull();
        }
        expect(screen.queryByRole('combobox', { name: 'Expand content on the page' })).toBeNull();
        expect(screen.queryByRole('spinbutton', { name: 'Max number of tagged operations displayed' })).toBeNull();
    });

    it('shows the SwaggerUI options for SwaggerUI only', async () => {
        const { user } = renderDialog();

        await chooseViewer(user, 'SwaggerUI');
        for (const name of SWAGGER_ONLY_SWITCHES) {
            expect(toggle(name)).not.toBeNull();
        }
        expect(screen.getByRole('combobox', { name: 'Expand content on the page' })).not.toBeNull();
        expect(screen.getByRole('spinbutton', { name: 'Max number of tagged operations displayed' })).not.toBeNull();

        await chooseViewer(user, 'Redoc');
        expect(screen.queryByRole('switch', { name: 'Enable "Try It!" mode' })).toBeNull();
    });

    it('opens on the saved SwaggerUI options', () => {
        renderDialog(SWAGGER);

        expect(viewerPicker().textContent).toBe('SwaggerUI');
        expect(toggle('Enable "Try It!" mode').getAttribute('aria-checked')).toBe('true');
        expect(toggle('Display the operationId in the operation list').getAttribute('aria-checked')).toBe('true');
        expect(toggle('Use PKCE with OAuth').getAttribute('aria-checked')).toBe('false');
        expect(screen.getByRole('combobox', { name: 'Expand content on the page' }).textContent).toBe('Only tags');
        expect((screen.getByRole('spinbutton', { name: 'Max number of tagged operations displayed' }) as HTMLInputElement).value).toBe(
            '10',
        );
    });

    it("starts SwaggerUI from the portal editor's defaults when the page has none", async () => {
        const { user } = renderDialog({ viewer: 'REDOC', tryItURL: '' });

        await chooseViewer(user, 'SwaggerUI');

        for (const name of SWAGGER_ONLY_SWITCHES) {
            expect(toggle(name).getAttribute('aria-checked')).toBe('false');
        }
        expect(screen.getByRole('combobox', { name: 'Expand content on the page' }).textContent).toBe('Default');
        expect((screen.getByRole('spinbutton', { name: 'Max number of tagged operations displayed' }) as HTMLInputElement).value).toBe('');
    });

    it('refuses a base URL that does not start with http:// or https://, and accepts an empty one', async () => {
        const { user } = renderDialog();

        await user.clear(baseUrlInput());
        await user.type(baseUrlInput(), 'api.example.com');
        expect(screen.getByText('Must be a valid URL starting with http:// or https://')).not.toBeNull();
        expect(saveButton().disabled).toBe(true);

        await user.clear(baseUrlInput());
        expect(screen.queryByText('Must be a valid URL starting with http:// or https://')).toBeNull();
        expect(saveButton().disabled).toBe(false);
    });

    it('refuses a base URL holding a space', async () => {
        const { user } = renderDialog();

        await user.type(baseUrlInput(), 'https://api.example.com/my docs');

        expect(screen.getByText('Must be a valid URL starting with http:// or https://')).not.toBeNull();
        expect(saveButton().disabled).toBe(true);
    });

    it('saves nothing when Enter is pressed on an invalid base URL', async () => {
        const { user, onClose } = renderDialog();

        await user.type(baseUrlInput(), 'api.example.com{Enter}');

        expect(mockSave).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
    });

    it('clears and disables the base URL while the API entrypoints are used as server URLs', async () => {
        const { user } = renderDialog(SWAGGER);

        await user.click(toggle('Use API entrypoints as server URLs'));
        expect(baseUrlInput().value).toBe('');
        expect(baseUrlInput().disabled).toBe(true);

        await user.click(toggle('Use API entrypoints as server URLs'));
        expect(baseUrlInput().disabled).toBe(false);
    });

    it('saves every SwaggerUI option, then closes', async () => {
        const { user, onClose } = renderDialog(SWAGGER);

        await user.click(toggle('Use PKCE with OAuth'));
        await user.click(screen.getByRole('combobox', { name: 'Expand content on the page' }));
        await user.click(await screen.findByRole('option', { name: 'Tags and operations' }));
        await user.click(saveButton());

        await waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(mockSave).toHaveBeenCalledWith('env-1', 'api-1', 'nav-1', { ...SWAGGER, usePkce: true, docExpansion: 'full' });
        expect(notify.success).toHaveBeenCalledWith('OpenAPI viewer configuration saved');
    });

    it('leaves the tag limit to the server when it is empty', async () => {
        const { user, onClose } = renderDialog(SWAGGER);

        await user.clear(screen.getByRole('spinbutton', { name: 'Max number of tagged operations displayed' }));
        await user.click(saveButton());

        await waitFor(() => expect(onClose).toHaveBeenCalled());
        const { maxDisplayedTags: _left, ...withoutLimit } = SWAGGER;
        expect(mockSave).toHaveBeenCalledWith('env-1', 'api-1', 'nav-1', withoutLimit);
    });

    it('saves only the base URL for Redoc', async () => {
        const { user, onClose } = renderDialog(SWAGGER);

        await chooseViewer(user, 'Redoc');
        await user.click(saveButton());

        await waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(mockSave).toHaveBeenCalledWith('env-1', 'api-1', 'nav-1', { viewer: 'REDOC', tryItURL: 'https://api.example.com' });
    });

    it('shows why the server refused the configuration, and stays open', async () => {
        mockSave.mockRejectedValue(new ApimApiError(404, 'Page content not found'));
        const { user, onClose } = renderDialog();

        await user.click(saveButton());

        expect(await screen.findByText('Page content not found')).not.toBeNull();
        expect(onClose).not.toHaveBeenCalled();
        expect(saveButton().disabled).toBe(false);
    });

    it('brings the refusal into view, as the SwaggerUI options scroll it out of sight', async () => {
        const scrollIntoView = jest.spyOn(Element.prototype, 'scrollIntoView').mockImplementation(() => {});
        mockSave.mockRejectedValue(new ApimApiError(400, 'Invalid configuration'));
        const { user } = renderDialog(SWAGGER);

        await user.click(saveButton());

        const refusal = (await screen.findByText('Invalid configuration')).closest('[role="alert"]');
        expect(scrollIntoView.mock.contexts).toContain(refusal);
    });

    it('brings the same refusal into view again when it is given again', async () => {
        const scrollIntoView = jest.spyOn(Element.prototype, 'scrollIntoView').mockImplementation(() => {});
        mockSave.mockRejectedValue(new ApimApiError(400, 'Invalid configuration'));
        const { user } = renderDialog(SWAGGER);

        await user.click(saveButton());
        await screen.findByText('Invalid configuration');
        await user.click(saveButton());

        await waitFor(() => expect(scrollIntoView).toHaveBeenCalledTimes(2));
    });

    it('saves nothing when cancelled', async () => {
        const { user, onClose } = renderDialog();

        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onClose).toHaveBeenCalled();
        expect(mockSave).not.toHaveBeenCalled();
    });
});
