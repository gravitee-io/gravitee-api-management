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
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { EntityResponse } from '../../../shared/api/authz-api.types';
import { EntitiesPage } from '../EntitiesPage';

// jsdom doesn't implement scrollIntoView; Radix Select calls it on item activation.
beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

const listEntitiesSpy = vi.fn();

vi.mock('@gravitee/gamma-modules-sdk', async importOriginal => ({
    ...(await importOriginal<object>()),
    useEnvironment: () => ({ id: 'DEFAULT' }),
}));

vi.mock('../../../shared/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listEntities: (env: string, params?: unknown) => listEntitiesSpy(env, params),
    },
}));

function makeEntity(overrides: Partial<EntityResponse> = {}): EntityResponse {
    return {
        id: overrides.id ?? `ent-${Math.random().toString(36).slice(2, 8)}`,
        environmentId: 'DEFAULT',
        uid: 'user.alice',
        attributes: {},
        parents: [],
        createdAt: '2026-04-27T10:00:00.000Z',
        updatedAt: '2026-04-27T10:00:00.000Z',
        ...overrides,
    };
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    return render(<EntitiesPage />, { wrapper });
}

beforeEach(() => {
    listEntitiesSpy.mockReset();
});

describe('EntitiesPage', () => {
    it('shows the empty principals state when the backend returns no entities', async () => {
        listEntitiesSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        renderPage();

        await waitFor(() => {
            expect(screen.getByText('No principals yet')).toBeInTheDocument();
        });
    });

    it('renders KPI tiles with correct counts and entity rows', async () => {
        listEntitiesSpy.mockResolvedValue({
            data: [
                makeEntity({ id: 'p1', uid: 'user.alice' }),
                makeEntity({ id: 'p2', uid: 'group.eng' }),
                makeEntity({ id: 'r1', uid: 'mcp.flight' }),
                makeEntity({ id: 'r2', uid: 'api.payments' }),
            ],
            total: 4,
            page: 1,
            perPage: 10,
        });
        renderPage();

        await waitFor(() => {
            expect(screen.getAllByText('alice').length).toBeGreaterThan(0);
        });
        expect(screen.getAllByText('eng').length).toBeGreaterThan(0);

        const totalTile = screen.getByLabelText('Total entities');
        expect(within(totalTile).getByText('4')).toBeInTheDocument();

        const principalsTile = screen.getByLabelText('Principals');
        expect(within(principalsTile).getByText('2')).toBeInTheDocument();

        const resourcesTile = screen.getByLabelText('Resources');
        expect(within(resourcesTile).getByText('2')).toBeInTheDocument();
    });

    it('switches to the Resources tab and shows resource entities', async () => {
        listEntitiesSpy.mockResolvedValue({
            data: [makeEntity({ id: 'p1', uid: 'user.alice' }), makeEntity({ id: 'r1', uid: 'mcp.flight' })],
            total: 2,
            page: 1,
            perPage: 10,
        });
        renderPage();

        await waitFor(() => expect(screen.getAllByText('alice').length).toBeGreaterThan(0));

        const resourcesTab = screen.getByRole('tab', { name: /Resources/i });
        const user = userEvent.setup();
        await user.click(resourcesTab);

        await waitFor(() => {
            expect(screen.getAllByText('flight').length).toBeGreaterThan(0);
        });
    });

    it('filters principals by search input', async () => {
        listEntitiesSpy.mockResolvedValue({
            data: [makeEntity({ id: 'p1', uid: 'user.alice' }), makeEntity({ id: 'p2', uid: 'user.bob' })],
            total: 2,
            page: 1,
            perPage: 10,
        });
        renderPage();

        await waitFor(() => expect(screen.getAllByText('alice').length).toBeGreaterThan(0));
        expect(screen.getAllByText('bob').length).toBeGreaterThan(0);

        const search = screen.getByLabelText('Search principals');
        fireEvent.change(search, { target: { value: 'alice' } });

        await waitFor(() => {
            expect(screen.queryByText('bob')).not.toBeInTheDocument();
            expect(screen.getAllByText('alice').length).toBeGreaterThan(0);
        });
    });
});
