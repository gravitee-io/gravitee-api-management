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
import { useQuery } from '@tanstack/react-query';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';

import { GroupAddMembersSheet } from './GroupAddMembersSheet';
import { installFormActionTestEnvironment } from '../../../shared/testing/formAction';
import type { SearchableUser } from '../../../shared/types/userSearch';
import type { GroupMember } from '../types/group';
import { GROUP_SEARCH_DEBOUNCE_MS } from '../utils/paginationConstants';

jest.mock('@tanstack/react-query', () => ({
    ...jest.requireActual('@tanstack/react-query'),
    useQuery: jest.fn(),
}));

let restoreTestEnvironment: () => void;

beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
    restoreTestEnvironment = installFormActionTestEnvironment();
});

afterAll(() => {
    restoreTestEnvironment();
});

const mockUseQuery = jest.mocked(useQuery);

const RESULTS: SearchableUser[] = [
    { id: 'user-1', reference: 'user-1', displayName: 'Anna Schmidt', email: 'anna@lufthansa.com' },
    { id: 'user-2', reference: 'user-2', displayName: 'Jonas Keller', email: 'jonas@lufthansa.com' },
    { id: null, reference: 'ldap-anna', displayName: 'LDAP Anna', email: 'ldap@example.com' },
];

function typeSearch(value: string) {
    fireEvent.change(screen.getByPlaceholderText('Search by name or email…'), { target: { value } });
    act(() => {
        jest.advanceTimersByTime(GROUP_SEARCH_DEBOUNCE_MS);
    });
}

async function submitMembers() {
    await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Add member' }));
        await Promise.resolve();
    });
}

function renderSheet(overrides: Partial<React.ComponentProps<typeof GroupAddMembersSheet>> = {}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(
        <GroupAddMembersSheet
            open
            groupName="API Team"
            groupRoles={undefined}
            members={[]}
            apiRoles={[
                { name: 'USER', scope: 'API' },
                { name: 'OWNER', scope: 'API' },
            ]}
            applicationRoles={[{ name: 'USER', scope: 'APPLICATION' }]}
            apiProductRoles={[{ name: 'USER', scope: 'API_PRODUCT' }]}
            integrationRoles={[{ name: 'USER', scope: 'INTEGRATION' }]}
            clusterRoles={[{ name: 'USER', scope: 'CLUSTER' }]}
            explorerRoles={[{ name: 'USER', scope: 'EXPLORER' }]}
            lockApiRole={false}
            lockApiProductRole={false}
            lockApplicationRole={false}
            canOverrideLocks
            maxInvitation={null}
            onClose={onClose}
            onSubmit={onSubmit}
            {...overrides}
        />,
    );
    return { onClose, onSubmit };
}

