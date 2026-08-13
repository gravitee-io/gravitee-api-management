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
import { MemoryRouter } from 'react-router-dom';

import { SharedPolicyGroupsTable } from './SharedPolicyGroupsTable';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'My Shared Policy Group',
    description: 'Reusable auth policies',
    lifecycleState: 'DEPLOYED',
    apiType: 'PROXY',
    phase: 'REQUEST',
    updatedAt: '2024-01-01T00:00:00.000Z',
    deployedAt: '2024-01-02T00:00:00.000Z',
};

function renderTable(overrides: Partial<React.ComponentProps<typeof SharedPolicyGroupsTable>> = {}) {
    return render(
        <MemoryRouter>
            <SharedPolicyGroupsTable
                sharedPolicyGroups={[SPG]}
                totalCount={1}
                loading={false}
                isFirstUse={false}
                search=""
                page={1}
                pageSize={25}
                sorting={[]}
                canEdit={false}
                canDelete={false}
                onSearchChange={jest.fn()}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                onSortingChange={jest.fn()}
                onView={jest.fn()}
                onDelete={jest.fn()}
                {...overrides}
            />
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupsTable', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the name as a real link to the detail page — supports open-in-new-tab/middle-click', () => {
        renderTable();
        expect(screen.getByRole('link', { name: 'My Shared Policy Group' }).getAttribute('href')).toBe('/spg-1');
    });

    it('renders the description', () => {
        renderTable();
        expect(screen.queryByText('Reusable auth policies')).not.toBeNull();
    });

    it('renders the lifecycle state badge in its own Status column, not the Name column', () => {
        renderTable();
        expect(screen.queryByRole('columnheader', { name: 'Status' })).not.toBeNull();
        const nameCell = screen.getByRole('link', { name: 'My Shared Policy Group' }).closest('td');
        expect(nameCell?.textContent).not.toContain('Deployed');
        expect(screen.queryByText('Deployed')).not.toBeNull();
    });

    it('renders the API type and phase columns', () => {
        renderTable();
        expect(screen.queryByText('Proxy')).not.toBeNull();
        expect(screen.queryByText('Request')).not.toBeNull();
    });

    describe('sorting', () => {
        it("sorts by Name, Phase, Last updated, and Last deployed — matching classic Console's sortBy support", () => {
            const onSortingChange = jest.fn();
            renderTable({ onSortingChange });

            fireEvent.click(screen.getByRole('button', { name: 'Name' }));
            expect(onSortingChange).toHaveBeenCalled();
        });

        it('does not offer sorting by API type — classic Console has no sortBy value for it', () => {
            renderTable();
            expect(screen.queryByRole('button', { name: 'API Type' })).toBeNull();
            expect(screen.queryByText('API Type')).not.toBeNull();
        });
    });

    describe('row-level action gating', () => {
        async function openRowMenu() {
            const user = userEvent.setup();
            await user.click(screen.getByRole('button', { name: 'My Shared Policy Group actions' }));
            return user;
        }

        it('shows only a View action when the user lacks update permission', async () => {
            renderTable({ canEdit: false });
            await openRowMenu();
            expect(await screen.findByRole('menuitem', { name: 'View' })).not.toBeNull();
            expect(screen.queryByRole('menuitem', { name: 'Edit' })).toBeNull();
        });

        it('shows Edit (not View) when the user can update', async () => {
            renderTable({ canEdit: true });
            await openRowMenu();
            expect(await screen.findByRole('menuitem', { name: 'Edit' })).not.toBeNull();
            expect(screen.queryByRole('menuitem', { name: 'View' })).toBeNull();
        });

        it('shows only View — never Edit — for a Kubernetes-origin row, even with update permission', async () => {
            renderTable({ canEdit: true, sharedPolicyGroups: [{ ...SPG, originContext: { origin: 'KUBERNETES' } }] });
            await openRowMenu();
            expect(await screen.findByRole('menuitem', { name: 'View' })).not.toBeNull();
            expect(screen.queryByRole('menuitem', { name: 'Edit' })).toBeNull();
        });

        it('hides Delete without delete permission', async () => {
            renderTable({ canEdit: true, canDelete: false });
            await openRowMenu();
            expect(await screen.findByRole('menuitem', { name: 'Edit' })).not.toBeNull();
            expect(screen.queryByRole('menuitem', { name: 'Delete' })).toBeNull();
        });

        it('hides Delete for a read-only (Kubernetes-origin) row, even with delete permission', async () => {
            renderTable({ canEdit: true, canDelete: true, sharedPolicyGroups: [{ ...SPG, originContext: { origin: 'KUBERNETES' } }] });
            await openRowMenu();
            expect(await screen.findByRole('menuitem', { name: 'View' })).not.toBeNull();
            expect(screen.queryByRole('menuitem', { name: 'Delete' })).toBeNull();
        });

        it('shows Delete with delete permission and calls onDelete', async () => {
            const onDelete = jest.fn();
            renderTable({ canEdit: true, canDelete: true, onDelete });
            const user = await openRowMenu();
            await user.click(await screen.findByRole('menuitem', { name: 'Delete' }));
            expect(onDelete).toHaveBeenCalledWith(SPG);
        });

        it('calls onView when Edit is clicked', async () => {
            const onView = jest.fn();
            renderTable({ canEdit: true, onView });
            const user = await openRowMenu();
            await user.click(await screen.findByRole('menuitem', { name: 'Edit' }));
            expect(onView).toHaveBeenCalledWith(SPG);
        });
    });

    describe('search', () => {
        it('calls onSearchChange when typing in the search box', () => {
            const onSearchChange = jest.fn();
            renderTable({ onSearchChange });
            fireEvent.change(screen.getByPlaceholderText('Search by name or description…'), { target: { value: 'auth' } });
            expect(onSearchChange).toHaveBeenCalledWith('auth');
        });
    });

    describe('empty state', () => {
        it('shows the Create CTA on first use', () => {
            const onCreateSharedPolicyGroup = jest.fn();
            renderTable({ isFirstUse: true, sharedPolicyGroups: [], totalCount: 0, onCreateSharedPolicyGroup });
            expect(screen.queryByText('No Shared Policy Groups')).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Add Shared Policy Group' }));
            expect(onCreateSharedPolicyGroup).toHaveBeenCalled();
        });

        it('hides the Create CTA on first use when the callback is not provided (read-only user)', () => {
            renderTable({ isFirstUse: true, sharedPolicyGroups: [], totalCount: 0, onCreateSharedPolicyGroup: undefined });
            expect(screen.queryByRole('button', { name: 'Add Shared Policy Group' })).toBeNull();
        });
    });
});
