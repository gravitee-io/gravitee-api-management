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

import { useQuery } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';

import { SharedPolicyGroupStudioPage } from './SharedPolicyGroupStudioPage';
import {
    useDeploySharedPolicyGroup,
    useUpdateSharedPolicyGroup,
} from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import { useHasEnvironmentPermission } from '../shared/hooks/useEnvironmentPermissions';
import { notify } from '../shared/notify';

jest.mock('../shared/hooks/useEnvironmentPermissions');
jest.mock('@tanstack/react-query', () => ({
    useQuery: jest.fn(),
}));
jest.mock('@gravitee/graphene-policy-studio', () => ({
    getProtocolType: () => 'HTTP_PROXY',
}));
jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));
jest.mock('../features/shared-policy-groups/components/SharedPolicyGroupPolicyStudio', () => ({
    SharedPolicyGroupPolicyStudio: ({
        readOnly,
        onSave,
        onDeploy,
    }: {
        readOnly: boolean;
        onSave: (steps: Array<{ policy: string }>) => Promise<SharedPolicyGroup>;
        onDeploy: () => Promise<void>;
    }) => (
        <div>
            <span>{readOnly ? 'Read only studio' : 'Editable studio'}</span>
            <button type="button" onClick={() => void onSave([{ policy: 'jwt' }])}>
                Save studio
            </button>
            <button type="button" onClick={() => void onDeploy()}>
                Deploy studio
            </button>
        </div>
    ),
}));

const mockUseHasEnvironmentPermission = jest.mocked(useHasEnvironmentPermission);
const mockUseQuery = jest.mocked(useQuery);
const mockUseUpdateSharedPolicyGroup = jest.mocked(useUpdateSharedPolicyGroup);
const mockUseDeploySharedPolicyGroup = jest.mocked(useDeploySharedPolicyGroup);

const BASE: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    apiType: 'PROXY',
    phase: 'REQUEST',
};

function renderPage(sharedPolicyGroup: SharedPolicyGroup) {
    return render(
        <MemoryRouter initialEntries={['/spg-1/studio']}>
            <Routes>
                <Route path=":sharedPolicyGroupId" element={<Outlet context={sharedPolicyGroup} />}>
                    <Route path="studio" element={<SharedPolicyGroupStudioPage />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupStudioPage', () => {
    const updateAsync = jest.fn();
    const deployAsync = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockUseHasEnvironmentPermission.mockReturnValue(true);
        mockUseQuery.mockReturnValue({
            data: [{ id: 'jwt', name: 'JWT' }],
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useQuery>);
        mockUseUpdateSharedPolicyGroup.mockReturnValue({
            mutateAsync: updateAsync,
        } as unknown as ReturnType<typeof useUpdateSharedPolicyGroup>);
        mockUseDeploySharedPolicyGroup.mockReturnValue({
            mutateAsync: deployAsync,
            isPending: false,
        } as unknown as ReturnType<typeof useDeploySharedPolicyGroup>);
        updateAsync.mockResolvedValue(BASE);
        deployAsync.mockResolvedValue(BASE);
    });

    it('loads an editable Policy Studio and saves steps through the Shared Policy Group update endpoint', async () => {
        renderPage(BASE);

        expect(screen.getByText('Editable studio')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Save studio' }));

        await waitFor(() =>
            expect(updateAsync).toHaveBeenCalledWith({
                id: 'spg-1',
                payload: {
                    name: 'Auth Bundle',
                    description: undefined,
                    prerequisiteMessage: undefined,
                    steps: [{ policy: 'jwt' }],
                },
            }),
        );
        expect(notify.success).toHaveBeenCalledWith('Shared Policy Group updated');
    });

    it('deploys through the lifecycle endpoint', async () => {
        renderPage(BASE);

        fireEvent.click(screen.getByRole('button', { name: 'Deploy studio' }));
        await waitFor(() => expect(deployAsync).toHaveBeenCalledWith('spg-1'));
        expect(notify.success).toHaveBeenCalledWith('Shared Policy Group deployed successfully');
    });

    it('reports deploy failures', async () => {
        deployAsync.mockRejectedValueOnce(new Error('Deploy failed'));
        renderPage(BASE);

        fireEvent.click(screen.getByRole('button', { name: 'Deploy studio' }));
        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Error during Shared Policy Group deployment!'));
    });

    it.each([
        ['missing update permission', false, undefined],
        ['Kubernetes origin', true, { origin: 'KUBERNETES' as const }],
    ])('makes the Policy Studio read-only for %s', (_, canUpdate, originContext) => {
        mockUseHasEnvironmentPermission.mockReturnValue(canUpdate);

        renderPage({ ...BASE, originContext });

        expect(screen.getByText('Read only studio')).not.toBeNull();
    });

    it('shows loading and error states for the policy catalog', () => {
        mockUseQuery.mockReturnValueOnce({ isLoading: true, isError: false } as ReturnType<typeof useQuery>);
        const { rerender } = renderPage(BASE);
        expect(document.querySelector('[data-slot="skeleton"]')).not.toBeNull();

        mockUseQuery.mockReturnValue({ isLoading: false, isError: true } as ReturnType<typeof useQuery>);
        rerender(
            <MemoryRouter initialEntries={['/spg-1/studio']}>
                <Routes>
                    <Route path=":sharedPolicyGroupId" element={<Outlet context={BASE} />}>
                        <Route path="studio" element={<SharedPolicyGroupStudioPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
        expect(screen.getByText('Failed to load the policy catalog. Please refresh and try again.')).not.toBeNull();
    });
});
