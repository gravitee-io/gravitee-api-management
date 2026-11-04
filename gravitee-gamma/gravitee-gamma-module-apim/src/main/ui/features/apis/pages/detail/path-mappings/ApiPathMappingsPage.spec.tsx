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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiPathMappingsPage } from './ApiPathMappingsPage';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { importApiPathMappings, listApiSwaggerPages, updateApiPathMappings } from '../../../services/apis';

// jsdom ships no ResizeObserver; RadioGroup / Checkbox mount one.
globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('../../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(),
}));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(() => ({ permissionsReady: true })),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('@tanstack/react-query', () => {
    const actual = jest.requireActual('@tanstack/react-query');
    return {
        ...actual,
        useMutation: jest.fn(config => ({
            mutateAsync: jest.fn(async args => {
                const result = await config.mutationFn(args);
                await config.onSuccess?.(result);
                return result;
            }),
            isPending: false,
        })),
    };
});

jest.mock('../../../services/apis', () => ({
    updateApiPathMappings: jest.fn(() => Promise.resolve({ id: 'api-1', listeners: [] })),
    importApiPathMappings: jest.fn(() => Promise.resolve({ pathMappings: ['/pets/:petId'] })),
    listApiSwaggerPages: jest.fn(() => Promise.resolve([{ id: 'sw-1', name: 'Petstore' }])),
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUpdate = updateApiPathMappings as jest.Mock;
const mockImport = importApiPathMappings as jest.Mock;
const mockListSwagger = listApiSwaggerPages as jest.Mock;

function renderPage(
    pathMappings: string[] = ['/orders', '/pets/:petId'],
    origin?: 'KUBERNETES',
    listeners?: { type: string; pathMappings?: string[] }[],
) {
    mockUseApiDetail.mockReturnValue({
        data: {
            id: 'api-1',
            name: 'Test API',
            definitionContext: origin ? { origin } : { origin: 'MANAGEMENT' },
            listeners: listeners ?? [{ type: 'HTTP', pathMappings }],
        },
        isLoading: false,
        isError: false,
    });
    render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <MemoryRouter initialEntries={['/apis/api-1/path-mappings']}>
                <Routes>
                    <Route path="apis/:apiId/path-mappings" element={<ApiPathMappingsPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

function savedMappings(): string[] {
    const updater = mockUpdate.mock.calls[0][2] as (current: string[]) => string[];
    return updater(['/orders', '/pets/:petId']);
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUseApiDetailContext.mockReturnValue({ permissionsReady: true });
    mockUpdate.mockResolvedValue({ id: 'api-1', listeners: [{ type: 'HTTP', pathMappings: [] }] });
    mockImport.mockResolvedValue({ pathMappings: ['/pets/:petId'] });
    mockListSwagger.mockResolvedValue([{ id: 'sw-1', name: 'Petstore' }]);
});

it('lists stored path mappings', () => {
    renderPage();
    expect(screen.getByText('/orders')).toBeInTheDocument();
    expect(screen.getByText('/pets/:petId')).toBeInTheDocument();
});

it('hides add when the user lacks api-definition-u', () => {
    mockUseHasPermission.mockReturnValue(false);
    renderPage();
    expect(screen.queryByTestId('add-path-mapping')).toBeNull();
});

it('shows kubernetes read-only alert and hides add', () => {
    renderPage(['/orders'], 'KUBERNETES');
    expect(screen.getByText(/managed by the Kubernetes operator/i)).toBeInTheDocument();
    expect(screen.queryByTestId('add-path-mapping')).toBeNull();
});

it('adds a path mapping through the dialog', async () => {
    const user = userEvent.setup();
    renderPage([]);
    await user.click(screen.getByTestId('add-path-mapping'));
    await user.type(screen.getByTestId('path-mapping-input'), '/charges/:chargeId');
    await user.click(screen.getByTestId('path-mapping-submit'));
    expect(mockUpdate).toHaveBeenCalled();
});

it('rejects an invalid path in the add dialog', async () => {
    const user = userEvent.setup();
    renderPage([]);
    await user.click(screen.getByTestId('add-path-mapping'));
    await user.type(screen.getByTestId('path-mapping-input'), '/users/@me');
    await user.tab();
    expect(screen.getByTestId('path-mapping-error')).toBeInTheDocument();
    expect(screen.getByTestId('path-mapping-submit')).toBeDisabled();
    expect(mockUpdate).not.toHaveBeenCalled();
});

it('imports path mappings from a Swagger page', async () => {
    const user = userEvent.setup();
    renderPage([]);
    await user.click(screen.getByTestId('add-path-mapping'));
    await user.click(screen.getByRole('tab', { name: 'Swagger Document' }));
    await screen.findByText('Petstore');
    await user.click(screen.getByText('Petstore'));
    await user.click(screen.getByTestId('path-mapping-submit'));
    expect(mockImport).toHaveBeenCalledWith('DEFAULT', 'api-1', { pageId: 'sw-1', overwrite: false });
    expect(notify.success).toHaveBeenCalledWith('The path mappings have been successfully imported!');
});

it('does not load Swagger pages until that tab is opened', async () => {
    const user = userEvent.setup();
    renderPage([]);
    await user.click(screen.getByTestId('add-path-mapping'));
    expect(mockListSwagger).not.toHaveBeenCalled();
    await user.click(screen.getByRole('tab', { name: 'Swagger Document' }));
    await screen.findByText('Petstore');
    expect(mockListSwagger).toHaveBeenCalledTimes(1);
});

it('edits a path mapping and reports success', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Actions for /orders' }));
    await user.click(screen.getByRole('menuitem', { name: 'Edit' }));
    const input = screen.getByTestId('path-mapping-input');
    await user.clear(input);
    await user.type(input, '/orders/:orderId');
    await user.click(screen.getByTestId('path-mapping-submit'));
    expect(savedMappings()).toEqual(['/orders/:orderId', '/pets/:petId']);
    expect(notify.success).toHaveBeenCalledWith('The path mapping has been successfully updated!');
});

it('deletes a path mapping after confirmation', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Actions for /orders' }));
    await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
    await user.click(screen.getByRole('button', { name: 'Delete' }));
    expect(savedMappings()).toEqual(['/pets/:petId']);
    expect(notify.success).toHaveBeenCalledWith('The path mapping /orders has been successfully deleted!');
});

it('shows the TCP notice instead of the path mapping table', () => {
    renderPage([], undefined, [{ type: 'TCP' }]);
    expect(screen.getByText('Path Mappings are not available for TCP Proxy APIs')).toBeInTheDocument();
    expect(screen.queryByText('/orders')).toBeNull();
});

it('shows the server message when a stored mapping is rejected', async () => {
    const user = userEvent.setup();
    const message = 'Path mappings contain invalid templates: [/users/@me]';
    mockUpdate.mockRejectedValue(new ApimApiError(400, message, { message }));
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Actions for /orders' }));
    await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
    await user.click(screen.getByRole('button', { name: 'Delete' }));
    expect(notify.error).toHaveBeenCalledWith(expect.objectContaining({ message }), 'Failed to update path mappings');
});

it('reloads when the mapping was removed before the save', async () => {
    const user = userEvent.setup();
    mockUpdate.mockImplementation(async (_environmentId: string, _apiId: string, updater: (current: string[]) => string[]) => {
        updater(['/pets/:petId']);
    });
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Actions for /orders' }));
    await user.click(screen.getByRole('menuitem', { name: 'Edit' }));
    const input = screen.getByTestId('path-mapping-input');
    await user.clear(input);
    await user.type(input, '/orders/:orderId');
    await user.click(screen.getByTestId('path-mapping-submit'));
    expect(notify.error).toHaveBeenCalledWith(
        expect.objectContaining({ message: 'The path mapping no longer exists.' }),
        expect.any(String),
    );
});
