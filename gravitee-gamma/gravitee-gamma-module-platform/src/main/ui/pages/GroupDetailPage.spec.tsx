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
import { useCurrentUserIsGroupAdmin } from '../features/groups/hooks/useCurrentUserGroupAdmin';
import {
    useEnvironmentSettings,
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupInvitations,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import {
    GroupMemberRemovalError,
    GroupMemberUpdateError,
    useAddGroupMembers,
    useAssociateGroupToExisting,
    useDeleteGroup,
    useDeleteGroupInvitation,
    useInviteGroupMember,
    useRemoveGroupMemberWithOwnershipTransfer,
    useUpdateGroupMembersWithRollback,
    useUpdateGroup,
} from '../features/groups/hooks/useGroupMutations';
import { useGroupRoles } from '../features/groups/hooks/useGroupRoles';
import type { Group, GroupInvitation, GroupMember, GroupMembershipItem, GroupMembershipPayload } from '../features/groups/types/group';
import type { RemovalOwnershipTransfer } from '../features/groups/utils/primaryOwnership';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('../features/groups/hooks/useGroupDetail');
jest.mock('../features/groups/hooks/useCurrentUserGroupAdmin');
jest.mock('../features/groups/hooks/useGroupRoles');
jest.mock('../features/groups/hooks/useGroupMutations', () => {
    const actual = jest.requireActual('../features/groups/hooks/useGroupMutations') as {
        GroupMemberRemovalError: typeof GroupMemberRemovalError;
        GroupMemberUpdateError: typeof GroupMemberUpdateError;
    };
    const mocked = jest.createMockFromModule('../features/groups/hooks/useGroupMutations') as Record<string, unknown>;
    return {
        ...mocked,
        GroupMemberRemovalError: actual.GroupMemberRemovalError,
        GroupMemberUpdateError: actual.GroupMemberUpdateError,
    };
});
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

// Stub the nested DataTable-backed components to avoid jsdom/Radix DataTable complexity;
// exposes the fetched rows as plain text so orchestration (what data flows in) stays covered.
// GroupMembersTable's own spec covers its columns/search internals.
jest.mock('../features/groups/components/GroupMembersTable', () => ({
    GroupMembersTable: ({
        members,
        canEditMembers,
        canRemoveMembers,
        onEditRoles,
        onRemove,
    }: {
        members: GroupMember[];
        canEditMembers: boolean;
        canRemoveMembers: boolean;
        onEditRoles: (member: GroupMember) => void;
        onRemove: (member: GroupMember) => void;
    }) => (
        <div data-testid="members-table" data-can-edit-members={canEditMembers} data-can-remove-members={canRemoveMembers}>
            {members.map(member => member.displayName).join(', ')}
            <button type="button" onClick={() => onEditRoles(members[0])}>
                Trigger edit roles
            </button>
            <button type="button" onClick={() => onRemove(members[0])}>
                Trigger remove
            </button>
        </div>
    ),
}));
jest.mock('../features/groups/components/GroupInvitationsTable', () => ({
    GroupInvitationsTable: ({
        invitations,
        onDelete,
    }: {
        invitations: GroupInvitation[];
        onDelete: (invitation: GroupInvitation) => void;
    }) => (
        <div data-testid="invitations-table">
            {invitations.map(invitation => (
                <button type="button" key={invitation.id} onClick={() => onDelete(invitation)}>
                    {invitation.email}
                </button>
            ))}
        </div>
    ),
}));
jest.mock('../features/groups/components/GroupMembershipTable', () => ({
    GroupMembershipTable: ({ items, ariaLabel }: { items: GroupMembershipItem[]; ariaLabel: string }) => (
        <div data-testid={`membership-table-${ariaLabel}`}>{items.map(i => i.name).join(', ')}</div>
    ),
}));
// GroupAddMembersSheet has its own spec covering its internals (search, role selects, selection);
// here we only need to verify the page wires it to the add-members mutation.
jest.mock('../features/groups/components/GroupAddMembersSheet', () => ({
    GroupAddMembersSheet: ({
        open,
        initialSearch,
        onSubmit,
    }: {
        open: boolean;
        initialSearch?: string;
        onSubmit: (m: GroupMembershipPayload[]) => Promise<void>;
    }) =>
        open ? (
            <div data-testid="add-members-sheet">
                {initialSearch ? <span>{initialSearch}</span> : null}
                <button type="button" onClick={() => onSubmit([{ id: 'user-1', roles: [{ scope: 'API', name: 'USER' }] }])}>
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
        onSubmit: (values: { email: string; apiRole: string; applicationRole: string }) => Promise<void>;
    }) =>
        open ? (
            <div data-testid="invite-member-sheet">
                <button type="button" onClick={() => onSubmit({ email: 'user@example.com', apiRole: 'USER', applicationRole: 'USER' })}>
                    Submit invitation
                </button>
            </div>
        ) : null,
}));
jest.mock('../features/groups/components/GroupEditMemberSheet', () => ({
    GroupEditMemberSheet: ({ open, onSubmit }: { open: boolean; onSubmit: (memberships: GroupMembershipPayload[]) => Promise<void> }) =>
        open ? (
            <div data-testid="edit-member-sheet">
                <button type="button" onClick={() => void onSubmit([{ id: 'member-1', roles: [{ scope: 'GROUP', name: 'ADMIN' }] }])}>
                    Submit edit roles
                </button>
                <button
                    type="button"
                    onClick={() =>
                        void onSubmit([
                            { id: 'member-1', roles: [{ scope: 'API', name: 'OWNER' }] },
                            { id: 'member-2', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
                        ])
                    }
                >
                    Submit ownership edit
                </button>
            </div>
        ) : null,
}));
jest.mock('../features/groups/components/GroupRemoveMemberDialog', () => ({
    GroupRemoveMemberDialog: ({
        open,
        onConfirm,
        associatedApiCount,
        associatedApiProductCount,
    }: {
        open: boolean;
        onConfirm: (ownershipTransfer?: RemovalOwnershipTransfer) => Promise<void>;
        associatedApiCount: number | null;
        associatedApiProductCount: number | null;
    }) =>
        open ? (
            <div
                data-testid="remove-member-dialog"
                data-associated-api-count={associatedApiCount}
                data-associated-api-product-count={associatedApiProductCount}
            >
                <button type="button" onClick={() => void onConfirm()}>
                    Submit remove
                </button>
                <button
                    type="button"
                    onClick={() =>
                        void onConfirm({
                            apply: [
                                { id: 'member-1', roles: [{ scope: 'API', name: 'OWNER' }] },
                                { id: 'member-2', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
                            ],
                            rollback: [
                                { id: 'member-2', roles: [{ scope: 'API', name: 'OWNER' }] },
                                { id: 'member-1', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
                            ],
                        })
                    }
                >
                    Submit remove with successor
                </button>
            </div>
        ) : null,
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
const mockUseCurrentUserIsGroupAdmin = jest.mocked(useCurrentUserIsGroupAdmin);
const mockUseGroupDetail = jest.mocked(useGroupDetail);
const mockUseGroupMembers = jest.mocked(useGroupMembers);
const mockUseGroupInvitations = jest.mocked(useGroupInvitations);
const mockUseEnvironmentSettings = jest.mocked(useEnvironmentSettings);
const mockUseGroupApis = jest.mocked(useGroupApis);
const mockUseGroupApplications = jest.mocked(useGroupApplications);
const mockUseGroupApiProducts = jest.mocked(useGroupApiProducts);
const mockUseGroupRoles = jest.mocked(useGroupRoles);
const mockUseUpdateGroup = jest.mocked(useUpdateGroup);
const mockUseDeleteGroup = jest.mocked(useDeleteGroup);
const mockUseAddGroupMembers = jest.mocked(useAddGroupMembers);
const mockUseInviteGroupMember = jest.mocked(useInviteGroupMember);
const mockUseRemoveGroupMemberWithOwnershipTransfer = jest.mocked(useRemoveGroupMemberWithOwnershipTransfer);
const mockUseUpdateGroupMembersWithRollback = jest.mocked(useUpdateGroupMembersWithRollback);
const mockUseDeleteGroupInvitation = jest.mocked(useDeleteGroupInvitation);
const mockUseAssociateGroupToExisting = jest.mocked(useAssociateGroupToExisting);

const GROUP: Group = { id: 'group-1', name: 'Support Team', event_rules: [{ event: 'API_CREATE' }], system_invitation: true };

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function GroupDetailTestRoute({ initialGroupId = 'group-1' }: Readonly<{ initialGroupId?: string }>) {
    return (
        <MemoryRouter initialEntries={[`/groups/${initialGroupId}`]}>
            <Routes>
                <Route path="/groups">
                    <Route index element={<div>Groups List</div>} />
                    <Route path=":groupId" element={<GroupDetailPage />} />
                </Route>
            </Routes>
        </MemoryRouter>
    );
}

function renderPage(initialGroupId = 'group-1') {
    return render(<GroupDetailTestRoute initialGroupId={initialGroupId} />);
}

async function openAddMembersViaSearch() {
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Add members/i }));
    await user.click(await screen.findByRole('menuitem', { name: 'User search' }));
}

async function openEmailInvitation() {
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Add members/i }));
    await user.click(await screen.findByRole('menuitem', { name: 'Email invitation' }));
}

