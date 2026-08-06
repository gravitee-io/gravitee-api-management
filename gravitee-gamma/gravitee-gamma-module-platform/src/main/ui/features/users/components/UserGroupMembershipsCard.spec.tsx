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
jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../testing/buildModuleNavPathForTests'));

import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { UserGroupMembershipsCard } from './UserGroupMembershipsCard';
import { notify } from '../../../shared/notify';
import { useOrganizationUserGroups } from '../hooks/useOrganizationUser';
import { useAddUserToGroup, useRemoveUserFromGroup, useUpdateUserGroupMembership } from '../hooks/useUserMutations';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../hooks/useOrganizationUser', () => ({
    useOrganizationUserGroups: jest.fn(),
    useOrganizationUserApis: jest.fn(() => ({ data: { data: [] }, isLoading: false })),
    useOrganizationUserApiProducts: jest.fn(() => ({ data: { data: [] }, isLoading: false })),
    useOrganizationUserApplications: jest.fn(() => ({ data: { data: [] }, isLoading: false })),
    useEnvironmentGroups: jest.fn(() => ({ data: { data: [] }, isLoading: false })),
    useGroupMembershipRoleCatalog: jest.fn(() => ({
        data: [
            { id: 'role-user', name: 'USER' },
            { id: 'role-owner', name: 'OWNER' },
        ],
    })),
}));

