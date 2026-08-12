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

import { OrganizationGroupsTable } from './OrganizationGroupsTable';
import type { OrganizationGroup } from '../types/group';

beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
});

const SUPPORT: OrganizationGroup = { id: 'group-1', name: 'Support Team', environmentId: 'env-1', environmentName: 'Default' };
const BILLING: OrganizationGroup = { id: 'group-2', name: 'Billing Admins', environmentId: 'env-2', environmentName: 'Staging' };

function renderTable(overrides: Partial<React.ComponentProps<typeof OrganizationGroupsTable>> = {}) {
    return render(<OrganizationGroupsTable groups={[SUPPORT, BILLING]} loading={false} {...overrides} />);
}

describe('OrganizationGroupsTable', () => {
    it('renders each group with its environment badge', () => {
        renderTable();

        expect(screen.getByText('Support Team').closest('tr')!.textContent).toContain('Default');
        expect(screen.getByText('Billing Admins').closest('tr')!.textContent).toContain('Staging');
    });

    it('filters by name client-side', () => {
        renderTable();
        fireEvent.change(screen.getByPlaceholderText('Search by name…'), { target: { value: 'support' } });

        expect(screen.queryByText('Support Team')).not.toBeNull();
        expect(screen.queryByText('Billing Admins')).toBeNull();
    });

    it('filters by environment', () => {
        renderTable();

        fireEvent.click(screen.getByRole('combobox', { name: 'Filter by environment' }));
        fireEvent.click(screen.getByRole('option', { name: 'Staging' }));

        expect(screen.queryByText('Support Team')).toBeNull();
        expect(screen.queryByText('Billing Admins')).not.toBeNull();
    });

    it('shows a first-use empty state with no groups', () => {
        renderTable({ groups: [] });
        expect(screen.queryByText('No groups')).not.toBeNull();
    });

    it('shows a no-results empty state when filters match nothing', () => {
        renderTable();
        fireEvent.change(screen.getByPlaceholderText('Search by name…'), { target: { value: 'nobody' } });
        expect(screen.queryByText('No groups match your filters')).not.toBeNull();
    });
});
