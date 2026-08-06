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

import { OrganizationGroupsPage } from './OrganizationGroupsPage';
import { useOrganizationGroups } from '../shared/hooks/useOrganizationGroups';

jest.mock('../shared/hooks/useOrganizationGroups');
jest.mock('../features/groups/components/OrganizationGroupsTable', () => ({
    OrganizationGroupsTable: ({ groups }: { groups: Array<{ name: string }> }) => (
        <div>
            {groups.map(group => (
                <span key={group.name}>{group.name}</span>
            ))}
        </div>
    ),
}));

const mockUseOrganizationGroups = jest.mocked(useOrganizationGroups);

describe('OrganizationGroupsPage', () => {
    beforeEach(() => {
        mockUseOrganizationGroups.mockReturnValue({
            data: [{ id: 'group-1', name: 'Support Team', environmentId: 'env-1', environmentName: 'Production' }],
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrganizationGroups>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the organization-wide groups list', () => {
        render(<OrganizationGroupsPage />);

        expect(screen.queryByRole('heading', { name: 'Organization Groups' })).not.toBeNull();
        expect(screen.queryByText('Support Team')).not.toBeNull();
    });

    it('shows a useful error when organization groups cannot be loaded', () => {
        mockUseOrganizationGroups.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useOrganizationGroups>);

        render(<OrganizationGroupsPage />);

        expect(screen.queryByText('Failed to load organization groups. Please refresh and try again.')).not.toBeNull();
    });
});
