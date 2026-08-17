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

import { SharedPolicyGroupsPage } from './SharedPolicyGroupsPage';
import {
    useCreateSharedPolicyGroup,
    useDeleteSharedPolicyGroup,
    useUpdateSharedPolicyGroup,
} from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import { useSharedPolicyGroupsPaged } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import { getSharedPolicyGroup } from '../features/shared-policy-groups/services/sharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: jest.fn(() => ({ id: 'env-1' })),
}));
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: jest.fn(),
}));
jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroups');
jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations');
jest.mock('../features/shared-policy-groups/services/sharedPolicyGroups', () => ({
    getSharedPolicyGroup: jest.fn(),
}));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

// Stub the table and create-menu to avoid DataTable/DropdownMenu Radix pointer-event complexity in
// jsdom (mirrors GroupsPage.spec.tsx) — both are exercised directly in their own component specs.
jest.mock('../features/shared-policy-groups/components/SharedPolicyGroupsTable', () => ({
    SharedPolicyGroupsTable: ({
        sharedPolicyGroups,
        isFirstUse,
        canDelete,
        onView,
        onEdit,
        onDelete,
        onCreateSharedPolicyGroup,
        onSortingChange,
    }: {
        sharedPolicyGroups: SharedPolicyGroup[];
        isFirstUse: boolean;
        canDelete: boolean;
        onView: (s: SharedPolicyGroup) => void;
        onEdit: (s: SharedPolicyGroup) => void;
        onDelete: (s: SharedPolicyGroup) => void;
        onCreateSharedPolicyGroup?: () => void;
        onSortingChange: (sorting: { id: string; desc: boolean }[]) => void;
    }) =>
        isFirstUse ? (
            <div>
                <p>No Shared Policy Groups</p>
                {onCreateSharedPolicyGroup && (
                    <button type="button" onClick={onCreateSharedPolicyGroup}>
                        Add Shared Policy Group
                    </button>
                )}
            </div>
        ) : (
            <div>
                <button type="button" onClick={() => onSortingChange([{ id: 'name', desc: true }])}>
                    Sort by Name
                </button>
                {sharedPolicyGroups.map(s => (
                    <div key={s.id} data-testid={`row-${s.id}`}>
                        <span>{s.name}</span>
                        <button type="button" onClick={() => onView(s)}>
                            View {s.name}
                        </button>
                        <button type="button" onClick={() => onEdit(s)}>
                            Edit {s.name}
                        </button>
                        {canDelete && (
                            <button type="button" onClick={() => onDelete(s)}>
                                Delete {s.name}
                            </button>
                        )}
                    </div>
                ))}
            </div>
        ),
}));
const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseNavigate = jest.mocked(useNavigate);
const mockUseSharedPolicyGroupsPaged = jest.mocked(useSharedPolicyGroupsPaged);
const mockUseCreateSharedPolicyGroup = jest.mocked(useCreateSharedPolicyGroup);
const mockUseUpdateSharedPolicyGroup = jest.mocked(useUpdateSharedPolicyGroup);
const mockUseDeleteSharedPolicyGroup = jest.mocked(useDeleteSharedPolicyGroup);
const mockGetSharedPolicyGroup = jest.mocked(getSharedPolicyGroup);

const SAMPLE_SPGS: SharedPolicyGroup[] = [
    { id: 'spg-1', name: 'Auth Bundle', apiType: 'PROXY', phase: 'REQUEST' },
    { id: 'spg-2', name: 'Rate Limit Bundle', apiType: 'MESSAGE', phase: 'PUBLISH' },
];

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function makeSpgsResult(data: SharedPolicyGroup[] = SAMPLE_SPGS, totalCount = data.length) {
    return {
        data: { data, pagination: { page: 1, perPage: 25, pageCount: 1, pageItemsCount: data.length, totalCount } },
        isLoading: false,
        isError: false,
    } as ReturnType<typeof useSharedPolicyGroupsPaged>;
}

