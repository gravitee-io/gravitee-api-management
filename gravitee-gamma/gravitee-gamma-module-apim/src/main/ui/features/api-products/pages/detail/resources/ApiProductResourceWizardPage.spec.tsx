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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { FieldValues, Resolver } from 'react-hook-form';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';

import { ApiProductResourceWizardPage } from './ApiProductResourceWizardPage';
import { useResourcePlugins, useResourceSchema } from '../../../../apis/hooks/useApiResources';
import type { ApiResource } from '../../../../apis/types/resource';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';
import { useUpdateApiProductResources } from '../../../hooks/useUpdateApiProductResources';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: () => ({ id: 'DEFAULT' }),
}));

jest.mock('@gravitee/graphene-core', () => {
    const actual = jest.requireActual<object>('@gravitee/graphene-core');
    return {
        ...actual,
        JsonSchemaForm: () => null,
        extractDefaults: () => ({}),
        jsonSchemaResolver: () => {
            const resolver: Resolver<FieldValues> = async values => ({ values, errors: {} });
            return resolver;
        },
    };
});

jest.mock('../../../context/ApiProductDetailContext');
jest.mock('../../../hooks/useApiProductPermissions');
jest.mock('../../../../apis/hooks/useApiResources');
jest.mock('../../../hooks/useUpdateApiProductResources');

const mockContext = useApiProductDetailContext as jest.Mock;
const mockPermissions = useApiProductResourcePermissions as jest.Mock;
const mockMutate = jest.fn();

const RESOURCES: ApiResource[] = [
    { name: 'MyCache', type: 'cache', enabled: true, configuration: {} },
    { name: 'OAuth', type: 'oauth2', enabled: false, configuration: {} },
];

function renderWizard(initialEntry: string) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route path="/api-products/:productId/resources" element={<Outlet />}>
                        <Route index element={<div data-testid="api_product_resources_list" />} />
                        <Route path="new" element={<ApiProductResourceWizardPage />} />
                        <Route path=":resourceName/edit" element={<ApiProductResourceWizardPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiProductResourceWizardPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (useUpdateApiProductResources as jest.Mock).mockReturnValue({ isPending: false, mutate: mockMutate });
        mockPermissions.mockReturnValue({ canRead: true, canUpdate: true, isLoading: false });
        mockContext.mockReturnValue({ product: { resources: RESOURCES }, isLoading: false });
        (useResourcePlugins as jest.Mock).mockReturnValue({
            data: [
                { id: 'cache', name: 'Cache' },
                { id: 'oauth2', name: 'OAuth2' },
                { id: 'gravitee-resource-cache-cloud-redis', name: 'Cloud Redis' },
            ],
            isLoading: false,
        });
        (useResourceSchema as jest.Mock).mockReturnValue({ data: { type: 'object' }, isLoading: false, isError: false });
    });

    it('resolves edit routes with resource names containing % without double-decoding', () => {
        mockContext.mockReturnValue({
            product: {
                resources: [{ name: '100%_cache', type: 'cache', enabled: true, configuration: {} }],
            },
            isLoading: false,
        });

        renderWizard('/api-products/product-1/resources/100%25_cache/edit');

        expect(screen.getByText('Edit Resource')).toBeInTheDocument();
        expect(screen.queryByText('Resource not found.')).not.toBeInTheDocument();
    });

    it('shows an error when the resource type plugin is not available', () => {
        mockContext.mockReturnValue({
            product: {
                resources: [{ name: 'legacy', type: 'unknown-plugin', enabled: true, configuration: {} }],
            },
            isLoading: false,
        });
        (useResourcePlugins as jest.Mock).mockReturnValue({ data: [{ id: 'cache', name: 'Cache' }], isLoading: false });

        renderWizard('/api-products/product-1/resources/legacy/edit');

        expect(screen.getByText(/resource type unknown-plugin is not available/i)).toBeInTheDocument();
    });

    it('shows only allow-listed plugins when creating a resource', () => {
        renderWizard('/api-products/product-1/resources/new');

        expect(screen.getByText('Cache')).toBeInTheDocument();
        expect(screen.getByText('OAuth2')).toBeInTheDocument();
        expect(screen.queryByText('Cloud Redis')).not.toBeInTheDocument();
    });

    it('redirects to the list when the user cannot update', () => {
        mockPermissions.mockReturnValue({ canRead: true, canUpdate: false, isLoading: false });

        renderWizard('/api-products/product-1/resources/new');

        expect(screen.getByTestId('api_product_resources_list')).toBeInTheDocument();
        expect(screen.queryByText('Add Resource')).not.toBeInTheDocument();
    });

    it('submits a new resource through mutate', async () => {
        const user = userEvent.setup();
        renderWizard('/api-products/product-1/resources/new');

        await user.click(screen.getByRole('button', { name: 'Cache' }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        const nameInput = screen.getByLabelText('Resource name');
        await user.clear(nameInput);
        await user.type(nameInput, 'SecondCache');
        await user.click(screen.getByRole('button', { name: 'Create resource' }));

        expect(mockMutate).toHaveBeenCalledWith(
            expect.any(Function),
            expect.objectContaining({ onSuccess: expect.any(Function), onError: expect.any(Function) }),
        );
        const apply = mockMutate.mock.calls[0]![0] as (current: ApiResource[]) => ApiResource[];
        expect(apply(RESOURCES)).toEqual([
            ...RESOURCES,
            { name: 'SecondCache', type: 'cache', enabled: true, configuration: {} },
        ]);
    });

    it('submits an edit by replacing the original resource name', async () => {
        const user = userEvent.setup();
        renderWizard('/api-products/product-1/resources/MyCache/edit');

        const nameInput = screen.getByLabelText('Resource name');
        await user.clear(nameInput);
        await user.type(nameInput, 'RenamedCache');
        await user.click(screen.getByRole('button', { name: 'Save changes' }));

        expect(mockMutate).toHaveBeenCalled();
        const apply = mockMutate.mock.calls[0]![0] as (current: ApiResource[]) => ApiResource[];
        expect(apply(RESOURCES)).toEqual([
            { name: 'RenamedCache', type: 'cache', enabled: true, configuration: {} },
            RESOURCES[1]!,
        ]);
    });

    it('rejects duplicate names before submit', async () => {
        const user = userEvent.setup();
        renderWizard('/api-products/product-1/resources/new');

        await user.click(screen.getByRole('button', { name: 'Cache' }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        const nameInput = screen.getByLabelText('Resource name');
        await user.clear(nameInput);
        await user.type(nameInput, 'OAuth');
        await user.tab();

        await waitFor(() => expect(screen.getByText('A resource with this name already exists.')).toBeInTheDocument());
        expect(mockMutate).not.toHaveBeenCalled();
    });
});
