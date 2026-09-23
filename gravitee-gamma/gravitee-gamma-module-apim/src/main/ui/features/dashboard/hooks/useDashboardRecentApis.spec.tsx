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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useDashboardRecentApis } from './useDashboardRecentApis';
import { searchApis } from '../../apis/services/apiList';
import type { ApiListItem } from '../../apis/types';
import { useFederationEnabled } from '../../license/useFederationEnabled';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../../apis/services/apiList', () => ({ searchApis: jest.fn() }));
jest.mock('../../license/useFederationEnabled', () => ({ useFederationEnabled: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockSearchApis = jest.mocked(searchApis);
const mockUseFederationEnabled = jest.mocked(useFederationEnabled);

const MOCK_ENV = { id: 'env-1', hrids: ['env-1'], organizationId: 'DEFAULT' };

const RECENT_PAGE = 1;
const RECENT_PER_PAGE = 6;

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useDashboardRecentApis', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue(MOCK_ENV);
        mockSearchApis.mockResolvedValue({ data: [], pagination: { page: 1, perPage: 6, pageCount: 0, totalCount: 0 } });
    });

    afterEach(() => jest.clearAllMocks());

    it('asks for federated proxies when the federation gate has resolved on', async () => {
        mockUseFederationEnabled.mockReturnValue({ enabled: true, isResolved: true });

        renderHook(() => useDashboardRecentApis(), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('env-1', {}, RECENT_PAGE, RECENT_PER_PAGE, undefined, true);
    });

    it('asks for proxies alone when the federation gate has resolved off', async () => {
        mockUseFederationEnabled.mockReturnValue({ enabled: false, isResolved: true });

        renderHook(() => useDashboardRecentApis(), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('env-1', {}, RECENT_PAGE, RECENT_PER_PAGE, undefined, false);
    });

    it('reports loading rather than an empty list while the federation gate is still resolving', () => {
        mockUseFederationEnabled.mockReturnValue({ enabled: false, isResolved: false });

        const { result } = renderHook(() => useDashboardRecentApis(), { wrapper: createWrapper() });

        expect(mockSearchApis).not.toHaveBeenCalled();
        expect(result.current.apis).toEqual([]);
        expect(result.current.isLoading).toBe(true);
    });

    it('returns the resolved rows as apis once the search succeeds', async () => {
        mockUseFederationEnabled.mockReturnValue({ enabled: false, isResolved: true });
        const rows = [{ id: 'api-1', name: 'Recent API' }] as unknown as ApiListItem[];
        mockSearchApis.mockResolvedValue({ data: rows, pagination: { page: 1, perPage: 6, pageCount: 1, totalCount: 1 } });

        const { result } = renderHook(() => useDashboardRecentApis(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.apis).toEqual(rows);
        expect(result.current.isError).toBe(false);
    });

    it('reports an error once the search rejects after the federation gate has resolved', async () => {
        mockUseFederationEnabled.mockReturnValue({ enabled: false, isResolved: true });
        mockSearchApis.mockRejectedValue(new Error('search failed'));

        const { result } = renderHook(() => useDashboardRecentApis(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.isLoading).toBe(false);
        expect(result.current.apis).toEqual([]);
    });
});
