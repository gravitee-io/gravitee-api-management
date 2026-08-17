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

import { SharedPolicyGroupHistoriesTable } from './SharedPolicyGroupHistoriesTable';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

const HISTORY: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth policies',
    lifecycleState: 'DEPLOYED',
    version: 2,
    apiType: 'PROXY',
    phase: 'REQUEST',
    updatedAt: '2024-01-01T00:00:00.000Z',
    deployedAt: '2024-01-02T00:00:00.000Z',
};

function renderTable(overrides: Partial<React.ComponentProps<typeof SharedPolicyGroupHistoriesTable>> = {}) {
    return render(
        <SharedPolicyGroupHistoriesTable
            histories={[HISTORY]}
            totalCount={1}
            loading={false}
            page={1}
            pageSize={25}
            sorting={[]}
            onPageChange={jest.fn()}
            onPageSizeChange={jest.fn()}
            onSortingChange={jest.fn()}
            {...overrides}
        />,
    );
}

describe('SharedPolicyGroupHistoriesTable', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders version, name, description, status, and deployed-at columns like classic Console', () => {
        renderTable();
        expect(screen.queryByRole('columnheader', { name: 'Version' })).not.toBeNull();
        expect(screen.queryByRole('columnheader', { name: 'Name' })).not.toBeNull();
        expect(screen.queryByRole('columnheader', { name: 'Deployed At' })).not.toBeNull();
        expect(screen.queryByText('2')).not.toBeNull();
        expect(screen.queryByText('Auth Bundle')).not.toBeNull();
        expect(screen.queryByText('Reusable auth policies')).not.toBeNull();
        expect(screen.queryByText('Deployed')).not.toBeNull();
    });

    it('places the lifecycle badge in the Name cell — matching classic Console history table', () => {
        renderTable();
        const nameCell = screen.getByText('Auth Bundle').closest('td');
        expect(nameCell?.textContent).toContain('Deployed');
    });

    it('offers sorting on Version and Deployed At only', () => {
        const onSortingChange = jest.fn();
        renderTable({ onSortingChange });

        fireEvent.click(screen.getByRole('button', { name: 'Version' }));
        expect(onSortingChange).toHaveBeenCalled();

        onSortingChange.mockClear();
        fireEvent.click(screen.getByRole('button', { name: 'Deployed At' }));
        expect(onSortingChange).toHaveBeenCalled();
    });

    it('does not offer sorting by Name — classic Console has no sortBy for it', () => {
        renderTable();
        expect(screen.queryByRole('button', { name: 'Name' })).toBeNull();
        expect(screen.queryByRole('columnheader', { name: 'Name' })).not.toBeNull();
    });

    it('shows an empty state when there are no history entries', () => {
        renderTable({ histories: [], totalCount: 0 });
        expect(screen.getByTestId('shared-policy-group-history-empty')).not.toBeNull();
        expect(screen.getByText('No version history yet')).not.toBeNull();
    });
});
