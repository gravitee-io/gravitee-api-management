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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { GroupInvitationsTable } from './GroupInvitationsTable';
import type { GroupInvitation } from '../types/group';

const ANNA: GroupInvitation = {
    id: 'invitation-1',
    reference_id: 'group-1',
    email: 'anna@lufthansa.com',
    api_role: 'USER',
    application_role: 'OWNER',
    created_at: 1_700_000_000_000,
};

const BEN: GroupInvitation = {
    id: 'invitation-2',
    reference_id: 'group-1',
    email: 'ben@lufthansa.com',
};

function renderTable(overrides: Partial<React.ComponentProps<typeof GroupInvitationsTable>> = {}) {
    return render(<GroupInvitationsTable invitations={[ANNA, BEN]} loading={false} canManageMembers onDelete={jest.fn()} {...overrides} />);
}

describe('GroupInvitationsTable', () => {
    it('renders each invitation’s email and roles', () => {
        renderTable();
        const annaRow = screen.getByText('anna@lufthansa.com').closest('tr')!;
        expect(annaRow.textContent).toContain('USER');
        expect(annaRow.textContent).toContain('OWNER');
    });

    it('sorts invitations by email like the classic Console', () => {
        renderTable({ invitations: [BEN, ANNA] });

        expect(screen.getAllByText(/@lufthansa\.com$/).map(element => element.textContent)).toEqual([
            'anna@lufthansa.com',
            'ben@lufthansa.com',
        ]);
    });

    it('shows a placeholder for missing roles or invitation date', () => {
        renderTable();
        const benRow = screen.getByText('ben@lufthansa.com').closest('tr')!;
        expect(benRow.textContent).toContain('—');
    });

    it('hides the actions column entirely without permission', () => {
        renderTable({ canManageMembers: false });
        expect(screen.queryByRole('button', { name: /Delete invitation/ })).toBeNull();
    });

    it('calls onDelete with the row’s invitation', async () => {
        const user = userEvent.setup();
        const onDelete = jest.fn();
        renderTable({ onDelete });

        await user.click(screen.getByRole('button', { name: 'Delete invitation sent to anna@lufthansa.com' }));

        expect(onDelete).toHaveBeenCalledWith(ANNA);
    });

    it('filters invitations by email client-side', async () => {
        renderTable();
        fireEvent.change(screen.getByLabelText('Search invitations'), { target: { value: 'anna' } });
        await waitFor(() => expect(screen.queryByText('ben@lufthansa.com')).toBeNull());
        expect(screen.getByText('anna@lufthansa.com')).not.toBeNull();
    });

    it('shows a first-use empty state with no invitations', () => {
        renderTable({ invitations: [] });
        expect(screen.queryByText('No invitations sent to display')).not.toBeNull();
        expect(screen.queryByRole('table')).toBeNull();
    });

    it('shows a no-results empty state when the search matches nothing', async () => {
        renderTable();
        fireEvent.change(screen.getByLabelText('Search invitations'), { target: { value: 'nobody' } });
        expect(await screen.findByText('No invitations match your search')).not.toBeNull();
    });
});
