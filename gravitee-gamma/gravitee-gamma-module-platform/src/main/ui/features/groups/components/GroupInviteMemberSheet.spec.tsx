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
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { GroupInviteMemberSheet } from './GroupInviteMemberSheet';
import { installFormActionTestEnvironment } from '../../../shared/testing/formAction';
import type { GroupMember } from '../types/group';

let restoreTestEnvironment: () => void;

beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
    restoreTestEnvironment = installFormActionTestEnvironment();
});

afterAll(() => {
    restoreTestEnvironment();
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

    it('keeps Send invitation disabled for a malformed email and describes the error', () => {
        renderSheet();

        fireEvent.change(screen.getByRole('textbox', { name: /Email/i }), { target: { value: 'not-an-email' } });

        expect(screen.getByRole('button', { name: 'Send invitation' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('textbox', { name: /Email/i }).getAttribute('aria-invalid')).toBe('true');
        expect(screen.getByRole('textbox', { name: /Email/i }).getAttribute('aria-describedby')).toBe('invite-email-error');
        expect(screen.getByText('Enter a valid email')).not.toBeNull();
    });

    it('does not show an email error while the field is empty', () => {
        renderSheet();
        expect(screen.getByRole('textbox', { name: /Email/i }).getAttribute('aria-invalid')).not.toBe('true');
        expect(screen.queryByText('Enter a valid email')).toBeNull();
    });

    it('submits the email with the USER fallback default roles when the group has no configured defaults', async () => {
        const user = userEvent.setup();
        const { onSubmit } = renderSheet();

        fireEvent.change(screen.getByRole('textbox', { name: /Email/i }), { target: { value: 'anna.schmidt@lufthansa.com' } });
        await user.click(screen.getByRole('button', { name: 'Send invitation' }));

        await waitFor(() =>
            expect(onSubmit).toHaveBeenCalledWith({ email: 'anna.schmidt@lufthansa.com', apiRole: 'USER', applicationRole: 'USER' }),
        );
    });

    it('has no "None" role option — classic\'s invite-member-dialog has no such mat-option', () => {
        renderSheet();
        fireEvent.click(screen.getAllByRole('combobox')[0]);
        expect(screen.queryByRole('option', { name: 'None' })).toBeNull();
    });

    describe('default role pre-fill', () => {
        it('pre-fills API/Application from the group’s configured default roles', async () => {
            const user = userEvent.setup();
            const { onSubmit } = renderSheet({ groupRoles: { API: 'OWNER', APPLICATION: 'USER' } });

            fireEvent.change(screen.getByRole('textbox', { name: /Email/i }), { target: { value: 'anna.schmidt@lufthansa.com' } });
            await user.click(screen.getByRole('button', { name: 'Send invitation' }));

            await waitFor(() =>
                expect(onSubmit).toHaveBeenCalledWith({ email: 'anna.schmidt@lufthansa.com', apiRole: 'OWNER', applicationRole: 'USER' }),
            );
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

    it('locks fields while the React action is pending', async () => {
        const user = userEvent.setup();
        let resolveInvitation: (() => void) | undefined;
        const onSubmit = jest.fn(
            () =>
                new Promise<void>(resolve => {
                    resolveInvitation = resolve;
                }),
        );
        renderSheet({ onSubmit });

        await user.type(screen.getByRole('textbox', { name: /Email/i }), 'anna.schmidt@lufthansa.com');
        await user.click(screen.getByRole('button', { name: 'Send invitation' }));

        expect(screen.getByRole('button', { name: 'Sending…' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('textbox', { name: /Email/i })).toHaveProperty('disabled', true);
        expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true);

        await act(async () => {
            resolveInvitation?.();
            await Promise.resolve();
        });
        await waitFor(() => expect(screen.getByRole('button', { name: 'Send invitation' })).toHaveProperty('disabled', false));
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
