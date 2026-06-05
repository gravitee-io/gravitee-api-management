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

import { EditRoleSheet } from './EditRoleSheet';
import type { ApplicationUiMember } from '../../types/applicationMembers.types';
import { querySheetHeading } from '../test/sheetSpecHelpers';

const member: ApplicationUiMember = {
    id: 'user-1',
    displayName: 'Abhishek Kumar',
    email: 'abhishek.kumar@graviteesource.com',
    roles: [{ name: 'USER', scope: 'APPLICATION' }],
};

function renderSheet(memberProp: ApplicationUiMember | null = member) {
    const onClose = jest.fn();
    const onSave = jest.fn();
    render(<EditRoleSheet member={memberProp} roles={['USER', 'ADMIN']} onClose={onClose} onSave={onSave} isSaving={false} />);
    return { onClose, onSave };
}

describe('EditRoleSheet', () => {
    it('does not show sheet content when member is null', () => {
        renderSheet(null);
        expect(querySheetHeading('Edit Role')).toBeNull();
    });

    it('shows sheet title and member description when member is set', () => {
        renderSheet(member);
        expect(screen.getByRole('heading', { name: 'Edit Role' })).not.toBeNull();
        expect(screen.getByText(/Change the role for Abhishek Kumar/i)).not.toBeNull();
    });

    it('invokes onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet(member);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('disables Save when the selected role is unchanged', () => {
        renderSheet(member);
        expect(screen.getByRole('button', { name: 'Save' }).disabled).toBe(true);
    });
});
