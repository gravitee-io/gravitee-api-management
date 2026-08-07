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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { UserDetailPage } from './UserDetailPage';
import {
    useEnvironmentRoleCatalog,
    useOrganizationEnvironments,
    useOrganizationRoleCatalog,
    useOrganizationUser,
} from '../features/users/hooks/useOrganizationUser';
import {
    useProcessUserRegistration,
    useResetOrganizationUserPassword,
    useUpdateOrganizationUserRoles,
    useUpdateOrganizationUserServiceAccount,
} from '../features/users/hooks/useUserMutations';
import type { OrganizationUser } from '../features/users/types/user';
import {
    ORGANIZATION_USER_CREATE_PERMISSION,
    ORGANIZATION_USER_DELETE_PERMISSION,
    ORGANIZATION_USER_UPDATE_PERMISSION,
} from '../features/users/utils/userPermissions';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: jest.fn().mockReturnValue({ id: 'DEFAULT' }),
}));

jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../features/users/hooks/useOrganizationUser', () => ({
    useOrganizationUser: jest.fn(),
    useOrganizationEnvironments: jest.fn(),
    useOrganizationRoleCatalog: jest.fn(),
    useEnvironmentRoleCatalog: jest.fn(),
}));

const mockUserGroupMembershipsCard = jest.fn(
    ({
        canAddToGroup,
        canRemoveFromGroup,
        rolesEditable,
    }: {
        canAddToGroup?: boolean;
        canRemoveFromGroup?: boolean;
        rolesEditable?: boolean;
    }) => (
        <div
            data-testid="group-memberships"
            data-can-add={String(canAddToGroup)}
            data-can-remove={String(canRemoveFromGroup)}
            data-roles-editable={String(rolesEditable)}
        />
    ),
);

jest.mock('../features/users/components/UserGroupMembershipsCard', () => ({
    UserGroupMembershipsCard: (props: Parameters<typeof mockUserGroupMembershipsCard>[0]) => mockUserGroupMembershipsCard(props),
}));

jest.mock('../features/users/components/UserPersonalAccessTokensCard', () => ({
    UserPersonalAccessTokensCard: ({ canGenerate, canRevoke }: { canGenerate: boolean; canRevoke: boolean }) => (
        <div data-testid="personal-access-tokens" data-can-generate={String(canGenerate)} data-can-revoke={String(canRevoke)} />
    ),
}));

