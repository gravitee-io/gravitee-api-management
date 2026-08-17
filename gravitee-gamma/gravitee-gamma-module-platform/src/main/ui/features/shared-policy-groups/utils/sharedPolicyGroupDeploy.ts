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

import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';
import { isKubernetesOrigin } from './sharedPolicyGroupPermissions';

type LifecycleState = SharedPolicyGroup['lifecycleState'];

export function canShowDeployActions(
    sharedPolicyGroup: Pick<SharedPolicyGroup, 'originContext'>,
    canUpdate: boolean,
): boolean {
    return canUpdate && !isKubernetesOrigin(sharedPolicyGroup);
}

export function isDeployDisabled(lifecycleState: LifecycleState, hasUnsavedChanges = false): boolean {
    return lifecycleState === 'DEPLOYED' || hasUnsavedChanges;
}

export function isUndeployDisabled(lifecycleState: LifecycleState): boolean {
    return lifecycleState === 'UNDEPLOYED';
}
