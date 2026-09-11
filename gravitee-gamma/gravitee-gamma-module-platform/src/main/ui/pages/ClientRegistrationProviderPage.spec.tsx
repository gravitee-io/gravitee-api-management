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
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ClientRegistrationProviderPage } from './ClientRegistrationProviderPage';
import {
    useCreateClientRegistrationProvider,
    useUpdateClientRegistrationProvider,
} from '../features/client-registration/hooks/useClientRegistrationMutations';
import { useClientRegistrationPermissions } from '../features/client-registration/hooks/useClientRegistrationPermissions';
import { useClientRegistrationProvider } from '../features/client-registration/hooks/useClientRegistrationProvider';
import type { ClientRegistrationProvider } from '../features/client-registration/types/clientRegistrationProvider';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: jest.fn(),
}));

jest.mock('../features/client-registration/hooks/useClientRegistrationPermissions');
jest.mock('../features/client-registration/hooks/useClientRegistrationProvider');
jest.mock('../features/client-registration/hooks/useClientRegistrationMutations');

const mockNotifySuccess = jest.fn();
const mockNotifyError = jest.fn();
jest.mock('../shared/notify', () => ({
    notify: {
        success: (msg: string) => mockNotifySuccess(msg),
        error: (err: unknown, fallback?: string) => mockNotifyError(err, fallback),
    },
}));

const mockPermissions = jest.mocked(useClientRegistrationPermissions);
const mockProviderQuery = jest.mocked(useClientRegistrationProvider);
const mockCreate = jest.mocked(useCreateClientRegistrationProvider);
const mockUpdate = jest.mocked(useUpdateClientRegistrationProvider);

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

function nameInput() {
    return screen.getByRole('textbox', { name: /Name/ });
}

async function selectTokenType(user: ReturnType<typeof userEvent.setup>, label: string) {
    await user.click(screen.getByRole('combobox', { name: /Initial Access Token Provider/i }));
    await user.click(await screen.findByRole('option', { name: label }));
}

const PROVIDER: ClientRegistrationProvider = {
    id: 'prov-1',
    name: 'Okta DCR',
    description: 'Prod',
    discovery_endpoint: 'https://idp.example.com/.well-known/openid-configuration',
    initial_access_token_type: 'CLIENT_CREDENTIALS',
    client_id: 'id',
    client_secret: 'secret',
    claim_mappings: { org_id: 'metadata.organization' },
};