function renderPage() {
    return render(<SharedPolicyGroupsPage />);
}

describe('SharedPolicyGroupsPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseNavigate.mockReturnValue(jest.fn());
        mockUseSharedPolicyGroupsPaged.mockReturnValue(makeSpgsResult());
        mockUseCreateSharedPolicyGroup.mockReturnValue(makeMutation());
        mockUseUpdateSharedPolicyGroup.mockReturnValue(makeMutation());
        mockUseDeleteSharedPolicyGroup.mockReturnValue(makeMutation());
        mockGetSharedPolicyGroup.mockResolvedValue({ ...SAMPLE_SPGS[0], steps: [{ name: 'policy-1' }] });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('page header', () => {
        it('renders the page title', () => {
            renderPage();
            expect(screen.queryByRole('heading', { name: 'Shared Policy Groups' })).not.toBeNull();
        });

        it('shows the Add Shared Policy Group button when user can create', () => {
            renderPage();
            expect(screen.queryByRole('button', { name: 'Add Shared Policy Group' })).not.toBeNull();
        });

        it('hides the Add Shared Policy Group button when user lacks create permission', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();
            expect(screen.queryByRole('button', { name: 'Add Shared Policy Group' })).toBeNull();
        });
    });

    describe('list', () => {
        it('renders shared policy groups from the API', () => {
            renderPage();
            expect(screen.queryByText('Auth Bundle')).not.toBeNull();
            expect(screen.queryByText('Rate Limit Bundle')).not.toBeNull();
        });

        it('shows an error message when the query fails', () => {
            mockUseSharedPolicyGroupsPaged.mockReturnValue({
                data: undefined,
                isLoading: false,
                isError: true,
            } as ReturnType<typeof useSharedPolicyGroupsPaged>);
            renderPage();
            expect(screen.queryByText('Failed to load Shared Policy Groups. Please refresh and try again.')).not.toBeNull();
        });

        it('hides the header menu during first-use and keeps the empty-state CTA', () => {
            mockUseSharedPolicyGroupsPaged.mockReturnValue(makeSpgsResult([], 0));
            renderPage();

            expect(screen.queryByText('No Shared Policy Groups')).not.toBeNull();
            expect(screen.getAllByRole('button', { name: 'Add Shared Policy Group' })).toHaveLength(1);
        });

        it('re-fetches with the classic Console sortBy value and resets to page 1 when sorting changes', () => {
            renderPage();
            mockUseSharedPolicyGroupsPaged.mockClear();

            fireEvent.click(screen.getByRole('button', { name: 'Sort by Name' }));

            expect(mockUseSharedPolicyGroupsPaged).toHaveBeenCalledWith(expect.objectContaining({ page: 1, sortBy: '-name' }));
        });
    });

    describe('create flow', () => {
        it('creates a Proxy shared policy group (the default API type), shows a success toast, and navigates to its detail page', async () => {
            const createMutateAsync = jest.fn().mockResolvedValue({ id: 'new-spg-id', name: 'My SPG' });
            const navigate = jest.fn();
            mockUseNavigate.mockReturnValue(navigate);
            mockUseCreateSharedPolicyGroup.mockReturnValue(makeMutation(createMutateAsync));

            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Add Shared Policy Group' }));
            expect(screen.queryByRole('heading', { name: 'Add Policy Group' })).not.toBeNull();

            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My SPG' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create' }));

            await waitFor(() => {
                expect(createMutateAsync).toHaveBeenCalledWith({
                    name: 'My SPG',
                    description: undefined,
                    prerequisiteMessage: undefined,
                    apiType: 'PROXY',
                    phase: 'REQUEST',
                });
            });
            expect(notify.success).toHaveBeenCalledWith('Shared Policy Group created');
            expect(navigate).toHaveBeenCalledWith('new-spg-id');
        });

        it('switches to Message-specific phases when the Message API type is selected in the sheet', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Add Shared Policy Group' }));

            fireEvent.click(screen.getByRole('radio', { name: 'Message' }));

            expect(screen.queryByRole('radio', { name: 'Publish' })).not.toBeNull();
            expect(screen.queryByRole('radio', { name: 'Subscribe' })).not.toBeNull();
        });

        it('creates a Message shared policy group when Message is selected', async () => {
            const createMutateAsync = jest.fn().mockResolvedValue({ id: 'new-spg-id', name: 'My SPG' });
            mockUseCreateSharedPolicyGroup.mockReturnValue(makeMutation(createMutateAsync));

            renderPage();
            fireEvent.click(screen.getByRole('button', { name: 'Add Shared Policy Group' }));
            fireEvent.click(screen.getByRole('radio', { name: 'Message' }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My SPG' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create' }));

            await waitFor(() => {
                expect(createMutateAsync).toHaveBeenCalledWith(expect.objectContaining({ apiType: 'MESSAGE' }));
            });
        });

        it('shows an error toast when create fails', async () => {
            const error = new Error('create failed');
            mockUseCreateSharedPolicyGroup.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Add Shared Policy Group' }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My SPG' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Error during Shared Policy Group creation!'));
        });
    });

    describe('delete flow', () => {
        it('deletes the selected shared policy group and shows a success toast', async () => {
            const deleteMutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseDeleteSharedPolicyGroup.mockReturnValue(makeMutation(deleteMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete Auth Bundle' }));
            expect(screen.queryByRole('heading', { name: 'Remove Shared Policy Group' })).not.toBeNull();

            fireEvent.click(screen.getByRole('button', { name: 'Remove' }));

            await waitFor(() => expect(deleteMutateAsync).toHaveBeenCalledWith('spg-1'));
            expect(notify.success).toHaveBeenCalledWith('Shared Policy Group removed');
        });

        it('shows an error toast when delete fails', async () => {
            const error = new Error('delete failed');
            mockUseDeleteSharedPolicyGroup.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete Auth Bundle' }));
            fireEvent.click(screen.getByRole('button', { name: 'Remove' }));

            await waitFor(() =>
                expect(notify.error).toHaveBeenCalledWith(error, 'An error occurred while removing the Shared Policy Group'),
            );
        });
    });

    describe('edit flow', () => {
        it('fetches the full shared policy group, updates metadata, preserves steps, and shows a success toast', async () => {
            const updateMutateAsync = jest.fn().mockResolvedValue({ id: 'spg-1', name: 'Updated Auth' });
            mockUseUpdateSharedPolicyGroup.mockReturnValue(makeMutation(updateMutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit Auth Bundle' }));

            await waitFor(() => expect(mockGetSharedPolicyGroup).toHaveBeenCalledWith('env-1', 'spg-1'));
            expect(await screen.findByRole('heading', { name: 'Edit Policy Group' })).not.toBeNull();

            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Updated Auth' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() => {
                expect(updateMutateAsync).toHaveBeenCalledWith({
                    id: 'spg-1',
                    payload: {
                        name: 'Updated Auth',
                        description: undefined,
                        prerequisiteMessage: undefined,
                        steps: [{ name: 'policy-1' }],
                    },
                });
            });
            expect(notify.success).toHaveBeenCalledWith('Shared Policy Group updated');
        });

        it('shows an error toast when update fails', async () => {
            const error = new Error('update failed');
            mockUseUpdateSharedPolicyGroup.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit Auth Bundle' }));
            await screen.findByRole('heading', { name: 'Edit Policy Group' });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Error during Shared Policy Group update!'));
        });
    });

    describe('view navigation', () => {
        it('navigates to the shared policy group detail page', () => {
            const navigate = jest.fn();
            mockUseNavigate.mockReturnValue(navigate);
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'View Auth Bundle' }));
            expect(navigate).toHaveBeenCalledWith('spg-1');
        });
    });
});
