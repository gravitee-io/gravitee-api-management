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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useApisScoring } from './useApisScoring';
import { listApisScoring } from '../services/scoring';
import { environmentScoringKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/scoring', () => ({
    listApisScoring: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockListApisScoring = jest.mocked(listApisScoring);

const PAGE = {
    data: [{ id: 'api-1', name: 'Petstore', score: 0.84, errors: 1, warnings: 0, infos: 2, hints: 1 }],
    pagination: { page: 2, perPage: 10, pageCount: 3, pageItemsCount: 1, totalCount: 21 },
};

function createHarness() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { queryClient, Wrapper };
}

describe('useApisScoring', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockListApisScoring.mockResolvedValue(PAGE);
    });

    it('loads GET /scoring/apis with page and perPage under the environment query key', async () => {
        const { queryClient, Wrapper } = createHarness();
        const { result } = renderHook(() => useApisScoring({ page: 2, perPage: 10 }), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(mockListApisScoring).toHaveBeenCalledWith('DEFAULT', { page: 2, perPage: 10 });
        expect(result.current.apis).toEqual(PAGE.data);
        expect(result.current.totalCount).toBe(21);
        expect(queryClient.getQueryData(environmentScoringKeys.apis('DEFAULT', 2, 10))).toEqual(PAGE);
    });

    it('does not fetch without env.id', () => {
        mockUseEnvironment.mockReturnValue({ id: undefined } as unknown as ReturnType<typeof useEnvironment>);
        const { Wrapper } = createHarness();
        renderHook(() => useApisScoring({ page: 1, perPage: 10 }), { wrapper: Wrapper });

        expect(mockListApisScoring).not.toHaveBeenCalled();
    });

    it('maps missing data to an empty list', async () => {
        mockListApisScoring.mockResolvedValue({
            data: undefined as unknown as typeof PAGE.data,
            pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
        });
        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApisScoring({ page: 1, perPage: 10 }), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.apis).toEqual([]);
        expect(result.current.totalCount).toBe(0);
    });
});
