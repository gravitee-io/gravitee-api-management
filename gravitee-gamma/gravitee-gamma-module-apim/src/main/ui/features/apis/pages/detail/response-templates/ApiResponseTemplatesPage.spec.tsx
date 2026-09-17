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
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';

import { ApiResponseTemplatesPage } from './ApiResponseTemplatesPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiResponseTemplates } from '../../../services/apis';
import type { ApiDetailDto } from '../../../types';
import { mergeApiDetailCache } from '../../../utils/apiDetailCache';
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

const mockSetQueryData = jest.fn();
const mockInvalidateQueries = jest.fn();

jest.mock('@tanstack/react-query', () => ({
    useMutation: jest.fn(),
    useQueryClient: jest.fn(),
}));

jest.mock('../../../services/apis', () => ({
    updateApiResponseTemplates: jest.fn(),
}));

jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseMutation = useMutation as jest.Mock;
const mockUseQueryClient = useQueryClient as jest.Mock;
const mockUpdate = updateApiResponseTemplates as jest.Mock;

const TWO_TEMPLATES = {
    DEFAULT: { 'application/json': { statusCode: 400 } },
    API_KEY_MISSING: { 'application/json': { statusCode: 401 } },
};

const AFTER_DELETE: ApiDetailDto = {
    id: 'api-1',
    responseTemplates: {
        API_KEY_MISSING: { 'application/json': { statusCode: 401 } },
    },
} as ApiDetailDto;

function renderPage(apiOverrides: Record<string, unknown> = {}) {
    mockUseApiDetail.mockReturnValue({
        data: {
            id: 'api-1',
            name: 'Petstore',
            listeners: [{ type: 'HTTP' }],
            responseTemplates: TWO_TEMPLATES,
            deploymentState: 'NEED_REDEPLOY',
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

    const view = render(<RouterProvider router={router} />);
    return { router, ...view };
}

describe('ApiResponseTemplatesPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseApiDetailContext.mockReturnValue({ permissionsReady: true });
        mockSetQueryData.mockReset();
        mockInvalidateQueries.mockReset();
        mockUpdate.mockReset();
        mockUseQueryClient.mockReturnValue({
            setQueryData: mockSetQueryData,
            invalidateQueries: mockInvalidateQueries,
        });
        mockUseMutation.mockImplementation(config => ({
            mutateAsync: jest.fn(async args => {
                const result = await config.mutationFn(args);
                config.onSuccess?.(result);
                return result;
            }),
            isPending: false,
        }));
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
        const { router } = renderPage();
        await user.click(screen.getByText('DEFAULT'));
        expect(router.state.location.pathname).toBe(
            `/apis/api-1/response-templates/${toResponseTemplatePath('DEFAULT', 'application/json')}`,
        );
    });

    it('merges PATCH response into API detail cache after delete and drops the row', async () => {
        const user = userEvent.setup();
        mockUpdate.mockResolvedValue(AFTER_DELETE);

        const { unmount } = renderPage();
        expect(screen.getByText('DEFAULT')).toBeInTheDocument();
        expect(screen.getByText('API_KEY_MISSING')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: /actions for default application\/json/i }));
        await user.click(await screen.findByRole('menuitem', { name: /^delete$/i }));

        const dialog = await screen.findByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: /^delete$/i }));

        expect(mockSetQueryData).toHaveBeenCalled();
        const updater = mockSetQueryData.mock.calls[0][1] as (prev: ApiDetailDto | undefined) => ApiDetailDto;
        const prev = {
            id: 'api-1',
            responseTemplates: TWO_TEMPLATES,
            deploymentState: 'NEED_REDEPLOY',
        } as ApiDetailDto;
        const merged = mergeApiDetailCache(prev, AFTER_DELETE);
        expect(updater(prev)).toEqual(merged);
        expect(updater(prev).responseTemplates).toEqual(AFTER_DELETE.responseTemplates);
        expect(updater(prev).deploymentState).toBe('NEED_REDEPLOY');

        unmount();
        renderPage({
            responseTemplates: merged.responseTemplates,
            deploymentState: merged.deploymentState,
        });

        expect(screen.queryByText('DEFAULT')).not.toBeInTheDocument();
        expect(screen.getByText('API_KEY_MISSING')).toBeInTheDocument();
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
