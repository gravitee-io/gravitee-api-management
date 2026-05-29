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
import { CreateActionDialog } from '../CreateActionDialog';

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

const createEntitySpy = vi.fn();
const toastSuccessSpy = vi.fn();

vi.mock('../../../shared/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        createEntity: (env: string, req: unknown) => createEntitySpy(env, req),
    },
}));

vi.mock('@gravitee/graphene-core', async importOriginal => {
    const actual = await importOriginal<Record<string, unknown>>();
    return {
        ...actual,
        toast: { success: (msg: string) => toastSuccessSpy(msg), error: vi.fn() },
    };
});

function wrapper({ children }: { children: ReactNode }) {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

beforeEach(() => {
    createEntitySpy.mockReset();
    toastSuccessSpy.mockReset();
    createEntitySpy.mockResolvedValue({
        id: 'new-1',
        environmentId: 'DEFAULT',
        uid: 'action.call_tool',
        attributes: { _displayName: 'Call Tool' },
        parents: [],
        createdAt: '2026-05-27T11:00:00.000Z',
        updatedAt: '2026-05-27T11:00:00.000Z',
    });
});

function renderDialog(props: Partial<Parameters<typeof CreateActionDialog>[0]> = {}) {
    const onOpenChange = vi.fn();
    const onCreated = vi.fn();
    render(<CreateActionDialog open environmentId="DEFAULT" onOpenChange={onOpenChange} onCreated={onCreated} {...props} />, { wrapper });
    return { onOpenChange, onCreated };
}

describe('CreateActionDialog', () => {
    it('renders the Add Action sheet', () => {
        renderDialog();
        expect(screen.getByRole('heading', { name: /Add Action/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Create Action/i })).toBeInTheDocument();
    });

    it('auto-derives the action id from the display name and previews the action. Entity ID', async () => {
        const user = userEvent.setup();
        renderDialog();

        await user.type(screen.getByLabelText(/Display name/i), 'Call Tool');
        const slug = screen.getByLabelText('Action ID') as HTMLInputElement;
        await waitFor(() => expect(slug.value).toBe('call-tool'));
        expect(screen.getByText(/^action\.call-tool$/)).toBeInTheDocument();
    });

    it('stops auto-deriving once the action id is edited manually', async () => {
        const user = userEvent.setup();
        renderDialog();

        await user.type(screen.getByLabelText(/Display name/i), 'Call Tool');
        const slug = screen.getByLabelText('Action ID') as HTMLInputElement;
        await waitFor(() => expect(slug.value).toBe('call-tool'));

        await user.clear(slug);
        await user.type(slug, 'call_tool');
        await user.type(screen.getByLabelText(/Display name/i), ' Now');
        expect(slug.value).toBe('call_tool');
    });

    it('disables Create until name + id are valid and flags a bad id', async () => {
        const user = userEvent.setup();
        renderDialog();
        const create = screen.getByRole('button', { name: /Create Action/i });
        expect(create).toBeDisabled();

        await user.type(screen.getByLabelText(/Display name/i), 'Call Tool');
        await waitFor(() => expect(create).toBeEnabled());

        const slug = screen.getByLabelText('Action ID') as HTMLInputElement;
        await user.clear(slug);
        await user.type(slug, 'Bad Id!');
        expect(create).toBeDisabled();
        expect(screen.getByText(/Action ID must match/i)).toBeInTheDocument();
    });

    it('creates a RESOURCE entity under the action. prefix with the right payload', async () => {
        const user = userEvent.setup();
        const { onOpenChange, onCreated } = renderDialog();

        await user.type(screen.getByLabelText(/Display name/i), 'Call Tool');
        const slug = screen.getByLabelText('Action ID') as HTMLInputElement;
        await user.clear(slug);
        await user.type(slug, 'call_tool');
        await user.type(screen.getByLabelText(/Description/i), 'Invoke an MCP tool');
        await user.click(screen.getByRole('button', { name: /Create Action/i }));

        await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(1));
        const [env, req] = createEntitySpy.mock.calls[0];
        expect(env).toBe('DEFAULT');
        expect(req).toMatchObject({ entityId: 'action.call_tool', kind: 'RESOURCE', source: 'local', parents: [] });
        expect((req as { attributes: Record<string, unknown> }).attributes).toMatchObject({
            _displayName: 'Call Tool',
            description: 'Invoke an MCP tool',
        });
        await waitFor(() => expect(toastSuccessSpy).toHaveBeenCalledWith('Created Call Tool'));
        await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('surfaces a duplicate-id message on 409 and keeps the dialog open', async () => {
        const user = userEvent.setup();
        createEntitySpy.mockRejectedValueOnce(new Error('HTTP 409 entity already exists'));
        const { onOpenChange } = renderDialog();

        await user.type(screen.getByLabelText(/Display name/i), 'Call Tool');
        const slug = screen.getByLabelText('Action ID') as HTMLInputElement;
        await user.clear(slug);
        await user.type(slug, 'call_tool');
        await user.click(screen.getByRole('button', { name: /Create Action/i }));

        await waitFor(() => expect(screen.getByText(/An action with ID "action\.call_tool" already exists/i)).toBeInTheDocument());
        expect(onOpenChange).not.toHaveBeenCalled();
    });
});
