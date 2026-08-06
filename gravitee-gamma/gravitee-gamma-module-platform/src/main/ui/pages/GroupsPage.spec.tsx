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
import { useNavigate } from 'react-router-dom';

import { GroupsPage } from './GroupsPage';
import { useCreateGroup, useDeleteGroup, useUpdateGroup } from '../features/groups/hooks/useGroupMutations';
import { useGroupApiProductRoles, useGroupApiRoles, useGroupApplicationRoles } from '../features/groups/hooks/useGroupRoles';
import { useGroupsPaged } from '../features/groups/hooks/useGroups';
import type { Group } from '../features/groups/types/group';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: jest.fn(),
}));
jest.mock('../features/groups/hooks/useGroups');
jest.mock('../features/groups/hooks/useGroupRoles');
jest.mock('../features/groups/hooks/useGroupMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

// Stub GroupsTable to avoid DataTable/DropdownMenu Radix pointer-event complexity in jsdom.
// Exposes Edit/Delete buttons and the first-use CTA wired directly to the page's callbacks.
jest.mock('../features/groups/components/GroupsTable', () => ({
    GroupsTable: ({
        groups,
        isFirstUse,
        canEdit,
        canDelete,
        onEdit,
        onDelete,
        onCreateGroup,
    }: {
        groups: Group[];
        isFirstUse: boolean;
        canEdit: boolean;
        canDelete: boolean;
        onEdit: (g: Group) => void;
        onDelete: (g: Group) => void;
        onCreateGroup?: () => void;
    }) =>
        isFirstUse ? (
            <div>
                <p>No groups</p>
                {onCreateGroup && (
                    <button type="button" onClick={onCreateGroup}>
                        Create Group
                    </button>
                )}
            </div>
        ) : (
            <div>
                {groups.map(g => (
                    <div key={g.id} data-testid={`row-${g.id}`}>
                        <span>{g.name}</span>
                        {canEdit && (
                            <button type="button" onClick={() => onEdit(g)}>
                                Edit {g.name}
                            </button>
                        )}
                        {canDelete && (
                            <button type="button" onClick={() => onDelete(g)}>
                                Delete {g.name}
                            </button>
                        )}
                    </div>
                ))}
            </div>
        ),
}));

// Radix Switch (rendered inside the real GroupSheet) measures its thumb via ResizeObserver, which jsdom lacks.
beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseNavigate = jest.mocked(useNavigate);
const mockUseGroupsPaged = jest.mocked(useGroupsPaged);
const mockUseGroupApiRoles = jest.mocked(useGroupApiRoles);
const mockUseGroupApplicationRoles = jest.mocked(useGroupApplicationRoles);
const mockUseGroupApiProductRoles = jest.mocked(useGroupApiProductRoles);
const mockUseCreateGroup = jest.mocked(useCreateGroup);
const mockUseUpdateGroup = jest.mocked(useUpdateGroup);
const mockUseDeleteGroup = jest.mocked(useDeleteGroup);

const SAMPLE_GROUPS: Group[] = [
    { id: 'group-1', name: 'Support Team', created_at: 1700000000000 },
    { id: 'group-2', name: 'Dev Team', created_at: 1700100000000 },
];

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function makeGroupsResult(groups: Group[] = SAMPLE_GROUPS, totalElements = groups.length) {
    return {
        data: { data: groups, page: { current: 1, size: 10, per_page: 10, total_pages: 1, total_elements: totalElements } },
        isLoading: false,
        isError: false,
    } as ReturnType<typeof useGroupsPaged>;
}

function renderPage() {
    return render(<GroupsPage />);
}

describe('GroupsPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseNavigate.mockReturnValue(jest.fn());
        mockUseGroupsPaged.mockReturnValue(makeGroupsResult());
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
        mockUseCreateGroup.mockReturnValue(makeMutation());
        mockUseUpdateGroup.mockReturnValue(makeMutation());
        mockUseDeleteGroup.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('page header', () => {
        it('renders the page title', () => {
            renderPage();
            expect(screen.queryByRole('heading', { name: 'Groups' })).not.toBeNull();
        });

        it('shows the Create Group button when user can create', () => {
            renderPage();
            expect(screen.queryByRole('button', { name: /Create Group/i })).not.toBeNull();
        });

        it('hides the Create Group button when user lacks create permission', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();
            expect(screen.queryByRole('button', { name: /Create Group/i })).toBeNull();
        });
    });

    describe('list', () => {
        it('renders groups from the API', () => {
            renderPage();
            expect(screen.queryByText('Support Team')).not.toBeNull();
            expect(screen.queryByText('Dev Team')).not.toBeNull();
        });

        it('shows an error message when the query fails', () => {
            mockUseGroupsPaged.mockReturnValue({ data: undefined, isLoading: false, isError: true } as ReturnType<typeof useGroupsPaged>);
            renderPage();
            expect(screen.queryByText('Failed to load groups. Please refresh and try again.')).not.toBeNull();
        });

        it('hides the header Create Group button during first-use and keeps the empty-state CTA', () => {
            mockUseGroupsPaged.mockReturnValue(makeGroupsResult([], 0));
            renderPage();

            expect(screen.queryByText('No groups')).not.toBeNull();
            expect(screen.getAllByRole('button', { name: /Create Group/i })).toHaveLength(1);
        });
    });

    describe('create flow', () => {
        it('creates the group, then follows up with a role update, shows a success toast, and navigates to the group detail page', async () => {
            const createMutateAsync = jest.fn().mockResolvedValue({
                id: 'new-group-id',
                name: 'My Group',
                lock_api_role: false,
                event_rules: [],
                lock_api_product_role: false,
                lock_application_role: false,
                roles: {},
            });
            const updateMutateAsync = jest.fn().mockResolvedValue({});
            const navigate = jest.fn();
            mockUseNavigate.mockReturnValue(navigate);
            mockUseCreateGroup.mockReturnValue(makeMutation(createMutateAsync));
            mockUseUpdateGroup.mockReturnValue(makeMutation(updateMutateAsync));

            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Create Group/i }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Group' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create group' }));

            await waitFor(() => {
                expect(createMutateAsync).toHaveBeenCalledWith({
                    name: 'My Group',
                    lock_api_role: false,
                    lock_api_product_role: false,
                    lock_application_role: false,
                    event_rules: [],
                    max_invitation: null,
                    system_invitation: false,
                    email_invitation: false,
                    disable_membership_notifications: false,
                });
            });
            await waitFor(() => {
                expect(updateMutateAsync).toHaveBeenCalledWith({
                    groupId: 'new-group-id',
                    data: expect.objectContaining({ roles: { API: 'USER', APPLICATION: 'USER', API_PRODUCT: 'USER' } }),
                });
            });
            expect(notify.success).toHaveBeenCalledWith('Group created successfully');
            expect(navigate).toHaveBeenCalledWith('new-group-id');
        });

        it('shows an error toast when create fails', async () => {
            const error = new Error('create failed');
            mockUseCreateGroup.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: /Create Group/i }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Group' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create group' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to create group'));
        });

        it('closes the sheet and warns (not errors) when the group was created but the role update fails', async () => {
            const createMutateAsync = jest.fn().mockResolvedValue({
                id: 'new-group-id',
                name: 'My Group',
                lock_api_role: false,
                event_rules: [],
                lock_api_product_role: false,
                lock_application_role: false,
                roles: {},
            });
            const updateMutateAsync = jest.fn().mockRejectedValue(new Error('role update failed'));
            mockUseCreateGroup.mockReturnValue(makeMutation(createMutateAsync));
            mockUseUpdateGroup.mockReturnValue(makeMutation(updateMutateAsync));

            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Create Group/i }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Group' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create group' }));

            await waitFor(() => expect(updateMutateAsync).toHaveBeenCalled());
            expect(notify.warning).toHaveBeenCalledWith(
                'Group "My Group" was created, but default roles could not be applied. Edit the group to set them.',
            );
            expect(notify.error).not.toHaveBeenCalled();
            expect(notify.success).not.toHaveBeenCalled();
            await waitFor(() => expect(screen.queryByRole('heading', { name: 'Create group' })).toBeNull());
        });
    });

    describe('edit flow', () => {
        it('updates the group and shows a success toast', async () => {
            const updateMutateAsync = jest.fn().mockResolvedValue({});
            mockUseUpdateGroup.mockReturnValue(makeMutation(updateMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit Support Team' }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Support Team v2' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() =>
                expect(updateMutateAsync).toHaveBeenCalledWith(
                    expect.objectContaining({ groupId: 'group-1', data: expect.objectContaining({ name: 'Support Team v2' }) }),
                ),
            );
            expect(notify.success).toHaveBeenCalledWith('Group updated successfully');
        });

        it('shows an error toast when update fails', async () => {
            const error = new Error('update failed');
            mockUseUpdateGroup.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit Support Team' }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Support Team v2' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to update group'));
        });
    });

    describe('delete flow', () => {
        it('deletes the selected group and shows a success toast', async () => {
            const deleteMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseDeleteGroup.mockReturnValue(makeMutation(deleteMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete Support Team' }));
            expect(screen.queryByRole('heading', { name: 'Delete Group' })).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => expect(deleteMutateAsync).toHaveBeenCalledWith('group-1'));
            expect(notify.success).toHaveBeenCalledWith('Group deleted successfully');
        });
    });
});
