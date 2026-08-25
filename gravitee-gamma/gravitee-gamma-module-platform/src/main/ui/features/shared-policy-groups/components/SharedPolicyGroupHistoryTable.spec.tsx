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

import { SharedPolicyGroupHistoryTable } from './SharedPolicyGroupHistoryTable';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

const HISTORIES: SharedPolicyGroup[] = [
    {
        id: 'spg-1',
        name: 'Auth Bundle',
        description: 'Authentication policies',
        apiType: 'PROXY',
        phase: 'REQUEST',
        lifecycleState: 'DEPLOYED',
        version: 2,
        deployedAt: '2026-08-20T10:00:00.000Z',
    },
    {
        id: 'spg-1',
        name: 'Original Auth Bundle',
        apiType: 'PROXY',
        phase: 'REQUEST',
        lifecycleState: 'DEPLOYED',
        version: 1,
        deployedAt: '2026-08-19T10:00:00.000Z',
    },
];

function renderTable(overrides: Partial<React.ComponentProps<typeof SharedPolicyGroupHistoryTable>> = {}) {
    const props: React.ComponentProps<typeof SharedPolicyGroupHistoryTable> = {
        histories: HISTORIES,
        totalCount: 30,
        loading: false,
        selected: [],
        page: 1,
        pageSize: 25,
        sorting: [{ id: 'version', desc: true }],
        onToggleSelected: jest.fn(),
        onShowJson: jest.fn(),
        onShowDetails: jest.fn(),
        onPageChange: jest.fn(),
        onPageSizeChange: jest.fn(),
        onSortingChange: jest.fn(),
        ...overrides,
    };
    render(<SharedPolicyGroupHistoryTable {...props} />);
    return props;
}

describe('SharedPolicyGroupHistoryTable', () => {
    it('selects versions and opens JSON or details actions', () => {
        const props = renderTable();

        fireEvent.click(screen.getByRole('checkbox', { name: 'Select version 1' }));
        fireEvent.click(screen.getByRole('button', { name: 'Show JSON for version 1' }));
        fireEvent.click(screen.getByRole('button', { name: 'View or restore version 2' }));

        expect(props.onToggleSelected).toHaveBeenCalledWith(HISTORIES[1]);
        expect(props.onShowJson).toHaveBeenCalledWith(HISTORIES[1]);
        expect(props.onShowDetails).toHaveBeenCalledWith(HISTORIES[0]);
    });

    it('limits selection to two versions and forwards server sorting and paging', () => {
        const thirdVersion = { ...HISTORIES[0], version: 3 };
        const props = renderTable({ histories: [...HISTORIES, thirdVersion], selected: HISTORIES, page: 1, totalCount: 30 });

        expect(screen.getByRole('checkbox', { name: 'Select version 1' })).toHaveProperty('disabled', false);
        expect(screen.getByRole('checkbox', { name: 'Select version 3' })).toHaveProperty('disabled', true);
        fireEvent.click(screen.getByRole('button', { name: 'Version' }));
        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));

        expect(props.onSortingChange).toHaveBeenCalled();
        expect(props.onPageChange).toHaveBeenCalledWith(2);
    });

    it('shows the deployed-version empty state', () => {
        renderTable({ histories: [], totalCount: 0 });

        expect(screen.getByText('No deployed versions yet')).not.toBeNull();
        expect(screen.getByText('Deploy this Shared Policy Group to create its first version.')).not.toBeNull();
    });
});