describe('GroupDetailPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseCurrentUserIsGroupAdmin.mockReturnValue(false);
        mockUseGroupDetail.mockReturnValue({ data: GROUP, isLoading: false, isError: false } as ReturnType<typeof useGroupDetail>);
        mockUseGroupMembers.mockReturnValue({
            data: [{ id: 'member-1', displayName: 'Anna Schmidt', roles: {} }],
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useGroupMembers>);
        mockUseGroupInvitations.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
        } as unknown as ReturnType<typeof useGroupInvitations>);
        mockUseEnvironmentSettings.mockReturnValue({ data: undefined } as ReturnType<typeof useEnvironmentSettings>);
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
        mockUseGroupApiProducts.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
        } as unknown as ReturnType<typeof useGroupApiProducts>);
        mockUseGroupRoles.mockReturnValue({
            apiRoles: [{ name: 'USER', scope: 'API', default: true }],
            apiRolesLoading: false,
            applicationRoles: [{ name: 'USER', scope: 'APPLICATION', default: true }],
            applicationRolesLoading: false,
            apiProductRoles: [{ name: 'USER', scope: 'API_PRODUCT', default: true }],
            apiProductRolesLoading: false,
            integrationRoles: [],
            clusterRoles: [],
            explorerRoles: [],
        });
        mockUseUpdateGroup.mockReturnValue(makeMutation());
        mockUseDeleteGroup.mockReturnValue(makeMutation());
        mockUseAddGroupMembers.mockReturnValue(makeMutation());
        mockUseInviteGroupMember.mockReturnValue(makeMutation());
        mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(makeMutation());
        mockUseUpdateGroupMembersWithRollback.mockReturnValue(makeMutation());
        mockUseDeleteGroupInvitation.mockReturnValue(makeMutation());
        mockUseAssociateGroupToExisting.mockReturnValue(makeMutation());
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

            expect(mockUseGroupRoles).toHaveBeenCalledWith({ core: false, extra: false });
        });

        it('fetches only default-group role catalogs once the user opens the Edit sheet', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));

            expect(mockUseGroupRoles).toHaveBeenCalledWith({ core: true, extra: false });
        });

        it('fetches all six role catalogs once the Add members sheet is open', async () => {
            renderPage();
            await openAddMembersViaSearch();

            expect(mockUseGroupRoles).toHaveBeenCalledWith({ core: true, extra: true });
        });

        it('fetches core roles once the Email invitation sheet is open', async () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, email_invitation: true },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();
            await openEmailInvitation();

            expect(mockUseGroupRoles).toHaveBeenCalledWith({ core: true, extra: false });
        });

        it('skips fetching role catalogs when the user cannot edit or add members, since no sheet can open', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();

            expect(mockUseGroupRoles).toHaveBeenCalledWith({ core: false, extra: false });
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

        it('opens the delete dialog, confirms, and navigates back to the groups list', async () => {
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
            mockUseGroupMembers.mockReturnValue({
                data: [],
                isLoading: false,
                isError: true,
            } as unknown as ReturnType<typeof useGroupMembers>);
            renderPage();

            expect(screen.getByText('Failed to load members. Please refresh and try again.')).not.toBeNull();
            expect(screen.queryByTestId('members-table')).toBeNull();
        });

        it('shows a section error instead of the APIs table when APIs fail to load', () => {
            mockUseGroupApis.mockReturnValue({
                data: [],
                isLoading: false,
                isError: true,
            } as unknown as ReturnType<typeof useGroupApis>);
            renderPage();

            expect(screen.getByText('Failed to load associated APIs. Please refresh and try again.')).not.toBeNull();
            expect(screen.queryByTestId('membership-table-APIs')).toBeNull();
        });

        it('shows a section error instead of the API Products table when API Products fail to load', () => {
            mockUseGroupApiProducts.mockReturnValue({ data: [], isLoading: false, isError: true } as unknown as ReturnType<
                typeof useGroupApiProducts
            >);
            renderPage();

            expect(screen.getByText('Failed to load associated API Products. Please refresh and try again.')).not.toBeNull();
            expect(screen.queryByTestId('membership-table-API Products')).toBeNull();
        });

        it('shows a section error instead of the Applications table when Applications fail to load', () => {
            mockUseGroupApplications.mockReturnValue({ data: [], isLoading: false, isError: true } as unknown as ReturnType<
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
        it('hides Add members without permission', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();

            expect(screen.queryByRole('button', { name: /Add members/i })).toBeNull();
            expect(mockUseEnvironmentSettings).toHaveBeenCalledWith({ enabled: false });
        });

        it('hides Add members when both invitation methods are disabled, even with update permission', () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, system_invitation: false, email_invitation: false },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.queryByRole('button', { name: /Add members/i })).toBeNull();
            expect(mockUseEnvironmentSettings).toHaveBeenCalledWith({ enabled: false });
        });

        it('shows Add members when only email invitations are enabled', () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, system_invitation: false, email_invitation: true },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);

            renderPage();

            expect(screen.queryByRole('button', { name: /Add members/i })).not.toBeNull();
        });

        it.each([
            ['User search', { system_invitation: false, email_invitation: true }],
            ['Email invitation', { system_invitation: true, email_invitation: false }],
        ] as const)('disables %s when its group setting is disabled', async (menuItemName, invitationSettings) => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, ...invitationSettings },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            await userEvent.setup().click(screen.getByRole('button', { name: /Add members/i }));

            expect((await screen.findByRole('menuitem', { name: menuItemName })).getAttribute('aria-disabled')).toBe('true');
        });

        it('opens the Add members sheet, submits, and shows a success toast', async () => {
            const addMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseAddGroupMembers.mockReturnValue(makeMutation(addMutateAsync));
            renderPage();

            await openAddMembersViaSearch();
            expect(screen.getByTestId('add-members-sheet')).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Submit add members' }));

            await waitFor(() =>
                expect(addMutateAsync).toHaveBeenCalledWith({
                    groupId: 'group-1',
                    memberships: [{ id: 'user-1', roles: [{ scope: 'API', name: 'USER' }] }],
                }),
            );
            expect(notify.success).toHaveBeenCalledWith('Member added successfully');
            await waitFor(() => expect(screen.queryByTestId('add-members-sheet')).toBeNull());
        });

        it('shows an error toast and keeps the sheet open when adding members fails', async () => {
            const error = new Error('failed');
            mockUseAddGroupMembers.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            await openAddMembersViaSearch();
            fireEvent.click(screen.getByRole('button', { name: 'Submit add members' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to add members'));
            expect(screen.queryByTestId('add-members-sheet')).not.toBeNull();
        });
    });

    describe('Edit roles', () => {
        it('does not expose Remove to an update-only operator', () => {
            mockUseHasPermission.mockImplementation(({ anyOf }) => anyOf?.includes('environment-group-u') ?? false);
            renderPage();

            expect(screen.getByTestId('members-table').getAttribute('data-can-edit-members')).toBe('true');
            expect(screen.getByTestId('members-table').getAttribute('data-can-remove-members')).toBe('false');
        });

        it('opens the edit sheet, submits role changes, and shows a success toast', async () => {
            const editMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseAddGroupMembers.mockReturnValue(makeMutation(editMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger edit roles' }));
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

        it('shows an error and keeps the sheet open when updating roles fails', async () => {
            const error = new Error('failed');
            mockUseAddGroupMembers.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger edit roles' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit edit roles' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to update member roles'));
            expect(screen.getByTestId('edit-member-sheet')).not.toBeNull();
        });

        it('reports that original roles were restored when an ownership edit fails', async () => {
            const updateError = new Error('update failed');
            mockUseUpdateGroupMembersWithRollback.mockReturnValue(
                makeMutation(jest.fn().mockRejectedValue(new GroupMemberUpdateError('update', updateError, true))),
            );
            mockUseGroupMembers.mockReturnValue({
                data: [
                    { id: 'member-1', displayName: 'Anna Schmidt', roles: { API: 'PRIMARY_OWNER' } },
                    { id: 'member-2', displayName: 'Ravi Patel', roles: { API: 'OWNER' } },
                ],
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof useGroupMembers>);
            mockUseGroupApis.mockReturnValue({ data: [], isLoading: false, isError: false } as unknown as ReturnType<typeof useGroupApis>);
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger edit roles' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit ownership edit' }));

            await waitFor(() =>
                expect(notify.error).toHaveBeenCalledWith(updateError, 'Member roles could not be updated. Original roles were restored.'),
            );
        });
    });

    describe('Remove member', () => {
        it('removes the member and closes the dialog', async () => {
            const removeMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(makeMutation(removeMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove' }));

            await waitFor(() =>
                expect(removeMutateAsync).toHaveBeenCalledWith({
                    groupId: 'group-1',
                    memberId: 'member-1',
                    ownershipTransfer: undefined,
                }),
            );
            expect(notify.success).toHaveBeenCalledWith('Anna Schmidt removed from the group');
            await waitFor(() => expect(screen.queryByTestId('remove-member-dialog')).toBeNull());
        });

        it('shows an error and keeps the dialog open when removal fails', async () => {
            const error = new Error('failed');
            mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to remove member'));
            expect(screen.getByTestId('remove-member-dialog')).not.toBeNull();
        });

        it('submits the ownership transfer plan with the member removal', async () => {
            const removeMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(makeMutation(removeMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove with successor' }));

            await waitFor(() =>
                expect(removeMutateAsync).toHaveBeenCalledWith({
                    groupId: 'group-1',
                    memberId: 'member-1',
                    ownershipTransfer: {
                        apply: [
                            { id: 'member-1', roles: [{ scope: 'API', name: 'OWNER' }] },
                            { id: 'member-2', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
                        ],
                        rollback: [
                            { id: 'member-2', roles: [{ scope: 'API', name: 'OWNER' }] },
                            { id: 'member-1', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
                        ],
                    },
                }),
            );
        });

        it('reports that ownership was restored when removal fails after transfer', async () => {
            const removeError = new Error('remove failed');
            mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(
                makeMutation(jest.fn().mockRejectedValue(new GroupMemberRemovalError('remove', removeError, true))),
            );
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove with successor' }));

            await waitFor(() =>
                expect(notify.error).toHaveBeenCalledWith(removeError, 'The member could not be removed. Primary ownership was restored.'),
            );
            expect(screen.getByTestId('remove-member-dialog')).not.toBeNull();
        });

        it('does not remove a primary owner without a transfer payload even if the dialog guard is bypassed', async () => {
            const removeMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseGroupApis.mockReturnValue({
                data: [],
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof useGroupApis>);
            mockUseGroupMembers.mockReturnValue({
                data: [{ id: 'member-1', displayName: 'Anna Schmidt', roles: { API: 'PRIMARY_OWNER' } }],
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof useGroupMembers>);
            mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(makeMutation(removeMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove' }));

            await waitFor(() =>
                expect(notify.warning).toHaveBeenCalledWith('Primary ownership must be transferred before removing this member'),
            );
            expect(removeMutateAsync).not.toHaveBeenCalled();
            expect(screen.getByTestId('remove-member-dialog')).not.toBeNull();
        });

        it('does not call ownership transfer while an API primary-owner group still owns APIs', async () => {
            const removeMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseGroupMembers.mockReturnValue({
                data: [
                    { id: 'member-1', displayName: 'Anna Schmidt', roles: { API: 'PRIMARY_OWNER' } },
                    { id: 'member-2', displayName: 'Ravi Patel', roles: { API: 'OWNER' } },
                ],
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof useGroupMembers>);
            mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(makeMutation(removeMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove with successor' }));

            await waitFor(() =>
                expect(notify.warning).toHaveBeenCalledWith(
                    'This member cannot be removed while the group is the primary owner of 1 API. Transfer that API to another primary owner first.',
                ),
            );
            expect(removeMutateAsync).not.toHaveBeenCalled();
        });

        it('does not remove the member when ownership transfer fails', async () => {
            const error = new Error('transfer failed');
            const removeMutateAsync = jest.fn().mockResolvedValue(undefined);
            removeMutateAsync.mockRejectedValue(new GroupMemberRemovalError('transfer', error, true));
            mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(makeMutation(removeMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove with successor' }));

            await waitFor(() =>
                expect(notify.error).toHaveBeenCalledWith(
                    error,
                    'Primary ownership could not be transferred. Original roles were restored.',
                ),
            );
            expect(removeMutateAsync).toHaveBeenCalledTimes(1);
            expect(screen.getByTestId('remove-member-dialog')).not.toBeNull();
        });

        it('reports when ownership rollback also fails', async () => {
            const removeError = new Error('remove failed');
            const rollbackError = new Error('rollback failed');
            mockUseRemoveGroupMemberWithOwnershipTransfer.mockReturnValue(
                makeMutation(jest.fn().mockRejectedValue(new GroupMemberRemovalError('rollback', removeError, false, rollbackError))),
            );
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));
            fireEvent.click(screen.getByRole('button', { name: 'Submit remove with successor' }));

            await waitFor(() =>
                expect(notify.error).toHaveBeenCalledWith(
                    rollbackError,
                    'The member could not be removed and primary ownership could not be restored. Refresh the member list before retrying.',
                ),
            );
        });

        it('passes the loaded API and API Product counts to the removal preflight', () => {
            mockUseGroupApiProducts.mockReturnValue({
                data: [{ id: 'product-1', name: 'Billing Product', version: '1.0' }],
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupApiProducts>);
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Trigger remove' }));

            expect(screen.getByTestId('remove-member-dialog').getAttribute('data-associated-api-count')).toBe('1');
            expect(screen.getByTestId('remove-member-dialog').getAttribute('data-associated-api-product-count')).toBe('1');
        });
    });

    describe('Email invitations', () => {
        beforeEach(() => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, email_invitation: true },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
        });

        it('switches to Invitations after creating a pending invitation', async () => {
            const inviteMutateAsync = jest.fn().mockResolvedValue({ outcome: 'invitation-created' });
            mockUseInviteGroupMember.mockReturnValue(makeMutation(inviteMutateAsync));
            renderPage();

            await openEmailInvitation();
            fireEvent.click(screen.getByRole('button', { name: 'Submit invitation' }));

            await waitFor(() =>
                expect(inviteMutateAsync).toHaveBeenCalledWith({
                    groupId: 'group-1',
                    data: {
                        reference_type: 'GROUP',
                        reference_id: 'group-1',
                        email: 'user@example.com',
                        api_role: 'USER',
                        application_role: 'USER',
                    },
                }),
            );
            expect(notify.success).toHaveBeenCalledWith('Successfully invited user to the group.');
            await waitFor(() => expect(screen.getByRole('tab', { name: 'Invitations' }).getAttribute('data-state')).toBe('active'));
        });

        it('switches to Members when the email belongs to one existing user', async () => {
            mockUseInviteGroupMember.mockReturnValue(makeMutation(jest.fn().mockResolvedValue({ outcome: 'member-added' })));
            renderPage();
            await userEvent.setup().click(screen.getByRole('tab', { name: 'Invitations' }));

            await openEmailInvitation();
            fireEvent.click(screen.getByRole('button', { name: 'Submit invitation' }));

            await waitFor(() => expect(notify.success).toHaveBeenCalledWith('Member added successfully'));
            expect(screen.getByRole('tab', { name: 'Members' }).getAttribute('data-state')).toBe('active');
        });

        it('continues an ambiguous email response in user search with the email prefilled', async () => {
            mockUseInviteGroupMember.mockReturnValue(makeMutation(jest.fn().mockResolvedValue({ outcome: 'ambiguous' })));
            renderPage();

            await openEmailInvitation();
            fireEvent.click(screen.getByRole('button', { name: 'Submit invitation' }));

            expect(await screen.findByRole('heading', { name: 'Many Users Found' })).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Continue' }));

            expect(screen.getByTestId('add-members-sheet').textContent).toContain('user@example.com');
        });

        it('loads invitations only after the invitations tab is selected', async () => {
            renderPage();

            expect(mockUseGroupInvitations).toHaveBeenLastCalledWith(undefined);
            await userEvent.setup().click(screen.getByRole('tab', { name: 'Invitations' }));

            expect(mockUseGroupInvitations).toHaveBeenLastCalledWith('group-1');
        });

        it('confirms and deletes an invitation', async () => {
            const user = userEvent.setup();
            const invitation: GroupInvitation = {
                id: 'invitation-1',
                reference_id: 'group-1',
                email: 'user@example.com',
            };
            const deleteMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseGroupInvitations.mockReturnValue({
                data: [invitation],
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof useGroupInvitations>);
            mockUseDeleteGroupInvitation.mockReturnValue(makeMutation(deleteMutateAsync));
            renderPage();

            await user.click(screen.getByRole('tab', { name: 'Invitations' }));
            await user.click(screen.getByRole('button', { name: 'user@example.com' }));
            expect(screen.getByRole('heading', { name: 'Delete Invitation' })).not.toBeNull();
            await user.click(screen.getByRole('button', { name: 'Continue' }));

            await waitFor(() =>
                expect(deleteMutateAsync).toHaveBeenCalledWith({
                    groupId: 'group-1',
                    invitationId: 'invitation-1',
                }),
            );
            expect(notify.success).toHaveBeenCalledWith('Invitation deleted successfully');
        });
    });

    describe('self-service group admin (manageable)', () => {
        it('shows Add members via manageable + system_invitation even without environment-group-u', () => {
            mockUseHasPermission.mockReturnValue(false);
            mockUseCurrentUserIsGroupAdmin.mockReturnValue(true);
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: true, system_invitation: true },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.queryByRole('button', { name: /Add members/i })).not.toBeNull();
            expect(screen.getByTestId('members-table').getAttribute('data-can-edit-members')).toBe('true');
            expect(screen.getByTestId('members-table').getAttribute('data-can-remove-members')).toBe('true');
            expect(mockUseEnvironmentSettings).toHaveBeenCalledWith({ enabled: false });
        });

        it('hides Add members when manageable but system invitation is disabled', () => {
            mockUseHasPermission.mockReturnValue(false);
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: true, system_invitation: false, email_invitation: false },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.queryByRole('button', { name: /Add members/i })).toBeNull();
        });

        it('hides Add members when not manageable and lacking environment-group-u', () => {
            mockUseHasPermission.mockReturnValue(false);
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: false },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.queryByRole('button', { name: /Add members/i })).toBeNull();
        });

        it('allows a group admin to edit and remove members without environment permissions', () => {
            mockUseHasPermission.mockReturnValue(false);
            mockUseCurrentUserIsGroupAdmin.mockReturnValue(true);
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: true },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.getByTestId('members-table').getAttribute('data-can-edit-members')).toBe('true');
            expect(screen.getByTestId('members-table').getAttribute('data-can-remove-members')).toBe('true');
        });

        it('hides member actions without environment permissions or group admin membership', () => {
            mockUseHasPermission.mockReturnValue(false);
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: false },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.getByTestId('members-table').getAttribute('data-can-edit-members')).toBe('false');
            expect(screen.getByTestId('members-table').getAttribute('data-can-remove-members')).toBe('false');
        });
    });

    describe('member limit', () => {
        it('shows an info banner and hides Add members once the group has reached max_invitation', () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, max_invitation: 1 },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            mockUseGroupMembers.mockReturnValue({
                data: [{ id: 'member-1', displayName: 'Anna Schmidt', roles: {} }],
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupMembers>);
            renderPage();

            expect(
                screen.getByText(
                    'The number of members in this group has reached maximum allowed. Adding users via search and email invitation have been disabled.',
                ),
            ).not.toBeNull();
            expect(screen.queryByRole('button', { name: /Add members/i })).toBeNull();
        });

        it('does not show the banner below the limit', () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, max_invitation: 5 },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.queryByText(/reached maximum allowed/)).toBeNull();
            expect(screen.queryByRole('button', { name: /Add members/i })).not.toBeNull();
        });
    });

    describe('Add existing APIs/API Products/Applications', () => {
        beforeEach(() => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: true },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
        });

        it('hides the Add existing buttons without permission', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();

            expect(screen.queryByRole('button', { name: 'Add group to existing APIs' })).toBeNull();
            expect(screen.queryByRole('button', { name: 'Add group to existing API Products' })).toBeNull();
            expect(screen.queryByRole('button', { name: 'Add group to existing applications' })).toBeNull();
        });

        it('shows the Add existing buttons with update permission even when the group is not manageable', () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: false },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));

            expect(screen.queryByRole('button', { name: 'Add group to existing APIs' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Add group to existing API Products' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Add group to existing applications' })).not.toBeNull();
        });

        it('shows the Add existing buttons with update permission when manageability is unspecified', () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: undefined },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));

            expect(screen.queryByRole('button', { name: 'Add group to existing APIs' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Add group to existing API Products' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Add group to existing applications' })).not.toBeNull();
        });

        it.each([
            ['api', 'APIs'],
            ['api_product', 'API Products'],
            ['application', 'applications'],
        ] as const)('confirms and associates the group with all existing %s, then shows a success toast', async (type, label) => {
            const associateMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseAssociateGroupToExisting.mockReturnValue(makeMutation(associateMutateAsync));
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));

            fireEvent.click(screen.getByRole('button', { name: `Add group to existing ${label}` }));
            expect(screen.getByRole('heading', { name: `Add group to existing ${label}` })).not.toBeNull();
            expect(
                screen.getByText(`You are trying to add the group to all the existing ${label}. Do you want to continue?`),
            ).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Continue' }));

            await waitFor(() => expect(associateMutateAsync).toHaveBeenCalledWith({ groupId: 'group-1', type }));
            expect(notify.success).toHaveBeenCalledWith(`Successfully added the group to existing ${label}.`);
        });

        it('shows an error toast when association fails', async () => {
            const error = new Error('failed');
            mockUseAssociateGroupToExisting.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));

            fireEvent.click(screen.getByRole('button', { name: 'Add group to existing APIs' }));
            fireEvent.click(screen.getByRole('button', { name: 'Continue' }));

            await waitFor(() =>
                expect(notify.error).toHaveBeenCalledWith(error, 'Error occurred while adding the group to existing APIs.'),
            );
        });

        it('disables confirmation when update permission is lost after the dialog opens', () => {
            const associateMutateAsync = jest.fn();
            mockUseAssociateGroupToExisting.mockReturnValue(makeMutation(associateMutateAsync));
            const view = renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));
            fireEvent.click(screen.getByRole('button', { name: 'Add group to existing APIs' }));

            mockUseHasPermission.mockReturnValue(false);
            view.rerender(<GroupDetailTestRoute />);

            expect(screen.getByRole('button', { name: 'Continue' }).hasAttribute('disabled')).toBe(true);
            expect(associateMutateAsync).not.toHaveBeenCalled();
        });

        it('closes the dialog when Cancel is clicked', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));

            fireEvent.click(screen.getByRole('button', { name: 'Add group to existing APIs' }));
            expect(screen.getByRole('heading', { name: 'Add group to existing APIs' })).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

            expect(screen.queryByRole('heading', { name: 'Add group to existing APIs' })).toBeNull();
        });
    });
});
