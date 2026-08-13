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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { GroupDetailPage } from './GroupDetailPage';
import {
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import { useDeleteGroup, useUpdateGroup } from '../features/groups/hooks/useGroupMutations';
import { useGroupApiProductRoles, useGroupApiRoles, useGroupApplicationRoles } from '../features/groups/hooks/useGroupRoles';
import type { Group, GroupMembershipItem } from '../features/groups/types/group';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('../features/groups/hooks/useGroupDetail');
jest.mock('../features/groups/hooks/useGroupRoles');
jest.mock('../features/groups/hooks/useGroupMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

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

// Radix Switch (rendered inside the real GroupSheet, mounted unconditionally so Edit can open it) measures
// its thumb via ResizeObserver, which jsdom lacks.
beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseGroupDetail = jest.mocked(useGroupDetail);
const mockUseGroupMembers = jest.mocked(useGroupMembers);
const mockUseGroupApis = jest.mocked(useGroupApis);
const mockUseGroupApplications = jest.mocked(useGroupApplications);
const mockUseGroupApiProducts = jest.mocked(useGroupApiProducts);
const mockUseGroupApiRoles = jest.mocked(useGroupApiRoles);
const mockUseGroupApplicationRoles = jest.mocked(useGroupApplicationRoles);
const mockUseGroupApiProductRoles = jest.mocked(useGroupApiProductRoles);
const mockUseUpdateGroup = jest.mocked(useUpdateGroup);
const mockUseDeleteGroup = jest.mocked(useDeleteGroup);

const GROUP: Group = { id: 'group-1', name: 'Support Team', event_rules: [{ event: 'API_CREATE' }] };

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

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
        mockUseHasPermission.mockReturnValue(true);
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
        mockUseGroupApiRoles.mockReturnValue({ data: [{ name: 'USER', scope: 'API', default: true }], isLoading: false } as ReturnType<
            typeof useGroupApiRoles
        >);
        mockUseGroupApplicationRoles.mockReturnValue({
            data: [{ name: 'USER', scope: 'APPLICATION', default: true }],
            isLoading: false,
        } as ReturnType<typeof useGroupApplicationRoles>);
        mockUseGroupApiProductRoles.mockReturnValue({
            data: [{ name: 'USER', scope: 'API_PRODUCT', default: true }],
            isLoading: false,
        } as ReturnType<typeof useGroupApiProductRoles>);
        mockUseUpdateGroup.mockReturnValue(makeMutation());
        mockUseDeleteGroup.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the group name and dependent sections', () => {
        renderPage();

        expect(screen.queryByRole('heading', { name: 'Support Team' })).not.toBeNull();
        // GroupSettingsSection's own spec covers its rendering in detail — this just confirms the group
        // data actually reaches it.
        expect(screen.getByText('Max members').nextElementSibling?.textContent).toBe('Unlimited');
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

    describe('role queries', () => {
        it('skips fetching role catalogs on initial load, even when the user can edit — deferred until Edit opens', () => {
            renderPage();

            expect(mockUseGroupApiRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupApplicationRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupApiProductRoles).toHaveBeenCalledWith({ enabled: false });
        });

        it('fetches role catalogs once the user opens the Edit sheet', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));

            expect(mockUseGroupApiRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupApplicationRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupApiProductRoles).toHaveBeenCalledWith({ enabled: true });
        });

        it('skips fetching role catalogs when the user cannot edit, since the sheet can never open', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();

            expect(mockUseGroupApiRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupApplicationRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupApiProductRoles).toHaveBeenCalledWith({ enabled: false });
        });
    });

    describe('edit and delete actions', () => {
        it('shows Edit group and Delete when the user has permission', () => {
            renderPage();

            expect(screen.queryByRole('button', { name: 'Edit group' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeNull();
        });

        it('hides Edit group and Delete without permission', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();

            expect(screen.queryByRole('button', { name: 'Edit group' })).toBeNull();
            expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull();
        });

        it('opens the edit sheet, submits, and shows a success toast', async () => {
            const mutateAsync = jest.fn().mockResolvedValue({});
            mockUseUpdateGroup.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));
            expect(screen.getByRole('heading', { name: 'Edit group' })).not.toBeNull();

            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Support Team v2' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() =>
                expect(mutateAsync).toHaveBeenCalledWith(
                    expect.objectContaining({ groupId: 'group-1', data: expect.objectContaining({ name: 'Support Team v2' }) }),
                ),
            );
            expect(notify.success).toHaveBeenCalledWith('Group updated successfully');
        });

        it('shows an error toast when updating fails', async () => {
            const error = new Error('update failed');
            mockUseUpdateGroup.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Support Team v2' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to update group'));
        });

        it('opens the delete sheet, confirms, and navigates back to the groups list', async () => {
            const mutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseDeleteGroup.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
            expect(screen.getByRole('heading', { name: 'Delete group' })).not.toBeNull();

            // Radix's Dialog marks the rest of the page aria-hidden while open, so the header trigger
            // drops out of the accessibility tree here — only the dialog's own "Delete" is queryable.
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith('group-1'));
            expect(notify.success).toHaveBeenCalledWith('Group deleted successfully');
            await waitFor(() => expect(screen.queryByText('Groups List')).not.toBeNull());
        });

        it('shows an error toast when deleting fails', async () => {
            const error = new Error('delete failed');
            mockUseDeleteGroup.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to delete group'));
        });
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
