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

import { useDeploySharedPolicyGroup, useUndeploySharedPolicyGroup } from './useSharedPolicyGroupMutations';
import { notify } from '../../../shared/notify';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';
import {
    canShowDeployActions,
    isDeployDisabled,
    isUndeployDisabled,
} from '../utils/sharedPolicyGroupDeploy';
import { ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION } from '../utils/sharedPolicyGroupPermissions';

export function useSharedPolicyGroupDeployActions(
    sharedPolicyGroup: SharedPolicyGroup,
    hasUnsavedChanges = false,
) {
    const canUpdate = useHasPermission({ anyOf: [ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION] });
    const deployMutation = useDeploySharedPolicyGroup();
    const undeployMutation = useUndeploySharedPolicyGroup();

    const visible = canShowDeployActions(sharedPolicyGroup, canUpdate);
    const deployDisabled =
        isDeployDisabled(sharedPolicyGroup.lifecycleState, hasUnsavedChanges) || deployMutation.isPending;
    const undeployDisabled = isUndeployDisabled(sharedPolicyGroup.lifecycleState) || undeployMutation.isPending;

    async function onDeploy() {
        try {
            await deployMutation.mutateAsync(sharedPolicyGroup.id);
            notify.success('Shared Policy Group deployed successfully');
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group deployment!');
        }
    }

    async function onUndeploy() {
        try {
            await undeployMutation.mutateAsync(sharedPolicyGroup.id);
            notify.success('Shared Policy Group undeployed successfully');
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group undeployment!');
        }
    }

    return {
        visible,
        deployDisabled,
        undeployDisabled,
        isDeploying: deployMutation.isPending,
        isUndeploying: undeployMutation.isPending,
        onDeploy,
        onUndeploy,
    };
}
