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
import { fireEvent, render, screen } from '@testing-library/react';

import { DirectMembersTable } from './DirectMembersTable';
import type { ApplicationUiMember, EditState } from '../../types/applicationMembers.types';

const member: ApplicationUiMember = {
    id: 'user-1',
    displayName: 'Abhishek Kumar',
    email: 'abhishek.kumar@graviteesource.com',
    roles: [{ name: 'USER', scope: 'APPLICATION' }],
};

const otherMember: ApplicationUiMember = {
    id: 'user-2',
    displayName: 'Jane Doe',
    roles: [{ name: 'ADMIN', scope: 'APPLICATION' }],
};

function renderTable(editState: EditState = null) {
    const onStartEdit = jest.fn();
    const onRoleChange = jest.fn();
    const onSaveRole = jest.fn();
    const onCancelEdit = jest.fn();
    const onRemove = jest.fn();

    render(
        <DirectMembersTable
            members={[member, otherMember]}
            roles={['USER', 'ADMIN']}
            editState={editState}
            onStartEdit={onStartEdit}
            onRoleChange={onRoleChange}
            onSaveRole={onSaveRole}
            onCancelEdit={onCancelEdit}
            onRemove={onRemove}
            isSaving={false}
            isRemoving={false}
            canManageMembers
            canEditRole
            canRemoveMember
        />,
    );

    return { onStartEdit, onRoleChange, onSaveRole, onCancelEdit, onRemove };
}

describe('DirectMembersTable', () => {
    it('shows role badge when the row is not being edited', () => {
        renderTable(null);
        expect(screen.getByText('User')).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
    });

    it('shows inline edit controls when editState.memberId matches the row', () => {
        renderTable({ memberId: 'user-1', role: 'USER' });
        expect(screen.getByRole('button', { name: 'Save' })).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Cancel edit' })).not.toBeNull();
        expect(screen.getByText('Jane Doe')).not.toBeNull();
        expect(screen.getByText('Admin')).not.toBeNull();
    });

    it('disables Save when the selected role is unchanged', () => {
        renderTable({ memberId: 'user-1', role: 'USER' });
        expect(screen.getByRole('button', { name: 'Save' }).disabled).toBe(true);
    });

    it('enables Save when the selected role differs from the current role', () => {
        renderTable({ memberId: 'user-1', role: 'ADMIN' });
        expect(screen.getByRole('button', { name: 'Save' }).disabled).toBe(false);
    });

    it('invokes onCancelEdit when cancel is clicked', () => {
        const { onCancelEdit } = renderTable({ memberId: 'user-1', role: 'USER' });
        fireEvent.click(screen.getByRole('button', { name: 'Cancel edit' }));
        expect(onCancelEdit).toHaveBeenCalledTimes(1);
    });

    it('invokes onSaveRole when Save is clicked with a changed role', () => {
        const { onSaveRole } = renderTable({ memberId: 'user-1', role: 'ADMIN' });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        expect(onSaveRole).toHaveBeenCalledTimes(1);
    });
});
