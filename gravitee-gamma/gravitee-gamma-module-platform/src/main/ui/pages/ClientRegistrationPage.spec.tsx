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
import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { ClientRegistrationPage } from './ClientRegistrationPage';
import { useDeleteClientRegistrationProvider } from '../features/client-registration/hooks/useClientRegistrationMutations';
import { useClientRegistrationPermissions } from '../features/client-registration/hooks/useClientRegistrationPermissions';
import { useClientRegistrationProviders } from '../features/client-registration/hooks/useClientRegistrationProviders';
import type { ClientRegistrationProvider } from '../features/client-registration/types/clientRegistrationProvider';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import type { PortalSettings } from '../features/security-plan-types/services/portalSettings';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasFeature: jest.fn(),
    useHasPermission: jest.fn(),
    useEnvironment: jest.fn(),
}));

jest.mock('../features/client-registration/hooks/useClientRegistrationPermissions');
jest.mock('../features/client-registration/hooks/useClientRegistrationProviders');
jest.mock('../features/client-registration/hooks/useClientRegistrationMutations');
jest.mock('../features/security-plan-types/hooks/usePortalSettings');
jest.mock('../features/security-plan-types/hooks/useSavePortalSettings');

const mockNotifySuccess = jest.fn();
const mockNotifyError = jest.fn();
jest.mock('../shared/notify', () => ({
    notify: {
        success: (msg: string) => mockNotifySuccess(msg),
        error: (err: unknown, fallback?: string) => mockNotifyError(err, fallback),
    },
}));

const mockUseHasFeature = jest.mocked(useHasFeature);
const mockPermissions = jest.mocked(useClientRegistrationPermissions);
const mockProviders = jest.mocked(useClientRegistrationProviders);
const mockDelete = jest.mocked(useDeleteClientRegistrationProvider);
const mockPortalSettings = jest.mocked(usePortalSettings);
const mockSavePortalSettings = jest.mocked(useSavePortalSettings);

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

const SETTINGS: PortalSettings = {
    plan: { security: { jwt: { enabled: true } } },
    application: {
        registration: { enabled: false },
        types: {
            simple: { enabled: true },
            browser: { enabled: true },
            web: { enabled: true },
            native: { enabled: true },
            backend_to_backend: { enabled: true },
        },
    },
};

const PROVIDER: ClientRegistrationProvider = {
    id: 'prov-1',
    name: 'Okta DCR',
    description: 'Prod',
    discovery_endpoint: 'https://idp.example.com/.well-known/openid-configuration',
    initial_access_token_type: 'CLIENT_CREDENTIALS',
};

function renderPage() {
    return render(
        <MemoryRouter>
            <ClientRegistrationPage />
        </MemoryRouter>,
    );
}

describe('ClientRegistrationPage', () => {
    const mutate = jest.fn();
    const deleteMutate = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockNavigate.mockReset();
        mockUseHasFeature.mockReturnValue(true);
        mockPermissions.mockReturnValue({
            canRead: true,
            canCreate: true,
            canUpdate: true,
            canDelete: true,
            canUpdateSettings: true,
            canSaveProvider: true,
        });
        mockPortalSettings.mockReturnValue({
            data: SETTINGS,
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof usePortalSettings>);
        mockProviders.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProviders>);
        mockSavePortalSettings.mockReturnValue({ mutate, isPending: false } as unknown as ReturnType<typeof useSavePortalSettings>);
        mockDelete.mockReturnValue({ mutate: deleteMutate, isPending: false } as unknown as ReturnType<
            typeof useDeleteClientRegistrationProvider
        >);
    });

    it('shows the educational empty state when there are no providers', () => {
        renderPage();
        expect(screen.getByText('Why add a DCR provider?')).not.toBeNull();
        expect(screen.getByRole('button', { name: /Add a provider/ })).not.toBeNull();
    });

    it('renders a provider row and disables Add at one provider', () => {
        mockProviders.mockReturnValue({
            data: [PROVIDER],
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProviders>);
        renderPage();
        expect(screen.getByText('Okta DCR')).not.toBeNull();
        expect(screen.queryByText('Why add a DCR provider?')).toBeNull();
        expect(screen.getByRole('button', { name: /Add a provider/ })).toBeDisabled();
    });

    it('hides Add without create permission', () => {
        mockPermissions.mockReturnValue({
            canRead: true,
            canCreate: false,
            canUpdate: true,
            canDelete: true,
            canUpdateSettings: true,
            canSaveProvider: true,
        });
        renderPage();
        expect(screen.queryByRole('button', { name: /Add a provider/ })).toBeNull();
    });

    it('hides Delete without delete permission', async () => {
        mockPermissions.mockReturnValue({
            canRead: true,
            canCreate: true,
            canUpdate: true,
            canDelete: false,
            canUpdateSettings: true,
            canSaveProvider: true,
        });
        mockProviders.mockReturnValue({
            data: [PROVIDER],
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProviders>);
        const user = userEvent.setup();
        renderPage();
        await user.click(screen.getByRole('button', { name: 'Provider actions' }));
        expect(screen.queryByText('Delete')).toBeNull();
    });

    it('opens the license dialog when Add is clicked without apim-dcr-registration', async () => {
        mockUseHasFeature.mockReturnValue(false);
        const user = userEvent.setup();
        renderPage();
        await user.click(screen.getByRole('button', { name: /Add a provider/ }));
        expect(mockNavigate).not.toHaveBeenCalled();
        expect(screen.getByText('Dynamic Client Registration')).not.toBeNull();
    });

    it('POSTs the full settings document when a type toggle changes', () => {
        renderPage();
        fireEvent.click(screen.getByLabelText('Simple'));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                plan: SETTINGS.plan,
                application: expect.objectContaining({
                    types: expect.objectContaining({ simple: { enabled: false } }),
                }),
            }),
            expect.any(Object),
        );
    });

    it('disables the DCR toggle when metadata.readonly includes application.registration.enabled', () => {
        mockPortalSettings.mockReturnValue({
            data: { ...SETTINGS, metadata: { readonly: ['application.registration.enabled'] } },
            isLoading: false,
            isError: false,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.getByLabelText('Enable Dynamic Client Registration')).toBeDisabled();
    });

    it('shows Try again on load error', () => {
        mockProviders.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useClientRegistrationProviders>);
        renderPage();
        expect(screen.getByText('Could not load client registration.')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Try again' })).not.toBeNull();
    });
});
