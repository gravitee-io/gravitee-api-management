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
import type { EntityResponse, PagedResponse } from '../../api/authz-api.types';
import { useEntityOptions, type UseEntityOptionsOpts } from '../useEntityOptions';

const listEntitiesSpy = vi.fn();
vi.mock('../../api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listEntities: (env: string, params?: unknown) => listEntitiesSpy(env, params),
    },
}));

function makeWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function entity(uid: string, attributes: Record<string, unknown> = {}): EntityResponse {
    return {
        id: `id-${uid}`,
        environmentId: 'DEFAULT',
        uid,
        attributes,
        parents: [],
        createdAt: new Date(0).toISOString(),
        updatedAt: new Date(0).toISOString(),
    };
}

function paged(data: readonly EntityResponse[], total = data.length): PagedResponse<EntityResponse> {
    return { data, total, page: 1, perPage: 200 };
}

interface ProbeProps {
    readonly env: string;
    readonly opts?: UseEntityOptionsOpts;
}

function Probe({ env, opts }: ProbeProps) {
    const state = useEntityOptions(env, opts);
    return (
        <div>
            <span data-testid="loading">{String(state.isLoading)}</span>
            <span data-testid="count">{state.options.length}</span>
            <span data-testid="error">{state.error ?? 'none'}</span>
            <ul data-testid="options">
                {state.options.map(o => (
                    <li key={o.id} data-group={o.group} data-label={o.label} data-description={o.description ?? ''}>
                        {o.id}
                    </li>
                ))}
            </ul>
        </div>
    );
}

beforeEach(() => {
    listEntitiesSpy.mockReset();
});

describe('useEntityOptions', () => {
    it('returns empty options and no error for an empty list', async () => {
        listEntitiesSpy.mockResolvedValue(paged([]));

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('loading').textContent).toBe('false'));
        expect(getByTestId('count').textContent).toBe('0');
        expect(getByTestId('error').textContent).toBe('none');
        expect(listEntitiesSpy).toHaveBeenCalledWith('env-1', { page: 1, perPage: 200 });
    });

    it('groups options by entity type parsed from uid', async () => {
        listEntitiesSpy.mockResolvedValue(
            paged([
                entity('User::"alice"', { email: 'alice@example.com' }),
                entity('Group::"admins"'),
                entity('ServiceAccount::"deploy-bot"', { _source: 'imported', role: 'ci' }),
            ]),
        );

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('count').textContent).toBe('3'));
        const items = getByTestId('options').querySelectorAll('li');
        expect(items[0].getAttribute('data-group')).toBe('User');
        expect(items[0].getAttribute('data-label')).toBe('alice');
        expect(items[0].id || items[0].textContent).toBe('User::"alice"');
        expect(items[0].getAttribute('data-description')).toBe('email=alice@example.com');
        expect(items[1].getAttribute('data-group')).toBe('Group');
        expect(items[1].getAttribute('data-description')).toBe('');
        expect(items[2].getAttribute('data-group')).toBe('ServiceAccount');
        // Meta keys prefixed with `_` must be skipped in the description.
        expect(items[2].getAttribute('data-description')).toBe('role=ci');
    });

    it('filters options by typeFilter', async () => {
        listEntitiesSpy.mockResolvedValue(paged([entity('User::"alice"'), entity('Group::"admins"'), entity('AgentIdentity::"agent-1"')]));

        const { getByTestId } = render(<Probe env="env-1" opts={{ typeFilter: ['User'] }} />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('count').textContent).toBe('1'));
        const items = getByTestId('options').querySelectorAll('li');
        expect(items[0].getAttribute('data-group')).toBe('User');
    });

    it('sets error when total exceeds page size and still returns the loaded slice', async () => {
        const data = Array.from({ length: 200 }, (_, i) => entity(`User::"u${i}"`));
        listEntitiesSpy.mockResolvedValue(paged(data, 503));

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('count').textContent).toBe('200'));
        expect(getByTestId('error').textContent).toMatch(/too many entities/i);
    });

    it('does not warn after unmount during in-flight fetch', async () => {
        let resolveFn: (v: PagedResponse<EntityResponse>) => void = () => undefined;
        listEntitiesSpy.mockImplementation(
            () =>
                new Promise<PagedResponse<EntityResponse>>(resolve => {
                    resolveFn = resolve;
                }),
        );
        const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

        const { unmount } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });
        unmount();
        resolveFn(paged([entity('User::"alice"')]));
        await Promise.resolve();
        await Promise.resolve();

        const noisy = errorSpy.mock.calls.some(args => args.some(a => typeof a === 'string' && a.includes('unmounted')));
        expect(noisy).toBe(false);
        errorSpy.mockRestore();
    });

    it('refetches when typeFilter changes by value (not reference)', async () => {
        listEntitiesSpy.mockResolvedValue(paged([entity('User::"alice"'), entity('Group::"admins"')]));

        const wrapper = makeWrapper();
        const { rerender, getByTestId } = render(<Probe env="env-1" opts={{ typeFilter: ['User'] }} />, { wrapper });
        await waitFor(() => expect(getByTestId('count').textContent).toBe('1'));
        const callsAfterFirst = listEntitiesSpy.mock.calls.length;

        // Same values, fresh array literal — must NOT refetch.
        rerender(<Probe env="env-1" opts={{ typeFilter: ['User'] }} />);
        await Promise.resolve();
        expect(listEntitiesSpy.mock.calls.length).toBe(callsAfterFirst);

        // Different filter — must refetch (or at least re-derive options).
        rerender(<Probe env="env-1" opts={{ typeFilter: ['Group'] }} />);
        await waitFor(() => {
            const items = getByTestId('options').querySelectorAll('li');
            expect(items.length).toBe(1);
            expect(items[0].getAttribute('data-group')).toBe('Group');
        });
    });
});
