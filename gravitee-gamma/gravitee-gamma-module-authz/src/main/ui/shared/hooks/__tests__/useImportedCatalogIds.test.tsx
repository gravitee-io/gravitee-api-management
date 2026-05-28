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
import { useImportedCatalogIds } from '../useImportedCatalogIds';

const listSpy = vi.fn();

vi.mock('../../api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listEntities: (env: string, params?: unknown) => listSpy(env, params),
    },
}));

function makeWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function Probe({ env }: { env: string }) {
    const { catalogIds, isLoading, error } = useImportedCatalogIds(env);
    return (
        <div>
            <span data-testid="loading">{String(isLoading)}</span>
            <span data-testid="error">{error ?? 'none'}</span>
            <span data-testid="ids">{[...catalogIds].sort().join(',')}</span>
        </div>
    );
}

function makeEntity(overrides: Partial<EntityResponse> = {}): EntityResponse {
    return {
        id: overrides.id ?? `e-${Math.random().toString(36).slice(2)}`,
        environmentId: 'DEFAULT',
        uid: 'mcp.something',
        attributes: {},
        parents: [],
        createdAt: '2026-05-27T10:00:00.000Z',
        updatedAt: '2026-05-27T10:00:00.000Z',
        ...overrides,
    };
}

beforeEach(() => {
    listSpy.mockReset();
});

describe('useImportedCatalogIds', () => {
    it('does not fire the query when environmentId is empty', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 1000 });
        render(<Probe env="" />, { wrapper: makeWrapper() });

        await Promise.resolve();
        await Promise.resolve();
        expect(listSpy).not.toHaveBeenCalled();
    });

    it('queries RESOURCE entities filtered by source=gravitee-catalog with a large page size', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 1000 });

        render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(1));
        expect(listSpy).toHaveBeenCalledWith(
            'env-1',
            expect.objectContaining({ kind: 'RESOURCE', source: 'gravitee-catalog', perPage: 1000, page: 1 }),
        );
    });

    it('extracts a set of _catalogId attribute values from the response', async () => {
        listSpy.mockResolvedValue({
            data: [
                makeEntity({ uid: 'mcp.a', attributes: { _catalogId: 'cat-1' } }),
                makeEntity({ uid: 'model.b', attributes: { _catalogId: 'cat-2', other: 'x' } }),
                makeEntity({ uid: 'agent.c', attributes: { _catalogId: 'cat-3' } }),
            ],
            total: 3,
            page: 1,
            perPage: 1000,
        });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('ids').textContent).toBe('cat-1,cat-2,cat-3'));
    });

    it('silently skips entities without a _catalogId attribute', async () => {
        listSpy.mockResolvedValue({
            data: [
                makeEntity({ uid: 'mcp.a', attributes: { _catalogId: 'cat-1' } }),
                makeEntity({ uid: 'mcp.b', attributes: {} }), // hand-created — no catalogId
                makeEntity({ uid: 'mcp.c', attributes: { _catalogId: '' } }), // empty string ignored
                makeEntity({ uid: 'mcp.d', attributes: { _catalogId: 42 as unknown as string } }), // wrong type ignored
            ],
            total: 4,
            page: 1,
            perPage: 1000,
        });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('ids').textContent).toBe('cat-1'));
    });

    it('exposes an error string when the query rejects', async () => {
        listSpy.mockRejectedValue(new Error('listEntities down'));

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('error').textContent).toBe('listEntities down'));
    });

    it('treats a nullish data slice as empty without crashing', async () => {
        // Partial payload: a total is reported but `data` is missing. The hook
        // must coalesce to [] rather than reading `.length` off undefined.
        listSpy.mockResolvedValue({ data: undefined, total: 3, page: 1, perPage: 1000 });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('loading').textContent).toBe('false'));
        expect(getByTestId('error').textContent).toBe('none');
        expect(getByTestId('ids').textContent).toBe('');
    });
});
