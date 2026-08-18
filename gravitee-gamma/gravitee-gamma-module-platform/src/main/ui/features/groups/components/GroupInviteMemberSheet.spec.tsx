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

import { GroupInviteMemberSheet } from './GroupInviteMemberSheet';
import type { GroupMember } from '../types/group';

beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
});

function renderSheet(overrides: Partial<React.ComponentProps<typeof GroupInviteMemberSheet>> = {}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(
        <GroupInviteMemberSheet
            open
            groupName="API Team"
            groupRoles={undefined}
            members={[]}
            apiRoles={[
                { name: 'USER', scope: 'API' },
                { name: 'OWNER', scope: 'API' },
                { name: 'PRIMARY_OWNER', scope: 'API', system: true },
            ]}
            applicationRoles={[{ name: 'USER', scope: 'APPLICATION' }]}
            lockApiRole={false}
            lockApplicationRole={false}
            canOverrideLocks
            apiPrimaryOwnerMode="HYBRID"
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={false}
            {...overrides}
        />,
    );
    return { onClose, onSubmit };
}

describe('GroupInviteMemberSheet', () => {
    it('does not render sheet content when closed', () => {
        renderSheet({ open: false });
        expect(screen.queryByRole('heading', { name: 'Email invitation' })).toBeNull();
    });

    it('shows the group name in the description', () => {
        renderSheet();
        expect(screen.getByText('Invite a new user to API Team and assign their default roles.')).not.toBeNull();
    });

    it('disables Send invitation until an email is entered', () => {
        renderSheet();
        expect(screen.getByRole('button', { name: 'Send invitation' })).toHaveProperty('disabled', true);
    });

    it('submits the email with the USER fallback default roles when the group has no configured defaults', () => {
        const { onSubmit } = renderSheet();

        fireEvent.change(screen.getByRole('textbox', { name: /Email/i }), { target: { value: 'anna.schmidt@lufthansa.com' } });
        fireEvent.click(screen.getByRole('button', { name: 'Send invitation' }));

        expect(onSubmit).toHaveBeenCalledWith({ email: 'anna.schmidt@lufthansa.com', apiRole: 'USER', applicationRole: 'USER' });
    });

    it('has no "None" role option — classic\'s invite-member-dialog has no such mat-option', () => {
        renderSheet();
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.queryByRole('option', { name: 'None' })).toBeNull();
    });

    describe('default role pre-fill', () => {
        it('pre-fills API/Application from the group’s configured default roles', () => {
            const { onSubmit } = renderSheet({ groupRoles: { API: 'OWNER', APPLICATION: 'USER' } });

            fireEvent.change(screen.getByRole('textbox', { name: /Email/i }), { target: { value: 'anna.schmidt@lufthansa.com' } });
            fireEvent.click(screen.getByRole('button', { name: 'Send invitation' }));

            expect(onSubmit).toHaveBeenCalledWith({ email: 'anna.schmidt@lufthansa.com', apiRole: 'OWNER', applicationRole: 'USER' });
        });
    });

    it('calls onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('disables the PRIMARY_OWNER option once one already exists', () => {
        const existingOwner: GroupMember = { id: 'user-3', displayName: 'Ravi Patel', roles: { API: 'PRIMARY_OWNER' } };
        renderSheet({ members: [existingOwner] });

        fireEvent.click(screen.getAllByRole('combobox')[0]);

        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).toBe('true');
        expect(screen.getByRole('option', { name: 'OWNER' }).getAttribute('aria-disabled')).not.toBe('true');
    });

    it('leaves PRIMARY_OWNER selectable when no one holds it yet', () => {
        renderSheet();
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).not.toBe('true');
    });

    it('disables PRIMARY_OWNER when API primary-owner mode is USER', () => {
        renderSheet({ apiPrimaryOwnerMode: 'USER' });
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).toBe('true');
    });

    it('disables non-PRIMARY_OWNER system roles on the API select', () => {
        renderSheet({
            apiRoles: [
                { name: 'USER', scope: 'API' },
                { name: 'ADMIN', scope: 'API', system: true },
                { name: 'PRIMARY_OWNER', scope: 'API', system: true },
            ],
        });
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.getByRole('option', { name: 'ADMIN' }).getAttribute('aria-disabled')).toBe('true');
        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).not.toBe('true');
    });

    it('locks fields while saving', () => {
        renderSheet({ isSaving: true });
        expect(screen.getByRole('textbox', { name: /Email/i })).toHaveProperty('disabled', true);
        expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true);
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
    });
});
