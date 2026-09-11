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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';

import { ApiResponseTemplatesPage } from './ApiResponseTemplatesPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiDetail } from '../../../hooks/useApiDetail';
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
    useMutation: jest.fn(() => ({ mutateAsync: jest.fn(), isPending: false })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

jest.mock('../../../services/apis', () => ({
    updateApiResponseTemplates: jest.fn(() => Promise.resolve()),
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;

function renderPage(apiOverrides: Record<string, unknown> = {}) {
    mockUseApiDetail.mockReturnValue({
        data: {
            id: 'api-1',
            name: 'Petstore',
            listeners: [{ type: 'HTTP' }],
            responseTemplates: {
                DEFAULT: { 'application/json': { statusCode: 400 } },
                API_KEY_MISSING: { 'application/json': { statusCode: 401 } },
            },
            ...apiOverrides,
        },
        isLoading: false,
        isError: false,
    });

    const router = createMemoryRouter(
        [
            {
                path: '/apis/:apiId/response-templates',
                element: <ApiResponseTemplatesPage />,
            },
            {
                path: '/apis/:apiId/response-templates/:templateKey/:contentType',
                element: <div>Edit route</div>,
            },
        ],
        { initialEntries: ['/apis/api-1/response-templates'] },
    );

    render(<RouterProvider router={router} />);
    return router;
}

describe('ApiResponseTemplatesPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseApiDetailContext.mockReturnValue({ permissionsReady: true });
    });

    it('lists templates with key, content-type, and status', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: /response templates/i })).toBeInTheDocument();
        expect(screen.getByText('DEFAULT')).toBeInTheDocument();
        expect(screen.getByText('API_KEY_MISSING')).toBeInTheDocument();
        expect(screen.getByText('400')).toBeInTheDocument();
        expect(screen.getByText('401')).toBeInTheDocument();
    });

    it('navigates with Gamma encodeURIComponent route pattern', async () => {
        const user = userEvent.setup();
        const router = renderPage();
        await user.click(screen.getByText('DEFAULT'));
        expect(router.state.location.pathname).toBe(
            `/apis/api-1/response-templates/${toResponseTemplatePath('DEFAULT', 'application/json')}`,
        );
    });

    it('shows an educational empty state with create CTA when none exist', () => {
        renderPage({ responseTemplates: {} });
        expect(screen.getByText(/no response templates/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /add new response template/i })).toBeInTheDocument();
    });

    it('shows a Kubernetes read-only banner', () => {
        renderPage({ definitionContext: { origin: 'KUBERNETES' } });
        expect(screen.getByText(/managed by the kubernetes operator/i)).toBeInTheDocument();
    });

    it('shows an unavailable message for TCP Proxy APIs', () => {
        renderPage({ listeners: [{ type: 'TCP' }], responseTemplates: {} });
        expect(screen.getByText(/not available for tcp proxy apis/i)).toBeInTheDocument();
    });

    it('shows an unavailable message for MCP_PROXY APIs', () => {
        renderPage({ type: 'MCP_PROXY', responseTemplates: {} });
        expect(screen.getByText(/not available for mcp and llm proxy apis/i)).toBeInTheDocument();
    });

    it('shows an unavailable message for LLM_PROXY APIs', () => {
        renderPage({ type: 'LLM_PROXY', responseTemplates: {} });
        expect(screen.getByText(/not available for mcp and llm proxy apis/i)).toBeInTheDocument();
    });

    it('hides the create CTA when the user lacks create permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => {
            if (anyOf.includes('api-response_templates-c')) return false;
            return true;
        });
        renderPage({ responseTemplates: {} });
        expect(screen.queryByRole('button', { name: /add new response template/i })).not.toBeInTheDocument();
    });

    it('hides the create CTA when the user lacks update permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => {
            if (anyOf.includes('api-response_templates-u')) return false;
            return true;
        });
        renderPage({ responseTemplates: {} });
        expect(screen.queryByRole('button', { name: /add new response template/i })).not.toBeInTheDocument();
    });
});
