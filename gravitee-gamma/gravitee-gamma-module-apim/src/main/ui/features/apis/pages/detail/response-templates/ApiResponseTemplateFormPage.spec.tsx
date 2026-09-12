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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiResponseTemplateFormPage } from './ApiResponseTemplateFormPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiResponseTemplates } from '../../../services/apis';
import { toResponseTemplatePath } from '../../../utils/responseTemplates';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(() => ({ permissionsReady: true })),
}));

jest.mock('../../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('@tanstack/react-query', () => ({
    useMutation: jest.fn(config => ({
        mutate: jest.fn(async args => {
            try {
                await config.mutationFn(args);
                config.onSuccess?.();
            } catch (error) {
                config.onError?.(error);
            }
        }),
        isPending: false,
    })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

jest.mock('../../../services/apis', () => ({
    updateApiResponseTemplates: jest.fn(() => Promise.resolve()),
}));

jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUpdate = updateApiResponseTemplates as jest.Mock;

const EXISTING = {
    DEFAULT: { 'application/json': { statusCode: 400, body: '{}' } },
};

beforeAll(() => {
    global.ResizeObserver = class {
        observe() {}
        unobserve() {}
        disconnect() {}
    };
    Element.prototype.scrollIntoView = jest.fn();
});

function renderForm(path: string, apiOverrides: Record<string, unknown> = {}) {
    mockUseApiDetail.mockReturnValue({
        data: {
            id: 'api-1',
            name: 'Petstore',
            listeners: [{ type: 'HTTP' }],
            responseTemplates: EXISTING,
            ...apiOverrides,
        },
        isLoading: false,
        isError: false,
    });

    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="apis/:apiId/response-templates">
                    <Route index element={<div>List</div>} />
                    <Route path="new" element={<ApiResponseTemplateFormPage />} />
                    <Route path=":templateKey/:contentType" element={<ApiResponseTemplateFormPage />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApiResponseTemplateFormPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseApiDetailContext.mockReturnValue({ permissionsReady: true });
        mockUpdate.mockClear();
        mockUpdate.mockImplementation((_env, _id, updater) => {
            updater(EXISTING);
            return Promise.resolve();
        });
    });

    it('renders the create form with defaults', async () => {
        renderForm('/apis/api-1/response-templates/new');
        expect(await screen.findByRole('heading', { name: /create a new response template/i })).toBeInTheDocument();
        expect(screen.getByDisplayValue('*/*')).toBeInTheDocument();
        expect(screen.getByDisplayValue('400')).toBeInTheDocument();
        expect(screen.getByText('BAD_REQUEST')).toBeInTheDocument();
    });

    it('loads an existing template from two independently encoded path segments', async () => {
        renderForm(`/apis/api-1/response-templates/${toResponseTemplatePath('DEFAULT', 'application/json')}`);
        expect(await screen.findByRole('heading', { name: /edit response template/i })).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: /template key/i })).toHaveTextContent('DEFAULT');
        expect(screen.getByDisplayValue('application/json')).toBeInTheDocument();
    });

    it('loads a template whose key contains a literal % (useParams already decoded)', async () => {
        mockUseApiDetail.mockReturnValue({
            data: {
                id: 'api-1',
                name: 'Petstore',
                listeners: [{ type: 'HTTP' }],
                responseTemplates: {
                    'ERR%RETRY': { 'application/json': { statusCode: 500, body: 'retry' } },
                },
            },
            isLoading: false,
            isError: false,
        });
        render(
            <MemoryRouter initialEntries={[`/apis/api-1/response-templates/${toResponseTemplatePath('ERR%RETRY', 'application/json')}`]}>
                <Routes>
                    <Route path="apis/:apiId/response-templates">
                        <Route index element={<div>List</div>} />
                        <Route path=":templateKey/:contentType" element={<ApiResponseTemplateFormPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
        expect(await screen.findByRole('heading', { name: /edit response template/i })).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: /template key/i })).toHaveTextContent('ERR%RETRY');
        expect(screen.getByDisplayValue('application/json')).toBeInTheDocument();
    });

    it('shows a Kubernetes read-only banner and hides Create', async () => {
        renderForm('/apis/api-1/response-templates/new', { definitionContext: { origin: 'KUBERNETES' } });
        expect(await screen.findByText(/managed by the kubernetes operator/i)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /^create$/i })).not.toBeInTheDocument();
    });

    it('shows an unavailable message for TCP Proxy APIs', () => {
        renderForm('/apis/api-1/response-templates/new', { listeners: [{ type: 'TCP' }] });
        expect(screen.getByText(/not available for tcp proxy apis/i)).toBeInTheDocument();
    });

    it('shows an unavailable message for MCP_PROXY APIs', () => {
        renderForm('/apis/api-1/response-templates/new', { type: 'MCP_PROXY' });
        expect(screen.getByText(/not available for mcp and llm proxy apis/i)).toBeInTheDocument();
    });

    it('shows an unavailable message for LLM_PROXY APIs', () => {
        renderForm('/apis/api-1/response-templates/new', { type: 'LLM_PROXY' });
        expect(screen.getByText(/not available for mcp and llm proxy apis/i)).toBeInTheDocument();
    });

    it('submits a create payload via a fresh-GET updater', async () => {
        const user = userEvent.setup();
        renderForm('/apis/api-1/response-templates/new');
        await screen.findByRole('heading', { name: /create a new response template/i });

        await user.click(screen.getByRole('combobox', { name: /template key/i }));
        await user.click(await screen.findByText('API_KEY_MISSING'));

        await user.click(screen.getByRole('button', { name: /^create$/i }));

        await waitFor(() => expect(mockUpdate).toHaveBeenCalled());
        const updater = mockUpdate.mock.calls[0][2] as (current: typeof EXISTING) => unknown;
        const next = updater(EXISTING) as Record<string, Record<string, { statusCode: number }>>;
        expect(next.API_KEY_MISSING['*/*'].statusCode).toBe(400);
        expect(next.DEFAULT['application/json'].statusCode).toBe(400);
    });

    it('blocks create when the key + Accept pair already exists', async () => {
        const user = userEvent.setup();
        renderForm('/apis/api-1/response-templates/new');
        await screen.findByRole('heading', { name: /create a new response template/i });

        await user.click(screen.getByRole('combobox', { name: /template key/i }));
        await user.click(await screen.findByText('DEFAULT'));

        const accept = screen.getByDisplayValue('*/*');
        await user.clear(accept);
        await user.type(accept, 'application/json');

        await user.click(screen.getByRole('button', { name: /^create$/i }));

        expect(await screen.findByText(/already exists/i)).toBeInTheDocument();
        expect(mockUpdate).not.toHaveBeenCalled();
    });

    it('makes create form read-only when the user lacks update permission', async () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => {
            if (anyOf.includes('api-response_templates-u')) return false;
            return true;
        });
        renderForm('/apis/api-1/response-templates/new');
        expect(await screen.findByRole('heading', { name: /create a new response template/i })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /^create$/i })).not.toBeInTheDocument();
    });
});
