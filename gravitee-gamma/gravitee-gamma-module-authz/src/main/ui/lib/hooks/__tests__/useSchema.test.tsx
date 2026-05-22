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
import { render, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../api/authz-api-client';
import { useSchema } from '../useSchema';

const getSchemaSpy = vi.fn();

vi.mock('../../api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        getSchema: (env: string) => getSchemaSpy(env),
    },
}));

function Probe({ env }: { env: string }) {
    const state = useSchema(env);
    return (
        <div>
            <span data-testid="loading">{String(state.isLoading)}</span>
            <span data-testid="schemaText">{state.schema?.schemaText ?? 'null'}</span>
            <span data-testid="notFound">{String(state.notFound)}</span>
            <span data-testid="error">{state.error ?? 'none'}</span>
        </div>
    );
}

beforeEach(() => {
    getSchemaSpy.mockReset();
});

describe('useSchema', () => {
    it('loads schema on mount', async () => {
        getSchemaSpy.mockResolvedValue({
            environmentId: 'env-1',
            schemaText: 'entity User {};',
            updatedAt: '2026-04-24T00:00:00Z',
        });

        const { getByTestId } = render(<Probe env="env-1" />);

        await waitFor(() => expect(getByTestId('schemaText').textContent).toBe('entity User {};'));
        expect(getByTestId('notFound').textContent).toBe('false');
    });

    it('treats 404 as notFound, not error', async () => {
        getSchemaSpy.mockRejectedValue(new ApiError(404, 'Not found'));

        const { getByTestId } = render(<Probe env="env-1" />);

        await waitFor(() => expect(getByTestId('notFound').textContent).toBe('true'));
        expect(getByTestId('error').textContent).toBe('none');
    });

    it('captures non-404 errors', async () => {
        getSchemaSpy.mockRejectedValue(new Error('boom'));

        const { getByTestId } = render(<Probe env="env-1" />);

        await waitFor(() => expect(getByTestId('error').textContent).toBe('boom'));
    });

    it('does not warn after unmount during in-flight fetch', async () => {
        let resolveFn: (value: unknown) => void = () => undefined;
        getSchemaSpy.mockImplementation(
            () =>
                new Promise(resolve => {
                    resolveFn = resolve;
                }),
        );
        const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const { unmount } = render(<Probe env="env-1" />);
        unmount();

        resolveFn({ environmentId: 'env-1', schemaText: 'x', updatedAt: '' });
        await Promise.resolve();
        await Promise.resolve();

        const noisy = errorSpy.mock.calls.some(args => args.some(a => typeof a === 'string' && a.includes('unmounted')));
        expect(noisy).toBe(false);
        errorSpy.mockRestore();
    });

    it('ignores stale response after env change', async () => {
        let resolveStale: (value: unknown) => void = () => undefined;
        getSchemaSpy.mockImplementationOnce(
            () =>
                new Promise(resolve => {
                    resolveStale = resolve;
                }),
        );
        getSchemaSpy.mockImplementationOnce(() =>
            Promise.resolve({
                environmentId: 'env-B',
                schemaText: 'fresh',
                updatedAt: '2026-01-01',
            }),
        );

        const { rerender, getByTestId } = render(<Probe env="env-A" />);
        rerender(<Probe env="env-B" />);

        await waitFor(() => expect(getByTestId('schemaText').textContent).toBe('fresh'));

        resolveStale({ environmentId: 'env-A', schemaText: 'stale', updatedAt: '' });
        await Promise.resolve();
        await Promise.resolve();
        expect(getByTestId('schemaText').textContent).toBe('fresh');
    });
});
