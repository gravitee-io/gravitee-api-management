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
import { buttonHarness, renderWithGraphene, switchHarness } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { AuthenticationPage } from './AuthenticationPage';
import { useAuthenticationPage } from '../features/authentication/hooks/useAuthenticationPage';
import {
    useDeleteIdentityProvider,
    useSaveLocalLogin,
    useUpdateActivatedIdentityProviders,
} from '../features/authentication/hooks/useIdentityProviderMutations';
import type { IdentityProviderRow } from '../features/authentication/types/identityProvider';
import {
    LOCAL_LOGIN_LOAD_FAILED_TOOLTIP,
    LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP,
    LOCAL_LOGIN_NO_PERMISSION_TOOLTIP,
} from '../features/authentication/utils/identityProviderDisplay';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import { ApimApiError } from '../shared/api/apimClient';
import { notify } from '../shared/notify';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: () => ({ id: 'env-1' }),
    permissionService: { load: jest.fn() },
}));
jest.mock('../features/authentication/hooks/useAuthenticationPage');
jest.mock('../features/authentication/hooks/useIdentityProviderMutations');
jest.mock('../features/organization-settings/hooks/useOrgConsoleSettings');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../features/authentication/components/IdentityProvidersTable', () => ({
    IdentityProvidersTable: ({
        rows,
        canActivate,
        canDelete,
        onToggle,
        onDelete,
    }: {
        rows: IdentityProviderRow[];
        canActivate: boolean;
        canDelete: boolean;
        onToggle: (row: IdentityProviderRow) => void;
        onDelete: (row: IdentityProviderRow) => void;
    }) => (
        <div>
            {rows.map(row => (
                <div key={row.id} data-testid={`row-${row.id}`}>
                    <span>{row.name}</span>
                    {canActivate && (
                        <button type="button" onClick={() => onToggle(row)}>
                            Toggle {row.name}
                        </button>
                    )}
                    {canDelete && (
                        <button type="button" onClick={() => onDelete(row)}>
                            Delete {row.name}
                        </button>
                    )}
                </div>
            ))}
        </div>
    ),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseAuthenticationPage = jest.mocked(useAuthenticationPage);
const mockUseOrgConsoleSettings = jest.mocked(useOrgConsoleSettings);
const mockUseDeleteIdentityProvider = jest.mocked(useDeleteIdentityProvider);
const mockUseUpdateActivatedIdentityProviders = jest.mocked(useUpdateActivatedIdentityProviders);
const mockUseSaveLocalLogin = jest.mocked(useSaveLocalLogin);

const STUB_PROVIDERS = [
    {
        id: 'google-idp',
        name: 'Google',
        description: 'Google SSO',
        enabled: true,
        sync: false,
        type: 'GOOGLE' as const,
        created_at: 1,
        updated_at: 1,
    },
];

function makeQuerySlice<T>(
    successData: T,
    overrides: { data?: T; isLoading?: boolean; isError?: boolean; isSuccess?: boolean; error?: unknown } = {},
) {
    const isError = overrides.isError ?? false;
    const isLoading = overrides.isLoading ?? false;
    return {
        data: 'data' in overrides ? overrides.data : successData,
        isLoading,
        isError,
        isSuccess: overrides.isSuccess ?? (!isError && !isLoading),
        error: overrides.error ?? null,
    };
}

function makeAuthenticationPage({
    providers,
    activations,
}: {
    providers?: Parameters<typeof makeQuerySlice<typeof STUB_PROVIDERS>>[1];
    activations?: Parameters<typeof makeQuerySlice<{ identityProvider: string }[]>>[1];
} = {}): ReturnType<typeof useAuthenticationPage> {
    return {
        providersQuery: makeQuerySlice(STUB_PROVIDERS, providers),
        activationsQuery: makeQuerySlice([{ identityProvider: 'google-idp' }], activations),
    } as ReturnType<typeof useAuthenticationPage>;
}

function makeSettingsResult(overrides: Partial<ReturnType<typeof useOrgConsoleSettings>> = {}): ReturnType<typeof useOrgConsoleSettings> {
    return {
        data: { authentication: { localLogin: { enabled: true } } },
        isLoading: false,
        isError: false,
        ...overrides,
    } as ReturnType<typeof useOrgConsoleSettings>;
}

function makeMutation(mutateAsync = jest.fn()) {
    return { mutateAsync, isPending: false } as never;
}

