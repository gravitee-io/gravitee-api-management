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
const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));
jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('../features/roles/hooks/useRoleMemberships');
jest.mock('@tanstack/react-query', () => ({
    ...jest.requireActual('@tanstack/react-query'),
    useQuery: jest.fn(),
}));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useQuery } from '@tanstack/react-query';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { RoleMembersPage } from './RoleMembersPage';
import { useAddRoleMembers, useDeleteRoleMember, useRoleMemberships } from '../features/roles/hooks/useRoleMemberships';
import { ROLE_SEARCH_DEBOUNCE_MS } from '../features/roles/utils/paginationConstants';
import { notify } from '../shared/notify';
import { installFormActionTestEnvironment } from '../shared/testing/formAction';

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseRoleMemberships = jest.mocked(useRoleMemberships);
const mockUseAddRoleMembers = jest.mocked(useAddRoleMembers);
const mockUseDeleteRoleMember = jest.mocked(useDeleteRoleMember);
const mockUseQuery = jest.mocked(useQuery);

let restoreTestEnvironment: () => void;

beforeAll(() => {
    restoreTestEnvironment = installFormActionTestEnvironment();
});

afterAll(() => {
    restoreTestEnvironment();
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function renderPage(path = '/roles/ORGANIZATION/ADMIN/members') {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="roles/:roleScope/:roleName/members" element={<RoleMembersPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

/** Types into the search field, then advances the debounce timer so the (mocked) query results render. */
async function searchFor(user: ReturnType<typeof userEvent.setup>, query: string) {
    await user.type(screen.getByPlaceholderText('Search a user by name or email…'), query);
    await act(async () => {
        jest.advanceTimersByTime(ROLE_SEARCH_DEBOUNCE_MS);
    });
}

describe('RoleMembersPage', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockNavigate.mockClear();
        mockUseHasPermission.mockReturnValue(true);
        mockUseRoleMemberships.mockReturnValue({
            data: [{ id: 'user-1', displayName: 'Jane Doe' }],
            isLoading: false,
        } as ReturnType<typeof useRoleMemberships>);
        mockUseAddRoleMembers.mockReturnValue(makeMutation());
        mockUseDeleteRoleMember.mockReturnValue(makeMutation());
        // The search sheet's useQuery call — no active search by default in these tests.
        mockUseQuery.mockReturnValue({ data: [], isFetching: false } as ReturnType<typeof useQuery>);
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    it('shows the scope/role heading and lists members', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'Organization - ADMIN' })).toBeInTheDocument();
        expect(screen.getByText('Jane Doe')).toBeInTheDocument();
    });

    it('goes back to the roles list', async () => {
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Back to roles' }));

        expect(mockNavigate).toHaveBeenCalledWith('..');
    });

    it('redirects to the roles list for a scope outside ROLE_SCOPES', () => {
        renderPage('/roles/NOT_A_SCOPE/ADMIN/members');

        expect(screen.queryByRole('heading', { name: /ADMIN/ })).not.toBeInTheDocument();
    });

    it('shows an error instead of the members table when the membership list fails to load', () => {
        mockUseRoleMemberships.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useRoleMemberships>);
        renderPage();

        expect(screen.getByText('Failed to load members for this role. Please refresh and try again.')).toBeInTheDocument();
        expect(screen.queryByText('Jane Doe')).not.toBeInTheDocument();
    });

    it('adds a member and shows a success toast', async () => {
        const mutateAsync = jest.fn().mockResolvedValue({ succeededCount: 1, failedCount: 0 });
        mockUseAddRoleMembers.mockReturnValue(makeMutation(mutateAsync));
        mockUseQuery.mockReturnValue({
            data: [{ id: 'user-2', reference: 'USER', displayName: 'John Smith' }],
            isFetching: false,
        } as ReturnType<typeof useQuery>);
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Button to add a member' }));
        await searchFor(user, 'jo');
        await user.click(screen.getByText('John Smith'));
        await user.click(screen.getByRole('button', { name: 'Add a member' }));

        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({
                scope: 'ORGANIZATION',
                roleName: 'ADMIN',
                users: [{ id: 'user-2', reference: 'USER' }],
            });
            expect(notify.success).toHaveBeenCalledWith('Membership successfully created');
        });
    });

    // SearchableUser.id is null for users who come from an external identity provider (Classic: "id can be
    // null if user comes from an LDAP"). Sending id: null forces the backend down the wrong lookup branch —
    // omit the key entirely instead, matching useGroupAddMembersForm's existing pattern.
    it('omits id for a member with no local id (external identity provider user)', async () => {
        const mutateAsync = jest.fn().mockResolvedValue({ succeededCount: 1, failedCount: 0 });
        mockUseAddRoleMembers.mockReturnValue(makeMutation(mutateAsync));
        mockUseQuery.mockReturnValue({
            data: [{ id: null, reference: 'LDAP:cn=jsmith', displayName: 'John Smith' }],
            isFetching: false,
        } as ReturnType<typeof useQuery>);
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Button to add a member' }));
        await searchFor(user, 'jo');
        await user.click(screen.getByText('John Smith'));
        await user.click(screen.getByRole('button', { name: 'Add a member' }));

        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({
                scope: 'ORGANIZATION',
                roleName: 'ADMIN',
                users: [{ reference: 'LDAP:cn=jsmith' }],
            });
        });
    });

    it('reports a partial failure without losing the memberships that succeeded', async () => {
        const mutateAsync = jest.fn().mockResolvedValue({ succeededCount: 1, failedCount: 1 });
        mockUseAddRoleMembers.mockReturnValue(makeMutation(mutateAsync));
        mockUseQuery.mockReturnValue({
            data: [{ id: 'user-2', reference: 'USER', displayName: 'John Smith' }],
            isFetching: false,
        } as ReturnType<typeof useQuery>);
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Button to add a member' }));
        await searchFor(user, 'jo');
        await user.click(screen.getByText('John Smith'));
        await user.click(screen.getByRole('button', { name: 'Add a member' }));

        await waitFor(() => {
            expect(notify.error).toHaveBeenCalledWith('1 of 2 members added; 1 failed.');
        });
    });

    it('keeps the sheet open when every add fails', async () => {
        const mutateAsync = jest.fn().mockResolvedValue({ succeededCount: 0, failedCount: 1 });
        mockUseAddRoleMembers.mockReturnValue(makeMutation(mutateAsync));
        mockUseQuery.mockReturnValue({
            data: [{ id: 'user-2', reference: 'USER', displayName: 'John Smith' }],
            isFetching: false,
        } as ReturnType<typeof useQuery>);
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Button to add a member' }));
        await searchFor(user, 'jo');
        await user.click(screen.getByText('John Smith'));
        await user.click(screen.getByRole('button', { name: 'Add a member' }));

        await waitFor(() => {
            expect(notify.error).toHaveBeenCalledWith('Failed to add the selected members.');
        });
        expect(screen.getByRole('heading', { name: 'Add Members' })).toBeInTheDocument();
    });

    it('clears the search and selection when the add-member sheet is reopened after being cancelled', async () => {
        mockUseQuery.mockReturnValue({
            data: [{ id: 'user-2', reference: 'USER', displayName: 'John Smith' }],
            isFetching: false,
        } as ReturnType<typeof useQuery>);
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Button to add a member' }));
        await searchFor(user, 'jo');
        await user.click(screen.getByText('John Smith'));
        expect(screen.getByText('1 user selected')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Cancel' }));
        await user.click(screen.getByRole('button', { name: 'Button to add a member' }));

        expect(screen.getByPlaceholderText('Search a user by name or email…')).toHaveValue('');
        expect(screen.queryByText('1 user selected')).not.toBeInTheDocument();
    });

    it('deletes a member and shows a success toast', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseDeleteRoleMember.mockReturnValue(makeMutation(mutateAsync));
        const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Button to delete a member' }));
        expect(screen.getByRole('heading', { name: 'Delete a membership' })).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Delete' }));

        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({ scope: 'ORGANIZATION', roleName: 'ADMIN', userId: 'user-1' });
            expect(notify.success).toHaveBeenCalledWith('Membership has been successfully deleted');
        });
    });

    it('hides the add-member action without manage permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();

        expect(screen.queryByRole('button', { name: 'Button to add a member' })).not.toBeInTheDocument();
    });
});
