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
import { fireEvent, render, screen } from '@testing-library/react';

import { GroupAddMembersSheet } from './GroupAddMembersSheet';
import type { GroupMember, SearchableUser } from '../types/group';

jest.mock('@tanstack/react-query', () => ({
    ...jest.requireActual('@tanstack/react-query'),
    useQuery: jest.fn(),
}));

// Radix Select scrolls the highlighted option into view — not implemented in jsdom.
beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
});

const mockUseQuery = jest.mocked(useQuery);

const RESULTS: SearchableUser[] = [
    { id: 'user-1', reference: 'user-1', displayName: 'Anna Schmidt', email: 'anna@lufthansa.com' },
    { id: 'user-2', reference: 'user-2', displayName: 'Jonas Keller', email: 'jonas@lufthansa.com' },
];

function renderSheet(overrides: Partial<React.ComponentProps<typeof GroupAddMembersSheet>> = {}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(
        <GroupAddMembersSheet
            open
            groupName="API Team"
            groupRoles={undefined}
            members={[]}
            apiRoles={[{ name: 'USER', scope: 'API' }, { name: 'OWNER', scope: 'API' }]}
            applicationRoles={[{ name: 'USER', scope: 'APPLICATION' }]}
            apiProductRoles={[{ name: 'USER', scope: 'API_PRODUCT' }]}
            integrationRoles={[{ name: 'USER', scope: 'INTEGRATION' }]}
            clusterRoles={[{ name: 'USER', scope: 'CLUSTER' }]}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={false}
            {...overrides}
        />,
    );
    return { onClose, onSubmit };
}

describe('GroupAddMembersSheet', () => {
    beforeEach(() => {
        mockUseQuery.mockReturnValue({ data: RESULTS, isFetching: false } as ReturnType<typeof useQuery>);
    });

    it('does not render sheet content when closed', () => {
        renderSheet({ open: false });
        expect(screen.queryByRole('heading', { name: 'Add members' })).toBeNull();
    });

    it('shows the group name in the description and the default-roles selects', () => {
        renderSheet();
        expect(screen.getByText('Search platform users and assign roles for membership in API Team.')).not.toBeNull();
        expect(screen.getByText('API')).not.toBeNull();
        expect(screen.getByText('API product')).not.toBeNull();
        expect(screen.getByText('Application')).not.toBeNull();
        expect(screen.getByText('Integration')).not.toBeNull();
        expect(screen.getByText('Cluster')).not.toBeNull();
    });

    it('has no "make selected users group admins" option — that only applies when editing an existing member', () => {
        renderSheet();
        expect(screen.queryByText(/group admin/i)).toBeNull();
    });

    it('prompts for at least 2 characters before showing results', () => {
        renderSheet();
        fireEvent.change(screen.getByPlaceholderText('Search by name or email…'), { target: { value: 'a' } });
        expect(screen.queryByText('Type at least 2 characters to search for users.')).not.toBeNull();
        expect(screen.queryByText('Anna Schmidt')).toBeNull();
    });

    it('excludes users who are already members from the results', () => {
        const existingMember: GroupMember = { id: 'user-1', displayName: 'Anna Schmidt', roles: {} };
        renderSheet({ members: [existingMember] });
        fireEvent.change(screen.getByPlaceholderText('Search by name or email…'), { target: { value: 'an' } });
        expect(screen.queryByText('Anna Schmidt')).toBeNull();
        expect(screen.queryByText('Jonas Keller')).not.toBeNull();
    });

    it('disables the submit button until at least one user is selected', () => {
        renderSheet();
        expect(screen.getByRole('button', { name: 'Add member' })).toHaveProperty('disabled', true);
    });

    it('submits selected users with the pre-filled API/API product/Application roles (Integration/Cluster left unset)', () => {
        const { onSubmit } = renderSheet();

        fireEvent.change(screen.getByPlaceholderText('Search by name or email…'), { target: { value: 'an' } });
        fireEvent.click(screen.getByText('Anna Schmidt'));

        fireEvent.click(screen.getByRole('button', { name: 'Add member' }));

        expect(onSubmit).toHaveBeenCalledWith([
            {
                id: 'user-1',
                reference: 'user-1',
                roles: [
                    { scope: 'API', name: 'USER' },
                    { scope: 'API_PRODUCT', name: 'USER' },
                    { scope: 'APPLICATION', name: 'USER' },
                ],
            },
        ]);
    });

    describe('default role pre-fill', () => {
        // Mirrors classic AddMembersDialogComponent.initializeForm(): `group.roles['API'] ?? 'USER'` — the
        // Add Members form starts from the group's own configured defaults, not blank.
        it('pre-fills API/API product/Application from the group’s configured default roles', () => {
            const { onSubmit } = renderSheet({ groupRoles: { API: 'OWNER', API_PRODUCT: 'OWNER', APPLICATION: 'USER' } });

            fireEvent.change(screen.getByPlaceholderText('Search by name or email…'), { target: { value: 'an' } });
            fireEvent.click(screen.getByText('Anna Schmidt'));
            fireEvent.click(screen.getByRole('button', { name: 'Add member' }));

            expect(onSubmit).toHaveBeenCalledWith([
                {
                    id: 'user-1',
                    reference: 'user-1',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'API_PRODUCT', name: 'OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
            ]);
        });

        it('falls back to USER for any scope missing from the group’s configured roles', () => {
            const { onSubmit } = renderSheet({ groupRoles: { API: 'OWNER' } });

            fireEvent.change(screen.getByPlaceholderText('Search by name or email…'), { target: { value: 'an' } });
            fireEvent.click(screen.getByText('Anna Schmidt'));
            fireEvent.click(screen.getByRole('button', { name: 'Add member' }));

            expect(onSubmit).toHaveBeenCalledWith([
                {
                    id: 'user-1',
                    reference: 'user-1',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'API_PRODUCT', name: 'USER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
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

    it('calls onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
