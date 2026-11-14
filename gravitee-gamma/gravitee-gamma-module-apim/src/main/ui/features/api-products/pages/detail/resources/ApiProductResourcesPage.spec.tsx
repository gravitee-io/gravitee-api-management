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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiProductResourcesPage } from './ApiProductResourcesPage';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { useResourcePlugins } from '../../../../apis/hooks/useApiResources';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';
import { useUpdateApiProductResources } from '../../../hooks/useUpdateApiProductResources';

jest.mock('../../../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));
jest.mock('../../../context/ApiProductDetailContext');
jest.mock('../../../hooks/useApiProductPermissions');
jest.mock('../../../../apis/hooks/useApiResources');
jest.mock('../../../hooks/useUpdateApiProductResources');

const mockContext = useApiProductDetailContext as jest.Mock;
const mockPermissions = useApiProductResourcePermissions as jest.Mock;
const mockMutate = jest.fn();

const RESOURCES = [
    { name: 'MyCache', type: 'cache', enabled: true, configuration: {} },
    { name: 'OAuth', type: 'oauth2', enabled: false, configuration: {} },
];

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/api-products/product-1/resources']}>
                <Routes>
                    <Route path="/api-products/:productId/resources" element={<ApiProductResourcesPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiProductResourcesPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (useResourcePlugins as jest.Mock).mockReturnValue({
            data: [
                { id: 'cache', name: 'Cache' },
                { id: 'oauth2', name: 'OAuth2' },
            ],
        });
        (useUpdateApiProductResources as jest.Mock).mockReturnValue({ isPending: false, mutate: mockMutate });
        mockPermissions.mockReturnValue({ canRead: true, canUpdate: true, isLoading: false });
        mockContext.mockReturnValue({ product: { resources: RESOURCES }, isLoading: false });
    });

    it('lists resources and their enabled state', () => {
        renderPage();
        expect(screen.getAllByTestId('api_product_resources_name')[0]).toHaveTextContent('MyCache');
        expect(screen.getByText('Enabled')).toBeInTheDocument();
        expect(screen.getByText('Disabled')).toBeInTheDocument();
    });

    it('shows an empty state when the product has no resources', () => {
        mockContext.mockReturnValue({ product: { resources: [] }, isLoading: false });
        renderPage();
        expect(screen.getByTestId('api_product_resources_empty')).toHaveTextContent('No resources');
    });

    it('denies access when the user cannot read the product definition', () => {
        mockPermissions.mockReturnValue({ canRead: false, canUpdate: false, isLoading: false });
        mockContext.mockReturnValue({ product: { resources: [] }, isLoading: false });
        renderPage();
        expect(screen.getByText(/do not have permission/i)).toBeInTheDocument();
    });

    it('hides add when the user cannot update the product definition', () => {
        mockPermissions.mockReturnValue({ canRead: true, canUpdate: false, isLoading: false });
        mockContext.mockReturnValue({ product: { resources: [] }, isLoading: false });
        renderPage();
        expect(screen.queryByTestId('api_product_resources_add_button')).not.toBeInTheDocument();
    });

    it('hides row actions when the user cannot update', () => {
        mockPermissions.mockReturnValue({ canRead: true, canUpdate: false, isLoading: false });
        renderPage();
        expect(screen.queryByTestId('api_product_resources_toggle_button')).not.toBeInTheDocument();
    });

    it('submits a toggle by resource name using a fresh apply function', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getAllByRole('button', { name: /Actions for/i })[0]!);
        await user.click(screen.getAllByTestId('api_product_resources_toggle_button')[0]!);

        expect(mockMutate).toHaveBeenCalledWith(
            expect.any(Function),
            expect.objectContaining({ onSuccess: expect.any(Function), onError: expect.any(Function) }),
        );
        const apply = mockMutate.mock.calls[0]![0] as (current: typeof RESOURCES) => typeof RESOURCES;
        expect(apply(RESOURCES)).toEqual([
            { name: 'MyCache', type: 'cache', enabled: false, configuration: {} },
            { name: 'OAuth', type: 'oauth2', enabled: false, configuration: {} },
        ]);
    });

    it('removes a resource after confirmation', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getAllByRole('button', { name: /Actions for/i })[0]!);
        await user.click(screen.getAllByTestId('api_product_resources_remove_button')[0]!);
        await user.click(screen.getByRole('button', { name: 'Remove' }));

        expect(mockMutate).toHaveBeenCalledWith(
            expect.any(Function),
            expect.objectContaining({ onSuccess: expect.any(Function), onError: expect.any(Function) }),
        );
        const apply = mockMutate.mock.calls[0]![0] as (current: typeof RESOURCES) => typeof RESOURCES;
        expect(apply(RESOURCES)).toEqual([{ name: 'OAuth', type: 'oauth2', enabled: false, configuration: {} }]);
    });

    it('cancels remove from the confirm dialog', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getAllByRole('button', { name: /Actions for/i })[0]!);
        await user.click(screen.getAllByTestId('api_product_resources_remove_button')[0]!);
        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(mockMutate).not.toHaveBeenCalled();
    });

    it('surfaces server errors from a failed save', async () => {
        mockMutate.mockImplementation((_apply, { onError }) => {
            onError(new ApimApiError(400, 'Duplicate resource name'));
        });
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getAllByRole('button', { name: /Actions for/i })[0]!);
        await user.click(screen.getAllByTestId('api_product_resources_toggle_button')[0]!);

        await waitFor(() => expect(notify.error).toHaveBeenCalled());
    });
});
