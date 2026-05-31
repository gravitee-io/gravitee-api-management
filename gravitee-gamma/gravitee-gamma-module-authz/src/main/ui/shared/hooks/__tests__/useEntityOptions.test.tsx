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
    it('does not fire the query when environmentId is empty', async () => {
        listEntitiesSpy.mockResolvedValue(paged([]));
        render(<Probe env="" />, { wrapper: makeWrapper() });

        await Promise.resolve();
        await Promise.resolve();
        expect(listEntitiesSpy).not.toHaveBeenCalled();
    });

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
                entity('user.alice', { email: 'alice@example.com' }),
                entity('group.admins'),
                entity('serviceaccount.deploy-bot', { _source: 'imported', role: 'ci' }),
            ]),
        );

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('count').textContent).toBe('3'));
        const items = getByTestId('options').querySelectorAll('li');
        expect(items[0].getAttribute('data-group')).toBe('User');
        // Label prefers a readable attribute (email here) over the entity id.
        expect(items[0].getAttribute('data-label')).toBe('alice@example.com');
        expect(items[0].getAttribute('data-description')).toBe('email=alice@example.com');
        expect(items[1].getAttribute('data-group')).toBe('Group');
        expect(items[1].getAttribute('data-description')).toBe('');
        expect(items[2].getAttribute('data-group')).toBe('ServiceAccount');
        // Meta keys prefixed with `_` must be skipped in the description.
        expect(items[2].getAttribute('data-description')).toBe('role=ci');
    });

    it('labels a synced principal by its readable attribute, not the entity id', async () => {
        const sub = '0d3f8c8a-1b2c-3d4e-5f60-718293a4b5c6';
        listEntitiesSpy.mockResolvedValue(
            paged([
                // displayName wins over email/username
                entity(`user.${sub}`, { sub, displayName: 'Alice Smith', email: 'alice@example.com', username: 'asmith' }),
                // email is next when displayName is absent
                entity('user.u2', { email: 'bob@example.com', username: 'bob' }),
                // falls back to the id when no readable attribute is present
                entity('user.u3', { enabled: true }),
            ]),
        );

        const { getByTestId } = render(<Probe env="env-1" />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('count').textContent).toBe('3'));
        const items = getByTestId('options').querySelectorAll('li');
        expect(items[0].getAttribute('data-label')).toBe('Alice Smith');
        // The unreadable sub stays reachable via the GAPL ref used to match at eval time.
        expect(items[0].textContent).toBe(`User::"${sub}"`);
        expect(items[1].getAttribute('data-label')).toBe('bob@example.com');
        expect(items[2].getAttribute('data-label')).toBe('u3');
    });

    it('issues a single kind=PRINCIPAL fetch and filters client-side when typeFilter is a principal subset', async () => {
        listEntitiesSpy.mockImplementation((_env: string, params: { kind?: string } = {}) => {
            if (params.kind === 'PRINCIPAL') {
                return Promise.resolve(paged([entity('user.alice'), entity('group.admins')]));
            }
            return Promise.resolve(paged([]));
        });

        const { getByTestId } = render(<Probe env="env-1" opts={{ typeFilter: ['User'] }} />, { wrapper: makeWrapper() });

        await waitFor(() => expect(getByTestId('count').textContent).toBe('1'));
        const items = getByTestId('options').querySelectorAll('li');
        expect(items[0].getAttribute('data-group')).toBe('User');
        // One umbrella request — backend already groups principals by kind.
        expect(listEntitiesSpy).toHaveBeenCalledTimes(1);
        expect(listEntitiesSpy).toHaveBeenCalledWith('env-1', { page: 1, perPage: 200, kind: 'PRINCIPAL' });
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
        resolveFn(paged([entity('user.alice')]));
        await Promise.resolve();
        await Promise.resolve();

        const noisy = errorSpy.mock.calls.some(args => args.some(a => typeof a === 'string' && a.includes('unmounted')));
        expect(noisy).toBe(false);
        errorSpy.mockRestore();
    });

    it('does not refetch when typeFilter changes between principal types — same kind=PRINCIPAL cache entry', async () => {
        listEntitiesSpy.mockResolvedValue(paged([entity('user.alice'), entity('group.admins')]));

        const wrapper = makeWrapper();
        const { rerender, getByTestId } = render(<Probe env="env-1" opts={{ typeFilter: ['User'] }} />, { wrapper });
        await waitFor(() => expect(getByTestId('count').textContent).toBe('1'));
        expect(listEntitiesSpy).toHaveBeenCalledTimes(1);

        // Same filter — no refetch.
        rerender(<Probe env="env-1" opts={{ typeFilter: ['User'] }} />);
        await Promise.resolve();
        expect(listEntitiesSpy).toHaveBeenCalledTimes(1);

        // Different principal type — still one cache entry (kind=PRINCIPAL), client-side re-derives.
        rerender(<Probe env="env-1" opts={{ typeFilter: ['Group'] }} />);
        await waitFor(() => {
            const items = getByTestId('options').querySelectorAll('li');
            expect(items.length).toBe(1);
            expect(items[0].getAttribute('data-group')).toBe('Group');
        });
        expect(listEntitiesSpy).toHaveBeenCalledTimes(1);
    });
});