jest.mock('../hooks/useUserMutations', () => ({
    useAddUserToGroup: jest.fn(),
    useUpdateUserGroupMembership: jest.fn(),
    useRemoveUserFromGroup: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseOrganizationUserGroups = jest.mocked(useOrganizationUserGroups);
const mockUseAddUserToGroup = jest.mocked(useAddUserToGroup);
const mockUseUpdateUserGroupMembership = jest.mocked(useUpdateUserGroupMembership);
const mockUseRemoveUserFromGroup = jest.mocked(useRemoveUserFromGroup);

const ENVIRONMENTS = [
    { id: 'DEFAULT', name: 'Default environment' },
    { id: 'ENV_A', name: 'Default environment A' },
];

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
    Element.prototype.scrollIntoView = jest.fn();
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
});

function renderCard({
    canAddToGroup = false,
    canRemoveFromGroup = false,
    rolesEditable = false,
    environments = ENVIRONMENTS,
}: {
    canAddToGroup?: boolean;
    canRemoveFromGroup?: boolean;
    rolesEditable?: boolean;
    environments?: typeof ENVIRONMENTS;
} = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/environments/default/platform/users/user-1']}>
                <UserGroupMembershipsCard
                    userId="user-1"
                    userDisplayName="Jane Doe"
                    environments={environments}
                    rolesEditable={rolesEditable}
                    canAddToGroup={canAddToGroup}
                    canRemoveFromGroup={canRemoveFromGroup}
                />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('UserGroupMembershipsCard', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockUseAddUserToGroup.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useAddUserToGroup>);
        mockUseUpdateUserGroupMembership.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateUserGroupMembership>);
        mockUseRemoveUserFromGroup.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useRemoveUserFromGroup>);
        mockUseOrganizationUserGroups.mockReturnValue({
            data: {
                data: [
                    {
                        id: 'group-1',
                        name: 'Platform Admins',
                        roles: {
                            GROUP: 'ADMIN',
                            API: 'USER',
                            API_PRODUCT: 'USER',
                            APPLICATION: 'USER',
                            INTEGRATION: 'USER',
                        },
                    },
                ],
                pagination: { page: 1, perPage: 100, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
            },
            isLoading: false,
            isFetching: false,
        } as ReturnType<typeof useOrganizationUserGroups>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('loads groups for the selected environment', async () => {
        renderCard();

        await screen.findByText('Platform Admins');
        expect(screen.getAllByText('Group Admin').length).toBeGreaterThanOrEqual(2);
        expect(mockUseOrganizationUserGroups).toHaveBeenCalledWith('user-1', 'DEFAULT');
    });

    it('lists read-only group roles when editing is disabled', async () => {
        renderCard();

        expect(await screen.findByRole('table', { name: 'Groups table' })).toBeTruthy();
        expect(screen.getByLabelText('Group admin')).toBeTruthy();
        expect(screen.queryByLabelText('API role for Platform Admins')).toBeNull();
    });

    it('renders editable role dropdowns when group roles can be updated', async () => {
        renderCard({ rolesEditable: true });

        expect(await screen.findByLabelText('API role for Platform Admins')).toBeTruthy();
        expect(screen.getByLabelText('API Product role for Platform Admins')).toBeTruthy();
        expect(screen.getByRole('checkbox', { name: 'Group admin for Platform Admins' })).toBeTruthy();
    });

    it('updates group membership roles when a dropdown value changes', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseUpdateUserGroupMembership.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateUserGroupMembership>);

        renderCard({ rolesEditable: true });
        await screen.findByText('Platform Admins');

        await user.click(document.getElementById('group-api-product-role-group-1')!);
        await user.click(await screen.findByRole('option', { name: 'OWNER' }));

        await waitFor(() =>
            expect(mutate).toHaveBeenCalledWith(
                {
                    groupId: 'group-1',
                    isGroupAdmin: true,
                    apiRole: 'USER',
                    apiProductRole: 'OWNER',
                    applicationRole: 'USER',
                    integrationRole: 'USER',
                },
                expect.any(Object),
            ),
        );
    });

    it('blocks clearing every group role', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseUpdateUserGroupMembership.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useUpdateUserGroupMembership>);
        mockUseOrganizationUserGroups.mockReturnValue({
            data: {
                data: [
                    {
                        id: 'group-1',
                        name: 'Platform Admins',
                        roles: { APPLICATION: 'USER' },
                    },
                ],
                pagination: { page: 1, perPage: 100, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
            },
            isLoading: false,
            isFetching: false,
        } as ReturnType<typeof useOrganizationUserGroups>);

        renderCard({ rolesEditable: true });
        await screen.findByText('Platform Admins');

        await user.click(document.getElementById('group-application-role-group-1')!);
        await user.click(await screen.findByRole('option', { name: 'None' }));

        expect(notify.error).toHaveBeenCalledWith('At least one role is mandatory.');
        expect(mutate).not.toHaveBeenCalled();
    });

    it('shows environment tabs and loads groups when switching environments', async () => {
        const user = userEvent.setup();
        renderCard();

        expect(await screen.findByRole('tab', { name: 'Default environment' })).toBeTruthy();
        await user.click(screen.getByRole('tab', { name: 'Default environment A' }));

        expect(mockUseOrganizationUserGroups).toHaveBeenCalledWith('user-1', 'ENV_A');
    });

    it('opens the add group sheet from Add to Group in the card header', async () => {
        const user = userEvent.setup();
        renderCard({ canAddToGroup: true });

        await user.click(await screen.findByRole('button', { name: 'Add to Group' }));
        expect(await screen.findByRole('heading', { name: 'Add a group with roles' })).toBeTruthy();
    });

    it('removes a user from a group after confirmation', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_groupId, options) => options?.onSuccess?.());
        mockUseRemoveUserFromGroup.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useRemoveUserFromGroup>);

        renderCard({ canRemoveFromGroup: true });
        await screen.findByText('Platform Admins');

        await user.click(screen.getByRole('button', { name: 'Remove from Platform Admins' }));
        await user.click(await screen.findByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(mutate).toHaveBeenCalledWith('group-1', expect.any(Object)));
        expect(notify.success).toHaveBeenCalledWith('"Jane Doe" has been deleted from the group "Platform Admins"');
    });

    it('disables remove for API primary owners in a group', async () => {
        mockUseOrganizationUserGroups.mockReturnValue({
            data: {
                data: [
                    {
                        id: 'group-1',
                        name: 'Platform Admins',
                        roles: { API: 'PRIMARY_OWNER' },
                        isApiPrimaryOwner: true,
                    },
                ],
                pagination: { page: 1, perPage: 100, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
            },
            isLoading: false,
            isFetching: false,
        } as ReturnType<typeof useOrganizationUserGroups>);

        renderCard({ canRemoveFromGroup: true });
        await screen.findByText('Platform Admins');

        expect(screen.getByRole('button', { name: 'Remove from Platform Admins' })).toHaveProperty('disabled', true);
    });

    it('locks the API role select when the user is API primary owner in the group', async () => {
        mockUseOrganizationUserGroups.mockReturnValue({
            data: {
                data: [
                    {
                        id: 'group-1',
                        name: 'Platform Admins',
                        roles: { API: 'PRIMARY_OWNER', APPLICATION: 'USER' },
                        isApiPrimaryOwner: true,
                    },
                ],
                pagination: { page: 1, perPage: 100, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
            },
            isLoading: false,
            isFetching: false,
        } as ReturnType<typeof useOrganizationUserGroups>);

        renderCard({ rolesEditable: true, canRemoveFromGroup: true });
        await screen.findByText('Platform Admins');

        expect(document.getElementById('group-api-role-group-1')).toHaveProperty('disabled', true);
        expect(document.getElementById('group-application-role-group-1')).toHaveProperty('disabled', false);
    });

    it('hides the delete column when remove permission is not granted', async () => {
        renderCard({ canRemoveFromGroup: false });
        await screen.findByText('Platform Admins');

        expect(screen.queryByRole('button', { name: 'Remove from Platform Admins' })).toBeNull();
    });

    it('shows inherited permissions tables for the selected environment', async () => {
        renderCard();

        expect(await screen.findByRole('region', { name: 'Inherited APIs table' })).toBeTruthy();
        expect(screen.getByRole('region', { name: 'Inherited API Products table' })).toBeTruthy();
        expect(screen.getByRole('region', { name: 'Inherited Applications table' })).toBeTruthy();
        expect(screen.getByPlaceholderText('Search APIs…')).toBeTruthy();
        expect(screen.getByPlaceholderText('Search API products…')).toBeTruthy();
        expect(screen.getByPlaceholderText('Search applications…')).toBeTruthy();
        expect(screen.getAllByText('No API').length).toBeGreaterThanOrEqual(1);
    });

    it('shows empty groups table and inherited permissions when the user has no groups', async () => {
        mockUseOrganizationUserGroups.mockReturnValue({
            data: { data: [], pagination: { page: 1, perPage: 100, pageCount: 0, pageItemsCount: 0, totalCount: 0 } },
            isLoading: false,
            isFetching: false,
        } as ReturnType<typeof useOrganizationUserGroups>);

        renderCard();

        expect(await screen.findByText('No group')).toBeTruthy();
        expect(screen.getByRole('region', { name: 'Inherited APIs table' })).toBeTruthy();
        expect(screen.getByRole('region', { name: 'Inherited API Products table' })).toBeTruthy();
        expect(screen.getByRole('region', { name: 'Inherited Applications table' })).toBeTruthy();
    });
});
