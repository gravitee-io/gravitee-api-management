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

import { SharedPolicyGroupDetailPage } from './SharedPolicyGroupDetailPage';
import { useUpdateSharedPolicyGroup } from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import { useSharedPolicyGroupDetail } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroups');
jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseSharedPolicyGroupDetail = jest.mocked(useSharedPolicyGroupDetail);
const mockUseUpdateSharedPolicyGroup = jest.mocked(useUpdateSharedPolicyGroup);

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth policies',
    prerequisiteMessage: 'Requires the "auth-cache" resource',
    lifecycleState: 'DEPLOYED',
    apiType: 'PROXY',
    phase: 'REQUEST',
    steps: [{ name: 'jwt' }],
    updatedAt: '2024-01-01T00:00:00.000Z',
    deployedAt: '2024-01-02T00:00:00.000Z',
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/spg-1/overview']}>
            <Routes>
                <Route path=":sharedPolicyGroupId/overview" element={<SharedPolicyGroupDetailPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupDetailPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseUpdateSharedPolicyGroup.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders overview details: API type, phase, and prerequisite', () => {
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: SPG,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();

        expect(screen.getByTestId('shared-policy-group-overview')).not.toBeNull();
        expect(screen.getByText('Proxy')).not.toBeNull();
        expect(screen.getByText('Request')).not.toBeNull();
        expect(screen.getByText('Requires the "auth-cache" resource')).not.toBeNull();
    });

    it('renders nothing while loading (layout owns the skeleton)', () => {
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();
        expect(screen.queryByTestId('shared-policy-group-overview')).toBeNull();
    });

    it('shows Edit when the user can update and opens the edit sheet', async () => {
        const updateMutateAsync = jest.fn().mockResolvedValue(SPG);
        mockUseUpdateSharedPolicyGroup.mockReturnValue(makeMutation(updateMutateAsync));
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: SPG,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();
        fireEvent.click(screen.getByRole('button', { name: 'Edit' }));
        expect(await screen.findByRole('heading', { name: 'Edit Policy Group' })).not.toBeNull();

        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Renamed Bundle' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => {
            expect(updateMutateAsync).toHaveBeenCalledWith({
                id: 'spg-1',
                payload: {
                    name: 'Renamed Bundle',
                    description: 'Reusable auth policies',
                    prerequisiteMessage: 'Requires the "auth-cache" resource',
                    steps: [{ name: 'jwt' }],
                },
            });
        });
        expect(notify.success).toHaveBeenCalledWith('Shared Policy Group updated');
    });

    it('hides Edit when the user lacks update permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: SPG,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();
        expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
    });

    it('hides Edit for a Kubernetes-origin shared policy group', () => {
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: { ...SPG, originContext: { origin: 'KUBERNETES' } },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();
        expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull();
    });
});
