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

import { GroupDetailPage } from './GroupDetailPage';
import {
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import type { Group, GroupMembershipItem } from '../features/groups/types/group';

jest.mock('../features/groups/hooks/useGroupDetail');

// Stub the nested DataTable-backed components to avoid jsdom/Radix DataTable complexity;
// exposes the fetched rows as plain text so orchestration (what data flows in) stays covered.
// GroupMembersTable's own spec covers its columns/search internals.
jest.mock('../features/groups/components/GroupMembersTable', () => ({
    GroupMembersTable: ({ members }: { members: { id: string; displayName: string }[] }) => (
        <div data-testid="members-table">{members.map(m => m.displayName).join(', ')}</div>
    ),
}));
jest.mock('../features/groups/components/GroupMembershipTable', () => ({
    GroupMembershipTable: ({ items, ariaLabel }: { items: GroupMembershipItem[]; ariaLabel: string }) => (
        <div data-testid={`membership-table-${ariaLabel}`}>{items.map(i => i.name).join(', ')}</div>
    ),
}));

const mockUseGroupDetail = jest.mocked(useGroupDetail);
const mockUseGroupMembers = jest.mocked(useGroupMembers);
const mockUseGroupApis = jest.mocked(useGroupApis);
const mockUseGroupApplications = jest.mocked(useGroupApplications);
const mockUseGroupApiProducts = jest.mocked(useGroupApiProducts);

const GROUP: Group = { id: 'group-1', name: 'Support Team', event_rules: [{ event: 'API_CREATE' }] };

function renderPage(initialGroupId = 'group-1') {
    return render(
        <MemoryRouter initialEntries={[`/user-groups/${initialGroupId}`]}>
            <Routes>
                <Route path="/user-groups">
                    <Route index element={<div>Groups List</div>} />
                    <Route path=":groupId" element={<GroupDetailPage />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('GroupDetailPage', () => {
    beforeEach(() => {
        mockUseGroupDetail.mockReturnValue({ data: GROUP, isLoading: false, isError: false } as ReturnType<typeof useGroupDetail>);
        mockUseGroupMembers.mockReturnValue({
            data: [{ id: 'member-1', displayName: 'Anna Schmidt', roles: {} }],
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useGroupMembers>);
        mockUseGroupApis.mockReturnValue({
            data: [{ id: 'api-1', name: 'Billing API', version: '1.0' }],
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useGroupApis>);
        mockUseGroupApplications.mockReturnValue({
            data: [{ id: 'app-1', name: 'Mobile App' }],
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useGroupApplications>);
        mockUseGroupApiProducts.mockReturnValue({ data: [], isLoading: false, isError: false } as ReturnType<typeof useGroupApiProducts>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the group name and dependent sections', () => {
        renderPage();

        expect(screen.queryByRole('heading', { name: 'Support Team' })).not.toBeNull();
        expect(screen.getByTestId('members-table').textContent).toContain('Anna Schmidt');
        expect(screen.getByTestId('membership-table-APIs').textContent).toContain('Billing API');
        expect(screen.getByTestId('membership-table-Applications').textContent).toContain('Mobile App');
    });

    it('navigates back to the groups list', () => {
        renderPage();

        fireEvent.click(screen.getByRole('link', { name: /Back to groups/i }));

        expect(screen.queryByText('Groups List')).not.toBeNull();
    });

    it('shows a not-found message when the group fails to load', () => {
        mockUseGroupDetail.mockReturnValue({ data: undefined, isLoading: false, isError: true } as ReturnType<typeof useGroupDetail>);
        renderPage();

        expect(screen.queryByText('Group not found or failed to load.')).not.toBeNull();
    });

    it('has no Edit or Delete action — classic only allows those from the list', () => {
        renderPage();

        expect(screen.queryByRole('button', { name: /Edit/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /Delete/i })).toBeNull();
    });

    describe('created/updated metadata', () => {
        it('shows only Created when there is no distinct updated_at', () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, created_at: 1700000000000 },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.getByText(/^Created/)).not.toBeNull();
            expect(screen.queryByText(/Updated/)).toBeNull();
        });

        it('shows Created and Updated when updated_at differs from created_at', () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, created_at: 1700000000000, updated_at: 1700003600000 },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.getByText(/Updated/)).not.toBeNull();
        });

        it('renders no created/updated line when created_at is absent', () => {
            renderPage();

            expect(screen.queryByText(/^Created/)).toBeNull();
        });
    });

    describe('per-section errors', () => {
        it('shows a section error instead of the members table when members fail to load', () => {
            mockUseGroupMembers.mockReturnValue({ data: [], isLoading: false, isError: true } as ReturnType<typeof useGroupMembers>);
            renderPage();

            expect(screen.getByText('Failed to load members. Please refresh and try again.')).not.toBeNull();
            expect(screen.queryByTestId('members-table')).toBeNull();
        });

        it('shows a section error instead of the APIs table when APIs fail to load', () => {
            mockUseGroupApis.mockReturnValue({ data: [], isLoading: false, isError: true } as ReturnType<typeof useGroupApis>);
            renderPage();

            expect(screen.getByText('Failed to load associated APIs. Please refresh and try again.')).not.toBeNull();
            expect(screen.queryByTestId('membership-table-APIs')).toBeNull();
        });

        it('shows a section error instead of the API Products table when API Products fail to load', () => {
            mockUseGroupApiProducts.mockReturnValue({ data: [], isLoading: false, isError: true } as ReturnType<
                typeof useGroupApiProducts
            >);
            renderPage();

            expect(screen.getByText('Failed to load associated API Products. Please refresh and try again.')).not.toBeNull();
            expect(screen.queryByTestId('membership-table-API Products')).toBeNull();
        });

        it('shows a section error instead of the Applications table when Applications fail to load', () => {
            mockUseGroupApplications.mockReturnValue({ data: [], isLoading: false, isError: true } as ReturnType<
                typeof useGroupApplications
            >);
            renderPage();

            expect(screen.getByText('Failed to load associated applications. Please refresh and try again.')).not.toBeNull();
            expect(screen.queryByTestId('membership-table-Applications')).toBeNull();
        });
    });

    describe('settings section', () => {
        it('shows default roles, lock flags, and invitation settings read-only', () => {
            mockUseGroupDetail.mockReturnValue({
                data: {
                    ...GROUP,
                    roles: { API: 'OWNER', API_PRODUCT: 'USER', APPLICATION: 'USER' },
                    lock_api_role: true,
                    lock_api_product_role: false,
                    lock_application_role: false,
                    max_invitation: 25,
                    system_invitation: true,
                    email_invitation: false,
                    disable_membership_notifications: true,
                },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.getByText('OWNER')).not.toBeNull();
            expect(screen.getByText('25')).not.toBeNull();
            expect(screen.getAllByText('Allowed')).toHaveLength(1);
            expect(screen.getAllByText('Not allowed')).toHaveLength(1);
            // Two "Yes"/"No" pairs: lock_api_product_role=false, lock_application_role=false,
            // disable_membership_notifications=true (→ Notify = No) all render 'No'; lock_api_role=true
            // renders 'Yes' alongside them, so assert via the labels instead of raw Yes/No counts.
            expect(screen.getByText('Lock API role').nextSibling?.textContent).toBe('Yes');
            expect(screen.getByText('Lock API product role').nextSibling?.textContent).toBe('No');
            expect(screen.getByText('Notify on new members').nextSibling?.textContent).toBe('No');
        });

        it('falls back to "Not set"/"Unlimited" when roles and max members are absent', () => {
            renderPage();

            expect(screen.getAllByText('Not set')).toHaveLength(3);
            expect(screen.getByText('Unlimited')).not.toBeNull();
        });
    });

    describe('header badges', () => {
        // Unlike the list, the detail page never shows a "Primary owner" badge — that's summary info for
        // the list view; here it's already conveyed per-member in the Members table's role columns.
        it.each([
            ['primary_owner is true', { primary_owner: true }],
            ['apiPrimaryOwner is set', { apiPrimaryOwner: 'user-1' }],
            ['apiProductPrimaryOwner is set', { apiProductPrimaryOwner: 'user-1' }],
        ])('never shows a Primary owner badge, even when %s', (_label, override) => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, ...override },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.queryByText('Primary owner')).toBeNull();
        });

        it('shows Auto APIs/API Products/Applications badges based on event rules', () => {
            mockUseGroupDetail.mockReturnValue({
                data: {
                    ...GROUP,
                    event_rules: [{ event: 'API_CREATE' }, { event: 'API_PRODUCT_CREATE' }, { event: 'APPLICATION_CREATE' }],
                },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.queryByText('Auto APIs')).not.toBeNull();
            expect(screen.queryByText('Auto API Products')).not.toBeNull();
            expect(screen.queryByText('Auto Applications')).not.toBeNull();
        });
    });
});
