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
import { MemoryRouter } from 'react-router-dom';

import { GroupsTable } from './GroupsTable';
import type { Group } from '../types/group';

const GROUP: Group = { id: 'group-1', name: 'Support Team', created_at: 1700000000000 };

function renderTable(overrides: Partial<React.ComponentProps<typeof GroupsTable>> = {}) {
    return render(
        <MemoryRouter>
            <GroupsTable
                groups={[GROUP]}
                totalCount={1}
                loading={false}
                isFirstUse={false}
                search=""
                page={1}
                pageSize={10}
                canEdit={false}
                canDelete={false}
                onSearchChange={jest.fn()}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                onEdit={jest.fn()}
                onDelete={jest.fn()}
                {...overrides}
            />
        </MemoryRouter>,
    );
}

describe('GroupsTable', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the name as a real link to the group detail page — supports open-in-new-tab/middle-click', () => {
        renderTable();
        expect(screen.getByRole('link', { name: 'Support Team' }).getAttribute('href')).toBe('/group-1');
    });

    describe('Primary owner badge', () => {
        it.each([
            ['primary_owner is true', { primary_owner: true }],
            ['apiPrimaryOwner is set (API Product PO-only groups miss primary_owner)', { apiPrimaryOwner: 'user-1' }],
            ['apiProductPrimaryOwner is set', { apiProductPrimaryOwner: 'user-1' }],
        ])('shows the badge when %s', (_label, override) => {
            renderTable({ groups: [{ ...GROUP, ...override }] });
            expect(screen.queryByText('Primary owner')).not.toBeNull();
        });

        it('hides the badge when the group has no primary-owner field set', () => {
            renderTable({ groups: [GROUP] });
            expect(screen.queryByText('Primary owner')).toBeNull();
        });
    });

    describe('row-level action gating', () => {
        it('does not gate Edit/Delete on manageable — classic only uses it for member invitations', async () => {
            const user = userEvent.setup();
            renderTable({ groups: [{ ...GROUP, manageable: false }], canEdit: true, canDelete: true });
            await user.click(screen.getByRole('button', { name: 'Group actions' }));
            expect(await screen.findByRole('menuitem', { name: /^Edit$/ })).not.toBeNull();
            expect(screen.queryByRole('menuitem', { name: /^Delete$/ })).not.toBeNull();
        });

        it('still shows Delete for a primary-owner group — GroupDeleteSheet explains why it is blocked, not this menu', async () => {
            const user = userEvent.setup();
            renderTable({ groups: [{ ...GROUP, primary_owner: true }], canEdit: true, canDelete: true });
            await user.click(screen.getByRole('button', { name: 'Group actions' }));
            expect(await screen.findByRole('menuitem', { name: /^Edit$/ })).not.toBeNull();
            expect(screen.queryByRole('menuitem', { name: /^Delete$/ })).not.toBeNull();
        });

        it('allows Delete for a non-primary-owner, manageable group', async () => {
            const user = userEvent.setup();
            renderTable({ groups: [GROUP], canEdit: true, canDelete: true });
            await user.click(screen.getByRole('button', { name: 'Group actions' }));
            expect(await screen.findByRole('menuitem', { name: /^Delete$/ })).not.toBeNull();
        });
    });

    describe('search', () => {
        it('calls onSearchChange when typing in the search box', () => {
            const onSearchChange = jest.fn();
            renderTable({ onSearchChange });
            fireEvent.change(screen.getByPlaceholderText('Search by name…'), { target: { value: 'support' } });
            expect(onSearchChange).toHaveBeenCalledWith('support');
        });
    });
});
