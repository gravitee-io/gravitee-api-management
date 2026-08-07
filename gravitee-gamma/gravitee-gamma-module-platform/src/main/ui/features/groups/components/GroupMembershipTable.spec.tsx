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

import { GroupMembershipTable } from './GroupMembershipTable';
import type { GroupMembershipItem } from '../types/group';

const BILLING_API: GroupMembershipItem = { id: 'api-1', name: 'Billing API', version: '1.0' };
const CATALOG_API: GroupMembershipItem = { id: 'api-2', name: 'Catalog API', version: '2.1' };
const MOBILE_APP: GroupMembershipItem = { id: 'app-1', name: 'Mobile App' };

function renderTable(overrides: Partial<React.ComponentProps<typeof GroupMembershipTable>> = {}) {
    return render(
        <GroupMembershipTable
            items={[BILLING_API, CATALOG_API]}
            loading={false}
            ariaLabel="APIs"
            searchPlaceholder="Search APIs…"
            emptyTitle="No dependent APIs to display"
            emptyDescription="APIs associated with this group will appear here."
            {...overrides}
        />,
    );
}

describe('GroupMembershipTable', () => {
    it('renders each item’s name and version', () => {
        renderTable();

        expect(screen.getByText('Billing API').closest('tr')!.textContent).toContain('1.0');
        expect(screen.getByText('Catalog API').closest('tr')!.textContent).toContain('2.1');
    });

    it('omits the Version column when showVersionColumn is false — Applications have no version', () => {
        renderTable({ items: [MOBILE_APP], showVersionColumn: false });

        expect(screen.queryByText('Version')).toBeNull();
        expect(screen.getByText('Mobile App')).not.toBeNull();
    });

    it('filters items by name client-side', () => {
        renderTable();
        fireEvent.change(screen.getByPlaceholderText('Search APIs…'), { target: { value: 'billing' } });
        expect(screen.queryByText('Catalog API')).toBeNull();
        expect(screen.queryByText('Billing API')).not.toBeNull();
    });

    it('paginates client-side once items exceed the page size', () => {
        const items = Array.from({ length: 12 }, (_, i) => ({ id: `api-${i}`, name: `API ${i}` }));
        renderTable({ items });

        expect(screen.queryByText('API 0')).not.toBeNull();
        expect(screen.queryByText('API 11')).toBeNull();
    });

    it('shows a first-use empty state with no items', () => {
        renderTable({ items: [] });
        expect(screen.queryByText('No dependent APIs to display')).not.toBeNull();
    });

    it('shows a no-results empty state when the search matches nothing', () => {
        renderTable();
        fireEvent.change(screen.getByPlaceholderText('Search APIs…'), { target: { value: 'nobody' } });
        expect(screen.queryByText('No results match your search')).not.toBeNull();
    });

    it('clears the search from the no-results empty state', () => {
        renderTable();
        fireEvent.change(screen.getByPlaceholderText('Search APIs…'), { target: { value: 'nobody' } });
        fireEvent.click(screen.getByRole('button', { name: 'Clear search' }));
        expect(screen.getByText('Billing API')).not.toBeNull();
    });
});