const IDP_READ_PERMISSION = 'organization-identity_provider-r';

function renderPage() {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['environment-permissions', 'env-1'], [IDP_READ_PERMISSION]);
    const view = renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/authentication']}>
                <AuthenticationPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return { queryClient, ...view };
}

describe('AuthenticationPage', () => {
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
            })),
        });
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseHasPermission.mockReturnValue(true);
        mockUseAuthenticationPage.mockReturnValue(makeAuthenticationPage());
        mockUseOrgConsoleSettings.mockReturnValue(makeSettingsResult());
        mockUseDeleteIdentityProvider.mockReturnValue(makeMutation());
        mockUseUpdateActivatedIdentityProviders.mockReturnValue(makeMutation());
        mockUseSaveLocalLogin.mockReturnValue(makeMutation());
        Element.prototype.scrollIntoView = jest.fn();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the page title and local-login setting', () => {
        renderPage();
        expect(screen.queryByRole('heading', { name: 'Authentication' })).not.toBeNull();
        expect(switchHarness({ name: 'Show login form on management console' }).getElement()).not.toBeNull();
        expect(screen.getByRole('switch', { name: 'Show login form on management console' }).getAttribute('aria-checked')).toBe('true');
        expect(screen.queryByRole('heading', { name: 'Identity Providers' })).not.toBeNull();
    });

    it('treats a missing local-login setting as enabled', () => {
        mockUseOrgConsoleSettings.mockReturnValue(
            makeSettingsResult({
                data: { authentication: { google: { clientId: 'keep-me' } } },
            }),
        );
        renderPage();
        expect(screen.getByRole('switch', { name: 'Show login form on management console' }).getAttribute('aria-checked')).toBe('true');
    });

    it('navigates to the create page from the add button', async () => {
        renderPage();
        await buttonHarness({ name: /Add an identity provider/i }).click();
        expect(mockNavigate).toHaveBeenCalledWith('new');
    });

    it('hides the add button when the user cannot create', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => !anyOf?.includes('organization-identity_provider-c'));
        renderPage();
        expect(screen.queryByRole('button', { name: /Add an identity provider/i })).toBeNull();
    });

    it('shows the educational empty state when there are no providers', () => {
        mockUseAuthenticationPage.mockReturnValue(makeAuthenticationPage({ providers: { data: [] }, activations: { data: [] } }));
        renderPage();
        expect(screen.getByText('No identity providers yet')).not.toBeNull();
        expect(screen.getAllByRole('button', { name: /Add an identity provider/i })).toHaveLength(1);
    });

    it('creates from the empty state instead of a header button', async () => {
        mockUseAuthenticationPage.mockReturnValue(makeAuthenticationPage({ providers: { data: [] }, activations: { data: [] } }));
        renderPage();
        await buttonHarness({ name: /Add an identity provider/i }).click();
        expect(mockNavigate).toHaveBeenCalledWith('new');
    });

    it('shows skeleton rows while data is loading', () => {
        mockUseAuthenticationPage.mockReturnValue(
            makeAuthenticationPage({ providers: { data: undefined, isLoading: true, isSuccess: false } }),
        );
        const { container } = renderPage();
        expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
    });

    it('redirects away on an identity-provider 403', async () => {
        mockUseAuthenticationPage.mockReturnValue(
            makeAuthenticationPage({
                providers: { data: undefined, isError: true, isSuccess: false, error: new ApimApiError(403, 'Forbidden') },
            }),
        );
        renderPage();
        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../applications', { replace: true }));
    });

    it('keeps the identity provider table when organization settings return 403', () => {
        mockUseOrgConsoleSettings.mockReturnValue(
            makeSettingsResult({ data: undefined, isError: true, error: new ApimApiError(403, 'Forbidden') }),
        );
        renderPage();
        expect(screen.getByTestId('row-google-idp')).not.toBeNull();
        expect(mockNavigate).not.toHaveBeenCalled();
        expect(screen.getByText(LOCAL_LOGIN_LOAD_FAILED_TOOLTIP)).not.toBeNull();
    });

    it('confirms delete and shows a success toast', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseDeleteIdentityProvider.mockReturnValue(makeMutation(mutateAsync));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: 'Delete Google' }));
        fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith('google-idp');
            expect(notify.success).toHaveBeenCalledWith('Identity Provider Google successfully deleted!');
        });
    });

    it('confirms deactivation and updates activations', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseUpdateActivatedIdentityProviders.mockReturnValue(makeMutation(mutateAsync));
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: 'Toggle Google' }));
        fireEvent.click(screen.getByRole('button', { name: 'Ok' }));
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith([]);
            expect(notify.success).toHaveBeenCalledWith('Identity Provider Google successfully deactivated!');
        });
    });

    it('posts the full settings object when toggling local login', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseSaveLocalLogin.mockReturnValue(makeMutation(mutateAsync));
        mockUseOrgConsoleSettings.mockReturnValue(
            makeSettingsResult({
                data: {
                    metadata: { readonly: [] },
                    authentication: { google: { clientId: 'keep-me' }, localLogin: { enabled: true } },
                },
            }),
        );
        renderPage();
        await switchHarness({ name: 'Show login form on management console' }).toggle();
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({
                metadata: { readonly: [] },
                authentication: { google: { clientId: 'keep-me' }, localLogin: { enabled: false } },
            });
            expect(notify.success).toHaveBeenCalledWith('Configuration successfully updated!');
        });
    });

    it('does not post local login when settings were not loaded', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseSaveLocalLogin.mockReturnValue(makeMutation(mutateAsync));
        mockUseOrgConsoleSettings.mockReturnValue(makeSettingsResult({ data: undefined }));
        renderPage();
        await switchHarness({ name: 'Show login form on management console' }).toggle();
        expect(mutateAsync).not.toHaveBeenCalled();
    });

    it('disables local login when no identity provider is activated', () => {
        mockUseAuthenticationPage.mockReturnValue(
            makeAuthenticationPage({
                activations: { data: [] },
            }),
        );
        renderPage();
        expect(switchHarness({ name: 'Show login form on management console' }).isDisabled()).toBe(true);
        expect(screen.getByText(LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP)).not.toBeNull();
    });

    it('does not tell the operator to activate a provider while the page is loading', () => {
        mockUseAuthenticationPage.mockReturnValue(
            makeAuthenticationPage({ providers: { data: undefined, isLoading: true, isSuccess: false } }),
        );
        renderPage();
        expect(screen.queryByText(LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP)).toBeNull();
    });

    it('explains a settings load failure on the local-login setting', () => {
        mockUseOrgConsoleSettings.mockReturnValue(makeSettingsResult({ data: undefined, isError: true, error: new Error('failed') }));
        renderPage();
        expect(screen.getByText(LOCAL_LOGIN_LOAD_FAILED_TOOLTIP)).not.toBeNull();
        expect(screen.queryByText(LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP)).toBeNull();
        expect(screen.getByTestId('row-google-idp')).not.toBeNull();
    });

    it('explains why local login is locked when the user cannot update settings', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => !anyOf?.includes('organization-settings-u'));
        renderPage();
        expect(switchHarness({ name: 'Show login form on management console' }).isDisabled()).toBe(true);
        expect(screen.getByText(LOCAL_LOGIN_NO_PERMISSION_TOOLTIP)).not.toBeNull();
    });

    it('keeps the provider list when activations return 403', () => {
        mockUseAuthenticationPage.mockReturnValue(
            makeAuthenticationPage({
                activations: { data: undefined, isError: true, isSuccess: false, error: new ApimApiError(403, 'Forbidden') },
            }),
        );
        const { queryClient } = renderPage();
        expect(screen.getByTestId('row-google-idp')).not.toBeNull();
        expect(mockNavigate).not.toHaveBeenCalled();
        expect(screen.queryByRole('button', { name: 'Toggle Google' })).toBeNull();
        expect(screen.getByText(LOCAL_LOGIN_LOAD_FAILED_TOOLTIP)).not.toBeNull();
        expect(screen.queryByText(LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP)).toBeNull();
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual([IDP_READ_PERMISSION]);
    });

    it('explains a provider list failure on the local-login setting', () => {
        mockUseAuthenticationPage.mockReturnValue(
            makeAuthenticationPage({
                providers: { data: undefined, isError: true, isSuccess: false, error: new Error('failed') },
            }),
        );
        renderPage();
        expect(switchHarness({ name: 'Show login form on management console' }).isDisabled()).toBe(true);
        expect(screen.getByText(LOCAL_LOGIN_LOAD_FAILED_TOOLTIP)).not.toBeNull();
        expect(screen.queryByText(LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP)).toBeNull();
        expect(screen.getByText('Failed to load identity providers. Please refresh and try again.')).not.toBeNull();
    });
});
