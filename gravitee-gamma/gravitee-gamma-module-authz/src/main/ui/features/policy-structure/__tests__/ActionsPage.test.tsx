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
import { ActionsPage } from '../ActionsPage';

const toastSuccessSpy = vi.fn();
const toastErrorSpy = vi.fn();

vi.mock('@gravitee/graphene-core', async importOriginal => {
    const actual = await importOriginal<Record<string, unknown>>();
    return {
        ...actual,
        toast: { success: (msg: string) => toastSuccessSpy(msg), error: (msg: string) => toastErrorSpy(msg) },
    };
});

// The add dialog is unit-tested separately; stub it so the page test stays focused.
vi.mock('../CreateActionDialog', () => ({
    CreateActionDialog: ({ open }: { open: boolean }) => (open ? <div data-testid="create-action-dialog-stub" /> : null),
}));

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

interface ListEntitiesParams {
    readonly kind?: 'PRINCIPAL' | 'RESOURCE';
    readonly entityIdPrefix?: string;
}

const listEntitiesSpy = vi.fn();
const deleteEntitySpy = vi.fn();

vi.mock('@gravitee/gamma-modules-sdk', async importOriginal => ({
    ...(await importOriginal<object>()),
    useEnvironment: () => ({ id: 'DEFAULT' }),
}));

vi.mock('../../../shared/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listEntities: (env: string, params?: ListEntitiesParams) => listEntitiesSpy(env, params),
        deleteEntity: (env: string, entityId: string) => deleteEntitySpy(env, entityId),
    },
}));

function makeAction(id: string, overrides: Partial<EntityResponse> = {}): EntityResponse {
    return {
        id: `ent-${id}`,
        environmentId: 'DEFAULT',
        uid: `action.${id}`,
        attributes: { _displayName: id },
        parents: [],
        createdAt: '2026-04-27T10:00:00.000Z',
        updatedAt: '2026-04-27T10:00:00.000Z',
        ...overrides,
    };
}

function mockActions(actions: readonly EntityResponse[]) {
    listEntitiesSpy.mockImplementation((_env: string, params?: ListEntitiesParams) => {
        if (params?.kind === 'RESOURCE' && params?.entityIdPrefix === 'action.') {
            return Promise.resolve({ data: actions, total: actions.length, page: 1, perPage: 10 });
        }
        return Promise.resolve({ data: [], total: 0, page: 1, perPage: 10 });
    });
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    return render(<ActionsPage />, { wrapper });
}

beforeEach(() => {
    listEntitiesSpy.mockReset();
    deleteEntitySpy.mockReset();
    toastSuccessSpy.mockReset();
    toastErrorSpy.mockReset();
});

describe('ActionsPage', () => {
    it('queries RESOURCE entities under the action. prefix', async () => {
        mockActions([]);
        renderPage();

        await waitFor(() => expect(listEntitiesSpy).toHaveBeenCalled());
        expect(listEntitiesSpy).toHaveBeenCalledWith('DEFAULT', expect.objectContaining({ kind: 'RESOURCE', entityIdPrefix: 'action.' }));
    });

    it('shows the empty state when there are no actions', async () => {
        mockActions([]);
        renderPage();
        await waitFor(() => expect(screen.getByText('No actions yet')).toBeInTheDocument());
    });

    it('lists action entities by Entity ID', async () => {
        mockActions([makeAction('call_tool'), makeAction('invoke')]);
        renderPage();

        await waitFor(() => expect(screen.getByText('action.call_tool')).toBeInTheDocument());
        expect(screen.getByText('action.invoke')).toBeInTheDocument();
    });

    it('filters actions by the search input', async () => {
        mockActions([makeAction('call_tool'), makeAction('invoke')]);
        renderPage();

        await waitFor(() => expect(screen.getByText('action.call_tool')).toBeInTheDocument());
        fireEvent.change(screen.getByLabelText('Search actions'), { target: { value: 'invoke' } });

        await waitFor(() => {
            expect(screen.queryByText('action.call_tool')).not.toBeInTheDocument();
            expect(screen.getByText('action.invoke')).toBeInTheDocument();
        });
    });

    it('opens the add-action dialog stub on "Add action"', async () => {
        mockActions([]);
        renderPage();

        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByRole('button', { name: /Add action/i })).toBeInTheDocument());
        expect(screen.queryByTestId('create-action-dialog-stub')).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: /Add action/i }));
        expect(screen.getByTestId('create-action-dialog-stub')).toBeInTheDocument();
    });

    it('hides the Remove action for non-local (read-only) actions', async () => {
        mockActions([makeAction('read', { attributes: { _displayName: 'read', _source: 'gravitee-catalog' } })]);
        renderPage();

        await waitFor(() => expect(screen.getByText('action.read')).toBeInTheDocument());
        expect(screen.queryByLabelText('Delete action.read')).not.toBeInTheDocument();
    });

    it('removes a local action through the confirmation dialog', async () => {
        mockActions([makeAction('call_tool')]);
        deleteEntitySpy.mockResolvedValue(undefined);
        renderPage();

        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByLabelText('Delete action.call_tool')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Delete action.call_tool'));

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Remove action from Authorization?')).toBeInTheDocument();
        await user.click(within(dialog).getByRole('button', { name: /Confirm remove/i }));

        await waitFor(() => expect(deleteEntitySpy).toHaveBeenCalledWith('DEFAULT', 'action.call_tool'));
        await waitFor(() => expect(toastSuccessSpy).toHaveBeenCalled());
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('does not call deleteEntity when the remove dialog is cancelled', async () => {
        mockActions([makeAction('call_tool')]);
        renderPage();

        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByLabelText('Delete action.call_tool')).toBeInTheDocument());
        await user.click(screen.getByLabelText('Delete action.call_tool'));

        const dialog = await screen.findByRole('dialog');
        await user.click(within(dialog).getByRole('button', { name: /^Cancel$/i }));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(deleteEntitySpy).not.toHaveBeenCalled();
    });
});
