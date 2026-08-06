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
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { GroupDetailPage } from './GroupDetailPage';
import {
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import {
    useAddGroupMembers,
    useDeleteGroup,
    useInviteGroupMember,
    useRemoveGroupMember,
    useUpdateGroup,
} from '../features/groups/hooks/useGroupMutations';
import {
    useGroupApiProductRoles,
    useGroupApiRoles,
    useGroupApplicationRoles,
    useGroupClusterRoles,
    useGroupIntegrationRoles,
} from '../features/groups/hooks/useGroupRoles';
import type { Group, GroupMember, GroupMembershipItem, GroupMembershipPayload } from '../features/groups/types/group';
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
// GroupMembersTable's own spec covers its columns/search/actions-menu internals — here we only need
// buttons that let us trigger the onEditRoles/onRemove callbacks the page wires up.
jest.mock('../features/groups/components/GroupMembersTable', () => ({
    GroupMembersTable: ({
        members,
        onEditRoles,
        onRemove,
    }: {
        members: GroupMember[];
        onEditRoles: (member: GroupMember) => void;
        onRemove: (member: GroupMember) => void;
    }) => (
        <div data-testid="members-table">
            {members.map(m => m.displayName).join(', ')}
            <button type="button" onClick={() => onEditRoles(members[0])}>
                Trigger edit roles
            </button>
            <button type="button" onClick={() => onRemove(members[0])}>
                Trigger remove
            </button>
        </div>
    ),
}));
jest.mock('../features/groups/components/GroupMembershipTable', () => ({
    GroupMembershipTable: ({ items, ariaLabel }: { items: GroupMembershipItem[]; ariaLabel: string }) => (
        <div data-testid={`membership-table-${ariaLabel}`}>{items.map(i => i.name).join(', ')}</div>
    ),
}));
// GroupAddMembersSheet / GroupInviteMemberSheet each have their own dedicated spec covering internals
// (search, role selects, validation); here we only need to verify the page wires them correctly.
jest.mock('../features/groups/components/GroupAddMembersSheet', () => ({
    GroupAddMembersSheet: ({ open, onSubmit }: { open: boolean; onSubmit: (m: GroupMembershipPayload[]) => void }) =>
        open ? (
            <div data-testid="add-members-sheet">
                <button type="button" onClick={() => onSubmit([{ id: 'user-1', roles: [{ scope: 'GROUP', name: 'ADMIN' }] }])}>
                    Submit add members
                </button>
            </div>
        ) : null,
}));
jest.mock('../features/groups/components/GroupInviteMemberSheet', () => ({
    GroupInviteMemberSheet: ({
        open,
        onSubmit,
    }: {
        open: boolean;
        onSubmit: (v: { email: string; apiRole: string; applicationRole: string }) => void;
    }) =>
        open ? (
            <div data-testid="invite-member-sheet">
                <button type="button" onClick={() => onSubmit({ email: 'anna@lufthansa.com', apiRole: '', applicationRole: '' })}>
                    Submit invite
                </button>
            </div>
        ) : null,
}));
// GroupEditMemberSheet / GroupRemoveMemberSheet each have their own dedicated spec covering internals;
// here we only need to verify the page wires them correctly to the right mutations.
jest.mock('../features/groups/components/GroupEditMemberSheet', () => ({
    GroupEditMemberSheet: ({ open, onSubmit }: { open: boolean; onSubmit: (payload: GroupMembershipPayload) => void }) =>
        open ? (
            <div data-testid="edit-member-sheet">
                <button type="button" onClick={() => onSubmit({ id: 'member-1', roles: [{ scope: 'GROUP', name: 'ADMIN' }] })}>
                    Submit edit roles
                </button>
            </div>
        ) : null,
}));
jest.mock('../features/groups/components/GroupRemoveMemberSheet', () => ({
    GroupRemoveMemberSheet: ({ open, onConfirm }: { open: boolean; onConfirm: () => void }) =>
        open ? (
            <div data-testid="remove-member-sheet">
                <button type="button" onClick={onConfirm}>
                    Submit remove
                </button>
            </div>
        ) : null,
}));

// Radix Switch/Select (rendered inside the real GroupSheet, mounted unconditionally so Edit can open it)
// measure via ResizeObserver, which jsdom lacks.
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
const mockUseGroupIntegrationRoles = jest.mocked(useGroupIntegrationRoles);
const mockUseGroupClusterRoles = jest.mocked(useGroupClusterRoles);
const mockUseUpdateGroup = jest.mocked(useUpdateGroup);
const mockUseDeleteGroup = jest.mocked(useDeleteGroup);
const mockUseAddGroupMembers = jest.mocked(useAddGroupMembers);
const mockUseInviteGroupMember = jest.mocked(useInviteGroupMember);
const mockUseRemoveGroupMember = jest.mocked(useRemoveGroupMember);

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
        mockUseGroupIntegrationRoles.mockReturnValue({ data: [], isLoading: false } as ReturnType<typeof useGroupIntegrationRoles>);
        mockUseGroupClusterRoles.mockReturnValue({ data: [], isLoading: false } as ReturnType<typeof useGroupClusterRoles>);
        mockUseUpdateGroup.mockReturnValue(makeMutation());
        mockUseDeleteGroup.mockReturnValue(makeMutation());
        mockUseAddGroupMembers.mockReturnValue(makeMutation());
        mockUseInviteGroupMember.mockReturnValue(makeMutation());
        mockUseRemoveGroupMember.mockReturnValue(makeMutation());
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

    describe('Add members', () => {
        it('hides the Add members trigger without permission', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();

            expect(screen.queryByRole('button', { name: /Add members/i })).toBeNull();
        });

        it('offers User search and Email invitation from the dropdown', async () => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: /Add members/i }));

            expect(await screen.findByRole('menuitem', { name: /User search/i })).not.toBeNull();
            expect(screen.queryByRole('menuitem', { name: /Email invitation/i })).not.toBeNull();
        });

        it('opens GroupAddMembersSheet from the User search option, submits, and shows a success toast', async () => {
            const addMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseAddGroupMembers.mockReturnValue(makeMutation(addMutateAsync));
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: /Add members/i }));
            await user.click(await screen.findByRole('menuitem', { name: /User search/i }));

            expect(screen.getByTestId('add-members-sheet')).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Submit add members' }));

            await waitFor(() =>
                expect(addMutateAsync).toHaveBeenCalledWith({
                    groupId: 'group-1',
                    memberships: [{ id: 'user-1', roles: [{ scope: 'GROUP', name: 'ADMIN' }] }],
                }),
            );
            expect(notify.success).toHaveBeenCalledWith('Member added successfully');
            await waitFor(() => expect(screen.queryByTestId('add-members-sheet')).toBeNull());
        });

        it('opens GroupInviteMemberSheet from the Email invitation option, submits, and shows a success toast', async () => {
            const inviteMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseInviteGroupMember.mockReturnValue(makeMutation(inviteMutateAsync));
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: /Add members/i }));
            await user.click(await screen.findByRole('menuitem', { name: /Email invitation/i }));

            expect(screen.getByTestId('invite-member-sheet')).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Submit invite' }));

            await waitFor(() =>
                expect(inviteMutateAsync).toHaveBeenCalledWith({
                    groupId: 'group-1',
                    data: { reference_id: 'group-1', email: 'anna@lufthansa.com', api_role: undefined, application_role: undefined },
                }),
            );
            expect(notify.success).toHaveBeenCalledWith('Invitation sent to anna@lufthansa.com');
            await waitFor(() => expect(screen.queryByTestId('invite-member-sheet')).toBeNull());
        });

        it('shows an error toast when adding members fails', async () => {
            const error = new Error('failed');
            mockUseAddGroupMembers.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('button', { name: /Add members/i }));
            await user.click(await screen.findByRole('menuitem', { name: /User search/i }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit add members' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to add members'));
        });
    });

    describe('Edit roles', () => {
        it('opens GroupEditMemberSheet, submits, and shows a success toast', async () => {
            const editMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseAddGroupMembers.mockReturnValue(makeMutation(editMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger edit roles' }));
            expect(screen.getByTestId('edit-member-sheet')).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Submit edit roles' }));

            await waitFor(() =>
                expect(editMutateAsync).toHaveBeenCalledWith({
                    groupId: 'group-1',
                    memberships: [{ id: 'member-1', roles: [{ scope: 'GROUP', name: 'ADMIN' }] }],
                }),
            );
            expect(notify.success).toHaveBeenCalledWith('Member roles updated successfully');
            await waitFor(() => expect(screen.queryByTestId('edit-member-sheet')).toBeNull());
        });

        it('shows an error toast when editing roles fails', async () => {
            const error = new Error('failed');
            mockUseAddGroupMembers.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger edit roles' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit edit roles' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to update member roles'));
        });
    });

    describe('Remove member', () => {
        it('opens GroupRemoveMemberSheet, confirms, and shows a success toast', async () => {
            const removeMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseRemoveGroupMember.mockReturnValue(makeMutation(removeMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            expect(screen.getByTestId('remove-member-sheet')).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove' }));

            await waitFor(() => expect(removeMutateAsync).toHaveBeenCalledWith({ groupId: 'group-1', memberId: 'member-1' }));
            expect(notify.success).toHaveBeenCalledWith('Anna Schmidt removed from the group');
            await waitFor(() => expect(screen.queryByTestId('remove-member-sheet')).toBeNull());
        });

        it('shows an error toast when removing a member fails', async () => {
            const error = new Error('failed');
            mockUseRemoveGroupMember.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to remove member'));
        });
    });
});
