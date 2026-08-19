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
    useEnvironmentSettings,
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupInvitations,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import {
    useAddGroupMembers,
    useDeleteGroup,
    useDeleteGroupInvitation,
    useInviteGroupMember,
    useUpdateGroup,
} from '../features/groups/hooks/useGroupMutations';
import {
    useGroupApiProductRoles,
    useGroupApiRoles,
    useGroupApplicationRoles,
    useGroupClusterRoles,
    useGroupExplorerRoles,
    useGroupIntegrationRoles,
} from '../features/groups/hooks/useGroupRoles';
import type { Group, GroupInvitation, GroupMembershipItem, GroupMembershipPayload } from '../features/groups/types/group';
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
const mockUseGroupInvitations = jest.mocked(useGroupInvitations);
const mockUseEnvironmentSettings = jest.mocked(useEnvironmentSettings);
const mockUseGroupApis = jest.mocked(useGroupApis);
const mockUseGroupApplications = jest.mocked(useGroupApplications);
const mockUseGroupApiProducts = jest.mocked(useGroupApiProducts);
const mockUseGroupApiRoles = jest.mocked(useGroupApiRoles);
const mockUseGroupApplicationRoles = jest.mocked(useGroupApplicationRoles);
const mockUseGroupApiProductRoles = jest.mocked(useGroupApiProductRoles);
const mockUseGroupIntegrationRoles = jest.mocked(useGroupIntegrationRoles);
const mockUseGroupClusterRoles = jest.mocked(useGroupClusterRoles);
const mockUseGroupExplorerRoles = jest.mocked(useGroupExplorerRoles);
const mockUseUpdateGroup = jest.mocked(useUpdateGroup);
const mockUseDeleteGroup = jest.mocked(useDeleteGroup);
const mockUseAddGroupMembers = jest.mocked(useAddGroupMembers);
const mockUseInviteGroupMember = jest.mocked(useInviteGroupMember);
const mockUseDeleteGroupInvitation = jest.mocked(useDeleteGroupInvitation);

const GROUP: Group = { id: 'group-1', name: 'Support Team', event_rules: [{ event: 'API_CREATE' }], system_invitation: true };

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
        mockUseGroupIntegrationRoles.mockReturnValue({
            data: [],
            isLoading: false,
        } as unknown as ReturnType<typeof useGroupIntegrationRoles>);
        mockUseGroupClusterRoles.mockReturnValue({
            data: [],
            isLoading: false,
        } as unknown as ReturnType<typeof useGroupClusterRoles>);
        mockUseGroupExplorerRoles.mockReturnValue({
            data: [],
            isLoading: false,
        } as unknown as ReturnType<typeof useGroupExplorerRoles>);
        mockUseUpdateGroup.mockReturnValue(makeMutation());
        mockUseDeleteGroup.mockReturnValue(makeMutation());
        mockUseAddGroupMembers.mockReturnValue(makeMutation());
        mockUseInviteGroupMember.mockReturnValue(makeMutation());
        mockUseDeleteGroupInvitation.mockReturnValue(makeMutation());
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
            expect(mockUseGroupIntegrationRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupClusterRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupExplorerRoles).toHaveBeenCalledWith({ enabled: false });
        });

        it('fetches only default-group role catalogs once the user opens the Edit sheet', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Edit group' }));

            expect(mockUseGroupApiRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupApplicationRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupApiProductRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupIntegrationRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupClusterRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupExplorerRoles).toHaveBeenCalledWith({ enabled: false });
        });

        it('fetches all six role catalogs once the Add members sheet is open', async () => {
            renderPage();
            await openAddMembersViaSearch();

            expect(mockUseGroupApiRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupApplicationRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupApiProductRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupIntegrationRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupClusterRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupExplorerRoles).toHaveBeenCalledWith({ enabled: true });
        });

        it('fetches only API and application roles once the Email invitation sheet is open', async () => {
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, email_invitation: true },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();
            await openEmailInvitation();

            expect(mockUseGroupApiRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupApplicationRoles).toHaveBeenCalledWith({ enabled: true });
            expect(mockUseGroupApiProductRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupIntegrationRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupClusterRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupExplorerRoles).toHaveBeenCalledWith({ enabled: false });
        });

        it('skips fetching role catalogs when the user cannot edit or add members, since no sheet can open', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();

            expect(mockUseGroupApiRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupApplicationRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupApiProductRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupIntegrationRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupClusterRoles).toHaveBeenCalledWith({ enabled: false });
            expect(mockUseGroupExplorerRoles).toHaveBeenCalledWith({ enabled: false });
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
            mockUseGroupDetail.mockReturnValue({
                data: { ...GROUP, manageable: true, system_invitation: true },
                isLoading: false,
                isError: false,
            } as ReturnType<typeof useGroupDetail>);
            renderPage();

            expect(screen.queryByRole('button', { name: /Add members/i })).not.toBeNull();
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
});
