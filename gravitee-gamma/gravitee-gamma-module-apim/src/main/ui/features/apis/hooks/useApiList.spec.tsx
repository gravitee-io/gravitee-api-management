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

import { useApiList } from './useApiList';
import { searchApis } from '../services/apiList';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../services/apiList', () => ({ searchApis: jest.fn() }));

const mockUseEnvironment = useEnvironment as jest.Mock;
const mockSearchApis = searchApis as jest.Mock;

const MOCK_RESPONSE = {
    data: [],
    pagination: { page: 1, perPage: 10, pageCount: 0, totalCount: 0 },
};

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useApiList', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockSearchApis.mockResolvedValue(MOCK_RESPONSE);
    });

    afterEach(() => jest.clearAllMocks());

    it('does not call the API when the environment is unavailable', () => {
        mockUseEnvironment.mockReturnValue(null);

        renderHook(() => useApiList({ query: '', page: 1, perPage: 10 }), { wrapper: createWrapper() });

        expect(mockSearchApis).not.toHaveBeenCalled();
    });

    it('calls searchApis with the env id, page, and perPage — sorts by name when no query', async () => {
        renderHook(() => useApiList({ query: '', page: 2, perPage: 25 }), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('env-1', { query: undefined }, 2, 25, 'name');
    });

    it('passes the search query string when provided — no sortBy (relevance order)', async () => {
        renderHook(() => useApiList({ query: 'my-api', page: 1, perPage: 10 }), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('env-1', { query: 'my-api' }, 1, 10, undefined);
    });

    it('maps an empty query string to undefined in the request body and sorts by name', async () => {
        renderHook(() => useApiList({ query: '', page: 1, perPage: 10 }), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        const [, queryArg, , , sortByArg] = mockSearchApis.mock.calls[0];
        expect(queryArg.query).toBeUndefined();
        expect(sortByArg).toBe('name');
    });
});
