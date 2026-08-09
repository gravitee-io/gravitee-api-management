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
import userEvent from '@testing-library/user-event';

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
            members={[MEMBER]}
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
        expect(screen.queryByRole('heading', { name: 'Remove member?' })).toBeNull();
    });

    it('names the member and the group in the confirmation message', () => {
        renderSheet();
        expect(screen.getByText('Anna Schmidt')).not.toBeNull();
        expect(screen.getByText('API Team')).not.toBeNull();
    });

    it('calls onConfirm with no transfer membership for a non-primary-owner member', () => {
        const { onConfirm } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Remove' }));
        expect(onConfirm).toHaveBeenCalledWith(undefined);
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

    describe('primary owner reassignment', () => {
        const PRIMARY_OWNER_MEMBER: GroupMember = {
            id: 'user-1',
            displayName: 'Anna Schmidt',
            roles: { API: 'PRIMARY_OWNER', APPLICATION: 'USER' },
        };
        const OTHER_MEMBER: GroupMember = { id: 'user-2', displayName: 'Ravi Patel', roles: { API: 'OWNER' } };

        it('disables Remove until a successor is picked', () => {
            renderSheet({ member: PRIMARY_OWNER_MEMBER, members: [PRIMARY_OWNER_MEMBER, OTHER_MEMBER] });
            expect(screen.getByRole('button', { name: 'Remove' })).toHaveProperty('disabled', true);
        });

        it('searches members, selects a successor, and submits the transfer membership', async () => {
            const user = userEvent.setup();
            const { onConfirm } = renderSheet({ member: PRIMARY_OWNER_MEMBER, members: [PRIMARY_OWNER_MEMBER, OTHER_MEMBER] });

            await user.click(screen.getByLabelText('Search members'));
            await user.click(screen.getByRole('option', { name: 'Ravi Patel' }));

            expect(
                screen.getByText(
                    'Anna Schmidt is the API primary owner. Primary ownership of the group will be transferred from Anna Schmidt to Ravi Patel.',
                ),
            ).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Remove' }));

            expect(onConfirm).toHaveBeenCalledWith({
                id: 'user-2',
                roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }],
            });
        });

        it('excludes the member being removed from the successor search results', async () => {
            const user = userEvent.setup();
            renderSheet({ member: PRIMARY_OWNER_MEMBER, members: [PRIMARY_OWNER_MEMBER, OTHER_MEMBER] });
            await user.click(screen.getByLabelText('Search members'));
            expect(screen.queryByRole('option', { name: 'Anna Schmidt' })).toBeNull();
            expect(screen.getByRole('option', { name: 'Ravi Patel' })).not.toBeNull();
        });

        it('joins multiple owned scopes in the transfer message', async () => {
            const user = userEvent.setup();
            const member: GroupMember = {
                id: 'user-1',
                displayName: 'Anna Schmidt',
                roles: { API: 'PRIMARY_OWNER', API_PRODUCT: 'PRIMARY_OWNER', CLUSTER: 'PRIMARY_OWNER' },
            };
            renderSheet({ member, members: [member, OTHER_MEMBER] });

            await user.click(screen.getByLabelText('Search members'));
            await user.click(screen.getByRole('option', { name: 'Ravi Patel' }));

            expect(
                screen.getByText(
                    'Anna Schmidt is the API, API Product and Cluster primary owner. Primary ownership of the group will be transferred from Anna Schmidt to Ravi Patel.',
                ),
            ).not.toBeNull();
        });

        it.each(['APPLICATION', 'INTEGRATION', 'CLUSTER'])('forces a successor pick when the member is the %s primary owner', scope => {
            const member: GroupMember = { id: 'user-1', displayName: 'Anna Schmidt', roles: { [scope]: 'PRIMARY_OWNER' } };
            renderSheet({ member, members: [member, OTHER_MEMBER] });
            expect(screen.getByRole('button', { name: 'Remove' })).toHaveProperty('disabled', true);
            expect(screen.getByLabelText('Search members')).not.toBeNull();
        });
    });
});
