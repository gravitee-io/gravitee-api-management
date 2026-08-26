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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { RoleMembersTable } from './RoleMembersTable';
import type { RoleMembershipListItem } from '../types/role';

const MEMBERS: RoleMembershipListItem[] = [
    { id: 'user-1', displayName: 'Jane Doe' },
    { id: 'user-2', displayName: 'John Smith' },
];

describe('RoleMembersTable', () => {
    it('lists every member', () => {
        render(<RoleMembersTable members={MEMBERS} isLoading={false} canManage onDeleteMember={jest.fn()} />);

        expect(screen.getByText('Jane Doe')).toBeInTheDocument();
        expect(screen.getByText('John Smith')).toBeInTheDocument();
    });

    it('shows "No member" when there are none', () => {
        render(<RoleMembersTable members={[]} isLoading={false} canManage onDeleteMember={jest.fn()} />);

        expect(screen.getByText('No member')).toBeInTheDocument();
    });

    it('filters members via the search field', async () => {
        const user = userEvent.setup();
        render(<RoleMembersTable members={MEMBERS} isLoading={false} canManage onDeleteMember={jest.fn()} />);

        await user.type(screen.getByLabelText('Search members'), 'Jane');

        expect(screen.getByText('Jane Doe')).toBeInTheDocument();
        expect(screen.queryByText('John Smith')).not.toBeInTheDocument();
    });

    it('deletes a member', async () => {
        const user = userEvent.setup();
        const onDeleteMember = jest.fn();
        render(<RoleMembersTable members={MEMBERS} isLoading={false} canManage onDeleteMember={onDeleteMember} />);

        await user.click(screen.getAllByRole('button', { name: 'Button to delete a member' })[0]);

        expect(onDeleteMember).toHaveBeenCalledWith(MEMBERS[0]);
    });

    it('hides delete actions without manage permission', () => {
        render(<RoleMembersTable members={MEMBERS} isLoading={false} canManage={false} onDeleteMember={jest.fn()} />);

        expect(screen.queryByRole('button', { name: 'Button to delete a member' })).not.toBeInTheDocument();
    });
});
