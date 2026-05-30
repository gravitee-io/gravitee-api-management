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
import { render, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { EntityResponse } from '../../api/authz-api.types';
import { useAllEntities } from '../useAllEntities';

const listSpy = vi.fn();

vi.mock('../../api/authz-api.service', () => ({
    MAX_PER_PAGE: 100,
    authzApiService: {
        listEntities: (env: string, params?: unknown) => listSpy(env, params),
    },
}));

function makeWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function Probe({ env }: { env: string }) {
    const { data, total, truncated, isLoading, error } = useAllEntities(env, { kind: 'RESOURCE', excludeEntityIdPrefix: 'action.' });
    return (
        <div>
            <span data-testid="loading">{String(isLoading)}</span>
            <span data-testid="error">{error ?? 'none'}</span>
            <span data-testid="fetched">{data.length}</span>
            <span data-testid="total">{total}</span>
            <span data-testid="truncated">{String(truncated)}</span>
        </div>
    );
}

function makeEntity(id: string): EntityResponse {
    return {
        id,
        environmentId: 'DEFAULT',
        uid: `mcp.${id}`,
        attributes: {},
        parents: [],
        createdAt: '2026-05-27T10:00:00.000Z',
        updatedAt: '2026-05-27T10:00:00.000Z',
    };
}

beforeEach(() => listSpy.mockReset());

describe('useAllEntities', () => {
    it('does not fire the query when environmentId is empty', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 100 });
        render(<Probe env="" />, { wrapper: makeWrapper() });

        await Promise.resolve();
        await Promise.resolve();
        expect(listSpy).not.toHaveBeenCalled();
    });

    it('passes the kind + excludeEntityIdPrefix filter through and fetches one page when it covers the total', async () => {
        listSpy.mockResolvedValue({
            data: [makeEntity('a'), makeEntity('b')],
            total: 2,
            page: 1,
            perPage: 100,
        });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('fetched').textContent).toBe('2'));
        expect(getByTestId('truncated').textContent).toBe('false');
        expect(listSpy).toHaveBeenCalledTimes(1);
        expect(listSpy).toHaveBeenCalledWith(
            'env-1',
            expect.objectContaining({ kind: 'RESOURCE', excludeEntityIdPrefix: 'action.', page: 1, perPage: 100 }),
        );
    });

    it('walks every page until the accumulated set covers the reported total', async () => {
        listSpy
            .mockResolvedValueOnce({
                data: Array.from({ length: 100 }, (_, i) => makeEntity(`p1-${i}`)),
                total: 250,
                page: 1,
                perPage: 100,
            })
            .mockResolvedValueOnce({
                data: Array.from({ length: 100 }, (_, i) => makeEntity(`p2-${i}`)),
                total: 250,
                page: 2,
                perPage: 100,
            })
            .mockResolvedValueOnce({
                data: Array.from({ length: 50 }, (_, i) => makeEntity(`p3-${i}`)),
                total: 250,
                page: 3,
                perPage: 100,
            });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('fetched').textContent).toBe('250'));
        expect(getByTestId('truncated').textContent).toBe('false');
        expect(listSpy).toHaveBeenCalledTimes(3);
        expect(listSpy.mock.calls.map(c => (c[1] as { page: number }).page)).toEqual([1, 2, 3]);
    });

    it('stops early when the server returns an empty page (defensive against inconsistent total)', async () => {
        listSpy
            .mockResolvedValueOnce({ data: [makeEntity('a')], total: 999, page: 1, perPage: 100 })
            .mockResolvedValueOnce({ data: [], total: 999, page: 2, perPage: 100 });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('fetched').textContent).toBe('1'));
        expect(listSpy).toHaveBeenCalledTimes(2);
    });

    it('reports truncated=true when the MAX_PAGES safety cap is hit before all rows are fetched', async () => {
        // MAX_PAGES = 50, perPage = 100 → fetch 5000 of a larger total.
        listSpy.mockImplementation((_env: string, params?: { page: number }) =>
            Promise.resolve({
                data: Array.from({ length: 100 }, (_, i) => makeEntity(`p${params?.page}-${i}`)),
                total: 100000,
                page: params?.page ?? 1,
                perPage: 100,
            }),
        );

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('truncated').textContent).toBe('true'));
        expect(listSpy).toHaveBeenCalledTimes(50);
        expect(getByTestId('fetched').textContent).toBe('5000');
    });

    it('coalesces a nullish data slice to an empty page instead of crashing', async () => {
        listSpy.mockResolvedValue({ data: undefined, total: 3, page: 1, perPage: 100 });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('loading').textContent).toBe('false'));
        expect(getByTestId('error').textContent).toBe('none');
        expect(getByTestId('fetched').textContent).toBe('0');
        expect(listSpy).toHaveBeenCalledTimes(1);
    });

    it('exposes an error string when the query rejects', async () => {
        listSpy.mockRejectedValueOnce(new Error('listEntities down'));

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('error').textContent).toBe('listEntities down'));
    });
});
