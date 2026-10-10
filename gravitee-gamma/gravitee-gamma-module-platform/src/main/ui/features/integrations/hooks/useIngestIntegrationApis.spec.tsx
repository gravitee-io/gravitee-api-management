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
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useIngestIntegrationApis } from './useIngestIntegrationApis';
import { ingestIntegrationApis } from '../services/integrationDetail';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/integrationDetail', () => ({
    ingestIntegrationApis: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockIngestIntegrationApis = jest.mocked(ingestIntegrationApis);

const DETAIL_KEY = integrationKeys.detail('env-1', 'int-1');
const FIRST_INGESTED_PAGE_KEY = integrationKeys.ingestedApis('env-1', 'int-1', 1, 10);
const SECOND_INGESTED_PAGE_KEY = integrationKeys.ingestedApis('env-1', 'int-1', 2, 25);
const OTHER_DETAIL_KEY = integrationKeys.detail('env-1', 'int-2');
const OTHER_INGESTED_PAGE_KEY = integrationKeys.ingestedApis('env-1', 'int-2', 1, 10);
const SEEDED_KEYS = [DETAIL_KEY, FIRST_INGESTED_PAGE_KEY, SECOND_INGESTED_PAGE_KEY, OTHER_DETAIL_KEY, OTHER_INGESTED_PAGE_KEY];

describe('useIngestIntegrationApis', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockIngestIntegrationApis.mockResolvedValue({ status: 'PENDING' });
        SEEDED_KEYS.forEach(key => queryClient.setQueryData(key, {}));
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    function invalidatedKeys() {
        return SEEDED_KEYS.filter(key => queryClient.getQueryState(key)?.isInvalidated);
    }

    it('refreshes the integration and every page of its ingested APIs once ingestion has started', async () => {
        const { result } = renderHook(() => useIngestIntegrationApis(), { wrapper });

        await act(() => result.current.mutateAsync({ integrationId: 'int-1', scope: { kind: 'SELECTED', apiIds: ['api-n'] } }));

        expect(mockIngestIntegrationApis).toHaveBeenCalledWith('env-1', 'int-1', { kind: 'SELECTED', apiIds: ['api-n'] });
        expect(invalidatedKeys()).toEqual([DETAIL_KEY, FIRST_INGESTED_PAGE_KEY, SECOND_INGESTED_PAGE_KEY]);
    });

    it('refreshes nothing when ingestion fails to start', async () => {
        const failure = new Error('Boom');
        mockIngestIntegrationApis.mockRejectedValue(failure);
        const { result } = renderHook(() => useIngestIntegrationApis(), { wrapper });

        await act(() =>
            expect(result.current.mutateAsync({ integrationId: 'int-1', scope: { kind: 'SELECTED', apiIds: ['api-n'] } })).rejects.toBe(
                failure,
            ),
        );

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
        expect(invalidatedKeys()).toEqual([]);
    });
});
