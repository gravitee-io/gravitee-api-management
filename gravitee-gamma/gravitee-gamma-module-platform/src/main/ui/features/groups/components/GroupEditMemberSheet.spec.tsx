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

import { GroupEditMemberSheet } from './GroupEditMemberSheet';
import type { GroupMember } from '../types/group';

// Radix Select scrolls the highlighted option into view — not implemented in jsdom.
beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
});

const MEMBER: GroupMember = {
    id: 'user-1',
    displayName: 'Anna Schmidt',
    roles: { API: 'OWNER', APPLICATION: 'USER' },
};

function renderSheet(overrides: Partial<React.ComponentProps<typeof GroupEditMemberSheet>> = {}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(
        <GroupEditMemberSheet
            open
            groupName="API Team"
            member={MEMBER}
            members={[MEMBER]}
            apiRoles={[
                { name: 'OWNER', scope: 'API' },
                { name: 'PRIMARY_OWNER', scope: 'API', system: true },
            ]}
            applicationRoles={[{ name: 'USER', scope: 'APPLICATION' }]}
            apiProductRoles={[{ name: 'USER', scope: 'API_PRODUCT' }]}
            integrationRoles={[{ name: 'USER', scope: 'INTEGRATION' }]}
            clusterRoles={[{ name: 'USER', scope: 'CLUSTER' }]}
            groupAllowsGroupAdmin
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={false}
            {...overrides}
        />,
    );
    return { onClose, onSubmit };
}

describe('GroupEditMemberSheet', () => {
    it('does not render sheet content when closed', () => {
        renderSheet({ open: false });
        expect(screen.queryByRole('heading', { name: 'Edit roles' })).toBeNull();
    });

    it('renders nothing when there is no member', () => {
        const { container } = render(
            <GroupEditMemberSheet
                open
                groupName="API Team"
                member={undefined}
                members={[]}
                apiRoles={[]}
                applicationRoles={[]}
                apiProductRoles={[]}
                integrationRoles={[]}
                clusterRoles={[]}
                groupAllowsGroupAdmin
                onClose={jest.fn()}
                onSubmit={jest.fn()}
                isSaving={false}
            />,
        );
        expect(container.firstChild).toBeNull();
    });

    it('shows the member name in the description', () => {
        renderSheet();
        expect(screen.getByText(/Update Anna Schmidt.?s roles in API Team\./)).not.toBeNull();
    });

    it('pre-fills the Group admin checkbox from the member’s current GROUP role', () => {
        renderSheet({ member: { ...MEMBER, roles: { ...MEMBER.roles, GROUP: 'ADMIN' } } });
        expect(screen.getByRole('checkbox', { name: /Group admin/i }).getAttribute('data-state')).toBe('checked');
    });

    it('disables the Group admin checkbox when the group does not allow it', () => {
        renderSheet({ groupAllowsGroupAdmin: false });
        expect(screen.getByRole('checkbox', { name: /Group admin/i })).toHaveProperty('disabled', true);
        expect(screen.getByText(/Enable "Allow adding members via user search"/)).not.toBeNull();
    });

    it('locks the API role select when the member is already the primary owner', () => {
        renderSheet({ member: { ...MEMBER, roles: { ...MEMBER.roles, API: 'PRIMARY_OWNER' } } });
        expect(screen.getAllByRole('combobox')[0]).toHaveProperty('disabled', true);
        expect(screen.getByText(/Primary ownership can.t be transferred from here yet\./)).not.toBeNull();
    });

    it('disables the PRIMARY_OWNER option when another member already holds it', () => {
        const otherOwner: GroupMember = { id: 'user-2', displayName: 'Ravi Patel', roles: { API: 'PRIMARY_OWNER' } };
        renderSheet({ members: [MEMBER, otherOwner] });

        fireEvent.click(screen.getAllByRole('combobox')[0]);

        expect(screen.getByRole('option', { name: 'PRIMARY_OWNER' }).getAttribute('aria-disabled')).toBe('true');
    });

    it('submits the current roles plus GROUP/ADMIN only when Group admin is checked', () => {
        const { onSubmit } = renderSheet();

        fireEvent.click(screen.getByRole('checkbox', { name: /Group admin/i }));
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        expect(onSubmit).toHaveBeenCalledWith({
            id: 'user-1',
            roles: [
                { scope: 'API', name: 'OWNER' },
                { scope: 'APPLICATION', name: 'USER' },
                { scope: 'GROUP', name: 'ADMIN' },
            ],
        });
    });

    it('omits the GROUP scope entirely when Group admin is left unchecked', () => {
        const { onSubmit } = renderSheet();

        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        expect(onSubmit).toHaveBeenCalledWith({
            id: 'user-1',
            roles: [
                { scope: 'API', name: 'OWNER' },
                { scope: 'APPLICATION', name: 'USER' },
            ],
        });
    });

    it('calls onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
