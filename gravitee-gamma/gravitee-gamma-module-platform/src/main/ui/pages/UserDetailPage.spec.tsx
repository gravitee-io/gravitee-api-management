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
    useOrganizationUserGroups,
} from '../features/users/hooks/useOrganizationUser';
import { useProcessUserRegistration, useUpdateOrganizationUserRoles } from '../features/users/hooks/useUserMutations';
import type { OrganizationUser } from '../features/users/types/user';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../features/users/hooks/useOrganizationUser', () => ({
    useOrganizationUser: jest.fn(),
    useOrganizationEnvironments: jest.fn(),
    useOrganizationUserGroups: jest.fn(),
    useOrganizationRoleCatalog: jest.fn(),
    useEnvironmentRoleCatalog: jest.fn(),
}));

jest.mock('../features/users/hooks/useUserMutations', () => ({
    useProcessUserRegistration: jest.fn(),
    useUpdateOrganizationUserRoles: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseOrganizationUser = jest.mocked(useOrganizationUser);
const mockUseOrganizationEnvironments = jest.mocked(useOrganizationEnvironments);
const mockUseOrganizationUserGroups = jest.mocked(useOrganizationUserGroups);
const mockUseOrganizationRoleCatalog = jest.mocked(useOrganizationRoleCatalog);
const mockUseEnvironmentRoleCatalog = jest.mocked(useEnvironmentRoleCatalog);
const mockUseProcessUserRegistration = jest.mocked(useProcessUserRegistration);
const mockUseUpdateOrganizationUserRoles = jest.mocked(useUpdateOrganizationUserRoles);

async function commitOrganizationRoleChange(user: ReturnType<typeof userEvent.setup>, roleLabel: string) {
    await user.click(screen.getByRole('button', { name: 'Organization roles' }));
    await user.click(screen.getByRole('checkbox', { name: roleLabel }));
    await user.click(screen.getByRole('button', { name: 'Organization roles' }));
}

async function commitEnvironmentRoleChange(user: ReturnType<typeof userEvent.setup>, environmentName: string, roleLabel: string) {
    await user.click(screen.getByRole('button', { name: `Environment roles for ${environmentName}` }));
    await user.click(screen.getByRole('checkbox', { name: roleLabel }));
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
    roles: [{ id: 'org-user', name: 'User', scope: 'ORGANIZATION' }],
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
        mockUseOrganizationUserGroups.mockReturnValue({
            data: [],
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserGroups>);
        mockUseOrganizationRoleCatalog.mockReturnValue({
            data: [
                { id: 'org-admin', name: 'ADMIN' },
                { id: 'org-user', name: 'User' },
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
        mockUseUpdateOrganizationUserRoles.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateOrganizationUserRoles>);
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

        await commitOrganizationRoleChange(user, 'Admin');

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
        expect(screen.getByText('Not a member of any groups')).toBeTruthy();
        expect(screen.queryByText('Password')).toBeNull();
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

    it('lists group memberships when the user belongs to groups', async () => {
        mockUseOrganizationUserGroups.mockReturnValue({
            data: [
                { id: 'group-1', name: 'Platform Admins' },
                { id: 'group-2', name: 'API Owners' },
            ],
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserGroups>);

        renderUserDetailPage();

        expect(await screen.findByText('Platform Admins')).toBeTruthy();
        expect(screen.getByText('API Owners')).toBeTruthy();
        expect(screen.queryByText('Not a member of any groups')).toBeNull();
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

    it('shows a not-found message when the user fails to load', async () => {
        mockUseOrganizationUser.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useOrganizationUser>);

        renderUserDetailPage();

        expect(await screen.findByText('User not found or failed to load.')).toBeTruthy();
    });
});
