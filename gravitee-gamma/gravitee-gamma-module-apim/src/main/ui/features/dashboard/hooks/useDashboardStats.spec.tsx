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
import { useEnvironment, useHasFeature } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useDashboardStats } from './useDashboardStats';
import { searchApiProducts } from '../../api-products/services/apiProduct';
import { searchApis } from '../../apis/services/apiList';
import { ApimLicenseFeature } from '../../license/apimFeatures';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    useHasFeature: jest.fn(),
}));

jest.mock('../../api-products/services/apiProduct', () => ({
    searchApiProducts: jest.fn(),
}));

jest.mock('../../apis/services/apiList', () => ({
    searchApis: jest.fn(),
}));

const mockUseEnvironment = useEnvironment as jest.MockedFunction<typeof useEnvironment>;
const mockUseHasFeature = useHasFeature as jest.MockedFunction<typeof useHasFeature>;
const mockSearchApis = searchApis as jest.MockedFunction<typeof searchApis>;
const mockSearchApiProducts = searchApiProducts as jest.MockedFunction<typeof searchApiProducts>;

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useDashboardStats', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockSearchApis.mockResolvedValue({ data: [], pagination: { totalCount: 2 } } as Awaited<ReturnType<typeof searchApis>>);
        mockSearchApiProducts.mockResolvedValue({
            data: [],
            pagination: { totalCount: 5 },
        } as Awaited<ReturnType<typeof searchApiProducts>>);
    });

    it('should fetch API and product counts when API Products are licensed', async () => {
        mockUseHasFeature.mockReturnValue(true);

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.hasContent).not.toBeNull());

        expect(mockSearchApis).toHaveBeenCalled();
        expect(mockSearchApiProducts).toHaveBeenCalled();
        expect(result.current.totalApis).toBe(2);
        expect(result.current.totalProducts).toBe(5);
        expect(result.current.hasContent).toBe(true);
        expect(result.current.isError).toBe(false);
    });

    it('should skip the products query and report zero products when unlicensed', async () => {
        mockUseHasFeature.mockImplementation(feature => feature !== ApimLicenseFeature.API_PRODUCTS);

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.hasContent).not.toBeNull());

        expect(mockSearchApis).toHaveBeenCalled();
        expect(mockSearchApiProducts).not.toHaveBeenCalled();
        expect(result.current.totalApis).toBe(2);
        expect(result.current.totalProducts).toBe(0);
        expect(result.current.hasContent).toBe(true);
        expect(result.current.isError).toBe(false);
    });

    it('should not mark the dashboard as errored when the skipped products query would have failed', async () => {
        mockUseHasFeature.mockImplementation(feature => feature !== ApimLicenseFeature.API_PRODUCTS);
        mockSearchApiProducts.mockRejectedValue(new Error('forbidden'));

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.hasContent).not.toBeNull());

        expect(result.current.isError).toBe(false);
        expect(result.current.totalProducts).toBe(0);
    });
});