jest.mock('../features/users/hooks/useUserMutations', () => ({
    useProcessUserRegistration: jest.fn(),
    useUpdateOrganizationUserRoles: jest.fn(),
    useUpdateOrganizationUserServiceAccount: jest.fn(),
    useResetOrganizationUserPassword: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseOrganizationUser = jest.mocked(useOrganizationUser);
const mockUseOrganizationEnvironments = jest.mocked(useOrganizationEnvironments);
const mockUseOrganizationRoleCatalog = jest.mocked(useOrganizationRoleCatalog);
const mockUseEnvironmentRoleCatalog = jest.mocked(useEnvironmentRoleCatalog);
const mockUseProcessUserRegistration = jest.mocked(useProcessUserRegistration);
const mockUseUpdateOrganizationUserServiceAccount = jest.mocked(useUpdateOrganizationUserServiceAccount);
const mockUseUpdateOrganizationUserRoles = jest.mocked(useUpdateOrganizationUserRoles);
const mockUseResetOrganizationUserPassword = jest.mocked(useResetOrganizationUserPassword);

async function commitOrganizationRoleChange(user: ReturnType<typeof userEvent.setup>, roleLabel: string) {
    await user.click(screen.getByRole('button', { name: 'Organization roles' }));
    await user.click(screen.getByRole('button', { name: roleLabel }));
    await user.click(screen.getByRole('button', { name: 'Organization roles' }));
}

async function commitEnvironmentRoleChange(user: ReturnType<typeof userEvent.setup>, environmentName: string, roleLabel: string) {
    await user.click(screen.getByRole('button', { name: `Environment roles for ${environmentName}` }));
    await user.click(screen.getByRole('button', { name: roleLabel }));
    await user.click(screen.getByRole('button', { name: `Environment roles for ${environmentName}` }));
}

const DETAIL_USER: OrganizationUser = {
    id: 'user-1',
    displayName: 'Anna Schmidt',
    email: 'anna.schmidt@swissport.com',
    status: 'PENDING',
    source: 'ldap',
    hasPassword: false,
    isServiceAccount: false,
    created_at: Date.parse('2025-07-10'),
    roles: [{ id: 'org-user', name: 'USER', scope: 'ORGANIZATION' }],
    envRoles: {
        prod: [{ id: 'env-api-user', name: 'API_USER' }],
        staging: [
            { id: 'env-api-user', name: 'API_USER' },
            { id: 'env-api-publisher', name: 'API_PUBLISHER' },
        ],
    },
    customFields: { department: 'Operations' },
};

function renderUserDetailPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/users/user-1']}>
                <Routes>
                    <Route path="/users/:userId" element={<UserDetailPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

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

describe('UserDetailPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(false);
        mockUseOrganizationUser.mockReturnValue({
            data: DETAIL_USER,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);
        mockUseOrganizationEnvironments.mockReturnValue({
            data: [
                { id: 'prod', name: 'Production', description: 'Live production environment' },
                { id: 'staging', name: 'Staging', description: 'Pre-production staging' },
            ],
            isLoading: false,
        } as ReturnType<typeof useOrganizationEnvironments>);
        mockUseOrganizationRoleCatalog.mockReturnValue({
            data: [
                { id: 'org-admin', name: 'ADMIN' },
                { id: 'org-user', name: 'USER' },
            ],
            isLoading: false,
        } as ReturnType<typeof useOrganizationRoleCatalog>);
        mockUseEnvironmentRoleCatalog.mockReturnValue({
            data: [
                { id: 'env-api-user', name: 'API_USER' },
                { id: 'env-api-publisher', name: 'API_PUBLISHER' },
            ],
            isLoading: false,
        } as ReturnType<typeof useEnvironmentRoleCatalog>);
        mockUseProcessUserRegistration.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useProcessUserRegistration>);
        mockUseUpdateOrganizationUserServiceAccount.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateOrganizationUserServiceAccount>);
        mockUseUpdateOrganizationUserRoles.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateOrganizationUserRoles>);
        mockUseResetOrganizationUserPassword.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useResetOrganizationUserPassword>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('shows loading placeholders while the user is loading', () => {
        mockUseOrganizationUser.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        expect(document.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
        expect(screen.queryByRole('heading', { name: 'Anna Schmidt' })).toBeNull();
    });

    it('disables role selectors for active users without update permission', async () => {
        mockUseHasPermission.mockReturnValue(false);
        mockUseOrganizationUser.mockReturnValue({
            data: { ...DETAIL_USER, status: 'ACTIVE' },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        const organizationRolesButton = await screen.findByRole('button', { name: 'Organization roles' });
        expect(organizationRolesButton).toHaveProperty('disabled', true);
    });

    it('submits an organization role update when roles are changed', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: { ...DETAIL_USER, status: 'ACTIVE' },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);
        mockUseUpdateOrganizationUserRoles.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateOrganizationUserRoles>);

        renderUserDetailPage();
        await screen.findByRole('heading', { name: 'Anna Schmidt' });

        await commitOrganizationRoleChange(user, 'ADMIN');

        await waitFor(() =>
            expect(mutate).toHaveBeenCalledWith({ referenceType: 'ORGANIZATION', roles: ['org-user', 'org-admin'] }, expect.any(Object)),
        );
    });

    it('submits an environment role update when environment roles are changed', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: { ...DETAIL_USER, status: 'ACTIVE' },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);
        mockUseUpdateOrganizationUserRoles.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateOrganizationUserRoles>);

        renderUserDetailPage();
        await screen.findByRole('heading', { name: 'Anna Schmidt' });

        await commitEnvironmentRoleChange(user, 'Production', 'API_PUBLISHER');

        await waitFor(() =>
            expect(mutate).toHaveBeenCalledWith(
                { referenceType: 'ENVIRONMENT', referenceId: 'prod', roles: ['env-api-user', 'env-api-publisher'] },
                expect.any(Object),
            ),
        );
    });

    it('shows a success toast after registration is accepted', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_accepted: boolean, options?: { onSuccess?: () => void }) => options?.onSuccess?.());
        mockUseHasPermission.mockReturnValue(true);
        mockUseProcessUserRegistration.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useProcessUserRegistration>);

        renderUserDetailPage();

        await user.click(await screen.findByRole('button', { name: 'Accept user registration' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Accept' }));

        await waitFor(() => expect(notify.success).toHaveBeenCalledWith('User "Anna Schmidt" has been accepted'));
    });

    it('renders profile metadata, role selectors, and an empty group memberships state', async () => {
        renderUserDetailPage();

        expect(await screen.findByRole('heading', { name: 'Anna Schmidt' })).toBeTruthy();
        expect(screen.getByText('anna.schmidt@swissport.com')).toBeTruthy();
        expect(screen.getByText('Pending')).toBeTruthy();
        expect(screen.getByText('LDAP')).toBeTruthy();
        expect(screen.getAllByText('Organization Roles').length).toBeGreaterThanOrEqual(1);
        expect(screen.getByLabelText('Organization roles')).toBeTruthy();
        expect(screen.getByText('department')).toBeTruthy();
        expect(screen.getByText('Operations')).toBeTruthy();
        expect(screen.getByText('Environment Roles')).toBeTruthy();
        expect(screen.getByText('Production')).toBeTruthy();
        expect(screen.getByLabelText('Environment roles for Staging')).toBeTruthy();
        expect(screen.getByTestId('group-memberships')).toBeTruthy();
        expect(screen.queryByText('Password')).toBeNull();
    });

    it('passes group membership permissions for active users based on create, update, and delete access', async () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: readonly string[] }) => {
            if (anyOf.includes(ORGANIZATION_USER_UPDATE_PERMISSION)) return true;
            if (anyOf.includes(ORGANIZATION_USER_CREATE_PERMISSION)) return true;
            if (anyOf.includes(ORGANIZATION_USER_DELETE_PERMISSION)) return false;
            return false;
        });
        mockUseOrganizationUser.mockReturnValue({
            data: { ...DETAIL_USER, status: 'ACTIVE' },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();
        await screen.findByTestId('group-memberships');

        expect(mockUserGroupMembershipsCard).toHaveBeenCalledWith(
            expect.objectContaining({
                rolesEditable: true,
                canAddToGroup: true,
                canRemoveFromGroup: false,
            }),
        );
    });

    it('disables group membership actions for inactive users even when permissions are granted', async () => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: { ...DETAIL_USER, status: 'ARCHIVED' },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();
        await screen.findByTestId('group-memberships');

        expect(mockUserGroupMembershipsCard).toHaveBeenCalledWith(
            expect.objectContaining({
                rolesEditable: false,
                canAddToGroup: false,
                canRemoveFromGroup: false,
            }),
        );
    });

    it('disables role selectors for pending users even when update permission is granted', async () => {
        mockUseHasPermission.mockReturnValue(true);
        renderUserDetailPage();

        const organizationRolesButton = await screen.findByRole('button', { name: 'Organization roles' });
        const stagingRolesButton = screen.getByRole('button', { name: 'Environment roles for Staging' });

        expect(organizationRolesButton).toHaveProperty('disabled', true);
        expect(stagingRolesButton).toHaveProperty('disabled', true);
    });

    it('enables role selectors for active users with update permission', async () => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: { ...DETAIL_USER, status: 'ACTIVE' },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        const organizationRolesButton = await screen.findByRole('button', { name: 'Organization roles' });
        const stagingRolesButton = screen.getByRole('button', { name: 'Environment roles for Staging' });

        expect(organizationRolesButton).toHaveProperty('disabled', false);
        expect(stagingRolesButton).toHaveProperty('disabled', false);
    });

    it('opens a confirmation dialog and submits accept registration after confirm', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseHasPermission.mockReturnValue(true);
        mockUseProcessUserRegistration.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useProcessUserRegistration>);

        renderUserDetailPage();

        await user.click(await screen.findByRole('button', { name: 'Accept user registration' }));
        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText(/accept the registration request of Anna Schmidt/i)).toBeTruthy();

        await user.click(within(dialog).getByRole('button', { name: 'Accept' }));

        await waitFor(() => expect(mutate).toHaveBeenCalledWith(true, expect.any(Object)));
    });

    it('opens a confirmation dialog and submits reject registration after confirm', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseHasPermission.mockReturnValue(true);
        mockUseProcessUserRegistration.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useProcessUserRegistration>);

        renderUserDetailPage();

        await user.click(await screen.findByRole('button', { name: 'Reject user registration' }));
        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText(/reject the registration request of Anna Schmidt/i)).toBeTruthy();

        await user.click(within(dialog).getByRole('button', { name: 'Reject' }));

        await waitFor(() => expect(mutate).toHaveBeenCalledWith(false, expect.any(Object)));
    });

    it('does not submit registration when the confirmation dialog is cancelled', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseHasPermission.mockReturnValue(true);
        mockUseProcessUserRegistration.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useProcessUserRegistration>);

        renderUserDetailPage();

        await user.click(await screen.findByRole('button', { name: 'Accept user registration' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancel' }));

        expect(screen.queryByRole('dialog')).toBeNull();
        expect(mutate).not.toHaveBeenCalled();
    });

    it('shows registration actions for pending users when update permission is granted', async () => {
        mockUseHasPermission.mockReturnValue(true);
        renderUserDetailPage();

        expect(await screen.findByText('Registration Pending')).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Accept user registration' })).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Reject user registration' })).toBeTruthy();
    });

    it('hides registration actions without update permission', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderUserDetailPage();

        await screen.findByRole('heading', { name: 'Anna Schmidt' });
        expect(screen.queryByText('Registration Pending')).toBeNull();
    });

    it('shows convert to service account for eligible gravitee users when update permission is granted', async () => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'gravitee',
                isServiceAccount: undefined,
                hasPassword: false,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        expect(await screen.findByRole('button', { name: 'Convert to service account' })).toBeTruthy();
    });

    it('hides convert to service account when the user already has a password flag', async () => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'gravitee',
                isServiceAccount: undefined,
                hasPassword: true,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        await screen.findByRole('heading', { name: 'Anna Schmidt' });
        expect(screen.queryByRole('button', { name: 'Convert to service account' })).toBeNull();
    });

    it('opens a confirmation dialog and submits service account conversion after confirm', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'gravitee',
                isServiceAccount: undefined,
                hasPassword: false,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);
        mockUseUpdateOrganizationUserServiceAccount.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateOrganizationUserServiceAccount>);

        renderUserDetailPage();

        await user.click(await screen.findByRole('button', { name: 'Convert to service account' }));
        const dialog = await screen.findByRole('dialog');
        expect(dialog.textContent).toMatch(/convert Anna Schmidt to a service account/i);
        expect(dialog.textContent).toMatch(/cannot be undone/i);

        await user.click(within(dialog).getByRole('button', { name: 'Convert' }));
        expect(mutate).toHaveBeenCalledWith(true, expect.any(Object));
    });

    it('shows a success toast after service account conversion', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_serviceAccount: boolean, options?: { onSuccess?: () => void }) => options?.onSuccess?.());
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'gravitee',
                isServiceAccount: undefined,
                hasPassword: false,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);
        mockUseUpdateOrganizationUserServiceAccount.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateOrganizationUserServiceAccount>);

        renderUserDetailPage();

        await user.click(await screen.findByRole('button', { name: 'Convert to service account' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Convert' }));

        await waitFor(() => expect(notify.success).toHaveBeenCalledWith('User "Anna Schmidt" has been converted to a service account'));
    });

    it('passes token permissions separately for generate and revoke actions', async () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: readonly string[] }) => {
            if (anyOf.includes(ORGANIZATION_USER_UPDATE_PERMISSION)) {
                return true;
            }
            if (anyOf.includes(ORGANIZATION_USER_DELETE_PERMISSION)) {
                return false;
            }
            return false;
        });

        renderUserDetailPage();

        await screen.findByRole('heading', { name: 'Anna Schmidt' });
        const tokensCard = screen.getByTestId('personal-access-tokens');
        expect(tokensCard.getAttribute('data-can-generate')).toBe('true');
        expect(tokensCard.getAttribute('data-can-revoke')).toBe('false');
    });

    it('shows a not-found message when the user fails to load', async () => {
        mockUseOrganizationUser.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        expect(await screen.findByText('User not found or failed to load.')).toBeTruthy();
    });

    it('shows reset password for active gravitee users with a local password', async () => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'gravitee',
                hasPassword: true,
                isServiceAccount: false,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        expect(await screen.findByRole('button', { name: 'Reset password' })).toBeTruthy();
        expect(screen.queryByRole('button', { name: 'Convert to service account' })).toBeNull();
    });

    it('shows reset password for SSO-only gravitee users like classic console', async () => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'gravitee',
                hasPassword: false,
                isServiceAccount: undefined,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        expect(await screen.findByRole('button', { name: 'Reset password' })).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Convert to service account' })).toBeTruthy();
    });

    it('calls reset password for SSO-only gravitee users like classic console', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_value: undefined, options?: { onSuccess?: () => void }) => options?.onSuccess?.());
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'gravitee',
                hasPassword: false,
                isServiceAccount: undefined,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);
        mockUseResetOrganizationUserPassword.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useResetOrganizationUserPassword>);

        renderUserDetailPage();

        await user.click(await screen.findByRole('button', { name: 'Reset password' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Reset' }));

        expect(mutate).toHaveBeenCalled();
    });

    it('hides reset password for external identity provider users', async () => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'ldap',
                hasPassword: true,
                isServiceAccount: false,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        await screen.findByRole('heading', { name: 'Anna Schmidt' });
        expect(screen.queryByRole('button', { name: 'Reset password' })).toBeNull();
    });

    it('shows a success toast after password reset', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_value: undefined, options?: { onSuccess?: () => void }) => options?.onSuccess?.());
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUser.mockReturnValue({
            data: {
                ...DETAIL_USER,
                status: 'ACTIVE',
                source: 'gravitee',
                hasPassword: true,
                isServiceAccount: false,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUser>);
        mockUseResetOrganizationUserPassword.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useResetOrganizationUserPassword>);

        renderUserDetailPage();

        await user.click(await screen.findByRole('button', { name: 'Reset password' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Reset' }));

        await waitFor(() => expect(notify.success).toHaveBeenCalledWith('The password of user "Anna Schmidt" has been successfully reset'));
    });
});
