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

import { useApiStats } from './useApiStats';
import { useFederationEnabled } from '../../license/useFederationEnabled';
import { searchApis } from '../services/apiList';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiList', () => ({ searchApis: jest.fn() }));
jest.mock('../../license/useFederationEnabled', () => ({ useFederationEnabled: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockSearchApis = jest.mocked(searchApis);
const mockUseFederationEnabled = jest.mocked(useFederationEnabled);

const MOCK_ENV = { id: 'env-1', hrids: ['env-1'] };

const PROXY_ONLY_COUNT = 9;
const WITH_FEDERATED_COUNT = 12;

function countOf(totalCount: number) {
    return { data: [], pagination: { page: 1, perPage: 1, pageCount: 1, totalCount } };
}

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useApiStats', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue(MOCK_ENV);
        mockUseFederationEnabled.mockReturnValue({ enabled: false, isResolved: true });
    });

    afterEach(() => jest.clearAllMocks());

    it('re-counts every card when the federation gate flips on, rather than reusing the pre-flip count', async () => {
        mockSearchApis.mockResolvedValue(countOf(PROXY_ONLY_COUNT));

        const { result, rerender } = renderHook(() => useApiStats(), { wrapper: createWrapper() });
        await waitFor(() => expect(result.current.total).toBe(PROXY_ONLY_COUNT));

        mockUseFederationEnabled.mockReturnValue({ enabled: true, isResolved: true });
        mockSearchApis.mockResolvedValue(countOf(WITH_FEDERATED_COUNT));
        rerender();

        await waitFor(() => expect(result.current.total).toBe(WITH_FEDERATED_COUNT));
        expect(result.current.private).toBe(WITH_FEDERATED_COUNT);
        expect(result.current.published).toBe(WITH_FEDERATED_COUNT);
    });

    it('flags only the count whose search failed and keeps the other counts', async () => {
        jest.spyOn(console, 'warn').mockImplementation(() => {});
        mockSearchApis.mockImplementation((_envId, filters) =>
            filters.published ? Promise.reject(new Error('search failed')) : Promise.resolve(countOf(PROXY_ONLY_COUNT)),
        );

        const { result } = renderHook(() => useApiStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.failed.published).toBe(true));
        await waitFor(() => expect(result.current.total).toBe(PROXY_ONLY_COUNT));
        expect(result.current.failed).toEqual({ total: false, private: false, published: true });
        expect(result.current.private).toBe(PROXY_ONLY_COUNT);
        expect(result.current.published).toBeNull();
        expect(result.current.isError).toBe(true);
    });

    it('does not flag a count still in flight as failed when a sibling count fails', async () => {
        jest.spyOn(console, 'warn').mockImplementation(() => {});
        mockSearchApis.mockImplementation((_envId, filters) =>
            filters.published ? Promise.reject(new Error('search failed')) : new Promise(() => {}),
        );

        const { result } = renderHook(() => useApiStats(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.failed.published).toBe(true));
        expect(result.current.failed.total).toBe(false);
        expect(result.current.failed.private).toBe(false);
        expect(result.current.total).toBeNull();
        expect(result.current.isLoading).toBe(true);
    });
});
