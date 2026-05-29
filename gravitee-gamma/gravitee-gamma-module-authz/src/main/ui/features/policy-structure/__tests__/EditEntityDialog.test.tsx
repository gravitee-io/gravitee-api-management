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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { EntityInstance } from '../../../shared/entity.types';
import { EditEntityDialog } from '../EditEntityDialog';

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

const updateEntitySpy = vi.fn();
const toastSuccessSpy = vi.fn();

vi.mock('../../../shared/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        updateEntity: (env: string, id: string, req: unknown) => updateEntitySpy(env, id, req),
    },
}));

// Parents picker source — keep it empty so the test stays focused on the payload.
vi.mock('../../../shared/hooks/useEntities', () => ({
    useEntities: () => ({ data: { data: [], total: 0, page: 1, perPage: 200 }, isLoading: false, error: undefined }),
}));

vi.mock('@gravitee/graphene-core', async importOriginal => {
    const actual = await importOriginal<Record<string, unknown>>();
    return { ...actual, toast: { success: (m: string) => toastSuccessSpy(m), error: vi.fn() } };
});

function wrapper({ children }: { children: ReactNode }) {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

function aliceEntity(): EntityInstance {
    return {
        uid: { type: 'User', id: 'alice' },
        displayName: 'Alice',
        attrs: { department: 'engineering', email: 'alice@example.io', description: 'Eng lead' },
        parents: [{ type: 'Group', id: 'developers' }],
        source: 'local',
        createdAt: '2026-01-01T00:00:00.000Z',
        updatedAt: '2026-01-02T00:00:00.000Z',
    };
}

function renderDialog(entity: EntityInstance | null = aliceEntity()) {
    const onOpenChange = vi.fn();
    const onUpdated = vi.fn();
    render(
        <EditEntityDialog open entity={entity} kind="PRINCIPAL" environmentId="DEFAULT" onOpenChange={onOpenChange} onUpdated={onUpdated} />,
        { wrapper },
    );
    return { onOpenChange, onUpdated };
}

beforeEach(() => {
    updateEntitySpy.mockReset();
    toastSuccessSpy.mockReset();
    updateEntitySpy.mockResolvedValue({});
});

describe('EditEntityDialog', () => {
    it('prefills display name + description and shows the Entity ID read-only', () => {
        renderDialog();
        expect((screen.getByLabelText(/Display name/i) as HTMLInputElement).value).toBe('Alice');
        expect((screen.getByLabelText(/Description/i) as HTMLTextAreaElement).value).toBe('Eng lead');
        // Entity ID is rendered, not as an editable input.
        expect(screen.getByText('user.alice')).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: /Entity ID/i })).not.toBeInTheDocument();
    });

    it('disables Save when the display name is cleared', async () => {
        const user = userEvent.setup();
        renderDialog();
        const save = screen.getByRole('button', { name: /Save changes/i });
        expect(save).toBeEnabled();
        await user.clear(screen.getByLabelText(/Display name/i));
        expect(save).toBeDisabled();
    });

    it('sends the full attribute map (preserving unknown attrs) with only edited fields changed', async () => {
        const user = userEvent.setup();
        const { onUpdated, onOpenChange } = renderDialog();

        const name = screen.getByLabelText(/Display name/i);
        await user.clear(name);
        await user.type(name, 'Alice Smith');
        await user.click(screen.getByRole('button', { name: /Save changes/i }));

        await waitFor(() => expect(updateEntitySpy).toHaveBeenCalledTimes(1));
        const [env, id, req] = updateEntitySpy.mock.calls[0];
        expect(env).toBe('DEFAULT');
        expect(id).toBe('user.alice');
        const attrs = (req as { attributes: Record<string, unknown> }).attributes;
        // edited
        expect(attrs._displayName).toBe('Alice Smith');
        expect(attrs.description).toBe('Eng lead');
        // preserved (would be wiped by a partial patch)
        expect(attrs.department).toBe('engineering');
        expect(attrs.email).toBe('alice@example.io');
        expect(attrs._kind).toBe('user');
        // parents round-tripped to canonical entityIds
        expect((req as { parents: string[] }).parents).toEqual(['group.developers']);

        await waitFor(() => expect(toastSuccessSpy).toHaveBeenCalledWith('Updated Alice Smith'));
        await waitFor(() => expect(onUpdated).toHaveBeenCalledTimes(1));
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('drops the description attribute when the field is cleared', async () => {
        const user = userEvent.setup();
        renderDialog();

        await user.clear(screen.getByLabelText(/Description/i));
        await user.click(screen.getByRole('button', { name: /Save changes/i }));

        await waitFor(() => expect(updateEntitySpy).toHaveBeenCalledTimes(1));
        const attrs = (updateEntitySpy.mock.calls[0][2] as { attributes: Record<string, unknown> }).attributes;
        expect('description' in attrs).toBe(false);
        expect(attrs._displayName).toBe('Alice');
    });

    it('surfaces a backend error and keeps the dialog open', async () => {
        const user = userEvent.setup();
        updateEntitySpy.mockRejectedValueOnce(new Error('entity not found'));
        const { onOpenChange } = renderDialog();

        await user.click(screen.getByRole('button', { name: /Save changes/i }));

        await waitFor(() => expect(screen.getByText('entity not found')).toBeInTheDocument());
        expect(onOpenChange).not.toHaveBeenCalled();
    });
});
