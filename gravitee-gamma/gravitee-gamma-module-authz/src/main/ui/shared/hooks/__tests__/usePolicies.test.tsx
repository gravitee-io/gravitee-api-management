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
import { usePolicies, type UsePoliciesResult } from '../usePolicies';

const listSpy = vi.fn();
const createSpy = vi.fn();
const updateSpy = vi.fn();
const deleteSpy = vi.fn();
vi.mock('../../api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listPolicies: (env: string, params?: unknown) => listSpy(env, params),
        createPolicy: (env: string, req: unknown) => createSpy(env, req),
        updatePolicy: (env: string, id: string, req: unknown) => updateSpy(env, id, req),
        deletePolicy: (env: string, id: string) => deleteSpy(env, id),
    },
}));

function makeWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function Probe({ env, type }: { env: string; type?: 'MCP' | 'AGENT' | 'LLM' | 'API' | 'EVENT' | 'CUSTOM' }) {
    const state = usePolicies(env, { type });
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
    createSpy.mockReset();
    updateSpy.mockReset();
    deleteSpy.mockReset();
});

describe('usePolicies', () => {
    it('does not fire the query when environmentId is empty', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        render(<Probe env="" type="MCP" />, { wrapper: makeWrapper() });

        // Give react-query a few ticks to schedule any pending fetch.
        await Promise.resolve();
        await Promise.resolve();
        expect(listSpy).not.toHaveBeenCalled();
    });

    it('calls listPolicies with env + type on mount', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        const { getByTestId } = render(<Probe env="env-1" type="MCP" />, { wrapper: makeWrapper() });
        await waitFor(() => expect(getByTestId('total').textContent).toBe('0'));
        expect(listSpy).toHaveBeenCalledWith('env-1', { page: 1, perPage: 10, type: 'MCP', status: undefined });
    });

    it('captures error into state', async () => {
        listSpy.mockRejectedValue(new Error('boom'));
        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });
        await waitFor(() => expect(getByTestId('error').textContent).toBe('boom'));
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
        const { unmount } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });
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
            const state = usePolicies('env-pp', { initialPerPage: perPage });
            return <span data-testid="total">{state.data?.total ?? 'null'}</span>;
        }

        const wrapper = makeWrapper();
        const { rerender, getByTestId } = render(<PerPageProbe perPage={10} />, { wrapper });
        await waitFor(() => expect(getByTestId('total').textContent).toBe('50'));

        rerender(<PerPageProbe perPage={50} />);
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(2));

        expect(listSpy).toHaveBeenNthCalledWith(1, 'env-pp', { page: 1, perPage: 10, type: undefined, status: undefined });
        expect(listSpy).toHaveBeenNthCalledWith(2, 'env-pp', { page: 1, perPage: 50, type: undefined, status: undefined });
    });

    it('internal setPerPage still works', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });

        const ref: { current: UsePoliciesResult | undefined } = { current: undefined };
        function CaptureProbe() {
            const state = usePolicies('env-set', { initialPerPage: 10 });
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
        expect(listSpy).toHaveBeenNthCalledWith(2, 'env-set', { page: 1, perPage: 25, type: undefined, status: undefined });
    });

    it('ignores stale response after env change', async () => {
        let resolveStale: (value: unknown) => void = () => undefined;
        listSpy.mockImplementationOnce(
            () =>
                new Promise(resolve => {
                    resolveStale = resolve;
                }),
        );
        listSpy.mockImplementationOnce(() => Promise.resolve({ data: [], total: 7, page: 1, perPage: 10 }));

        const wrapper = makeWrapper();
        const { rerender, getByTestId } = render(<Probe env="env-A" />, { wrapper });
        rerender(<Probe env="env-B" />);

        await waitFor(() => expect(getByTestId('total').textContent).toBe('7'));

        resolveStale({ data: [], total: 999, page: 1, perPage: 10 });
        await Promise.resolve();
        await Promise.resolve();
        expect(getByTestId('total').textContent).toBe('7');
    });

    it('create() forwards payload + invalidates list', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        createSpy.mockResolvedValue({ id: 'p1', name: 'Allow' });

        const ref: { current: UsePoliciesResult | undefined } = { current: undefined };
        function CaptureProbe() {
            const state = usePolicies('env-c', {});
            useEffect(() => {
                ref.current = state;
            });
            return null;
        }

        render(<CaptureProbe />, { wrapper: makeWrapper() });
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(1));

        const payload = { name: 'Allow', type: 'MCP', target: null, policyText: '' };
        await act(async () => {
            await ref.current?.create(payload as never);
        });

        expect(createSpy).toHaveBeenCalledWith('env-c', payload);
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(2));
    });

    it('update() forwards id + payload + invalidates list', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        updateSpy.mockResolvedValue({ id: 'p1', name: 'Updated' });

        const ref: { current: UsePoliciesResult | undefined } = { current: undefined };
        function CaptureProbe() {
            const state = usePolicies('env-u', {});
            useEffect(() => {
                ref.current = state;
            });
            return null;
        }

        render(<CaptureProbe />, { wrapper: makeWrapper() });
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(1));

        const payload = { name: 'Updated', type: 'MCP', target: null, policyText: '' };
        await act(async () => {
            await ref.current?.update('p1', payload as never);
        });

        expect(updateSpy).toHaveBeenCalledWith('env-u', 'p1', payload);
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(2));
    });

    it('remove() forwards id + invalidates list', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        deleteSpy.mockResolvedValue(undefined);

        const ref: { current: UsePoliciesResult | undefined } = { current: undefined };
        function CaptureProbe() {
            const state = usePolicies('env-r', {});
            useEffect(() => {
                ref.current = state;
            });
            return null;
        }

        render(<CaptureProbe />, { wrapper: makeWrapper() });
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(1));

        await act(async () => {
            await ref.current?.remove('p1');
        });

        expect(deleteSpy).toHaveBeenCalledWith('env-r', 'p1');
        await waitFor(() => expect(listSpy).toHaveBeenCalledTimes(2));
    });

    it('exposes isCreating / isUpdating / isRemoving flags', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        const ref: { current: UsePoliciesResult | undefined } = { current: undefined };
        function CaptureProbe() {
            const state = usePolicies('env-flags', {});
            useEffect(() => {
                ref.current = state;
            });
            return null;
        }
        render(<CaptureProbe />, { wrapper: makeWrapper() });
        await waitFor(() => expect(ref.current).toBeDefined());

        expect(ref.current?.isCreating).toBe(false);
        expect(ref.current?.isUpdating).toBe(false);
        expect(ref.current?.isRemoving).toBe(false);
    });
});
