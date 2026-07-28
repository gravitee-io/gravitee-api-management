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
import { MemoryRouter } from 'react-router-dom';

import { UsersPage } from './UsersPage';
import { useOrganizationUsers } from '../features/users/hooks/useOrganizationUsers';
import { useCreateOrganizationUser } from '../features/users/hooks/useUserMutations';
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
}));

jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseOrganizationUsers = jest.mocked(useOrganizationUsers);
const mockUseCreateOrganizationUser = jest.mocked(useCreateOrganizationUser);

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
                page: { current: 1, size: 10, per_page: 10, total_pages: 1, total_elements: 2 },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationUsers>);
        mockUseCreateOrganizationUser.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useCreateOrganizationUser>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders users from the API with source and role columns', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderUsersPage();

        expect(await screen.findByRole('link', { name: 'Jane Doe' })).toBeTruthy();
        expect(screen.getByText('john@company.com')).toBeTruthy();
        expect(screen.getByText('Gravitee')).toBeTruthy();
        expect(screen.getByText('Admin')).toBeTruthy();
    });

    it('links each user name to the user detail page', async () => {
        mockUseHasPermission.mockReturnValue(false);
        renderUsersPage();

        const janeLink = await screen.findByRole('link', { name: 'Jane Doe' });
        expect(janeLink.getAttribute('href')).toBe('/user-1');
        expect(screen.getByRole('link', { name: 'John Doe' }).getAttribute('href')).toBe('/user-2');
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