function renderCreate() {
    return render(
        <MemoryRouter initialEntries={['/client-registration/new']}>
            <Routes>
                <Route path="/client-registration/new" element={<ClientRegistrationProviderPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

function EditHarness({ nonce = 0 }: { readonly nonce?: number }) {
    void nonce;
    return (
        <MemoryRouter initialEntries={['/client-registration/prov-1']}>
            <Routes>
                <Route path="/client-registration/:providerId" element={<ClientRegistrationProviderPage />} />
            </Routes>
        </MemoryRouter>
    );
}

function renderEdit() {
    return render(<EditHarness />);
}

describe('ClientRegistrationProviderPage', () => {
    const createMutate = jest.fn();
    const updateMutate = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockNavigate.mockReset();
        mockPermissions.mockReturnValue({
            canRead: true,
            canCreate: true,
            canUpdate: true,
            canDelete: true,
            canUpdateSettings: true,
            canSaveProvider: true,
        });
        mockProviderQuery.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProvider>);
        mockCreate.mockReturnValue({ mutate: createMutate, isPending: false } as unknown as ReturnType<
            typeof useCreateClientRegistrationProvider
        >);
        mockUpdate.mockReturnValue({ mutate: updateMutate, isPending: false } as unknown as ReturnType<
            typeof useUpdateClientRegistrationProvider
        >);
    });

    it('shows Client ID and a revealable Client Secret for CLIENT_CREDENTIALS and Initial Access Token otherwise', async () => {
        const user = userEvent.setup();
        renderCreate();
        expect(screen.queryByRole('textbox', { name: /Client ID/ })).toBeNull();

        await selectTokenType(user, 'Client Credentials');
        expect(screen.getByRole('textbox', { name: /Client ID/ })).not.toBeNull();
        const secret = document.getElementById('dcr-client-secret') as HTMLInputElement;
        expect(secret).not.toBeNull();
        expect(secret.type).toBe('password');
        await user.click(screen.getByRole('button', { name: 'Show password' }));
        expect(secret.type).toBe('text');
        expect(screen.queryByRole('textbox', { name: /Initial Access Token/ })).toBeNull();

        await selectTokenType(user, 'Initial Access Token');
        expect(screen.getByRole('textbox', { name: /Initial Access Token/ })).not.toBeNull();
        expect(screen.queryByRole('textbox', { name: /Client ID/ })).toBeNull();
    });

    it('shows validation errors after submitting an empty form', async () => {
        const user = userEvent.setup();
        renderCreate();
        await user.click(screen.getByRole('button', { name: 'Create provider' }));
        expect(screen.getAllByText('This field is required.').length).toBeGreaterThan(0);
        expect(createMutate).not.toHaveBeenCalled();
    });

    it('POSTs on create then navigates to the saved id', async () => {
        const user = userEvent.setup();
        createMutate.mockImplementation((_payload, options) => {
            options.onSuccess({ id: 'prov-1', name: 'Okta DCR' });
        });
        renderCreate();
        fireEvent.change(nameInput(), { target: { value: 'Okta DCR' } });
        fireEvent.change(screen.getByRole('textbox', { name: /OpenID Connect Discovery Endpoint/ }), {
            target: { value: 'https://idp.example.com/.well-known/openid-configuration' },
        });
        await selectTokenType(user, 'Client Credentials');
        fireEvent.change(screen.getByRole('textbox', { name: /Client ID/ }), { target: { value: 'id' } });
        fireEvent.change(document.getElementById('dcr-client-secret') as HTMLInputElement, { target: { value: 'secret' } });
        await user.click(screen.getByRole('button', { name: 'Create provider' }));

        expect(createMutate).toHaveBeenCalledWith(
            expect.objectContaining({
                name: 'Okta DCR',
                discovery_endpoint: 'https://idp.example.com/.well-known/openid-configuration',
                initial_access_token_type: 'CLIENT_CREDENTIALS',
            }),
            expect.any(Object),
        );
        expect(mockNavigate).toHaveBeenCalledWith('../prov-1', { replace: true });
        expect(mockNotifySuccess).toHaveBeenCalledWith('Client registration provider Okta DCR has been created.');
    });

    it('PUTs on update', async () => {
        mockProviderQuery.mockReturnValue({
            data: PROVIDER,
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProvider>);
        renderEdit();
        expect(screen.getByText('Claim Mappings')).not.toBeNull();
        expect(screen.getByDisplayValue('org_id')).not.toBeNull();
        expect(screen.getByRole('textbox', { name: /Client ID/ })).not.toBeNull();
        fireEvent.submit(document.querySelector('form') as HTMLFormElement);
        expect(updateMutate).toHaveBeenCalledWith(
            expect.objectContaining({
                providerId: 'prov-1',
                provider: expect.objectContaining({ name: 'Okta DCR', claim_mappings: { org_id: 'metadata.organization' } }),
            }),
            expect.any(Object),
        );
    });

    it('hides the save footer for a viewer', () => {
        mockPermissions.mockReturnValue({
            canRead: true,
            canCreate: false,
            canUpdate: false,
            canDelete: false,
            canUpdateSettings: false,
            canSaveProvider: false,
        });
        renderCreate();
        expect(screen.queryByRole('button', { name: 'Create provider' })).toBeNull();
        expect(nameInput()).toBeDisabled();
    });

    it('opens edit as read-only for a viewer', () => {
        mockPermissions.mockReturnValue({
            canRead: true,
            canCreate: false,
            canUpdate: false,
            canDelete: false,
            canUpdateSettings: false,
            canSaveProvider: false,
        });
        mockProviderQuery.mockReturnValue({
            data: PROVIDER,
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProvider>);
        renderEdit();
        expect(screen.queryByRole('button', { name: 'Save changes' })).toBeNull();
        expect(nameInput()).toBeDisabled();
        expect(screen.getByDisplayValue('org_id')).toBeDisabled();
        expect(screen.queryByRole('button', { name: 'Show password' })).toBeNull();
        const secret = document.getElementById('dcr-client-secret') as HTMLInputElement;
        expect(secret.type).toBe('password');
        expect(secret.value).toBe('********');
    });

    it('does not wipe in-progress edits when the same provider is refetched', () => {
        mockProviderQuery.mockReturnValue({
            data: PROVIDER,
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProvider>);
        const view = render(<EditHarness nonce={0} />);
        fireEvent.change(nameInput(), { target: { value: 'Draft name' } });

        mockProviderQuery.mockReturnValue({
            data: { ...PROVIDER, name: 'Name from server' },
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProvider>);
        view.rerender(<EditHarness nonce={1} />);

        expect((nameInput() as HTMLInputElement).value).toBe('Draft name');
    });
});
