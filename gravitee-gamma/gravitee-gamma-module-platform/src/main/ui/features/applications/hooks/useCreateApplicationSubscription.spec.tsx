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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useCanSearchApiProductsForSubscription, useSubscriptionReferenceSearch } from './useCreateApplicationSubscription';
import { useEnvironmentPermissionsReady } from '../../../shared/hooks/useEnvironmentPermissions';
import { useApplicationDetailContext } from '../context/ApplicationDetailContext';
import { searchApiProductsForSubscription, searchApisForSubscription } from '../services/applicationSubscriptions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    useHasPermission: jest.fn(),
}));

jest.mock('../context/ApplicationDetailContext', () => ({
    useApplicationDetailContext: jest.fn(),
}));

jest.mock('../../../shared/hooks/useEnvironmentPermissions', () => ({
    useEnvironmentPermissionsReady: jest.fn(),
}));

jest.mock('../services/applicationSubscriptions');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseApplicationDetailContext = jest.mocked(useApplicationDetailContext);
const mockUseEnvironmentPermissionsReady = jest.mocked(useEnvironmentPermissionsReady);
const mockSearchApis = jest.mocked(searchApisForSubscription);
const mockSearchApiProducts = jest.mocked(searchApiProductsForSubscription);

function mockPermissionsReady(application = true, environment = true) {
    mockUseApplicationDetailContext.mockReturnValue({
        application: null,
        isLoading: false,
        permissionsReady: application,
        permissionsError: false,
        refetchPermissions: jest.fn(),
    });
    mockUseEnvironmentPermissionsReady.mockReturnValue(environment);
}

function wrapper({ children }: { children: ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useCreateApplicationSubscription hooks', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' });
        mockPermissionsReady();
    });

    describe('useCanSearchApiProductsForSubscription', () => {
        it('checks environment-api_product-r after application and environment permissions load', () => {
            mockUseHasPermission.mockReturnValue(true);
            const { result } = renderHook(() => useCanSearchApiProductsForSubscription());
            expect(result.current).toBe(true);
            expect(mockUseHasPermission).toHaveBeenCalledWith({ anyOf: ['environment-api_product-r'] });
        });

        it('returns false while application permissions are loading', () => {
            mockPermissionsReady(false, true);
            mockUseHasPermission.mockReturnValue(true);
            const { result } = renderHook(() => useCanSearchApiProductsForSubscription());
            expect(result.current).toBe(false);
        });

        it('returns false while environment permissions are loading', () => {
            mockPermissionsReady(true, false);
            mockUseHasPermission.mockReturnValue(true);
            const { result } = renderHook(() => useCanSearchApiProductsForSubscription());
            expect(result.current).toBe(false);
        });
    });

    describe('useSubscriptionReferenceSearch', () => {
        it('merges API and API product hits when permitted', async () => {
            mockUseHasPermission.mockReturnValue(true);
            mockSearchApis.mockResolvedValue({ data: [{ id: 'api-2', name: 'Zebra API' }] });
            mockSearchApiProducts.mockResolvedValue({ data: [{ id: 'prod-1', name: 'Alpha Product' }] });

            const { result } = renderHook(() => useSubscriptionReferenceSearch('alpha'), { wrapper });

            await waitFor(() => expect(result.current.data).toHaveLength(2));

            expect(result.current.data?.[0].value.name).toBe('Alpha Product');
            expect(result.current.data?.[1].value.name).toBe('Zebra API');
            expect(mockSearchApiProducts).toHaveBeenCalled();
        });

        it('searches APIs only when API product permission is missing', async () => {
            mockUseHasPermission.mockReturnValue(false);
            mockSearchApis.mockResolvedValue({ data: [{ id: 'api-1', name: 'Only API' }] });

            const { result } = renderHook(() => useSubscriptionReferenceSearch('api'), { wrapper });

            await waitFor(() => expect(result.current.data).toHaveLength(1));

            expect(result.current.data?.[0].type).toBe('API');
            expect(mockSearchApiProducts).not.toHaveBeenCalled();
        });

        it('does not run when the query is empty', () => {
            renderHook(() => useSubscriptionReferenceSearch('   '), { wrapper });
            expect(mockSearchApis).not.toHaveBeenCalled();
        });

        it('does not run before permissions are ready', () => {
            mockPermissionsReady(false, true);
            mockUseHasPermission.mockReturnValue(true);
            renderHook(() => useSubscriptionReferenceSearch('alpha'), { wrapper });
            expect(mockSearchApis).not.toHaveBeenCalled();
            expect(mockSearchApiProducts).not.toHaveBeenCalled();
        });
    });
});
