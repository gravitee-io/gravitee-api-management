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
import type { CatalogItemKind, ModelCatalogItem } from '../../api/aim-catalog.types';
import { useAimCatalogItems } from '../useAimCatalogItems';

const listItemsSpy = vi.fn();

vi.mock('../../api/aim-catalog.service', () => ({
    aimCatalogService: {
        listItems: (env: string, kind: string, page: number, perPage: number) => listItemsSpy(env, kind, page, perPage),
    },
}));

function makeWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function Probe({ env, kind }: { env: string; kind: CatalogItemKind }) {
    const state = useAimCatalogItems<ModelCatalogItem>(env, kind);
    return (
        <div>
            <span data-testid="loading">{String(state.isLoading)}</span>
            <span data-testid="total">{state.data?.total ?? 'null'}</span>
            <span data-testid="fetched">{state.data?.data.length ?? 'null'}</span>
            <span data-testid="truncated">{String(state.truncated)}</span>
            <span data-testid="error">{state.error ?? 'none'}</span>
        </div>
    );
}

function makeModel(id: string): ModelCatalogItem {
    return {
        id,
        kind: 'model',
        sourceId: null,
        sourceKind: null,
        parentId: null,
        environmentId: 'DEFAULT',
        organizationId: 'DEFAULT',
        creationDate: '2026-05-27T10:00:00.000Z',
        updateDate: '2026-05-27T10:00:00.000Z',
        definition: { name: id, queryName: id },
    };
}

beforeEach(() => {
    listItemsSpy.mockReset();
});

describe('useAimCatalogItems', () => {
    it('does not fire the query when environmentId is empty', async () => {
        listItemsSpy.mockResolvedValue({ data: [], page: 1, perPage: 500, total: 0 });
        render(<Probe env="" kind="model" />, { wrapper: makeWrapper() });

        await Promise.resolve();
        await Promise.resolve();
        expect(listItemsSpy).not.toHaveBeenCalled();
    });

    it('issues a single call when the first page already covers the total', async () => {
        listItemsSpy.mockResolvedValue({ data: [makeModel('m1'), makeModel('m2')], page: 1, perPage: 500, total: 2 });

        const { getByTestId } = render(<Probe env="env-1" kind="model" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('total').textContent).toBe('2'));
        expect(getByTestId('fetched').textContent).toBe('2');
        expect(getByTestId('truncated').textContent).toBe('false');
        expect(listItemsSpy).toHaveBeenCalledTimes(1);
        expect(listItemsSpy).toHaveBeenCalledWith('env-1', 'model', 1, 500);
    });

    it('loops through pages until the accumulated set covers the reported total', async () => {
        listItemsSpy
            .mockResolvedValueOnce({ data: Array.from({ length: 500 }, (_, i) => makeModel(`m${i}`)), page: 1, perPage: 500, total: 1200 })
            .mockResolvedValueOnce({
                data: Array.from({ length: 500 }, (_, i) => makeModel(`m${500 + i}`)),
                page: 2,
                perPage: 500,
                total: 1200,
            })
            .mockResolvedValueOnce({
                data: Array.from({ length: 200 }, (_, i) => makeModel(`m${1000 + i}`)),
                page: 3,
                perPage: 500,
                total: 1200,
            });

        const { getByTestId } = render(<Probe env="env-1" kind="model" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('fetched').textContent).toBe('1200'));
        expect(getByTestId('truncated').textContent).toBe('false');
        expect(listItemsSpy).toHaveBeenCalledTimes(3);
        expect(listItemsSpy.mock.calls.map(c => c[2])).toEqual([1, 2, 3]);
    });

    it('reports truncated=true when MAX_PAGES safety cap is hit before all items are fetched', async () => {
        // MAX_PAGES = 20, PER_PAGE = 500 → fetch 10000, total 12000.
        listItemsSpy.mockImplementation((_env: string, _kind: string, page: number) =>
            Promise.resolve({
                data: Array.from({ length: 500 }, (_, i) => makeModel(`p${page}-i${i}`)),
                page,
                perPage: 500,
                total: 12000,
            }),
        );

        const { getByTestId } = render(<Probe env="env-1" kind="model" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('truncated').textContent).toBe('true'));
        expect(listItemsSpy).toHaveBeenCalledTimes(20);
        expect(getByTestId('fetched').textContent).toBe('10000');
        expect(getByTestId('total').textContent).toBe('12000');
    });

    it('stops early when the server returns an empty page (defensive against inconsistent total)', async () => {
        listItemsSpy
            .mockResolvedValueOnce({ data: [makeModel('m1')], page: 1, perPage: 500, total: 999 })
            .mockResolvedValueOnce({ data: [], page: 2, perPage: 500, total: 999 });

        const { getByTestId } = render(<Probe env="env-1" kind="model" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('fetched').textContent).toBe('1'));
        expect(listItemsSpy).toHaveBeenCalledTimes(2);
    });

    it('treats a nullish data slice as an empty page instead of crashing', async () => {
        // A garbled/partial payload may report a total but omit `data`. The hook
        // must not spread `undefined` — it should coalesce to [] and stop.
        listItemsSpy.mockResolvedValue({ data: undefined, page: 1, perPage: 500, total: 5 });

        const { getByTestId } = render(<Probe env="env-1" kind="model" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('loading').textContent).toBe('false'));
        expect(getByTestId('error').textContent).toBe('none');
        expect(getByTestId('fetched').textContent).toBe('0');
        expect(listItemsSpy).toHaveBeenCalledTimes(1);
    });

    it('exposes an error message when the service rejects', async () => {
        listItemsSpy.mockRejectedValue(new Error('catalog unreachable'));

        const { getByTestId } = render(<Probe env="env-1" kind="agent" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('error').textContent).toBe('catalog unreachable'));
    });

    it('issues a separate query per kind for the same environment', async () => {
        listItemsSpy.mockResolvedValue({ data: [], page: 1, perPage: 500, total: 0 });

        function ThreeProbes() {
            return (
                <div>
                    <Probe env="env-1" kind="model" />
                    <Probe env="env-1" kind="mcp-server" />
                    <Probe env="env-1" kind="agent" />
                </div>
            );
        }
        render(<ThreeProbes />, { wrapper: makeWrapper() });

        await waitFor(() => expect(listItemsSpy).toHaveBeenCalledTimes(3));
        const kinds = listItemsSpy.mock.calls.map(args => args[1]).sort();
        expect(kinds).toEqual(['agent', 'mcp-server', 'model']);
    });
});