describe('GroupAddMembersSheet', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockUseQuery.mockReturnValue({ data: RESULTS, isFetching: false } as ReturnType<typeof useQuery>);
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('does not render sheet content when closed', () => {
        renderSheet({ open: false });
        expect(screen.queryByRole('heading', { name: 'Add members' })).toBeNull();
    });

    it('has no "None" role option — classic\'s add-members-dialog has no such mat-option', () => {
        renderSheet();
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.queryByRole('option', { name: 'None' })).toBeNull();
    });

    it('shows the group name in the description and the default-roles selects', () => {
        renderSheet();
        expect(screen.getByText('Search platform users and assign roles for membership in API Team.')).not.toBeNull();
        expect(screen.getByText('API')).not.toBeNull();
        expect(screen.getByText('API product')).not.toBeNull();
        expect(screen.getByText('Application')).not.toBeNull();
        expect(screen.getByText('Integration')).not.toBeNull();
        expect(screen.getByText('Cluster')).not.toBeNull();
        expect(screen.getByText('Explorer')).not.toBeNull();
    });

    it('seeds the search field from initialSearch', () => {
        renderSheet({ initialSearch: 'anna@lufthansa.com' });
        expect(screen.getByPlaceholderText('Search by name or email…')).toHaveProperty('value', 'anna@lufthansa.com');
    });

    it('has no "make selected users group admins" option — that only applies when editing an existing member', () => {
        renderSheet();
        expect(screen.queryByText(/group admin/i)).toBeNull();
    });

    it('prompts for at least 2 characters before showing results', () => {
        renderSheet();
        typeSearch('a');
        expect(screen.queryByText('Type at least 2 characters to search for users.')).not.toBeNull();
        expect(screen.queryByText('Anna Schmidt')).toBeNull();
    });

    it('excludes users who are already members from the results', () => {
        const existingMember: GroupMember = { id: 'user-1', displayName: 'Anna Schmidt', roles: {} };
        renderSheet({ members: [existingMember] });
        typeSearch('an');
        expect(screen.queryByText('Anna Schmidt')).toBeNull();
        expect(screen.queryByText('Jonas Keller')).not.toBeNull();
    });

    it('disables the submit button until at least one user is selected', () => {
        renderSheet();
        expect(screen.getByRole('button', { name: 'Add member' })).toHaveProperty('disabled', true);
    });

    it('submits selected users with all classic group-member role scopes', async () => {
        const { onSubmit } = renderSheet();

        typeSearch('an');
        fireEvent.click(screen.getByText('Anna Schmidt'));

        await submitMembers();

        expect(onSubmit).toHaveBeenCalledWith([
            {
                id: 'user-1',
                reference: 'user-1',
                roles: [
                    { scope: 'API', name: 'USER' },
                    { scope: 'API_PRODUCT', name: 'USER' },
                    { scope: 'APPLICATION', name: 'USER' },
                    { scope: 'INTEGRATION', name: 'USER' },
                    { scope: 'CLUSTER', name: 'USER' },
                    { scope: 'EXPLORER', name: 'USER' },
                ],
            },
        ]);
    });

    it('omits id for LDAP users so the backend resolves via reference', async () => {
        const { onSubmit } = renderSheet();

        typeSearch('ld');
        fireEvent.click(screen.getByText('LDAP Anna'));
        await submitMembers();

        expect(onSubmit).toHaveBeenCalledWith([
            {
                reference: 'ldap-anna',
                roles: [
                    { scope: 'API', name: 'USER' },
                    { scope: 'API_PRODUCT', name: 'USER' },
                    { scope: 'APPLICATION', name: 'USER' },
                    { scope: 'INTEGRATION', name: 'USER' },
                    { scope: 'CLUSTER', name: 'USER' },
                    { scope: 'EXPLORER', name: 'USER' },
                ],
            },
        ]);
    });

    describe('default role pre-fill', () => {
        it('pre-fills API/API product/Application from the group’s configured default roles', async () => {
            const { onSubmit } = renderSheet({ groupRoles: { API: 'OWNER', API_PRODUCT: 'OWNER', APPLICATION: 'USER' } });

            typeSearch('an');
            fireEvent.click(screen.getByText('Anna Schmidt'));
            await submitMembers();

            expect(onSubmit).toHaveBeenCalledWith([
                {
                    id: 'user-1',
                    reference: 'user-1',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'API_PRODUCT', name: 'OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                        { scope: 'INTEGRATION', name: 'USER' },
                        { scope: 'CLUSTER', name: 'USER' },
                        { scope: 'EXPLORER', name: 'USER' },
                    ],
                },
            ]);
        });

        it('falls back to USER for any scope missing from the group’s configured roles', async () => {
            const { onSubmit } = renderSheet({ groupRoles: { API: 'OWNER' } });

            typeSearch('an');
            fireEvent.click(screen.getByText('Anna Schmidt'));
            await submitMembers();

            expect(onSubmit).toHaveBeenCalledWith([
                {
                    id: 'user-1',
                    reference: 'user-1',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'API_PRODUCT', name: 'USER' },
                        { scope: 'APPLICATION', name: 'USER' },
                        { scope: 'INTEGRATION', name: 'USER' },
                        { scope: 'CLUSTER', name: 'USER' },
                        { scope: 'EXPLORER', name: 'USER' },
                    ],
                },
            ]);
        });

        it('always defaults Integration/Cluster/Explorer to USER — classic hardcodes these regardless of group config', async () => {
            const { onSubmit } = renderSheet();

            typeSearch('an');
            fireEvent.click(screen.getByText('Anna Schmidt'));
            await submitMembers();

            expect(onSubmit).toHaveBeenCalledWith([
                expect.objectContaining({
                    roles: expect.arrayContaining([
                        { scope: 'INTEGRATION', name: 'USER' },
                        { scope: 'CLUSTER', name: 'USER' },
                        { scope: 'EXPLORER', name: 'USER' },
                    ]),
                }),
            ]);
        });
    });

    it('disables the PRIMARY_OWNER option for a scope that already has a primary owner', () => {
        const existingOwner: GroupMember = { id: 'user-3', displayName: 'Ravi Patel', roles: { API: 'PRIMARY_OWNER' } };
        renderSheet({
            members: [existingOwner],
            apiRoles: [
                { name: 'OWNER', scope: 'API' },
                { name: 'PRIMARY_OWNER', scope: 'API', system: true },
            ],
        });

        fireEvent.click(screen.getAllByRole('combobox')[0]);

        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).toBe('true');
        expect(screen.getByRole('option', { name: 'OWNER' }).getAttribute('aria-disabled')).not.toBe('true');
    });

    it('disables system roles other than PRIMARY_OWNER on API, and all system roles on Application', () => {
        renderSheet({
            apiPrimaryOwnerMode: 'GROUP',
            apiProductPrimaryOwnerMode: 'GROUP',
            apiRoles: [
                { name: 'USER', scope: 'API' },
                { name: 'ADMIN', scope: 'API', system: true },
                { name: 'PRIMARY_OWNER', scope: 'API', system: true },
            ],
            applicationRoles: [
                { name: 'USER', scope: 'APPLICATION' },
                { name: 'ADMIN', scope: 'APPLICATION', system: true },
            ],
        });

        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.getByRole('option', { name: 'ADMIN' }).getAttribute('aria-disabled')).toBe('true');
        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).not.toBe('true');

        // Close the open listbox before opening Application.
        fireEvent.keyDown(document.activeElement ?? document.body, { key: 'Escape' });
        fireEvent.click(screen.getAllByRole('combobox')[2]);
        expect(screen.getByRole('option', { name: 'ADMIN' }).getAttribute('aria-disabled')).toBe('true');
        expect(screen.getByRole('option', { name: 'USER' }).getAttribute('aria-disabled')).not.toBe('true');
    });

    it('locks role selects and search while the React action is pending', async () => {
        let resolveMembers: (() => void) | undefined;
        const onSubmit = jest.fn(
            () =>
                new Promise<void>(resolve => {
                    resolveMembers = resolve;
                }),
        );
        renderSheet({ onSubmit });
        typeSearch('an');
        fireEvent.click(screen.getByText('Anna Schmidt'));
        fireEvent.click(screen.getByRole('button', { name: 'Add member' }));

        expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', true);
        expect(screen.getByLabelText('Search users')).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Adding…' })).toHaveProperty('disabled', true);

        await act(async () => {
            resolveMembers?.();
            await Promise.resolve();
        });
        await waitFor(() => expect(screen.getByRole('button', { name: 'Add member' })).not.toBeNull());
    });

    it('associates the search label with the search input', () => {
        renderSheet();
        expect(screen.getByLabelText('Search users')).toHaveProperty('placeholder', 'Search by name or email…');
    });

    it('calls onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    describe('lock flags', () => {
        it('disables a locked role select without canOverrideLocks', () => {
            renderSheet({ lockApiRole: true, canOverrideLocks: false });
            expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', true);
        });

        it('leaves a locked role select enabled with canOverrideLocks', () => {
            renderSheet({ lockApiRole: true, canOverrideLocks: true });
            expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', false);
        });

        it('leaves an unlocked role select enabled without canOverrideLocks', () => {
            renderSheet({ lockApiRole: false, canOverrideLocks: false });
            expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', false);
        });

        it('disables Integration, Cluster, and Explorer without canOverrideLocks, regardless of lock flags', () => {
            renderSheet({ canOverrideLocks: false });
            const comboboxes = screen.getAllByRole('combobox');
            expect(comboboxes[3]).toHaveProperty('disabled', true);
            expect(comboboxes[4]).toHaveProperty('disabled', true);
            expect(comboboxes[5]).toHaveProperty('disabled', true);
        });
    });

    describe('member limit', () => {
        it('disables the search input once existing members reach the limit', () => {
            const existing: GroupMember = { id: 'user-9', displayName: 'Existing Member', roles: {} };
            renderSheet({ members: [existing], maxInvitation: 1 });
            expect(screen.getByPlaceholderText('Search by name or email…')).toHaveProperty('disabled', true);
            expect(screen.getByText('This group has reached its maximum number of members.')).not.toBeNull();
        });

        it('keeps selected users visible and removable when their selection reaches the limit', () => {
            renderSheet({ maxInvitation: 1 });

            typeSearch('an');
            fireEvent.click(screen.getByText('Anna Schmidt'));

            expect(screen.getByText('Anna Schmidt')).not.toBeNull();
            expect(screen.getByRole('button', { name: 'Remove Anna Schmidt' })).not.toBeNull();
            expect(screen.getByPlaceholderText('Search by name or email…')).toHaveProperty('disabled', true);
            expect(screen.getByText('Selection limit reached for this group.')).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Remove Anna Schmidt' }));

            expect(screen.queryByRole('button', { name: 'Remove Anna Schmidt' })).toBeNull();
            expect(screen.getByPlaceholderText('Search by name or email…')).toHaveProperty('disabled', false);
        });

        it('leaves the search input enabled below the limit', () => {
            const existing: GroupMember = { id: 'user-9', displayName: 'Existing Member', roles: {} };
            renderSheet({ members: [existing], maxInvitation: 2 });
            expect(screen.getByPlaceholderText('Search by name or email…')).toHaveProperty('disabled', false);
        });
    });

    describe('primary owner constraints', () => {
        const apiRolesWithOwner = [
            { name: 'USER', scope: 'API' },
            { name: 'OWNER', scope: 'API' },
            { name: 'PRIMARY_OWNER', scope: 'API', system: true },
        ];

        it('disables PRIMARY_OWNER while environment settings are still unknown', () => {
            renderSheet({ apiRoles: apiRolesWithOwner });

            fireEvent.click(screen.getAllByRole('combobox')[0]);

            expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).toBe('true');
            expect(screen.getByRole('option', { name: 'OWNER' }).getAttribute('aria-disabled')).not.toBe('true');
        });

        it('disables PRIMARY_OWNER when the environment primary owner mode is USER, even with no owner yet', () => {
            renderSheet({ apiPrimaryOwnerMode: 'USER', apiRoles: apiRolesWithOwner });

            fireEvent.click(screen.getAllByRole('combobox')[0]);

            expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).toBe('true');
            expect(screen.getByRole('option', { name: 'OWNER' }).getAttribute('aria-disabled')).not.toBe('true');
        });

        it('keeps only a single selected user once PRIMARY_OWNER is chosen', async () => {
            const { onSubmit } = renderSheet({
                apiPrimaryOwnerMode: 'GROUP',
                apiProductPrimaryOwnerMode: 'GROUP',
                apiRoles: apiRolesWithOwner,
            });

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'PRIMARY_OWNER' }));

            typeSearch('an');
            fireEvent.click(screen.getByText('Anna Schmidt'));
            fireEvent.click(screen.getByText('Jonas Keller'));

            expect(screen.getByText('1 user selected')).not.toBeNull();

            await submitMembers();

            expect(onSubmit).toHaveBeenCalledWith([
                {
                    id: 'user-2',
                    reference: 'user-2',
                    roles: [
                        { scope: 'API', name: 'PRIMARY_OWNER' },
                        { scope: 'API_PRODUCT', name: 'USER' },
                        { scope: 'APPLICATION', name: 'USER' },
                        { scope: 'INTEGRATION', name: 'USER' },
                        { scope: 'CLUSTER', name: 'USER' },
                        { scope: 'EXPLORER', name: 'USER' },
                    ],
                },
            ]);
        });

        it('clears the current selection when the API role changes', () => {
            renderSheet({
                apiPrimaryOwnerMode: 'GROUP',
                apiProductPrimaryOwnerMode: 'GROUP',
                apiRoles: apiRolesWithOwner,
            });

            typeSearch('an');
            fireEvent.click(screen.getByText('Anna Schmidt'));
            expect(screen.getByText('1 user selected')).not.toBeNull();

            fireEvent.click(screen.getAllByRole('combobox')[0]);
            fireEvent.click(screen.getByRole('option', { name: 'OWNER' }));

            expect(screen.queryByText('1 user selected')).toBeNull();
            expect(screen.getByRole('button', { name: 'Add member' })).toHaveProperty('disabled', true);
        });
    });
});
