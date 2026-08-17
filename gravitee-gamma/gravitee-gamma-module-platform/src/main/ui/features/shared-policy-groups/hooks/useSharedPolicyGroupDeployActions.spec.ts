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
import { renderHook, act } from '@testing-library/react';

import { useSharedPolicyGroupDeployActions } from './useSharedPolicyGroupDeployActions';
import { useDeploySharedPolicyGroup, useUndeploySharedPolicyGroup } from './useSharedPolicyGroupMutations';
import { notify } from '../../../shared/notify';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('./useSharedPolicyGroupMutations');
jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseDeploy = jest.mocked(useDeploySharedPolicyGroup);
const mockUseUndeploy = jest.mocked(useUndeploySharedPolicyGroup);

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    apiType: 'PROXY',
    phase: 'REQUEST',
    lifecycleState: 'UNDEPLOYED',
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn(), isPending = false): any {
    return { mutateAsync, isPending };
}

describe('useSharedPolicyGroupDeployActions', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseDeploy.mockReturnValue(makeMutation());
        mockUseUndeploy.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('hides actions without update permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        const { result } = renderHook(() => useSharedPolicyGroupDeployActions(SPG));
        expect(result.current.visible).toBe(false);
    });

    it('hides actions for Kubernetes-origin groups', () => {
        const { result } = renderHook(() =>
            useSharedPolicyGroupDeployActions({ ...SPG, originContext: { origin: 'KUBERNETES' } }),
        );
        expect(result.current.visible).toBe(false);
    });

    it('disables Deploy when DEPLOYED and Undeploy when UNDEPLOYED', () => {
        const { result: undeployed } = renderHook(() => useSharedPolicyGroupDeployActions(SPG));
        expect(undeployed.current.deployDisabled).toBe(false);
        expect(undeployed.current.undeployDisabled).toBe(true);

        const { result: deployed } = renderHook(() =>
            useSharedPolicyGroupDeployActions({ ...SPG, lifecycleState: 'DEPLOYED' }),
        );
        expect(deployed.current.deployDisabled).toBe(true);
        expect(deployed.current.undeployDisabled).toBe(false);
    });

    it('disables Deploy when there are unsaved changes', () => {
        const { result } = renderHook(() => useSharedPolicyGroupDeployActions(SPG, true));
        expect(result.current.deployDisabled).toBe(true);
    });

    it('deploys and shows a success toast', async () => {
        const mutateAsync = jest.fn().mockResolvedValue({ ...SPG, lifecycleState: 'DEPLOYED' });
        mockUseDeploy.mockReturnValue(makeMutation(mutateAsync));

        const { result } = renderHook(() => useSharedPolicyGroupDeployActions(SPG));
        await act(async () => {
            await result.current.onDeploy();
        });

        expect(mutateAsync).toHaveBeenCalledWith('spg-1');
        expect(notify.success).toHaveBeenCalledWith('Shared Policy Group deployed successfully');
    });

    it('undeploys and shows a success toast', async () => {
        const mutateAsync = jest.fn().mockResolvedValue({ ...SPG, lifecycleState: 'UNDEPLOYED' });
        mockUseUndeploy.mockReturnValue(makeMutation(mutateAsync));

        const { result } = renderHook(() =>
            useSharedPolicyGroupDeployActions({ ...SPG, lifecycleState: 'DEPLOYED' }),
        );
        await act(async () => {
            await result.current.onUndeploy();
        });

        expect(mutateAsync).toHaveBeenCalledWith('spg-1');
        expect(notify.success).toHaveBeenCalledWith('Shared Policy Group undeployed successfully');
    });

    it('shows an error toast when deploy fails', async () => {
        const mutateAsync = jest.fn().mockRejectedValue(new Error('boom'));
        mockUseDeploy.mockReturnValue(makeMutation(mutateAsync));

        const { result } = renderHook(() => useSharedPolicyGroupDeployActions(SPG));
        await act(async () => {
            await result.current.onDeploy();
        });

        expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Error during Shared Policy Group deployment!');
    });
});
