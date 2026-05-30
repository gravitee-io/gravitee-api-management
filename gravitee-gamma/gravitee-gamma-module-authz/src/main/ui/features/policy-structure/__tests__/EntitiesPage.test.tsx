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

const toastSuccessSpy = vi.fn();
const toastErrorSpy = vi.fn();

vi.mock('@gravitee/graphene-core', async importOriginal => {
    const actual = await importOriginal<Record<string, unknown>>();
    return {
        ...actual,
        toast: {
            success: (msg: string) => toastSuccessSpy(msg),
            error: (msg: string) => toastErrorSpy(msg),
        },
    };
});

// The dialog itself is unit-tested separately (ImportFromCatalogDialog.test.tsx);
// here we just need it to render without firing real catalog queries.
vi.mock('../CreateEntityDialog', () => ({
    CreateEntityDialog: ({ open, kind }: { open: boolean; kind: string }) =>
        open ? <div data-testid="create-entity-dialog-stub" data-kind={kind} /> : null,
}));

vi.mock('../EditEntityDialog', () => ({
    EditEntityDialog: ({ open, kind, entity }: { open: boolean; kind: string; entity: { uid: { id: string } } | null }) =>
        open ? <div data-testid="edit-entity-dialog-stub" data-kind={kind} data-entity={entity?.uid.id ?? ''} /> : null,
}));

vi.mock('../ImportFromCatalogDialog', () => ({
    ImportFromCatalogDialog: ({ open }: { open: boolean }) => (open ? <div data-testid="import-dialog-stub" /> : null),
}));

// jsdom doesn't implement scrollIntoView; Radix Select calls it on item activation.
beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

interface ListEntitiesParams {
    readonly page?: number;
    readonly perPage?: number;
    readonly kind?: 'PRINCIPAL' | 'RESOURCE';
    readonly excludeEntityIdPrefix?: string;
}

const listEntitiesSpy = vi.fn();
const deleteEntitySpy = vi.fn();

vi.mock('@gravitee/gamma-modules-sdk', async importOriginal => ({
    ...(await importOriginal<object>()),
    useEnvironment: () => ({ id: 'DEFAULT' }),
}));

