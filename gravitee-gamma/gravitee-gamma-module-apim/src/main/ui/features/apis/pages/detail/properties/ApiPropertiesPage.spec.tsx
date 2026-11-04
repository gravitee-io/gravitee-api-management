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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiPropertiesPage } from './ApiPropertiesPage';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiProperties } from '../../../services/apis';
import type { Property } from '../../../types';

// ─── SDK / context mocks ──────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('../../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── React Query mock — simulates mutate calling mutationFn synchronously ─────

jest.mock('@tanstack/react-query', () => ({
    useMutation: jest.fn(config => ({
        mutate: jest.fn(async args => {
            try {
                await config.mutationFn(args);
                config.onSuccess?.();
            } catch (error) {
                config.onError?.(error);
            }
        }),
        isPending: false,
    })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

// ─── Service mock — runs the page's change on the API as the server holds it ──

jest.mock('../../../services/apis', () => ({
    updateApiProperties: jest.fn(() => Promise.resolve()),
}));

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUpdateApiProperties = updateApiProperties as jest.Mock;

const PLAIN_PROP = { key: 'backend.timeout', value: '5000', encrypted: false };
const ENCRYPTED_PROP = { key: 'secret.key', value: '***', encrypted: true };
const DYNAMIC_PROP = { key: 'dynamic.host', value: 'http://source', dynamic: true };
const ENCRYPTED_DYNAMIC_PROP = { key: 'dynamic.secret', value: 'cipher-text', encrypted: true, dynamic: true };

let propertiesOnServer: Property[] = [];
let savedProperties: Property[] | undefined;

interface RenderOptions {
    /** What the server holds at save time, when it differs from what the page loaded. */
    onServer?: Property[];
    origin?: 'MANAGEMENT' | 'KUBERNETES';
}

function renderPage(properties: Property[] = [], { onServer = properties, origin }: RenderOptions = {}) {
    propertiesOnServer = onServer;
    savedProperties = undefined;
    mockUpdateApiProperties.mockImplementation(async (_environmentId, _apiId, change: (current: Property[]) => Property[]) => {
        savedProperties = change(propertiesOnServer);
    });
    mockUseApiDetail.mockReturnValue({
        data: { id: 'api-1', name: 'Test API', properties, definitionContext: origin ? { origin } : undefined },
        isLoading: false,
    });
    render(
        <MemoryRouter initialEntries={['/apis/api-1/properties']}>
            <Routes>
                <Route path="apis/:apiId/properties" element={<ApiPropertiesPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

async function openRowMenu(user: ReturnType<typeof userEvent.setup>, key: string) {
    await user.click(screen.getByRole('button', { name: `Actions for ${key}` }));
}

function menuItemNames() {
    return screen.getAllByRole('menuitem').map(item => item.textContent);
}

async function saved() {
    await waitFor(() => expect(savedProperties).toBeDefined());
    return savedProperties!;
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
});

// ─── Permission and origin: read-only renders no write controls ───────────────

describe('read-only', () => {
    it('hides Add button and action dropdowns when user lacks api-definition-u', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage([PLAIN_PROP]);

        expect(screen.queryByRole('button', { name: /add property/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /actions for/i })).toBeNull();
    });

    it('offers no action on any row to a user who lacks api-definition-u, dynamic rows included', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage([DYNAMIC_PROP, ENCRYPTED_DYNAMIC_PROP]);

        expect(screen.queryByRole('button', { name: /actions for|remove/i })).toBeNull();
        expect(screen.getAllByText('Dynamic')).toHaveLength(2);
    });

    it('offers no write control for an API managed by Kubernetes, like classic', () => {
        renderPage([PLAIN_PROP, DYNAMIC_PROP], { origin: 'KUBERNETES' });

        expect(screen.queryByRole('button', { name: /add property/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /^import$/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /actions for/i })).toBeNull();
        expect(screen.getByRole('button', { name: /manage dynamically/i })).toBeDisabled();
    });
});

// ─── Static properties keep their behavior ────────────────────────────────────

describe('static properties', () => {
    it('shows only Renew encryption and Delete for an encrypted property — no Edit value or Encrypt value', async () => {
        const user = userEvent.setup();
        renderPage([ENCRYPTED_PROP]);

        await openRowMenu(user, ENCRYPTED_PROP.key);

        expect(menuItemNames()).toEqual(['Renew encryption', 'Delete']);
    });

    it('offers Edit value, Encrypt value and Delete on a plain property', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP]);

        await openRowMenu(user, PLAIN_PROP.key);

        expect(menuItemNames()).toEqual(['Edit value', 'Encrypt value', 'Delete']);
    });

    it('adds a plain-text property with encryptable false', async () => {
        const user = userEvent.setup();
        renderPage([]);

        await user.click(screen.getByRole('button', { name: /add property/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/key/i), { target: { value: 'my.key' } });
        fireEvent.change(within(dialog).getByLabelText(/value/i), { target: { value: 'my-value' } });
        await user.click(within(dialog).getByRole('button', { name: /add property/i }));

        expect(await saved()).toEqual([{ key: 'my.key', value: 'my-value', encryptable: false }]);
    });

    it('adds an encrypted-on-save property with a masked input', async () => {
        const user = userEvent.setup();
        renderPage([]);

        await user.click(screen.getByRole('button', { name: /add property/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/key/i), { target: { value: 'my.secret' } });
        await user.click(within(dialog).getByRole('switch', { name: /encrypt on save/i }));
        const value = within(dialog).getByLabelText(/value/i);
        expect(value.getAttribute('type')).toBe('password');
        fireEvent.change(value, { target: { value: 's3cret' } });
        await user.click(within(dialog).getByRole('button', { name: /add property/i }));

        expect(await saved()).toEqual([{ key: 'my.secret', value: 's3cret', encryptable: true }]);
    });

    it('allows adding a property with an empty value when the key is unique', async () => {
        const user = userEvent.setup();
        renderPage([]);

        await user.click(screen.getByRole('button', { name: /add property/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/key/i), { target: { value: 'el.placeholder' } });
        expect(within(dialog).getByRole('button', { name: /add property/i })).toBeEnabled();
        await user.click(within(dialog).getByRole('button', { name: /add property/i }));

        expect(await saved()).toEqual([{ key: 'el.placeholder', value: '', encryptable: false }]);
    });

    it('refuses a key that already exists', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP]);

        await user.click(screen.getByRole('button', { name: /add property/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/key/i), { target: { value: PLAIN_PROP.key } });

        expect(within(dialog).getByText('A property with this key already exists.')).not.toBeNull();
        expect(within(dialog).getByRole('button', { name: /add property/i })).toBeDisabled();
    });

    it('edits the value of a plain property and keeps its other fields', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP]);

        await openRowMenu(user, PLAIN_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Edit value' }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/value/i), { target: { value: '8000' } });
        await user.click(within(dialog).getByRole('button', { name: 'Save' }));

        expect(await saved()).toEqual([{ ...PLAIN_PROP, value: '8000' }]);
    });

    it('sets encryptable=true on the property when Encrypt value is clicked', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP]);

        await openRowMenu(user, PLAIN_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Encrypt value' }));

        expect(await saved()).toEqual([{ ...PLAIN_PROP, encryptable: true }]);
    });

    it('renews an encrypted property with the new plaintext', async () => {
        const user = userEvent.setup();
        renderPage([ENCRYPTED_PROP]);

        await openRowMenu(user, ENCRYPTED_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Renew encryption' }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/value/i), { target: { value: 'n3w-s3cret' } });
        await user.click(within(dialog).getByRole('button', { name: 'Save' }));

        expect(await saved()).toEqual([{ key: 'secret.key', value: 'n3w-s3cret', encrypted: false, encryptable: true }]);
    });

    it('deletes a property', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP, ENCRYPTED_PROP]);

        await openRowMenu(user, PLAIN_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Delete' }));

        expect(await saved()).toEqual([ENCRYPTED_PROP]);
    });
});

