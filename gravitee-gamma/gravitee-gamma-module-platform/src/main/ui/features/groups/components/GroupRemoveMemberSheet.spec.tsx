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

import { GroupRemoveMemberSheet } from './GroupRemoveMemberSheet';
import type { GroupMember } from '../types/group';

const MEMBER: GroupMember = { id: 'user-1', displayName: 'Anna Schmidt', roles: {} };

function renderSheet(overrides: Partial<React.ComponentProps<typeof GroupRemoveMemberSheet>> = {}) {
    const onClose = jest.fn();
    const onConfirm = jest.fn();
    render(
        <GroupRemoveMemberSheet
            open
            member={MEMBER}
            groupName="API Team"
            onClose={onClose}
            onConfirm={onConfirm}
            isRemoving={false}
            {...overrides}
        />,
    );
    return { onClose, onConfirm };
}

describe('GroupRemoveMemberSheet', () => {
    it('does not render dialog content when closed', () => {
        renderSheet({ open: false });
        expect(screen.queryByRole('heading', { name: 'Remove member' })).toBeNull();
    });

    it('names the member and the group in the confirmation message', () => {
        renderSheet();
        expect(screen.getByText('Anna Schmidt')).not.toBeNull();
        expect(screen.getByText('API Team')).not.toBeNull();
    });

    it('calls onConfirm when Remove is clicked', () => {
        const { onConfirm } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Remove' }));
        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('disables both buttons while removing', () => {
        renderSheet({ isRemoving: true });
        expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Removing…' })).toHaveProperty('disabled', true);
    });
});
