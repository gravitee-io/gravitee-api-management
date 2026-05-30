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
import { CreateEntityDialog } from '../CreateEntityDialog';

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

const listEntitiesSpy = vi.fn();
const getEntitySpy = vi.fn();
const createEntitySpy = vi.fn();
const toastSuccessSpy = vi.fn();

vi.mock('../../../shared/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 200,
    authzApiService: {
        listEntities: (env: string, params?: unknown) => listEntitiesSpy(env, params),
        getEntity: (env: string, id: string) => getEntitySpy(env, id),
        createEntity: (env: string, req: unknown) => createEntitySpy(env, req),
    },
}));

vi.mock('@gravitee/graphene-core', async importOriginal => {
    const actual = await importOriginal<Record<string, unknown>>();
    return {
        ...actual,
        toast: {
            success: (msg: string) => toastSuccessSpy(msg),
            error: vi.fn(),
        },
    };
});

function wrapper({ children }: { children: ReactNode }) {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

function seedParents(uids: readonly string[]) {
    listEntitiesSpy.mockResolvedValue({
        data: uids.map((uid, i) => ({
            id: `e${i}`,
            environmentId: 'DEFAULT',
            uid,
            attributes: { _displayName: uid.split('.')[1] },
            parents: [],
            createdAt: '2026-05-27T10:00:00.000Z',
            updatedAt: '2026-05-27T10:00:00.000Z',
        })),
        total: uids.length,
        page: 1,
        perPage: 200,
    });
}

beforeEach(() => {
    listEntitiesSpy.mockReset();
    getEntitySpy.mockReset();
    createEntitySpy.mockReset();
    toastSuccessSpy.mockReset();
    seedParents([]);
    getEntitySpy.mockResolvedValue(null);
    createEntitySpy.mockResolvedValue({
        id: 'new-1',
        environmentId: 'DEFAULT',
        uid: 'user.alice',
        attributes: { _displayName: 'Alice' },
        parents: [],
        createdAt: '2026-05-27T11:00:00.000Z',
        updatedAt: '2026-05-27T11:00:00.000Z',
    });
});

function renderDialog(props: Partial<Parameters<typeof CreateEntityDialog>[0]> = {}) {
    const onOpenChange = vi.fn();
    const onCreated = vi.fn();
    render(
        <CreateEntityDialog open kind="PRINCIPAL" environmentId="DEFAULT" onOpenChange={onOpenChange} onCreated={onCreated} {...props} />,
        { wrapper },
    );
    return { onOpenChange, onCreated };
}

describe('CreateEntityDialog', () => {
    it('renders Principal-specific copy when kind=PRINCIPAL', () => {
        renderDialog({ kind: 'PRINCIPAL' });
        expect(screen.getByRole('heading', { name: /Add Principal/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Create Principal/i })).toBeInTheDocument();
    });

    it('renders Resource-specific copy when kind=RESOURCE', () => {
        renderDialog({ kind: 'RESOURCE' });
        expect(screen.getByRole('heading', { name: /Add Resource/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Create Resource/i })).toBeInTheDocument();
    });

    it('auto-derives the slug from the display name (kebab-case, accent-stripped)', async () => {
        const user = userEvent.setup();
        renderDialog();

        await user.type(screen.getByLabelText(/Display name/i), 'Aliçe Müller');
        const slug = screen.getByLabelText('Slug') as HTMLInputElement;
        await waitFor(() => expect(slug.value).toBe('alice-muller'));
        const preview = screen.getByText(/^user\.alice-muller$/);
        expect(preview).toBeInTheDocument();
    });

    it('stops auto-deriving the slug once the user edits it manually', async () => {
        const user = userEvent.setup();
        renderDialog();

        await user.type(screen.getByLabelText(/Display name/i), 'Alice');
        const slug = screen.getByLabelText('Slug') as HTMLInputElement;
        await waitFor(() => expect(slug.value).toBe('alice'));

        await user.clear(slug);
        await user.type(slug, 'a_l_i_c_e');
        await user.type(screen.getByLabelText(/Display name/i), ' the second');
        // Slug should not have been overwritten by the display-name change.
        expect(slug.value).toBe('a_l_i_c_e');
    });

    it('disables the Create button until display name + slug + type are valid', async () => {
        const user = userEvent.setup();
        renderDialog();
        const create = screen.getByRole('button', { name: /Create Principal/i });
        expect(create).toBeDisabled();

        await user.type(screen.getByLabelText(/Display name/i), 'Alice');
        await waitFor(() => expect(create).toBeEnabled());

        const slug = screen.getByLabelText('Slug') as HTMLInputElement;
        await user.clear(slug);
        await user.type(slug, 'Bad Slug!');
        expect(create).toBeDisabled();
        expect(screen.getByText(/Slug must match/i)).toBeInTheDocument();
    });

    it('submits with the right payload on Create (Principal + description)', async () => {
        const user = userEvent.setup();
        const { onOpenChange, onCreated } = renderDialog({ kind: 'PRINCIPAL' });

        await user.type(screen.getByLabelText(/Display name/i), 'Alice');
        await user.type(screen.getByLabelText(/Description/i), 'Engineering lead');
        await user.click(screen.getByRole('button', { name: /Create Principal/i }));

        await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(1));
        const [env, req] = createEntitySpy.mock.calls[0];
        expect(env).toBe('DEFAULT');
        expect(req).toMatchObject({
            entityId: 'user.alice',
            kind: 'PRINCIPAL',
            source: 'local',
            parents: [],
        });
        expect((req as { attributes: Record<string, unknown> }).attributes).toMatchObject({
            _displayName: 'Alice',
            description: 'Engineering lead',
        });
        await waitFor(() => expect(toastSuccessSpy).toHaveBeenCalledWith('Created Alice'));
        await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('omits the description attribute when the field is blank', async () => {
        const user = userEvent.setup();
        renderDialog({ kind: 'PRINCIPAL' });

        await user.type(screen.getByLabelText(/Display name/i), 'Bob');
        await user.click(screen.getByRole('button', { name: /Create Principal/i }));

        await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(1));
        const [, req] = createEntitySpy.mock.calls[0];
        const attrs = (req as { attributes: Record<string, unknown> }).attributes;
        expect(attrs._displayName).toBe('Bob');
        expect(attrs.description).toBeUndefined();
    });

    it('blocks the create when an entity with the same Entity ID already exists', async () => {
        const user = userEvent.setup();
        // Pre-check finds an existing entity → must not call the upsert endpoint,
        // which would silently overwrite it (data loss).
        getEntitySpy.mockResolvedValue({
            id: 'existing-1',
            environmentId: 'DEFAULT',
            uid: 'user.alice',
            attributes: { _displayName: 'Original Alice', department: 'engineering' },
            parents: [],
            createdAt: '2026-01-01T00:00:00.000Z',
            updatedAt: '2026-01-01T00:00:00.000Z',
        });
        const { onOpenChange } = renderDialog({ kind: 'PRINCIPAL' });

        await user.type(screen.getByLabelText(/Display name/i), 'Alice');
        await user.click(screen.getByRole('button', { name: /Create Principal/i }));

        await waitFor(() => expect(getEntitySpy).toHaveBeenCalledWith('DEFAULT', 'user.alice'));
        await waitFor(() => expect(screen.getByText(/An entity with ID "user\.alice" already exists/i)).toBeInTheDocument());
        expect(createEntitySpy).not.toHaveBeenCalled();
        // Dialog stays open; onOpenChange is only called by the caller closing it.
        expect(onOpenChange).not.toHaveBeenCalled();
    });

    it('still maps a backend 409 (race) to the duplicate-id message', async () => {
        const user = userEvent.setup();
        // Pre-check passes (null) but a concurrent create lands first → backend 409.
        getEntitySpy.mockResolvedValue(null);
        createEntitySpy.mockRejectedValueOnce(new Error('HTTP 409 entity already exists'));
        const { onOpenChange } = renderDialog({ kind: 'PRINCIPAL' });

        await user.type(screen.getByLabelText(/Display name/i), 'Alice');
        await user.click(screen.getByRole('button', { name: /Create Principal/i }));

        await waitFor(() => expect(screen.getByText(/An entity with ID "user\.alice" already exists/i)).toBeInTheDocument());
        expect(onOpenChange).not.toHaveBeenCalled();
    });

    it('switches to the custom prefix input when "Other (custom prefix)" is selected', async () => {
        const user = userEvent.setup();
        renderDialog({ kind: 'RESOURCE' });

        await user.click(screen.getByLabelText('Entity type'));
        await user.click(await screen.findByRole('option', { name: /Other \(custom prefix\)/i }));

        const customInput = screen.getByLabelText(/Custom prefix/i);
        await user.type(customInput, 'webhook');
        await user.type(screen.getByLabelText(/Display name/i), 'Slack hook');

        // Slug auto-derives from displayName.
        await waitFor(() => expect(screen.getByText(/^webhook\.slack-hook$/)).toBeInTheDocument());

        await user.click(screen.getByRole('button', { name: /Create Resource/i }));
        await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(1));
        const [, req] = createEntitySpy.mock.calls[0];
        expect((req as { entityId: string }).entityId).toBe('webhook.slack-hook');
    });

    it('includes a typed integer attribute in the create payload', async () => {
        const user = userEvent.setup();
        renderDialog({ kind: 'PRINCIPAL' });

        await user.type(screen.getByLabelText(/Display name/i), 'Alice');
        await user.click(screen.getByRole('button', { name: /Add attribute/i }));
        await user.type(screen.getByLabelText(/Attribute key/i), 'clearance');
        await user.click(screen.getByLabelText(/Attribute type/i));
        await user.click(await screen.findByRole('option', { name: 'Integer' }));
        await user.type(screen.getByLabelText(/Attribute value/i), '3');
        await user.click(screen.getByRole('button', { name: /Create Principal/i }));

        await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(1));
        const [, req] = createEntitySpy.mock.calls[0];
        const attrs = (req as { attributes: Record<string, unknown> }).attributes;
        expect(attrs.clearance).toBe(3);
        expect(attrs._displayName).toBe('Alice');
    });

    it('warns when a custom prefix collides with a preset canonical', async () => {
        const user = userEvent.setup();
        renderDialog({ kind: 'RESOURCE' });

        await user.click(screen.getByLabelText('Entity type'));
        await user.click(await screen.findByRole('option', { name: /Other \(custom prefix\)/i }));

        await user.type(screen.getByLabelText(/Custom prefix/i), 'mcp');
        await user.type(screen.getByLabelText(/Display name/i), 'Custom');

        expect(screen.getByText(/"mcp" is a preset type/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Create Resource/i })).toBeDisabled();
    });
});