vi.mock('../../../shared/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    MAX_PER_PAGE: 100,
    authzApiService: {
        listEntities: (env: string, params?: ListEntitiesParams) => listEntitiesSpy(env, params),
        deleteEntity: (env: string, entityId: string) => deleteEntitySpy(env, entityId),
        getUserSyncStatus: () => Promise.resolve(null),
        startUserSync: () => Promise.resolve({ jobId: 'job-1', status: 'PENDING' }),
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
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    return render(<EntitiesPage />, { wrapper });
}

function mockByKind(opts: {
    readonly principals?: readonly EntityResponse[];
    readonly resources?: readonly EntityResponse[];
    readonly principalTotal?: number;
    readonly resourceTotal?: number;
}) {
    const principals = opts.principals ?? [];
    const resources = opts.resources ?? [];
    const principalTotal = opts.principalTotal ?? principals.length;
    const resourceTotal = opts.resourceTotal ?? resources.length;
    listEntitiesSpy.mockImplementation((_env: string, params?: ListEntitiesParams) => {
        if (params?.kind === 'PRINCIPAL') {
            return Promise.resolve({ data: principals, total: principalTotal, page: 1, perPage: 10 });
        }
        if (params?.kind === 'RESOURCE') {
            return Promise.resolve({ data: resources, total: resourceTotal, page: 1, perPage: 10 });
        }
        return Promise.resolve({ data: [], total: 0, page: 1, perPage: 10 });
    });
}

beforeEach(() => {
    listEntitiesSpy.mockReset();
    deleteEntitySpy.mockReset();
    toastSuccessSpy.mockReset();
    toastErrorSpy.mockReset();
});

describe('EntitiesPage', () => {
    it('issues two server-side queries — PRINCIPAL and RESOURCE excluding action.*', async () => {
        mockByKind({});
        renderPage();

        await waitFor(() => expect(listEntitiesSpy).toHaveBeenCalledTimes(2));

        expect(listEntitiesSpy).toHaveBeenCalledWith(
            'DEFAULT',
            expect.objectContaining({ kind: 'PRINCIPAL', excludeEntityIdPrefix: undefined }),
        );
        expect(listEntitiesSpy).toHaveBeenCalledWith(
            'DEFAULT',
            expect.objectContaining({ kind: 'RESOURCE', excludeEntityIdPrefix: 'action.' }),
        );
    });

    it('shows the empty principals state when the backend returns no entities', async () => {
        mockByKind({});
        renderPage();

        await waitFor(() => {
            expect(screen.getByText('No principals yet')).toBeInTheDocument();
        });
    });

    it('renders KPI tiles using backend totals from both queries', async () => {
        mockByKind({
            principals: [makeEntity({ id: 'p1', uid: 'user.alice' }), makeEntity({ id: 'p2', uid: 'group.eng' })],
            resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' }), makeEntity({ id: 'r2', uid: 'api.payments' })],
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

    it('switches to the Resources tab and shows resource entities without refetching principals', async () => {
        mockByKind({
            principals: [makeEntity({ id: 'p1', uid: 'user.alice' })],
            resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' })],
        });
        renderPage();

        await waitFor(() => expect(screen.getAllByText('alice').length).toBeGreaterThan(0));
        await waitFor(() => expect(listEntitiesSpy).toHaveBeenCalledTimes(2));

        const callsBefore = listEntitiesSpy.mock.calls.length;
        const resourcesTab = screen.getByRole('tab', { name: /Resources/i });
        const user = userEvent.setup();
        await user.click(resourcesTab);

        await waitFor(() => {
            expect(screen.getAllByText('flight').length).toBeGreaterThan(0);
        });
        expect(listEntitiesSpy.mock.calls.length).toBe(callsBefore);
    });

    it('filters principals by search input', async () => {
        mockByKind({
            principals: [makeEntity({ id: 'p1', uid: 'user.alice' }), makeEntity({ id: 'p2', uid: 'user.bob' })],
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

    it('exposes the Import-from-Catalog button only on the Resources tab', async () => {
        mockByKind({
            principals: [makeEntity({ id: 'p1', uid: 'user.alice' })],
            resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' })],
        });
        renderPage();

        await waitFor(() => expect(screen.getAllByText('alice').length).toBeGreaterThan(0));
        expect(screen.queryByRole('button', { name: /Import from Context Catalog/i })).not.toBeInTheDocument();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));

        await waitFor(() => expect(screen.getByRole('button', { name: /Import from Context Catalog/i })).toBeInTheDocument());
    });

    it('opens the import dialog stub when the button is clicked', async () => {
        mockByKind({ resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' })] });
        renderPage();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));
        await waitFor(() => expect(screen.getByRole('button', { name: /Import from Context Catalog/i })).toBeInTheDocument());

        expect(screen.queryByTestId('import-dialog-stub')).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: /Import from Context Catalog/i }));
        expect(screen.getByTestId('import-dialog-stub')).toBeInTheDocument();
    });

    it('opens the create-entity dialog as PRINCIPAL when "Add principal" is clicked', async () => {
        mockByKind({ principals: [makeEntity({ id: 'p1', uid: 'user.alice' })] });
        renderPage();

        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByRole('button', { name: /Add principal/i })).toBeInTheDocument());

        expect(screen.queryByTestId('create-entity-dialog-stub')).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: /Add principal/i }));

        const stub = screen.getByTestId('create-entity-dialog-stub');
        expect(stub).toHaveAttribute('data-kind', 'PRINCIPAL');
    });

    it('opens the create-entity dialog as RESOURCE when "Add resource" is clicked', async () => {
        mockByKind({ resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' })] });
        renderPage();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));
        await waitFor(() => expect(screen.getByRole('button', { name: /Add resource/i })).toBeInTheDocument());

        await user.click(screen.getByRole('button', { name: /Add resource/i }));

        const stub = screen.getByTestId('create-entity-dialog-stub');
        expect(stub).toHaveAttribute('data-kind', 'RESOURCE');
    });

    it('opens the edit dialog (kind=PRINCIPAL) for a local principal row', async () => {
        mockByKind({ principals: [makeEntity({ id: 'p1', uid: 'user.alice' })] });
        renderPage();

        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByLabelText('Edit user.alice')).toBeInTheDocument());
        expect(screen.queryByTestId('edit-entity-dialog-stub')).not.toBeInTheDocument();
        await user.click(screen.getByLabelText('Edit user.alice'));

        const stub = screen.getByTestId('edit-entity-dialog-stub');
        expect(stub).toHaveAttribute('data-kind', 'PRINCIPAL');
        expect(stub).toHaveAttribute('data-entity', 'alice');
    });

    it('hides the edit action for non-local (read-only) entities', async () => {
        mockByKind({ resources: [makeEntity({ id: 'r1', uid: 'mcp.flight', attributes: { _source: 'gravitee-catalog' } })] });
        renderPage();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));
        await waitFor(() => expect(screen.getByText('mcp.flight')).toBeInTheDocument());
        // Catalog-sourced → no edit, but still removable.
        expect(screen.queryByLabelText('Edit mcp.flight')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Delete mcp.flight')).toBeInTheDocument();
    });

    it('opens the edit dialog (kind=RESOURCE) for a local resource row', async () => {
        mockByKind({ resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' })] });
        renderPage();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));
        await waitFor(() => expect(screen.getByLabelText('Edit mcp.flight')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Edit mcp.flight'));

        const stub = screen.getByTestId('edit-entity-dialog-stub');
        expect(stub).toHaveAttribute('data-kind', 'RESOURCE');
        expect(stub).toHaveAttribute('data-entity', 'flight');
    });

    it('shows the Remove action on local principal rows but not on synced ones', async () => {
        mockByKind({
            principals: [
                makeEntity({ id: 'p1', uid: 'user.alice' }),
                makeEntity({ id: 'p2', uid: 'user.bob', attributes: { _source: 'apim' } }),
            ],
        });
        renderPage();

        await waitFor(() => expect(screen.getByLabelText('Delete user.alice')).toBeInTheDocument());
        // Synced (APIM-sourced) principals are read-only — no remove.
        expect(screen.queryByLabelText('Delete user.bob')).not.toBeInTheDocument();
    });

    it('warns about permanent removal when deleting a local principal', async () => {
        mockByKind({
            principals: [makeEntity({ id: 'p1', uid: 'user.alice', attributes: { _displayName: 'Alice' } })],
        });
        renderPage();

        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByLabelText('Delete user.alice')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Delete user.alice'));

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText(/permanently removed/i)).toBeInTheDocument();
    });

    it('deletes a local principal on confirm', async () => {
        mockByKind({ principals: [makeEntity({ id: 'p1', uid: 'user.alice' })] });
        deleteEntitySpy.mockResolvedValue(undefined);
        renderPage();

        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByLabelText('Delete user.alice')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Delete user.alice'));

        const dialog = await screen.findByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: /Confirm remove/i }));

        await waitFor(() => expect(deleteEntitySpy).toHaveBeenCalledWith('DEFAULT', 'user.alice'));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('opens the Remove confirmation dialog when the trash icon is clicked', async () => {
        mockByKind({
            resources: [makeEntity({ id: 'r1', uid: 'mcp.flight', attributes: { _displayName: 'Flight Status' } })],
        });
        renderPage();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));

        await waitFor(() => expect(screen.getByLabelText('Delete mcp.flight')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Delete mcp.flight'));

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Remove entity from Authorization?')).toBeInTheDocument();
        expect(within(dialog).getByText(/mcp\.flight/)).toBeInTheDocument();
        expect(deleteEntitySpy).not.toHaveBeenCalled();
    });

    it('does not call deleteEntity when the confirmation dialog is cancelled', async () => {
        mockByKind({ resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' })] });
        renderPage();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));
        await waitFor(() => expect(screen.getByLabelText('Delete mcp.flight')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Delete mcp.flight'));

        const dialog = await screen.findByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: /^Cancel$/i }));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(deleteEntitySpy).not.toHaveBeenCalled();
    });

    it('calls deleteEntity, fires toast.success, and closes the dialog on confirm', async () => {
        mockByKind({ resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' })] });
        deleteEntitySpy.mockResolvedValue(undefined);
        renderPage();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));
        await waitFor(() => expect(screen.getByLabelText('Delete mcp.flight')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Delete mcp.flight'));

        const dialog = await screen.findByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: /Confirm remove/i }));

        await waitFor(() => expect(deleteEntitySpy).toHaveBeenCalledWith('DEFAULT', 'mcp.flight'));
        await waitFor(() => expect(toastSuccessSpy).toHaveBeenCalled());
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('keeps the dialog open and surfaces a toast.error when delete fails', async () => {
        mockByKind({ resources: [makeEntity({ id: 'r1', uid: 'mcp.flight' })] });
        deleteEntitySpy.mockRejectedValue(new Error('upstream 500'));
        renderPage();

        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));
        await waitFor(() => expect(screen.getByLabelText('Delete mcp.flight')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Delete mcp.flight'));

        const dialog = await screen.findByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: /Confirm remove/i }));

        await waitFor(() => expect(toastErrorSpy).toHaveBeenCalledWith(expect.stringContaining('upstream 500')));
        // Dialog stays open after a failed delete so the user can retry / cancel.
        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
});
