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

import { GroupMembersTable } from './GroupMembersTable';
import type { GroupMember } from '../types/group';

const RAVI: GroupMember = {
    id: 'user-1',
    displayName: 'Ravi Patel',
    roles: { API: 'PRIMARY_OWNER', API_PRODUCT: 'OWNER', APPLICATION: 'OWNER', GROUP: 'ADMIN' },
};

const MIA: GroupMember = {
    id: 'user-2',
    displayName: 'Mia Chen',
    roles: { API: 'OWNER', APPLICATION: 'USER' },
};

function renderTable(overrides: Partial<React.ComponentProps<typeof GroupMembersTable>> = {}) {
    return render(<GroupMembersTable members={[RAVI, MIA]} loading={false} {...overrides} />);
}

describe('GroupMembersTable', () => {
    it('renders each member’s roles across the API, API product, Application, Integration, and Cluster columns', () => {
        renderTable();

        const raviRow = screen.getByText('Ravi Patel').closest('tr')!;
        expect(raviRow.textContent).toContain('PRIMARY_OWNER');
        expect(raviRow.textContent).toContain('OWNER');

        const miaRow = screen.getByText('Mia Chen').closest('tr')!;
        expect(miaRow.textContent).toContain('USER');
    });

    it('shows a Group admin badge for members with the GROUP/ADMIN role', () => {
        renderTable();
        expect(screen.getByText('Ravi Patel').closest('tr')!.textContent).toContain('Group admin');
        expect(screen.getByText('Mia Chen').closest('tr')!.textContent).not.toContain('Group admin');
    });

    it('filters members by name client-side', () => {
        renderTable();
        fireEvent.change(screen.getByPlaceholderText('Search members…'), { target: { value: 'mia' } });
        expect(screen.queryByText('Ravi Patel')).toBeNull();
        expect(screen.queryByText('Mia Chen')).not.toBeNull();
    });

    it('shows a first-use empty state with no members', () => {
        renderTable({ members: [] });
        expect(screen.queryByText('No members yet')).not.toBeNull();
    });

    it('shows a no-results empty state when the search matches nothing', () => {
        renderTable();
        fireEvent.change(screen.getByPlaceholderText('Search members…'), { target: { value: 'nobody' } });
        expect(screen.queryByText('No members match your search')).not.toBeNull();
    });
});
