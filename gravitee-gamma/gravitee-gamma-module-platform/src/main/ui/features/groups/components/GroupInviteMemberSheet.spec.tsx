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

function renderSheet(overrides: Partial<React.ComponentProps<typeof GroupInviteMemberSheet>> = {}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(
        <GroupInviteMemberSheet
            open
            groupName="API Team"
            apiRoles={[{ name: 'OWNER', scope: 'API' }]}
            applicationRoles={[{ name: 'USER', scope: 'APPLICATION' }]}
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

    it('submits the email and default roles', () => {
        const { onSubmit } = renderSheet();

        fireEvent.change(screen.getByRole('textbox', { name: /Email/i }), { target: { value: 'anna.schmidt@lufthansa.com' } });
        fireEvent.click(screen.getByRole('button', { name: 'Send invitation' }));

        expect(onSubmit).toHaveBeenCalledWith({ email: 'anna.schmidt@lufthansa.com', apiRole: '', applicationRole: '' });
    });

    it('calls onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