// ─── Masking, as classic does ─────────────────────────────────────────────────

describe('encrypted value', () => {
    it('is masked with eight bullets, never shows the stored text, and cannot be selected', () => {
        renderPage([ENCRYPTED_DYNAMIC_PROP]);

        const mask = screen.getByText('•'.repeat(8));
        expect(screen.queryByText(ENCRYPTED_DYNAMIC_PROP.value)).toBeNull();
        expect(mask.className).toContain('select-none');
        expect(mask.getAttribute('title')).toBe('Encrypted - value hidden for security');
    });

    it('shows both the Encrypted and Dynamic badges on an encrypted dynamic row', () => {
        renderPage([ENCRYPTED_DYNAMIC_PROP]);

        const row = screen.getByRole('row', { name: /dynamic\.secret/ });
        expect(within(row).getByText('Encrypted')).not.toBeNull();
        expect(within(row).getByText('Dynamic')).not.toBeNull();
    });
});

// ─── Dynamic properties ───────────────────────────────────────────────────────

describe('dynamic properties', () => {
    it('offers Encrypt value and Remove, but no Edit value or Renew, on a plain dynamic row', async () => {
        const user = userEvent.setup();
        renderPage([DYNAMIC_PROP]);

        await openRowMenu(user, DYNAMIC_PROP.key);

        expect(menuItemNames()).toEqual(['Encrypt value', 'Remove (re-added on next sync)']);
    });

    it('encrypts a dynamic property, keeping dynamic=true and its value unchanged', async () => {
        const user = userEvent.setup();
        renderPage([DYNAMIC_PROP]);

        await openRowMenu(user, DYNAMIC_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Encrypt value' }));

        expect(await saved()).toEqual([{ ...DYNAMIC_PROP, encryptable: true }]);
    });

    it('offers only Remove on an encrypted dynamic row, like classic: the sync owns its value, so it cannot be renewed', async () => {
        const user = userEvent.setup();
        renderPage([ENCRYPTED_DYNAMIC_PROP]);

        await openRowMenu(user, ENCRYPTED_DYNAMIC_PROP.key);

        expect(menuItemNames()).toEqual(['Remove (re-added on next sync)']);
    });

    it('cannot renew with an empty or spaces-only value', async () => {
        const user = userEvent.setup();
        renderPage([ENCRYPTED_PROP]);

        await openRowMenu(user, ENCRYPTED_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Renew encryption' }));
        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByRole('button', { name: 'Save' })).toBeDisabled();

        fireEvent.change(within(dialog).getByLabelText(/value/i), { target: { value: '   ' } });
        expect(within(dialog).getByRole('button', { name: 'Save' })).toBeDisabled();
        expect(mockUpdateApiProperties).not.toHaveBeenCalled();
    });

    it('removes a dynamic property without leaving an encrypted flag behind', async () => {
        const user = userEvent.setup();
        renderPage([ENCRYPTED_DYNAMIC_PROP, PLAIN_PROP]);

        await openRowMenu(user, ENCRYPTED_DYNAMIC_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Remove (re-added on next sync)' }));

        expect(await saved()).toEqual([PLAIN_PROP]);
    });
});

// ─── Every save starts from the API as the server holds it ────────────────────

describe('saving from fresh API state', () => {
    const SYNCED_PROP = { key: 'synced.since.load', value: 'from-provider', dynamic: true };

    it('keeps a property synced since the page loaded when another property is encrypted', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP], { onServer: [PLAIN_PROP, SYNCED_PROP] });

        await openRowMenu(user, PLAIN_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Encrypt value' }));

        expect(await saved()).toEqual([{ ...PLAIN_PROP, encryptable: true }, SYNCED_PROP]);
    });

    it('encrypts the value the server holds now, not the one the page loaded', async () => {
        const user = userEvent.setup();
        renderPage([DYNAMIC_PROP], { onServer: [{ ...DYNAMIC_PROP, value: 'http://refreshed' }] });

        await openRowMenu(user, DYNAMIC_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Encrypt value' }));

        expect(await saved()).toEqual([{ ...DYNAMIC_PROP, value: 'http://refreshed', encryptable: true }]);
    });

    it('fails and saves nothing when the property to encrypt was removed by a sync', async () => {
        const user = userEvent.setup();
        renderPage([DYNAMIC_PROP], { onServer: [] });

        await openRowMenu(user, DYNAMIC_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Encrypt value' }));

        expect(await screen.findByText('Property "dynamic.host" no longer exists')).not.toBeNull();
        expect(savedProperties).toBeUndefined();
    });

    it('fails and saves nothing when the property to renew was removed by a sync', async () => {
        const user = userEvent.setup();
        renderPage([ENCRYPTED_PROP], { onServer: [] });

        await openRowMenu(user, ENCRYPTED_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Renew encryption' }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/value/i), { target: { value: 'n3w-s3cret' } });
        await user.click(within(dialog).getByRole('button', { name: 'Save' }));

        expect(await screen.findByText('Property "secret.key" no longer exists')).not.toBeNull();
        expect(savedProperties).toBeUndefined();
    });

    it('fails and saves nothing when Edit value targets a property the server now holds encrypted', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP], { onServer: [{ key: PLAIN_PROP.key, value: '***', encrypted: true }] });

        await openRowMenu(user, PLAIN_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Edit value' }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/value/i), { target: { value: '8000' } });
        await user.click(within(dialog).getByRole('button', { name: 'Save' }));

        expect(await within(dialog).findByText('Property "backend.timeout" is now encrypted, renew it instead')).not.toBeNull();
        expect(savedProperties).toBeUndefined();
    });

    it('does not show the failure of a row action in the next sheet that opens', async () => {
        const user = userEvent.setup();
        renderPage([DYNAMIC_PROP], { onServer: [] });

        await openRowMenu(user, DYNAMIC_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Encrypt value' }));
        await screen.findByText('Property "dynamic.host" no longer exists');

        await user.click(screen.getByRole('button', { name: /add property/i }));

        expect(within(screen.getByRole('dialog')).queryByText('Property "dynamic.host" no longer exists')).toBeNull();
    });

    it('refuses to add a key that a sync created since the page loaded', async () => {
        const user = userEvent.setup();
        renderPage([], { onServer: [SYNCED_PROP] });

        await user.click(screen.getByRole('button', { name: /add property/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByLabelText(/key/i), { target: { value: SYNCED_PROP.key } });
        await user.click(within(dialog).getByRole('button', { name: /add property/i }));

        expect(await screen.findByText(`A property with the key "${SYNCED_PROP.key}" already exists`)).not.toBeNull();
        expect(savedProperties).toBeUndefined();
    });

    it('keeps a synced property when another one is deleted', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP], { onServer: [PLAIN_PROP, SYNCED_PROP] });

        await openRowMenu(user, PLAIN_PROP.key);
        await user.click(screen.getByRole('menuitem', { name: 'Delete' }));

        expect(await saved()).toEqual([SYNCED_PROP]);
    });
});

// ─── Import ───────────────────────────────────────────────────────────────────

describe('import', () => {
    async function openImport(user: ReturnType<typeof userEvent.setup>, text: string) {
        await user.click(screen.getByRole('button', { name: /^import$/i }));
        const dialog = screen.getByRole('dialog');
        fireEvent.change(within(dialog).getByRole('textbox'), { target: { value: text } });
        return dialog;
    }

    it('names the encrypted keys it skips and leaves their value alone', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP, ENCRYPTED_PROP, ENCRYPTED_DYNAMIC_PROP]);

        const dialog = await openImport(user, 'secret.key=overwrite\ndynamic.secret=overwrite\nbackend.timeout=9000\nnew.key=1');

        expect(within(dialog).getByText(/Skipped keys \(encrypted\)/)).not.toBeNull();
        expect(within(dialog).getByText('secret.key, dynamic.secret')).not.toBeNull();
        await user.click(within(dialog).getByRole('button', { name: 'Import properties' }));

        expect(await saved()).toEqual([
            { key: 'backend.timeout', value: '9000' },
            { key: 'dynamic.secret', value: 'cipher-text', encrypted: true, dynamic: true },
            { key: 'new.key', value: '1' },
            ENCRYPTED_PROP,
        ]);
    });

    it('fails and saves nothing when the server now holds an imported key encrypted although the page loaded it plain', async () => {
        const user = userEvent.setup();
        renderPage([PLAIN_PROP], { onServer: [{ key: PLAIN_PROP.key, value: '***', encrypted: true }] });

        const dialog = await openImport(user, 'backend.timeout=9000');
        await user.click(within(dialog).getByRole('button', { name: 'Import properties' }));

        expect(await within(dialog).findByText('Property "backend.timeout" is now encrypted and cannot be overwritten')).not.toBeNull();
        expect(savedProperties).toBeUndefined();
    });
});
