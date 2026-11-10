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
import { renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { usePortals } from './usePortals';
import { usePortalsStats } from './usePortalsStats';
import { createPortal, createPortalsResponse, createPortalsStats, TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { createQueryClientWrapper } from '../../../testing/helpers';
import { server } from '../../../testing/server';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);

describe('portal queries', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: TEST_CONFIG.environmentId, organizationId: TEST_CONFIG.organizationId });
    });

    it('waits for an environment ID before fetching the list or statistics', async () => {
        mockUseEnvironment.mockReturnValue(null);
        const listRequests = jest.fn();
        const statsRequests = jest.fn();
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () => {
                listRequests();
                return HttpResponse.json(createPortalsResponse());
            }),
            http.get(`${TEST_V2_BASE}/portals/_stats`, () => {
                statsRequests();
                return HttpResponse.json(createPortalsStats());
            }),
        );
        const { wrapper } = createQueryClientWrapper();
        const { result, rerender } = renderHook(() => ({ list: usePortals(), stats: usePortalsStats() }), { wrapper });

        expect(result.current.list.fetchStatus).toBe('idle');
        expect(result.current.stats.fetchStatus).toBe('idle');
        expect(listRequests).not.toHaveBeenCalled();
        expect(statsRequests).not.toHaveBeenCalled();

        mockUseEnvironment.mockReturnValue({ id: '', organizationId: TEST_CONFIG.organizationId });
        rerender();
        expect(result.current.list.fetchStatus).toBe('idle');
        expect(result.current.stats.fetchStatus).toBe('idle');

        mockUseEnvironment.mockReturnValue({ id: TEST_CONFIG.environmentId, organizationId: TEST_CONFIG.organizationId });
        rerender();
        await waitFor(() => {
            expect(result.current.list.isSuccess).toBe(true);
            expect(result.current.stats.isSuccess).toBe(true);
        });
        expect(listRequests).toHaveBeenCalledTimes(1);
        expect(statsRequests).toHaveBeenCalledTimes(1);
    });

    it('refetches and caches list searches and pages independently without refetching statistics', async () => {
        const requests: URL[] = [];
        const statsRequests = jest.fn();
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                const url = new URL(request.url);
                requests.push(url);
                return HttpResponse.json(
                    createPortalsResponse({
                        data: [
                            createPortal({
                                name: `${url.searchParams.get('q')}:${url.searchParams.get('page')}:${url.searchParams.get('perPage')}`,
                            }),
                        ],
                    }),
                );
            }),
            http.get(`${TEST_V2_BASE}/portals/_stats`, () => {
                statsRequests();
                return HttpResponse.json(createPortalsStats());
            }),
        );
        const { wrapper, queryClient } = createQueryClientWrapper();
        const { result, rerender } = renderHook(params => ({ list: usePortals(params), stats: usePortalsStats() }), {
            wrapper,
            initialProps: { query: 'partners', page: 1, perPage: 10 },
        });
        await waitFor(() => {
            expect(result.current.list.data?.data[0].name).toBe('partners:1:10');
            expect(result.current.stats.isSuccess).toBe(true);
        });

        rerender({ query: 'internal', page: 1, perPage: 10 });
        await waitFor(() => expect(result.current.list.data?.data[0].name).toBe('internal:1:10'));
        rerender({ query: 'internal', page: 2, perPage: 10 });
        await waitFor(() => expect(result.current.list.data?.data[0].name).toBe('internal:2:10'));
        rerender({ query: 'internal', page: 2, perPage: 25 });
        await waitFor(() => expect(result.current.list.data?.data[0].name).toBe('internal:2:25'));

        expect(requests).toHaveLength(4);
        expect(queryClient.getQueryCache().getAll()).toHaveLength(5);
        expect(statsRequests).toHaveBeenCalledTimes(1);
        expect(result.current.stats.data).toEqual(createPortalsStats());
    });

    it('reuses the cached result without refetching when only surrounding search whitespace changes', async () => {
        const searches: (string | null)[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, ({ request }) => {
                searches.push(new URL(request.url).searchParams.get('q'));
                return HttpResponse.json(createPortalsResponse());
            }),
        );
        const { wrapper, queryClient } = createQueryClientWrapper();
        const { result, rerender } = renderHook(({ query }) => usePortals({ query }), {
            wrapper,
            initialProps: { query: '  partners  ' },
        });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        const cachedData = result.current.data;

        for (const query of ['partners', '\tpartners ']) {
            rerender({ query });

            expect(result.current.data).toBe(cachedData);
            expect(result.current.fetchStatus).toBe('idle');
            expect(queryClient.getQueryCache().getAll()).toHaveLength(1);
            expect(searches).toEqual(['partners']);
        }
    });

    it.each([false, true])('retains previous page data only when opted in (%s)', async keepPreviousData => {
        const firstList = createPortalsResponse({ data: [createPortal({ name: 'First page' })] });
        const secondList = createPortalsResponse({ data: [createPortal({ name: 'Second page' })] });
        let release: () => void = () => undefined;
        const pending = new Promise<void>(resolve => {
            release = resolve;
        });
        const requestedPages: string[] = [];
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, async ({ request }) => {
                const page = new URL(request.url).searchParams.get('page') ?? '';
                requestedPages.push(page);
                if (page === '2') await pending;
                return HttpResponse.json(page === '2' ? secondList : firstList);
            }),
        );
        const { wrapper } = createQueryClientWrapper();
        const { result, rerender } = renderHook(({ page }) => usePortals({ page }, { keepPreviousData }), {
            wrapper,
            initialProps: { page: 1 },
        });
        await waitFor(() => expect(result.current.data).toEqual(firstList));
        try {
            rerender({ page: 2 });
            await waitFor(() => expect(requestedPages).toEqual(['1', '2']));
            expect(result.current.isFetching).toBe(true);
            expect(result.current.isPlaceholderData).toBe(keepPreviousData);
            expect(result.current.data).toEqual(keepPreviousData ? firstList : undefined);
        } finally {
            release();
        }
        await waitFor(() => expect(result.current.data).toEqual(secondList));
        expect(result.current.isPlaceholderData).toBe(false);
    });

    it.each([false, true])('isolates previous environment data with retention=%s', async keepPreviousData => {
        const firstList = createPortalsResponse({ data: [createPortal({ name: 'First environment' })] });
        const secondList = createPortalsResponse({ data: [createPortal({ name: 'Second environment' })] });
        const firstStats = createPortalsStats({ publishedApisCount: 1 });
        const secondStats = createPortalsStats({ publishedApisCount: 2 });
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json(firstList)),
            http.get(`${TEST_V2_BASE}/portals/_stats`, () => HttpResponse.json(firstStats)),
            http.get(`${TEST_CONFIG.managementBaseURL}/v2/environments/second-env/portals`, () => HttpResponse.json(secondList)),
            http.get(`${TEST_CONFIG.managementBaseURL}/v2/environments/second-env/portals/_stats`, () => HttpResponse.json(secondStats)),
        );
        const { wrapper, queryClient } = createQueryClientWrapper();
        const { result, rerender } = renderHook(() => ({ list: usePortals({}, { keepPreviousData }), stats: usePortalsStats() }), {
            wrapper,
        });
        await waitFor(() => {
            expect(result.current.list.data).toEqual(firstList);
            expect(result.current.stats.data).toEqual(firstStats);
        });

        mockUseEnvironment.mockReturnValue({ id: 'second-env', organizationId: TEST_CONFIG.organizationId });
        rerender();
        expect(result.current.list.data).toBeUndefined();
        expect(result.current.stats.data).toBeUndefined();

        await waitFor(() => {
            expect(result.current.list.data).toEqual(secondList);
            expect(result.current.stats.data).toEqual(secondStats);
        });
        expect(queryClient.getQueryCache().getAll()).toHaveLength(4);
    });

    it.each([403, 500])('exposes HTTP %s to consumers instead of returning empty data', async status => {
        server.use(
            http.get(`${TEST_V2_BASE}/portals`, () => HttpResponse.json({ message: 'List failed' }, { status })),
            http.get(`${TEST_V2_BASE}/portals/_stats`, () => HttpResponse.json({ message: 'Stats failed' }, { status })),
        );
        const { wrapper } = createQueryClientWrapper();
        const { result } = renderHook(() => ({ list: usePortals(), stats: usePortalsStats() }), { wrapper });

        await waitFor(() => {
            expect(result.current.list.isError).toBe(true);
            expect(result.current.stats.isError).toBe(true);
        });
        expect(result.current.list.data).toBeUndefined();
        expect(result.current.stats.data).toBeUndefined();
        expect(result.current.list.error).toMatchObject({ status, message: 'List failed' });
        expect(result.current.stats.error).toMatchObject({ status, message: 'Stats failed' });
    });
});
