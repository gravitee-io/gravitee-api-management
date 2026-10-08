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
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useApiList } from './useApiList';
import { ApimApiError } from '../../../shared/api/apimClient';
import { searchApis } from '../services/apiList';
import { apiListKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiList', () => ({
    ...jest.requireActual<object>('../services/apiList'),
    searchApis: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockSearchApis = jest.mocked(searchApis);

const MOCK_ENV = { id: 'env-1', hrids: ['env-1'] };

const MOCK_RESPONSE = {
    data: [],
    pagination: { page: 1, perPage: 25, pageCount: 0, totalCount: 0 },
};

const FEDERATION_OFF = { includeFederated: false, isFederationResolved: true };
const FEDERATION_PENDING = { includeFederated: false, isFederationResolved: false };
const FEDERATION_ON = { includeFederated: true, isFederationResolved: true };

function makeQueryClient() {
    return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function createWrapper(queryClient = makeQueryClient()) {
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useApiList', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue(MOCK_ENV);
        mockSearchApis.mockResolvedValue(MOCK_RESPONSE);
    });

    afterEach(() => jest.clearAllMocks());

    it('calls searchApis with envId, page, and perPage — sorts by name when no query', async () => {
        renderHook(() => useApiList({ query: '', page: 2, perPage: 25, ...FEDERATION_OFF }), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('env-1', { query: undefined }, 2, 25, 'name', false);
    });

    it('passes the search query string when provided — no sortBy (relevance order)', async () => {
        renderHook(() => useApiList({ query: 'my-api', page: 1, perPage: 25, ...FEDERATION_OFF }), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('env-1', { query: 'my-api' }, 1, 25, undefined, false);
    });

    it('asks for federated proxies when the federation gate has resolved on', async () => {
        renderHook(() => useApiList({ query: '', page: 1, perPage: 25, ...FEDERATION_ON }), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('env-1', { query: undefined }, 1, 25, 'name', true);
    });

    it.each<[string, string, object, string]>([
        ['while browsing, instead of the name default', '', { query: undefined }, 'status'],
        ['while searching, instead of relevance order', 'my-api', { query: 'my-api' }, '-tags_desc'],
    ])('forwards an explicit column sort %s', async (_case, query, expectedQueryArg, sortBy) => {
        renderHook(() => useApiList({ query, page: 1, perPage: 25, sortBy, ...FEDERATION_ON }), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith('env-1', expectedQueryArg, 1, 25, sortBy, true);
    });

    it('holds its search until the federation gate resolves', () => {
        renderHook(() => useApiList({ query: '', page: 1, perPage: 25, ...FEDERATION_PENDING }), { wrapper: createWrapper() });

        expect(mockSearchApis).not.toHaveBeenCalled();
    });

    it('reports the wait for the federation gate as loading, not as a loaded empty list', () => {
        const { result } = renderHook(() => useApiList({ query: '', page: 1, perPage: 25, ...FEDERATION_PENDING }), {
            wrapper: createWrapper(),
        });

        expect(result.current.isLoading).toBe(true);
    });

    it('searches again rather than serving the gate-off result once the gate flips on', async () => {
        const wrapper = createWrapper();
        const { rerender } = renderHook((gate: typeof FEDERATION_OFF) => useApiList({ query: '', page: 1, perPage: 25, ...gate }), {
            wrapper,
            initialProps: FEDERATION_OFF,
        });
        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));

        rerender(FEDERATION_ON);

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(2));
        expect(mockSearchApis).toHaveBeenLastCalledWith('env-1', { query: undefined }, 1, 25, 'name', true);
    });

    it('searches again rather than serving the previous sort when the same column flips direction', async () => {
        const wrapper = createWrapper();
        const { rerender } = renderHook(
            ({ sortBy }: { sortBy: string }) => useApiList({ query: '', page: 1, perPage: 25, sortBy, ...FEDERATION_OFF }),
            {
                wrapper,
                initialProps: { sortBy: 'status' },
            },
        );
        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));

        rerender({ sortBy: '-status' });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(2));
        expect(mockSearchApis).toHaveBeenLastCalledWith('env-1', { query: undefined }, 1, 25, '-status', false);
    });

    it('searches again when the API list cache is invalidated at its root, whatever this page was built from', async () => {
        // The root key is what the API delete mutation invalidates, and the delete's own test mocks the
        // key module away — so nothing else proves this query sits under that root. Every argument is off
        // its default on purpose: the root has to reach a page cached under any query, sort, or gate state.
        const queryClient = makeQueryClient();
        renderHook(() => useApiList({ query: 'orders', page: 3, perPage: 25, sortBy: '-status', ...FEDERATION_ON }), {
            wrapper: createWrapper(queryClient),
        });
        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));

        await act(async () => {
            await queryClient.invalidateQueries({ queryKey: apiListKeys.all });
        });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(2));
        expect(mockSearchApis).toHaveBeenLastCalledWith('env-1', { query: 'orders' }, 3, 25, '-status', true);
    });

    it('maps an empty query string to undefined in the request body and sorts by name', async () => {
        renderHook(() => useApiList({ query: '', page: 1, perPage: 25, ...FEDERATION_OFF }), { wrapper: createWrapper() });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        const [, queryArg, , , sortByArg] = mockSearchApis.mock.calls[0];
        expect(queryArg.query).toBeUndefined();
        expect(sortByArg).toBe('name');
    });

    it('surfaces a rejected search as the query error', async () => {
        const failure = new Error('search failed');
        mockSearchApis.mockRejectedValue(failure);

        const { result } = renderHook(() => useApiList({ query: '', page: 1, perPage: 25, ...FEDERATION_OFF }), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.error).toBe(failure));
        expect(result.current.isLoading).toBe(false);
    });

    it('does not fire when environment is not yet ready', () => {
        mockUseEnvironment.mockReturnValue(null);
        renderHook(() => useApiList({ query: '', page: 1, perPage: 25, ...FEDERATION_OFF }), { wrapper: createWrapper() });
        expect(mockSearchApis).not.toHaveBeenCalled();
    });

    it('sends the search text and every active filter in a single search, and drops empty lists', async () => {
        renderHook(
            () =>
                useApiList({
                    query: 'orders',
                    page: 3,
                    perPage: 25,
                    filters: { apiTypes: ['V4_TCP_PROXY'], statuses: ['STARTED'], tags: ['eu-west'], categories: [] },
                    ...FEDERATION_OFF,
                }),
            { wrapper: createWrapper() },
        );

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));
        expect(mockSearchApis).toHaveBeenCalledWith(
            'env-1',
            { query: 'orders', apiTypes: ['V4_TCP_PROXY'], statuses: ['STARTED'], tags: ['eu-west'] },
            3,
            25,
            undefined,
            false,
        );
    });

    it('searches again when a filter changes and keeps the current search text', async () => {
        const wrapper = createWrapper();
        const { rerender } = renderHook(
            ({ apiTypes }: { apiTypes: string[] }) =>
                useApiList({ query: 'orders', page: 1, perPage: 25, filters: { apiTypes, statuses: ['STOPPED'] }, ...FEDERATION_OFF }),
            { wrapper, initialProps: { apiTypes: ['V4_TCP_PROXY'] } },
        );
        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(1));

        rerender({ apiTypes: ['V4_HTTP_PROXY'] });

        await waitFor(() => expect(mockSearchApis).toHaveBeenCalledTimes(2));
        expect(mockSearchApis).toHaveBeenLastCalledWith(
            'env-1',
            { query: 'orders', apiTypes: ['V4_HTTP_PROXY'], statuses: ['STOPPED'] },
            1,
            25,
            undefined,
            false,
        );
    });

    it('does not retry a 403 from the list', async () => {
        mockSearchApis.mockRejectedValue(new ApimApiError(403, 'forbidden'));
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 3 } } });
        const { result } = renderHook(() => useApiList({ query: '', page: 1, perPage: 25, ...FEDERATION_OFF }), {
            wrapper: createWrapper(queryClient),
        });
        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(mockSearchApis).toHaveBeenCalledTimes(1);
    });

    it('retries a 500 from the list', async () => {
        mockSearchApis.mockRejectedValue(new ApimApiError(500, 'unavailable'));
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 3 } } });
        const { result } = renderHook(() => useApiList({ query: '', page: 1, perPage: 25, ...FEDERATION_OFF }), {
            wrapper: createWrapper(queryClient),
        });
        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(mockSearchApis).toHaveBeenCalledTimes(3);
    });
});
