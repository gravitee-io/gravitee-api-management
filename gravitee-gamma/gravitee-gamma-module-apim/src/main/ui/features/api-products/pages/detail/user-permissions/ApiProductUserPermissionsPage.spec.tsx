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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

jest.mock('../../../services/apiProductMembers', () => ({
    addApiProductMember: jest.fn(),
    deleteApiProductMember: jest.fn(),
    transferApiProductOwnership: jest.fn(),
    updateApiProductMember: jest.fn(),
}));

jest.mock('../../../context/ApiProductDetailContext', () => ({ useApiProductDetailContext: jest.fn() }));
jest.mock('../../../hooks/useApiProductMembers', () => ({
    useApiProductMembers: jest.fn(() => ({ data: { data: [] }, isLoading: false })),
    useApiProductRoles: jest.fn(() => ({ data: [{ name: 'OWNER' }, { name: 'USER' }] })),
}));
jest.mock('../../../hooks/useApiProductGroupMembers', () => ({
    useApiProductGroupMembers: jest.fn(() => ({ data: {}, isLoading: false })),
}));
jest.mock('../../../hooks/useUpdateApiProduct', () => ({
    useUpdateApiProduct: jest.fn(() => ({ mutate: jest.fn(), isPending: false })),
}));
jest.mock('../../../../apis/hooks/useGroups', () => ({ useGroups: jest.fn() }));
jest.mock('../../../../apis/hooks/useEnvironmentPortalConfiguration', () => ({
    useEnvironmentPortalConfiguration: jest.fn(),
}));

import { ApiProductUserPermissionsPage } from './ApiProductUserPermissionsPage';
import { useEnvironmentPortalConfiguration } from '../../../../apis/hooks/useEnvironmentPortalConfiguration';
import { useGroups } from '../../../../apis/hooks/useGroups';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';

globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};

Element.prototype.hasPointerCapture ??= () => false;
Element.prototype.setPointerCapture ??= () => {};
Element.prototype.releasePointerCapture ??= () => {};
Element.prototype.scrollIntoView ??= () => {};

const mockUseApiProductDetailContext = useApiProductDetailContext as jest.Mock;
const mockUseGroups = useGroups as jest.Mock;
const mockUsePortalConfig = useEnvironmentPortalConfiguration as jest.Mock;

const PRODUCT = {
    id: 'product-1',
    name: 'Orders Product',
    version: '1.0',
    apiIds: [],
    groups: [],
    primaryOwner: { id: 'user-po', displayName: 'Owner' },
};

const ENV_GROUPS = [
    { id: 'g-api', name: 'API PO Only', apiPrimaryOwner: 'member-1', apiProductPrimaryOwner: null },
    { id: 'g-product', name: 'Product PO', apiPrimaryOwner: null, apiProductPrimaryOwner: 'member-2' },
];

function renderPage() {
    mockUseApiProductDetailContext.mockReturnValue({ product: PRODUCT, isLoading: false });
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/api-products/product-1/user-permissions']}>
                <Routes>
                    <Route path="api-products/:productId/user-permissions" element={<ApiProductUserPermissionsPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiProductUserPermissionsPage transfer ownership', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseGroups.mockReturnValue({ data: { data: ENV_GROUPS } });
        mockUsePortalConfig.mockReturnValue({
            data: { apiProduct: { primaryOwnerMode: 'GROUP' }, api: { primaryOwnerMode: 'USER' } },
            isFetched: true,
        });
    });

    it('keeps Transfer ownership disabled until portal configuration is fetched', () => {
        mockUsePortalConfig.mockReturnValue({ data: undefined, isFetched: false });
        renderPage();

        expect(screen.getByRole('button', { name: /transfer ownership/i })).toBeDisabled();
    });

    it('uses apiProduct.primaryOwnerMode GROUP and filters by apiProductPrimaryOwner', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('button', { name: /transfer ownership/i }));
        await screen.findByRole('heading', { name: /transfer ownership/i });

        expect(screen.queryByRole('button', { name: /API Product member/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /Other user/i })).toBeNull();
        expect(screen.getByText('Select a primary owner group')).toBeInTheDocument();

        await user.click(screen.getAllByRole('combobox')[0]);
        expect(await screen.findByRole('option', { name: 'Product PO' })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: 'API PO Only' })).toBeNull();
    });
});
