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

import { ManageGroupsSheet } from './ManageGroupsSheet';
import type { EnvironmentGroup } from '../../types/applicationMembers.types';
import { querySheetHeading } from '../test/sheetSpecHelpers';

const groups: EnvironmentGroup[] = [
    { id: 'g1', name: 'Platform team' },
    { id: 'g2', name: 'Billing' },
];

function renderSheet(open = true) {
    const onClose = jest.fn();
    const onSave = jest.fn();
    render(
        <ManageGroupsSheet open={open} allGroups={groups} currentGroupIds={['g1']} onClose={onClose} onSave={onSave} isSaving={false} />,
    );
    return { onClose, onSave };
}

describe('ManageGroupsSheet', () => {
    it('does not show sheet content when closed', () => {
        renderSheet(false);
        expect(querySheetHeading('Manage groups')).toBeNull();
    });

    it('shows sheet title when open', () => {
        renderSheet(true);
        expect(screen.getByRole('heading', { name: 'Manage groups' })).not.toBeNull();
    });

    it('invokes onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('invokes onSave with selected group ids', () => {
        const { onSave } = renderSheet(true);
        fireEvent.click(screen.getByRole('checkbox', { name: /Billing/i }));
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        expect(onSave).toHaveBeenCalledWith(expect.arrayContaining(['g1', 'g2']));
    });
});
