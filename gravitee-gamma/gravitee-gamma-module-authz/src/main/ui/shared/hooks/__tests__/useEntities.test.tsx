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
import { act, render, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { useEffect } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useEntities, type UseEntitiesResult } from '../useEntities';

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
    const state = useEntities(env);
    return (
        <div>
            <span data-testid="loading">{String(state.isLoading)}</span>
            <span data-testid="total">{state.data?.total ?? 'null'}</span>
            <span data-testid="error">{state.error ?? 'none'}</span>
        </div>
    );
}

beforeEach(() => {
    listSpy.mockReset();
});

describe('useEntities', () => {
    it('does not fire the query when environmentId is empty', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        render(<Probe env="" />, { wrapper: makeWrapper() });

        await Promise.resolve();
        await Promise.resolve();
        expect(listSpy).not.toHaveBeenCalled();
    });

    it('loads entities on mount with default pagination', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('total').textContent).toBe('0'));
        expect(listSpy).toHaveBeenCalledWith('env-1', { page: 1, perPage: 10 });
    });

    it('sets error when listEntities fails', async () => {
        listSpy.mockRejectedValue(new Error('fail'));

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('error').textContent).toBe('fail'));
    });

    it('does not warn after unmount during in-flight fetch', async () => {
        let resolveFn: (value: unknown) => void = () => undefined;
        listSpy.mockImplementation(
            () =>
                new Promise(resolve => {
                    resolveFn = resolve;
                }),
        );

        const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const { unmount } = render(<Probe env="env-unmount" />, { wrapper: makeWrapper() });
        unmount();

        resolveFn({ data: [], total: 0, page: 1, perPage: 10 });
        await Promise.resolve();
        await Promise.resolve();

        const noisy = errorSpy.mock.calls.some(args => args.some(a => typeof a === 'string' && a.includes('unmounted')));
        expect(noisy).toBe(false);
        errorSpy.mockRestore();
    });

    it('refetches when initialPerPage changes', async () => {
        listSpy.mockResolvedValue({ data: [], total: 50, page: 1, perPage: 10 });

        function PerPageProbe({ perPage }: { perPage: number }) {
            const state = useEntities('env-pp', perPage);
            return <span data-testid="total">{state.data?.total ?? 'null'}</span>;
        }

        const wrapper = makeWrapper();
        const { rerender, getByTestId } = render(<PerPageProbe perPage={10} />, { wrapper });
        await waitFor(() => expect(getByTestId('total').textContent).toBe('50'));

        rerender(<PerPageProbe perPage={50} />);
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(2));

        expect(listSpy).toHaveBeenNthCalledWith(1, 'env-pp', { page: 1, perPage: 10 });
        expect(listSpy).toHaveBeenNthCalledWith(2, 'env-pp', { page: 1, perPage: 50 });
    });

    it('internal setPerPage still works', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });

        const ref: { current: UseEntitiesResult | undefined } = { current: undefined };
        function CaptureProbe() {
            const state = useEntities('env-set', 10);
            useEffect(() => {
                ref.current = state;
            });
            return null;
        }

        render(<CaptureProbe />, { wrapper: makeWrapper() });
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(1));

        await act(async () => {
            ref.current?.setPerPage(25);
        });

        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(2));
        expect(listSpy).toHaveBeenNthCalledWith(2, 'env-set', { page: 1, perPage: 25 });
    });

    it('ignores stale response after env change', async () => {
        let resolveStale: (value: unknown) => void = () => undefined;
        listSpy.mockImplementationOnce(
            () =>
                new Promise(resolve => {
                    resolveStale = resolve;
                }),
        );
        listSpy.mockImplementationOnce(() => Promise.resolve({ data: [], total: 9, page: 1, perPage: 10 }));

        const wrapper = makeWrapper();
        const { rerender, getByTestId } = render(<Probe env="env-A" />, { wrapper });
        rerender(<Probe env="env-B" />);

        await waitFor(() => expect(getByTestId('total').textContent).toBe('9'));

        resolveStale({ data: [], total: 999, page: 1, perPage: 10 });
        await Promise.resolve();
        await Promise.resolve();
        expect(getByTestId('total').textContent).toBe('9');
    });
});
