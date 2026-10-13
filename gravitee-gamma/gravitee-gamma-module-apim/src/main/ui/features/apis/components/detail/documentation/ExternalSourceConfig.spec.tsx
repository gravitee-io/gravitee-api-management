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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ExternalSourceConfig, type ExternalSourceState } from './ExternalSourceConfig';
import { listFetchers } from '../../../services/fetchers';
import type { Fetcher } from '../../../types/apiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../../../services/fetchers');

// Monaco does not run in jsdom: a textarea stands in for the editor.
jest.mock('@gravitee/graphene-core/code-editor', () => ({
    CodeEditor: ({ value, onChange, disabled }: { value?: string; onChange?: (next: string) => void; disabled?: boolean }) => (
        <textarea
            aria-label="Source settings as JSON"
            value={value}
            onChange={event => onChange?.(event.target.value)}
            disabled={disabled}
        />
    ),
}));

// jsdom ships no ResizeObserver, nor the pointer-capture and scrolling APIs Radix's select calls when it opens.
globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};
Element.prototype.hasPointerCapture ??= () => false;
Element.prototype.setPointerCapture ??= () => {};
Element.prototype.releasePointerCapture ??= () => {};
Element.prototype.scrollIntoView ??= () => {};

const mockListFetchers = jest.mocked(listFetchers);

const HTTP_FETCHER: Fetcher = {
    id: 'http-fetcher',
    name: 'HTTP',
    schema: { type: 'object', properties: { url: { title: 'URL', type: 'string' } }, required: ['url'] },
};
const GITHUB_FETCHER: Fetcher = {
    id: 'github-fetcher',
    name: 'GitHub',
    schema: {
        type: 'object',
        properties: {
            repository: { title: 'Repository', type: 'string' },
            personalAccessToken: { title: 'Personal access token', type: 'string' },
        },
        required: ['repository'],
    },
};

function renderConfig(props: { forImport?: boolean } = {}) {
    const onChange = jest.fn<void, [ExternalSourceState]>();
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const config = (disabled: boolean) => (
        <QueryClientProvider client={queryClient}>
            <ExternalSourceConfig {...props} onChange={onChange} disabled={disabled} />
        </QueryClientProvider>
    );
    const { rerender } = render(config(false));
    const lastState = () => onChange.mock.calls.at(-1)?.[0];
    return { onChange, lastState, user: userEvent.setup(), disable: () => rerender(config(true)) };
}

async function chooseFetcher(user: ReturnType<typeof userEvent.setup>, name: string) {
    await user.click(await screen.findByRole('combobox', { name: 'Source type' }));
    await user.click(await screen.findByRole('option', { name }));
}

