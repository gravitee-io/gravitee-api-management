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
import { useFederationEnabled } from '../../license/useFederationEnabled';

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

jest.mock('../../license/useFederationEnabled', () => ({ useFederationEnabled: jest.fn() }));

const mockUseEnvironment = useEnvironment as jest.MockedFunction<typeof useEnvironment>;
const mockUseHasFeature = useHasFeature as jest.MockedFunction<typeof useHasFeature>;
const mockSearchApis = searchApis as jest.MockedFunction<typeof searchApis>;
const mockSearchApiProducts = searchApiProducts as jest.MockedFunction<typeof searchApiProducts>;
const mockUseFederationEnabled = jest.mocked(useFederationEnabled);

const STATS_PAGE = 1;
const STATS_PER_PAGE = 1;

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
        mockUseFederationEnabled.mockReturnValue({ enabled: false, isResolved: true });
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

    it('should report no content when there are no APIs and no products', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockSearchApis.mockResolvedValue({ data: [], pagination: { totalCount: 0 } } as Awaited<ReturnType<typeof searchApis>>);
        mockSearchApiProducts.mockResolvedValue({
            data: [],
            pagination: { totalCount: 0 },
        } as Awaited<ReturnType<typeof searchApiProducts>>);

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.hasContent).not.toBeNull());

        expect(result.current.hasContent).toBe(false);
        expect(result.current.totalApis).toBe(0);
        expect(result.current.totalProducts).toBe(0);
        expect(result.current.isError).toBe(false);
    });

    it('should report content when there are no APIs but at least one product', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockSearchApis.mockResolvedValue({ data: [], pagination: { totalCount: 0 } } as Awaited<ReturnType<typeof searchApis>>);
        mockSearchApiProducts.mockResolvedValue({
            data: [],
            pagination: { totalCount: 3 },
        } as Awaited<ReturnType<typeof searchApiProducts>>);

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.hasContent).not.toBeNull());

        expect(result.current.hasContent).toBe(true);
        expect(result.current.totalApis).toBe(0);
        expect(result.current.totalProducts).toBe(3);
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

    it('should count federated APIs once the federation gate has resolved on', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockUseFederationEnabled.mockReturnValue({ enabled: true, isResolved: true });

        renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('DEFAULT', {}, STATS_PAGE, STATS_PER_PAGE, undefined, true);
    });

    it('should count proxies alone when the federation gate has resolved off', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockUseFederationEnabled.mockReturnValue({ enabled: false, isResolved: true });

        renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('DEFAULT', {}, STATS_PAGE, STATS_PER_PAGE, undefined, false);
    });

    it('should publish no API count while the federation gate is still resolving', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockUseFederationEnabled.mockReturnValue({ enabled: false, isResolved: false });

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApiProducts).toHaveBeenCalled());

        expect(mockSearchApis).not.toHaveBeenCalled();
        expect(result.current.hasContent).toBeNull();
        expect(result.current.totalApis).toBeNull();
    });

    it('should not mark the dashboard as errored when the skipped products query would have failed', async () => {
        mockUseHasFeature.mockImplementation(feature => feature !== ApimLicenseFeature.API_PRODUCTS);
        mockSearchApiProducts.mockRejectedValue(new Error('forbidden'));

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.hasContent).not.toBeNull());

        expect(result.current.isError).toBe(false);
        expect(result.current.totalProducts).toBe(0);
    });

    it('should mark the dashboard as errored when the licensed products query fails', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockSearchApiProducts.mockRejectedValue(new Error('forbidden'));

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.totalApis).toBe(2);
        expect(result.current.totalProducts).toBeNull();
    });

    it('should mark the dashboard as errored when the API count query fails', async () => {
        mockUseHasFeature.mockImplementation(feature => feature !== ApimLicenseFeature.API_PRODUCTS);
        mockSearchApis.mockRejectedValue(new Error('boom'));

        const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.totalApis).toBeNull();
        expect(result.current.totalProducts).toBe(0);
        expect(result.current.hasContent).toBeNull();
    });

    describe('failure logging', () => {
        const API_FORBIDDEN_MESSAGE = '[DashboardStats] User lacks permission to count APIs in environment';
        const API_FAILURE_MESSAGE = '[DashboardStats] Failed to count APIs in environment';
        const PRODUCTS_FAILURE_MESSAGE = '[DashboardStats] Failed to count API Products in environment';

        let warnSpy: jest.SpyInstance;
        let errorSpy: jest.SpyInstance;

        function requestFailure(status: number) {
            return Object.assign(new Error('count failed'), { status });
        }

        function statsCalls(spy: jest.SpyInstance) {
            return spy.mock.calls.filter(([message]) => String(message).includes('[DashboardStats]'));
        }

        beforeEach(() => {
            warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
            errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        });

        afterEach(() => {
            warnSpy.mockRestore();
            errorSpy.mockRestore();
        });

        it('warns naming the environment when the API count is refused with 403', async () => {
            mockUseHasFeature.mockReturnValue(true);
            const failure = requestFailure(403);
            mockSearchApis.mockRejectedValue(failure);

            renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

            await waitFor(() => expect(warnSpy).toHaveBeenCalledWith(API_FORBIDDEN_MESSAGE, 'DEFAULT', failure));
            expect(statsCalls(errorSpy)).toEqual([]);
        });

        it('logs an error naming the environment when the API count fails with 500', async () => {
            mockUseHasFeature.mockReturnValue(true);
            const failure = requestFailure(500);
            mockSearchApis.mockRejectedValue(failure);

            renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

            await waitFor(() => expect(errorSpy).toHaveBeenCalledWith(API_FAILURE_MESSAGE, 'DEFAULT', failure));
            expect(statsCalls(warnSpy)).toEqual([]);
        });

        it('logs an error naming API Products when the licensed product count fails with 500', async () => {
            mockUseHasFeature.mockReturnValue(true);
            const failure = requestFailure(500);
            mockSearchApiProducts.mockRejectedValue(failure);

            renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

            await waitFor(() => expect(errorSpy).toHaveBeenCalledWith(PRODUCTS_FAILURE_MESSAGE, 'DEFAULT', failure));
        });
    });
});
