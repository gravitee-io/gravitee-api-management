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

import { ManageGroupsSheet } from './ManageGroupsSheet';
import type { EnvironmentGroup } from '../types/groupMembers';

const groups: EnvironmentGroup[] = [
    { id: 'g1', name: 'Platform team' },
    { id: 'g2', name: 'Billing' },
];

function renderSheet(open = true, allGroups = groups) {
    const onClose = jest.fn();
    const onSave = jest.fn();
    render(
        <ManageGroupsSheet
            open={open}
            allGroups={allGroups}
            currentGroupIds={['g1']}
            description="Select the groups that should have access to this resource."
            onClose={onClose}
            onSave={onSave}
            isSaving={false}
        />,
    );
    return { onClose, onSave };
}

describe('ManageGroupsSheet', () => {
    it('does not show sheet content when closed', () => {
        renderSheet(false);
        expect(screen.queryByRole('heading', { name: 'Manage groups' })).toBeNull();
    });

    it('shows the title and the given description when open', () => {
        renderSheet(true);
        expect(screen.getByRole('heading', { name: 'Manage groups' })).not.toBeNull();
        expect(screen.getByText('Select the groups that should have access to this resource.')).not.toBeNull();
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
        expect(onSave).toHaveBeenCalledTimes(1);
        expect([...onSave.mock.calls[0][0]].sort()).toEqual(['g1', 'g2']);
    });

    it('filters groups by a trimmed, case-insensitive search', async () => {
        const user = userEvent.setup();
        renderSheet(true);

        await user.type(screen.getByPlaceholderText('Search groups…'), '  BiLL ');

        expect(screen.getByRole('checkbox', { name: /Billing/i })).not.toBeNull();
        expect(screen.queryByRole('checkbox', { name: /Platform team/i })).toBeNull();
    });

    it.each([
        {
            scenario: 'there are no groups',
            allGroups: [],
            searchText: '',
            expectedMessage: 'No groups found in this environment.',
            otherMessage: 'No groups match your search.',
        },
        {
            scenario: 'no group matches the search',
            allGroups: groups,
            searchText: 'zzz',
            expectedMessage: 'No groups match your search.',
            otherMessage: 'No groups found in this environment.',
        },
    ])('shows the empty-state message when $scenario', async ({ allGroups, searchText, expectedMessage, otherMessage }) => {
        const user = userEvent.setup();
        renderSheet(true, allGroups);

        if (searchText) {
            await user.type(screen.getByPlaceholderText('Search groups…'), searchText);
        }

        expect(screen.getByText(expectedMessage)).not.toBeNull();
        expect(screen.queryByText(otherMessage)).toBeNull();
    });

    it('shows the Associated badge only on saved groups, regardless of the current selection', () => {
        renderSheet(true);

        const billingCheckbox = screen.getByRole('checkbox', { name: /Billing/i });
        fireEvent.click(billingCheckbox);

        expect(screen.getByRole('checkbox', { name: /Platform team/i }).closest('label')).toHaveTextContent('Associated');
        expect(billingCheckbox.closest('label')).not.toHaveTextContent('Associated');
    });
});
