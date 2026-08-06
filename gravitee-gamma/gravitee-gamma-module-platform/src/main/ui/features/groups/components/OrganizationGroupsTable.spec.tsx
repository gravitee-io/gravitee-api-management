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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { OrganizationGroupsTable } from './OrganizationGroupsTable';
import type { OrganizationGroup } from '../../../shared/types/organizationGroup';

const GROUPS: OrganizationGroup[] = [
    { id: 'group-1', name: 'Support Team', environmentId: 'env-1', environmentName: 'Production' },
    { id: 'group-2', name: 'Billing Team', environmentId: 'env-2', environmentName: 'Staging' },
];

beforeAll(() => {
    Element.prototype.hasPointerCapture = () => false;
    Element.prototype.setPointerCapture = () => undefined;
    Element.prototype.releasePointerCapture = () => undefined;
    Element.prototype.scrollIntoView = () => undefined;
});

describe('OrganizationGroupsTable', () => {
    it('shows the first-use state when the organization has no groups', () => {
        render(<OrganizationGroupsTable groups={[]} loading={false} />);

        expect(screen.queryByText('No organization groups')).not.toBeNull();
        expect(screen.queryByRole('table')).toBeNull();
    });

    it('shows each group with the environment it belongs to', () => {
        render(<OrganizationGroupsTable groups={GROUPS} loading={false} />);

        expect(screen.queryByText('Support Team')).not.toBeNull();
        expect(screen.queryByText('Production')).not.toBeNull();
        expect(screen.queryByText('Billing Team')).not.toBeNull();
        expect(screen.queryByText('Staging')).not.toBeNull();
    });

    it('searches by group name without matching environment names', async () => {
        const user = userEvent.setup();
        render(<OrganizationGroupsTable groups={GROUPS} loading={false} />);

        await user.type(screen.getByRole('textbox', { name: 'Search organization groups' }), 'Production');

        expect(screen.queryByText('Support Team')).toBeNull();
        expect(screen.queryByText('Billing Team')).toBeNull();
        expect(screen.queryByText('No organization groups match your filters')).not.toBeNull();
    });

    it('filters groups by environment', async () => {
        const user = userEvent.setup();
        render(<OrganizationGroupsTable groups={GROUPS} loading={false} />);

        await user.click(screen.getByRole('combobox', { name: 'Filter by environment' }));
        await user.click(await screen.findByRole('option', { name: 'Staging' }));

        expect(screen.queryByText('Support Team')).toBeNull();
        expect(screen.queryByText('Billing Team')).not.toBeNull();
    });

    it('resets an environment filter that is no longer available', async () => {
        const user = userEvent.setup();
        const { rerender } = render(<OrganizationGroupsTable groups={GROUPS} loading={false} />);
        await user.click(screen.getByRole('combobox', { name: 'Filter by environment' }));
        await user.click(await screen.findByRole('option', { name: 'Staging' }));

        rerender(<OrganizationGroupsTable groups={GROUPS.slice(0, 1)} loading={false} />);

        expect(screen.queryByText('Support Team')).not.toBeNull();
        await waitFor(() =>
            expect(screen.getByRole('combobox', { name: 'Filter by environment' }).textContent).toContain('All environments'),
        );

        rerender(<OrganizationGroupsTable groups={GROUPS} loading={false} />);

        expect(screen.queryByText('Support Team')).not.toBeNull();
        expect(screen.queryByText('Billing Team')).not.toBeNull();
    });

    it('clears search and environment filters from the no-results state', async () => {
        const user = userEvent.setup();
        render(<OrganizationGroupsTable groups={GROUPS} loading={false} />);
        await user.click(screen.getByRole('combobox', { name: 'Filter by environment' }));
        await user.click(await screen.findByRole('option', { name: 'Staging' }));
        await user.type(screen.getByRole('textbox', { name: 'Search organization groups' }), 'Support');

        await user.click(screen.getByRole('button', { name: 'Clear filters' }));

        expect(screen.queryByText('Support Team')).not.toBeNull();
        expect(screen.queryByText('Billing Team')).not.toBeNull();
    });

    it('paginates the organization-wide result', async () => {
        const user = userEvent.setup();
        const groups = Array.from({ length: 11 }, (_, index) => ({
            id: `group-${index + 1}`,
            name: `Team ${String(index + 1).padStart(2, '0')}`,
            environmentId: 'env-1',
            environmentName: 'Production',
        }));
        render(<OrganizationGroupsTable groups={groups} loading={false} />);

        expect(screen.queryByText('Team 01')).not.toBeNull();
        expect(screen.queryByText('Team 11')).toBeNull();

        await user.click(screen.getByRole('button', { name: 'Next page' }));

        expect(screen.queryByText('Team 01')).toBeNull();
        expect(screen.queryByText('Team 11')).not.toBeNull();
    });
});
