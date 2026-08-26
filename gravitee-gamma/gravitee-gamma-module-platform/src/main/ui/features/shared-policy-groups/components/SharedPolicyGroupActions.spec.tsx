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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';

import { SharedPolicyGroupActions } from './SharedPolicyGroupActions';
import type { SharedPolicyGroupEditFormValues } from './SharedPolicyGroupEditSheet';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import {
    useDeleteSharedPolicyGroup,
    useDeploySharedPolicyGroup,
    useUndeploySharedPolicyGroup,
    useUpdateSharedPolicyGroup,
} from '../hooks/useSharedPolicyGroupMutations';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

jest.mock('../../../shared/hooks/useEnvironmentPermissions');
jest.mock('../hooks/useSharedPolicyGroupMutations');
jest.mock('../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));
jest.mock('./SharedPolicyGroupEditSheet', () => ({
    SharedPolicyGroupEditSheet: ({ onSubmit }: { onSubmit: (values: SharedPolicyGroupEditFormValues) => Promise<void> | void }) => (
        <div>
            <h2>Edit Shared Policy Group</h2>
            <button type="button" onClick={() => void onSubmit({ name: 'Auth Bundle', description: '', prerequisiteMessage: '' })}>
                Save
            </button>
        </div>
    ),
}));

const mockUseHasEnvironmentPermission = jest.mocked(useHasEnvironmentPermission);
const mockUseUpdateSharedPolicyGroup = jest.mocked(useUpdateSharedPolicyGroup);
const mockUseDeploySharedPolicyGroup = jest.mocked(useDeploySharedPolicyGroup);
const mockUseUndeploySharedPolicyGroup = jest.mocked(useUndeploySharedPolicyGroup);
const mockUseDeleteSharedPolicyGroup = jest.mocked(useDeleteSharedPolicyGroup);

const SHARED_POLICY_GROUP: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    apiType: 'PROXY',
    phase: 'REQUEST',
    lifecycleState: 'DEPLOYED',
};

