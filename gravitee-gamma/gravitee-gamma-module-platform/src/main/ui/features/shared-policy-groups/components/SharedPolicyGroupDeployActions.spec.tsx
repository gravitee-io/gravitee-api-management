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

import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { SharedPolicyGroupDeployActions } from './SharedPolicyGroupDeployActions';
import { useSharedPolicyGroupDeployActions } from '../hooks/useSharedPolicyGroupDeployActions';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

jest.mock('../hooks/useSharedPolicyGroupDeployActions');

const mockUseDeployActions = jest.mocked(useSharedPolicyGroupDeployActions);

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    apiType: 'PROXY',
    phase: 'REQUEST',
    lifecycleState: 'UNDEPLOYED',
};

function mockActions(
    overrides: Partial<ReturnType<typeof useSharedPolicyGroupDeployActions>> = {},
): ReturnType<typeof useSharedPolicyGroupDeployActions> {
    return {
        visible: true,
        deployDisabled: false,
        undeployDisabled: true,
        isDeploying: false,
        isUndeploying: false,
        onDeploy: jest.fn(),
        onUndeploy: jest.fn(),
        ...overrides,
    };
}

describe('SharedPolicyGroupDeployActions', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders nothing when actions are not visible', () => {
        mockUseDeployActions.mockReturnValue(mockActions({ visible: false }));
        const { container } = render(<SharedPolicyGroupDeployActions sharedPolicyGroup={SPG} />);
        expect(container.firstChild).toBeNull();
    });

    it('renders Deploy and Undeploy and forwards clicks', async () => {
        const onDeploy = jest.fn();
        const onUndeploy = jest.fn();
        mockUseDeployActions.mockReturnValue(
            mockActions({
                deployDisabled: false,
                undeployDisabled: false,
                onDeploy,
                onUndeploy,
            }),
        );

        render(<SharedPolicyGroupDeployActions sharedPolicyGroup={{ ...SPG, lifecycleState: 'PENDING' }} />);

        fireEvent.click(screen.getByTestId('shared-policy-group-deploy'));
        fireEvent.click(screen.getByTestId('shared-policy-group-undeploy'));

        await waitFor(() => {
            expect(onDeploy).toHaveBeenCalledTimes(1);
            expect(onUndeploy).toHaveBeenCalledTimes(1);
        });
    });

    it('disables Deploy and Undeploy according to the hook', () => {
        mockUseDeployActions.mockReturnValue(
            mockActions({
                deployDisabled: true,
                undeployDisabled: true,
            }),
        );

        render(<SharedPolicyGroupDeployActions sharedPolicyGroup={{ ...SPG, lifecycleState: 'DEPLOYED' }} />);

        expect(screen.getByTestId('shared-policy-group-deploy')).toHaveProperty('disabled', true);
        expect(screen.getByTestId('shared-policy-group-undeploy')).toHaveProperty('disabled', true);
    });

    it('shows pending labels while mutations run', () => {
        mockUseDeployActions.mockReturnValue(
            mockActions({
                isDeploying: true,
                isUndeploying: true,
                deployDisabled: true,
                undeployDisabled: true,
            }),
        );

        render(<SharedPolicyGroupDeployActions sharedPolicyGroup={SPG} />);

        expect(screen.getByText('Deploying…')).not.toBeNull();
        expect(screen.getByText('Undeploying…')).not.toBeNull();
    });
});
