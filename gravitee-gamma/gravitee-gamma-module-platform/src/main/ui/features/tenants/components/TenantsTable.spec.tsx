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

import { TenantsTable } from './TenantsTable';
import type { Tenant } from '../types/tenant';

const ROWS: Tenant[] = [
    { id: 't-1', key: 'us-east', name: 'US East', description: 'Virginia gateway cluster' },
    { id: 't-2', key: 'eu-west', name: 'EU West', description: 'Frankfurt gateway cluster' },
];

describe('TenantsTable', () => {
    it('renders tenant rows', () => {
        render(<TenantsTable rows={ROWS} />);
        expect(screen.getByText('us-east')).not.toBeNull();
        expect(screen.getByText('eu-west')).not.toBeNull();
        expect(screen.getByText('US East')).not.toBeNull();
    });

    it('filters rows by search query', () => {
        render(<TenantsTable rows={ROWS} />);
        fireEvent.change(screen.getByLabelText('Search tenants'), { target: { value: 'eu-west' } });
        expect(screen.queryByText('us-east')).toBeNull();
        expect(screen.getByText('eu-west')).not.toBeNull();
    });

    it('calls onEdit when Edit is selected from the actions menu', async () => {
        const user = userEvent.setup();
        const onEdit = jest.fn();
        render(<TenantsTable rows={ROWS} canEdit onEdit={onEdit} />);
        await user.click(screen.getByRole('button', { name: /Actions for us-east/i }));
        await user.click(await screen.findByRole('menuitem', { name: /^Edit$/ }));
        expect(onEdit).toHaveBeenCalledWith(ROWS[0]);
    });

    it('shows no-results empty state when search matches nothing', () => {
        render(<TenantsTable rows={ROWS} />);
        fireEvent.change(screen.getByLabelText('Search tenants'), { target: { value: 'does-not-exist' } });
        expect(screen.getByText('No tenants found')).not.toBeNull();
    });

    it('calls onDelete when Delete is selected from the actions menu', async () => {
        const user = userEvent.setup();
        const onDelete = jest.fn();
        render(<TenantsTable rows={ROWS} canDelete onDelete={onDelete} />);
        await user.click(screen.getByRole('button', { name: /Actions for us-east/i }));
        await user.click(await screen.findByRole('menuitem', { name: /^Delete$/ }));
        expect(onDelete).toHaveBeenCalledWith(ROWS[0]);
    });

    it('hides the actions menu when the user cannot edit or delete', () => {
        render(<TenantsTable rows={ROWS} />);
        expect(screen.queryByRole('button', { name: /Actions for us-east/i })).toBeNull();
    });

    it('clamps to the last page when the row count shrinks', () => {
        const many = Array.from({ length: 11 }, (_, i) => ({
            id: `t-${i}`,
            key: `tenant-${i}`,
            name: `Tenant ${i}`,
            description: '',
        }));
        const { rerender } = render(<TenantsTable rows={many} />);
        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
        expect(screen.getByText('tenant-10')).not.toBeNull();
        expect(screen.queryByText('tenant-0')).toBeNull();

        rerender(<TenantsTable rows={many.slice(0, 10)} />);
        expect(screen.getByText('tenant-0')).not.toBeNull();
        expect(screen.queryByText('tenant-10')).toBeNull();
    });
});