function renderActions(sharedPolicyGroup: SharedPolicyGroup = SHARED_POLICY_GROUP) {
    return render(
        <MemoryRouter initialEntries={['/shared-policy-groups/spg-1/studio']}>
            <Routes>
                <Route
                    path="/shared-policy-groups/:sharedPolicyGroupId"
                    element={
                        <>
                            <SharedPolicyGroupActions sharedPolicyGroup={sharedPolicyGroup} listHref="/shared-policy-groups" />
                            <Outlet />
                        </>
                    }
                >
                    <Route path="studio" element={<div>Studio destination</div>} />
                    <Route path="history" element={<div>Version history destination</div>} />
                </Route>
                <Route path="/shared-policy-groups" element={<div>Shared Policy Groups list</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupActions', () => {
    const updateAsync = jest.fn();
    const deployAsync = jest.fn();
    const undeployAsync = jest.fn();
    const deleteAsync = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockUseHasEnvironmentPermission.mockReturnValue(true);
        mockUseUpdateSharedPolicyGroup.mockReturnValue({ mutateAsync: updateAsync, isPending: false } as never);
        mockUseDeploySharedPolicyGroup.mockReturnValue({ mutateAsync: deployAsync, isPending: false } as never);
        mockUseUndeploySharedPolicyGroup.mockReturnValue({ mutateAsync: undeployAsync, isPending: false } as never);
        mockUseDeleteSharedPolicyGroup.mockReturnValue({ mutateAsync: deleteAsync, isPending: false } as never);
        deployAsync.mockResolvedValue(SHARED_POLICY_GROUP);
        undeployAsync.mockResolvedValue(SHARED_POLICY_GROUP);
        deleteAsync.mockResolvedValue(undefined);
    });

    it('offers edit, undeploy, version history, and delete from the overflow menu', async () => {
        const user = userEvent.setup();
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));

        expect(screen.getByRole('menuitem', { name: 'Edit' })).not.toBeNull();
        expect(screen.getByRole('menuitem', { name: 'Undeploy' })).not.toBeNull();
        expect(screen.getByRole('menuitem', { name: 'Version History' })).not.toBeNull();
        expect(screen.getByRole('menuitem', { name: 'Delete' })).not.toBeNull();
    });

    it('undeploys from the overflow menu', async () => {
        const user = userEvent.setup();
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Undeploy' }));

        await waitFor(() => expect(undeployAsync).toHaveBeenCalledWith('spg-1'));
        expect(notify.success).toHaveBeenCalledWith('Shared Policy Group undeployed successfully');
    });

    it('opens the edit sheet from the overflow menu', async () => {
        const user = userEvent.setup();
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Edit' }));

        expect(screen.getByRole('heading', { name: 'Edit Shared Policy Group' })).not.toBeNull();
    });

    it('preserves the existing policy steps when submitting a metadata-only edit', async () => {
        const user = userEvent.setup();
        const steps = [{ policy: 'rate-limit', name: 'Rate Limit', enabled: true, configuration: {} }];
        renderActions({ ...SHARED_POLICY_GROUP, steps });

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Edit' }));
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() =>
            expect(updateAsync).toHaveBeenCalledWith({
                id: 'spg-1',
                payload: expect.objectContaining({ steps }),
            }),
        );
    });

    it('reports edit failures and keeps the edit sheet open', async () => {
        const user = userEvent.setup();
        const error = new Error('Update failed');
        updateAsync.mockRejectedValueOnce(error);
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Edit' }));
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Error during Shared Policy Group update!'));
        expect(screen.getByRole('heading', { name: 'Edit Shared Policy Group' })).not.toBeNull();
    });

    it('reports undeploy failures', async () => {
        const user = userEvent.setup();
        undeployAsync.mockRejectedValueOnce(new Error('Undeploy failed'));
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Undeploy' }));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Error during Shared Policy Group undeployment!'));
    });

    it('offers deploy instead of undeploy once the group is undeployed', async () => {
        const user = userEvent.setup();
        renderActions({ ...SHARED_POLICY_GROUP, lifecycleState: 'UNDEPLOYED' });

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));

        expect(screen.getByRole('menuitem', { name: 'Deploy' })).not.toBeNull();
        expect(screen.queryByRole('menuitem', { name: 'Undeploy' })).toBeNull();
    });

    it('offers both Deploy and Undeploy while the group has undeployed edits (PENDING)', async () => {
        const user = userEvent.setup();
        renderActions({ ...SHARED_POLICY_GROUP, lifecycleState: 'PENDING' });

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));

        expect(screen.getByRole('menuitem', { name: 'Deploy' })).not.toBeNull();
        expect(screen.getByRole('menuitem', { name: 'Undeploy' })).not.toBeNull();
    });

    it('deploys from the overflow menu', async () => {
        const user = userEvent.setup();
        renderActions({ ...SHARED_POLICY_GROUP, lifecycleState: 'UNDEPLOYED' });

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Deploy' }));

        await waitFor(() => expect(deployAsync).toHaveBeenCalledWith('spg-1'));
        expect(notify.success).toHaveBeenCalledWith('Shared Policy Group deployed successfully');
    });

    it('reports deploy failures', async () => {
        const user = userEvent.setup();
        deployAsync.mockRejectedValueOnce(new Error('Deploy failed'));
        renderActions({ ...SHARED_POLICY_GROUP, lifecycleState: 'UNDEPLOYED' });

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Deploy' }));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Error during Shared Policy Group deployment!'));
    });

    it('navigates to version history', async () => {
        const user = userEvent.setup();
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Version History' }));

        expect(await screen.findByText('Version history destination')).not.toBeNull();
    });

    it('deletes after confirmation and returns to the list', async () => {
        const user = userEvent.setup();
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
        await user.click(screen.getByRole('button', { name: 'Remove' }));

        await waitFor(() => expect(deleteAsync).toHaveBeenCalledWith('spg-1'));
        expect(await screen.findByText('Shared Policy Groups list')).not.toBeNull();
    });

    it('offers only the actions the user is allowed to perform', async () => {
        const user = userEvent.setup();
        mockUseHasEnvironmentPermission.mockImplementation(anyOf => anyOf.includes('environment-shared_policy_group-u'));
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));

        expect(screen.getByRole('menuitem', { name: 'Edit' })).not.toBeNull();
        expect(screen.queryByRole('menuitem', { name: 'Delete' })).toBeNull();
    });

    it('requires update permission before offering detail-page deletion', async () => {
        const user = userEvent.setup();
        mockUseHasEnvironmentPermission.mockImplementation(anyOf => anyOf.includes('environment-shared_policy_group-d'));
        renderActions();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));

        expect(screen.queryByRole('menuitem', { name: 'Edit' })).toBeNull();
        expect(screen.queryByRole('menuitem', { name: 'Delete' })).toBeNull();
    });

    it('keeps Kubernetes-origin groups read-only while retaining version history', async () => {
        const user = userEvent.setup();
        renderActions({ ...SHARED_POLICY_GROUP, lifecycleState: 'UNDEPLOYED', originContext: { origin: 'KUBERNETES' } });

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));

        expect(screen.queryByRole('menuitem', { name: 'Edit' })).toBeNull();
        expect(screen.queryByRole('menuitem', { name: 'Deploy' })).toBeNull();
        expect(screen.queryByRole('menuitem', { name: 'Undeploy' })).toBeNull();
        expect(screen.getByRole('menuitem', { name: 'Version History' })).not.toBeNull();
        expect(screen.queryByRole('menuitem', { name: 'Delete' })).toBeNull();
    });
});