describe('ExternalSourceConfig', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockListFetchers.mockResolvedValue([GITHUB_FETCHER, HTTP_FETCHER]);
    });

    it('lists the installed fetchers and shows the settings of the one chosen', async () => {
        const { user } = renderConfig();

        await user.click(await screen.findByRole('combobox', { name: 'Source type' }));
        expect(screen.getByRole('option', { name: 'GitHub' })).toBeInTheDocument();
        await user.click(screen.getByRole('option', { name: 'HTTP' }));

        expect(screen.getByRole('textbox', { name: /URL/ })).toBeInTheDocument();
        expect(mockListFetchers).toHaveBeenCalledWith('env-1', { forImport: false });
    });

    it('offers only the fetchers that can list a folder for an import, and says so', async () => {
        renderConfig({ forImport: true });

        await screen.findByRole('combobox', { name: 'Source type' });
        expect(mockListFetchers).toHaveBeenCalledWith('env-1', { forImport: true });
        expect(screen.getByText('Only sources that can list a folder are offered.')).toBeInTheDocument();
    });

    it('is invalid until a fetcher is chosen and its required settings are filled in', async () => {
        const { user, lastState } = renderConfig();

        await screen.findByRole('combobox', { name: 'Source type' });
        expect(lastState()).toEqual({ source: null, valid: false });

        await chooseFetcher(user, 'HTTP');
        await waitFor(() => expect(lastState()?.valid).toBe(false));

        await user.type(screen.getByRole('textbox', { name: /URL/ }), 'https://docs.acme.io/openapi.yaml');

        await waitFor(() =>
            expect(lastState()).toEqual({
                source: { type: 'http-fetcher', configuration: { url: 'https://docs.acme.io/openapi.yaml' }, useAutoFetch: false },
                valid: true,
            }),
        );
    });

    it('starts the settings of another fetcher afresh', async () => {
        const { user } = renderConfig();
        await chooseFetcher(user, 'GitHub');
        await user.type(screen.getByRole('textbox', { name: /Repository/ }), 'petstore');

        await chooseFetcher(user, 'HTTP');
        expect(screen.getByRole('textbox', { name: /URL/ })).toHaveValue('');

        await chooseFetcher(user, 'GitHub');
        expect(screen.getByRole('textbox', { name: /Repository/ })).toHaveValue('');
    });

    it('requires a valid schedule once automatic fetching is on', async () => {
        const { user, lastState } = renderConfig();
        await chooseFetcher(user, 'GitHub');
        await user.type(screen.getByRole('textbox', { name: /Repository/ }), 'petstore');
        await waitFor(() => expect(lastState()?.valid).toBe(true));

        await user.click(screen.getByRole('switch', { name: 'Fetch automatically' }));
        await waitFor(() => expect(lastState()?.valid).toBe(false));

        fireEvent.change(screen.getByRole('textbox', { name: 'Schedule (cron)' }), { target: { value: '0 0 */6 * * *' } });

        await waitFor(() =>
            expect(lastState()).toEqual({
                source: {
                    type: 'github-fetcher',
                    configuration: { repository: 'petstore' },
                    useAutoFetch: true,
                    fetchCron: '0 0 */6 * * *',
                },
                valid: true,
            }),
        );
    });

    it('says why a schedule is not valid, and is invalid until it is', async () => {
        const { user, lastState } = renderConfig();
        await chooseFetcher(user, 'GitHub');
        await user.type(screen.getByRole('textbox', { name: /Repository/ }), 'petstore');
        await user.click(screen.getByRole('switch', { name: 'Fetch automatically' }));

        fireEvent.change(screen.getByRole('textbox', { name: 'Schedule (cron)' }), { target: { value: '* * *' } });

        expect(await screen.findByText('Must have exactly 6 fields: sec min hr dom mon dow')).toBeInTheDocument();
        await waitFor(() => expect(lastState()?.valid).toBe(false));
    });

    it('says a schedule is needed once automatic fetching is on', async () => {
        const { user } = renderConfig();
        await chooseFetcher(user, 'GitHub');

        await user.click(screen.getByRole('switch', { name: 'Fetch automatically' }));

        expect(screen.getByText('Choose when to fetch.')).toBeInTheDocument();
    });

    it('edits the settings as JSON and brings the edit back to the form', async () => {
        const { user, lastState } = renderConfig();
        await chooseFetcher(user, 'HTTP');

        await user.click(screen.getByRole('switch', { name: 'Edit as JSON' }));
        fireEvent.change(screen.getByRole('textbox', { name: 'Source settings as JSON' }), {
            target: { value: '{ "url": "https://docs.acme.io/guide.md" }' },
        });

        await waitFor(() => expect(lastState()?.source?.configuration).toEqual({ url: 'https://docs.acme.io/guide.md' }));
        expect(lastState()?.valid).toBe(true);

        await user.click(screen.getByRole('switch', { name: 'Edit as JSON' }));
        expect(screen.getByRole('textbox', { name: /URL/ })).toHaveValue('https://docs.acme.io/guide.md');
    });

    it('is invalid while the JSON cannot be read, and says so', async () => {
        const { user, lastState } = renderConfig();
        await chooseFetcher(user, 'GitHub');

        await user.click(screen.getByRole('switch', { name: 'Edit as JSON' }));
        fireEvent.change(screen.getByRole('textbox', { name: 'Source settings as JSON' }), { target: { value: '{ "repository": ' } });

        expect(await screen.findByText('The settings are not valid JSON.')).toBeInTheDocument();
        await waitFor(() => expect(lastState()?.valid).toBe(false));
        expect(screen.getByRole('switch', { name: 'Edit as JSON' })).toBeDisabled();
    });

    it('takes JSON that is not an object as unreadable settings', async () => {
        const { user, lastState } = renderConfig();
        await chooseFetcher(user, 'GitHub');

        await user.click(screen.getByRole('switch', { name: 'Edit as JSON' }));
        fireEvent.change(screen.getByRole('textbox', { name: 'Source settings as JSON' }), { target: { value: '[]' } });

        expect(await screen.findByText('The settings are not valid JSON.')).toBeInTheDocument();
        await waitFor(() => expect(lastState()?.valid).toBe(false));
    });

    it('says why JSON that can be read is still not valid for the source type', async () => {
        const { user, lastState } = renderConfig();
        await chooseFetcher(user, 'GitHub');

        await user.click(screen.getByRole('switch', { name: 'Edit as JSON' }));
        fireEvent.change(screen.getByRole('textbox', { name: 'Source settings as JSON' }), { target: { value: '{}' } });

        expect(await screen.findByText('Some settings are missing or not valid for this source type.')).toBeInTheDocument();
        await waitFor(() => expect(lastState()?.valid).toBe(false));
    });

    it("starts from the defaults of the fetcher's schema", async () => {
        mockListFetchers.mockResolvedValue([
            {
                id: 'gitlab-fetcher',
                name: 'GitLab',
                schema: {
                    type: 'object',
                    properties: {
                        project: { title: 'Project', type: 'string' },
                        branchOrTag: { title: 'Branch or tag', type: 'string', default: 'main' },
                    },
                },
            },
        ]);
        const { user, lastState } = renderConfig();

        await chooseFetcher(user, 'GitLab');

        expect(screen.getByRole('textbox', { name: /Branch or tag/ })).toHaveValue('main');
        await waitFor(() => expect(lastState()?.source?.configuration).toEqual({ branchOrTag: 'main' }));
    });

    it('locks every setting while disabled', async () => {
        const { user, disable } = renderConfig();
        await chooseFetcher(user, 'GitHub');
        await user.click(screen.getByRole('switch', { name: 'Fetch automatically' }));

        disable();

        expect(screen.getByRole('combobox', { name: 'Source type' })).toBeDisabled();
        expect(screen.getByRole('textbox', { name: /Repository/ })).toBeDisabled();
        expect(screen.getByRole('switch', { name: 'Edit as JSON' })).toBeDisabled();
        expect(screen.getByRole('switch', { name: 'Fetch automatically' })).toBeDisabled();
        expect(screen.getByRole('textbox', { name: 'Schedule (cron)' })).toBeDisabled();
    });

    it('locks the JSON editor while disabled', async () => {
        const { user, disable } = renderConfig();
        await chooseFetcher(user, 'HTTP');
        await user.click(screen.getByRole('switch', { name: 'Edit as JSON' }));

        disable();

        expect(screen.getByRole('textbox', { name: 'Source settings as JSON' })).toBeDisabled();
    });

    it('says when the fetchers cannot be loaded, instead of offering an empty choice', async () => {
        mockListFetchers.mockRejectedValue(new Error('Network down'));
        renderConfig();

        expect(await screen.findByText(/could not be loaded/i)).toBeInTheDocument();
        expect(screen.queryByRole('combobox', { name: 'Source type' })).not.toBeInTheDocument();
    });

    it('says when no fetcher is installed', async () => {
        mockListFetchers.mockResolvedValue([]);
        renderConfig();

        expect(await screen.findByText(/no external source type is installed/i)).toBeInTheDocument();
    });
});
