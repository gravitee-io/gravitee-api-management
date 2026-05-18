import { act, render, waitFor } from '@testing-library/react';
import { useEffect } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useEntities, type UseEntitiesResult } from '../useEntities';

const listSpy = vi.fn();
const createSpy = vi.fn();

vi.mock('../../api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listEntities: (env: string, params?: unknown) => listSpy(env, params),
        createEntity: (env: string, req: unknown) => createSpy(env, req),
        updateEntity: vi.fn(),
        deleteEntity: vi.fn(),
    },
}));

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
    createSpy.mockReset();
});

describe('useEntities', () => {
    it('loads entities on mount with default pagination', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });

        const { getByTestId } = render(<Probe env="env-1" />);

        await waitFor(() => expect(getByTestId('total').textContent).toBe('0'));
        expect(listSpy).toHaveBeenCalledWith('env-1', { page: 1, perPage: 10 });
    });

    it('sets error when listEntities fails', async () => {
        listSpy.mockRejectedValue(new Error('fail'));

        const { getByTestId } = render(<Probe env="env-1" />);

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
        const { unmount } = render(<Probe env="env-unmount" />);
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

        const { rerender, getByTestId } = render(<PerPageProbe perPage={10} />);
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

        render(<CaptureProbe />);
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

        const { rerender, getByTestId } = render(<Probe env="env-A" />);
        rerender(<Probe env="env-B" />);

        await waitFor(() => expect(getByTestId('total').textContent).toBe('9'));

        resolveStale({ data: [], total: 999, page: 1, perPage: 10 });
        await Promise.resolve();
        await Promise.resolve();
        expect(getByTestId('total').textContent).toBe('9');
    });
});
