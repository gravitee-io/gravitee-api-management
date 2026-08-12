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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { OrganizationGroupsPage } from './OrganizationGroupsPage';
import { useOrganizationGroups } from '../features/groups/hooks/useGroups';
import type { OrganizationGroup } from '../features/groups/types/group';

jest.mock('../features/groups/hooks/useGroups');

jest.mock('../features/groups/components/OrganizationGroupsTable', () => ({
    OrganizationGroupsTable: ({ groups }: { groups: OrganizationGroup[] }) => (
        <div data-testid="organization-groups-table">{groups.map(g => g.name).join(', ')}</div>
    ),
}));

const mockUseOrganizationGroups = jest.mocked(useOrganizationGroups);

const GROUPS: OrganizationGroup[] = [{ id: 'group-1', name: 'Support Team', environmentId: 'env-1', environmentName: 'Default' }];

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/groups/all']}>
            <Routes>
                <Route path="/groups">
                    <Route index element={<div>Groups List</div>} />
                    <Route path="all" element={<OrganizationGroupsPage />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('OrganizationGroupsPage', () => {
    beforeEach(() => {
        mockUseOrganizationGroups.mockReturnValue({
            data: GROUPS,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationGroups>);
    });

    it('renders the org-wide groups table', () => {
        renderPage();
        expect(screen.getByTestId('organization-groups-table').textContent).toContain('Support Team');
    });

    it('navigates back to the environment groups list', () => {
        renderPage();
        fireEvent.click(screen.getByRole('link', { name: /Back to groups/i }));
        expect(screen.queryByText('Groups List')).not.toBeNull();
    });

    it('shows an error message when the list fails to load', () => {
        mockUseOrganizationGroups.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useOrganizationGroups>);
        renderPage();
        expect(screen.queryByText('Failed to load groups. Please refresh and try again.')).not.toBeNull();
    });
});
