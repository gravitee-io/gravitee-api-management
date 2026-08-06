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
import { buttonHarness, inputHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { UsersPage } from './UsersPage';
import { useOrganizationUsers } from '../features/users/hooks/useOrganizationUsers';
import { useCreateOrganizationUser, useDeleteOrganizationUser } from '../features/users/hooks/useUserMutations';
import type { OrganizationUser } from '../features/users/types/user';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

jest.mock('../features/users/hooks/useOrganizationUsers', () => ({
    useOrganizationUsers: jest.fn(),
    useIdentityProviders: jest.fn().mockReturnValue({ data: [{ id: 'gravitee', name: 'Gravitee' }], isLoading: false }),
}));

jest.mock('../features/users/hooks/useUserMutations', () => ({
    useCreateOrganizationUser: jest.fn(),
    useDeleteOrganizationUser: jest.fn(),
}));

jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseOrganizationUsers = jest.mocked(useOrganizationUsers);
const mockUseCreateOrganizationUser = jest.mocked(useCreateOrganizationUser);
const mockUseDeleteOrganizationUser = jest.mocked(useDeleteOrganizationUser);

const SAMPLE_USERS: OrganizationUser[] = [
    {
        id: 'user-1',
        displayName: 'Jane Doe',
        email: 'jane@company.com',
        status: 'ACTIVE',
        source: 'gravitee',
        roles: [{ name: 'User', scope: 'ORGANIZATION' }],
        lastConnectionAt: Date.now() - 5 * 60 * 1000,
        primary_owner: false,
        number_of_active_tokens: 0,
    },
    {
        id: 'user-2',
        displayName: 'John Doe',
        email: 'john@company.com',
        status: 'PENDING',
        source: 'ldap',
        roles: [{ name: 'Admin', scope: 'ORGANIZATION' }],
        primary_owner: true,
        number_of_active_tokens: 1,
    },
    {
        id: 'user-3',
        displayName: 'Automation Bot',
        email: 'bot@company.com',
        status: 'ACTIVE',
        source: 'gravitee',
        isServiceAccount: true,
        roles: [{ name: 'User', scope: 'ORGANIZATION' }],
        primary_owner: false,
        number_of_active_tokens: 0,
    },
];

function renderUsersPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <UsersPage />
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

describe('UsersPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => anyOf?.includes('organization-user-c') ?? false);
        mockUseOrganizationUsers.mockReturnValue({
            data: {
                data: SAMPLE_USERS,
                page: { current: 1, size: 10, per_page: 10, total_pages: 1, total_elements: 3 },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUsers>);
        mockUseCreateOrganizationUser.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useCreateOrganizationUser>);
        mockUseDeleteOrganizationUser.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useDeleteOrganizationUser>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders users from the API with source column', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderUsersPage();

        expect(await screen.findByRole('link', { name: 'Jane Doe' })).toBeTruthy();
        expect(screen.getByText('john@company.com')).toBeTruthy();
        expect(screen.getAllByText('Gravitee').length).toBeGreaterThanOrEqual(1);
    });

    it('links each user name to the user detail page', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderUsersPage();

        const janeLink = await screen.findByRole('link', { name: 'Jane Doe' });
        expect(janeLink.getAttribute('href')).toBe('/user-1');
        expect(screen.getByRole('link', { name: 'John Doe' }).getAttribute('href')).toBe('/user-2');
    });

    it('shows pending registration status in the users list', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderUsersPage();

        expect(await screen.findByText('Pending')).toBeTruthy();
        expect(screen.getAllByText('Active').length).toBeGreaterThanOrEqual(1);
    });

    it('shows a service account badge in the users list', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderUsersPage();

        expect(await screen.findByText('Service account')).toBeTruthy();
    });

    it('hides Add User when the user lacks create permission', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderUsersPage();

        await screen.findByText('Jane Doe');
        expect(screen.queryByRole('button', { name: /Add User/i })).toBeNull();
    });

    it('hides the header Add User button during first-use and keeps the empty-state CTA', async () => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationUsers.mockReturnValue({
            data: {
                data: [],
                page: { current: 1, size: 10, per_page: 10, total_pages: 0, total_elements: 0 },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUsers>);

        renderUsersPage();

        expect(await screen.findByText('No users yet')).toBeTruthy();
        expect(screen.getAllByRole('button', { name: /Add User/i })).toHaveLength(1);
    });

    it('shows Add User and submits create payload for admins', async () => {
        mockUseHasPermission.mockReturnValue(true);
        const mutate = jest.fn((_payload, options?: { onSuccess?: () => void }) => options?.onSuccess?.());
        mockUseCreateOrganizationUser.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useCreateOrganizationUser>);

        renderUsersPage();
        await screen.findByText('Jane Doe');

        await buttonHarness({ name: /Add User/i }).click();
        await inputHarness({ name: /First Name/i }).type('New');
        await inputHarness({ name: /Last Name/i }).type('Person');
        await inputHarness({ name: /^Email/i }).type('new@company.com');
        await buttonHarness({ name: /^Add User$/ }).click();

        await waitFor(() => expect(mutate).toHaveBeenCalled());
        expect(mutate.mock.calls[0]?.[0]).toEqual({
            firstname: 'New',
            lastname: 'Person',
            email: 'new@company.com',
            source: 'gravitee',
            sourceId: '',
            service: false,
        });
        expect(notify.success).toHaveBeenCalledWith('New user successfully registered!');
    });

    it('shows delete actions for deletable users when delete permission is granted', async () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => anyOf?.includes('organization-user-d') ?? false);
        renderUsersPage();

        expect(await screen.findByRole('button', { name: 'Delete user Jane Doe' })).toBeTruthy();
        expect(screen.queryByRole('button', { name: 'Delete user John Doe' })).toBeNull();
    });

    it('opens a confirmation dialog and deletes a user after confirm', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_userId: string, options?: { onSuccess?: () => void }) => options?.onSuccess?.());
        mockUseHasPermission.mockImplementation(({ anyOf }) => anyOf?.includes('organization-user-d') ?? false);
        mockUseDeleteOrganizationUser.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useDeleteOrganizationUser>);

        renderUsersPage();

        await user.click(await screen.findByRole('button', { name: 'Delete user Jane Doe' }));
        expect(await screen.findByRole('dialog')).toBeTruthy();
        await user.click(screen.getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(mutate).toHaveBeenCalledWith('user-1', expect.any(Object)));
        expect(notify.success).toHaveBeenCalledWith('User Jane Doe is being deleted!');
    });

    it('shows a load error message when the users query fails', async () => {
        mockUseHasPermission.mockReturnValue(false);
        mockUseOrganizationUsers.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useOrganizationUsers>);

        renderUsersPage();

        expect(await screen.findByText('Failed to load users. Please refresh and try again.')).toBeTruthy();
    });
});
