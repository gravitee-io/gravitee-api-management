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

/** Mirrors classic Console's shared-policy-groups.component.ts permission checks. */
export const ENVIRONMENT_SHARED_POLICY_GROUP_READ_PERMISSION = 'environment-shared_policy_group-r' as const;
export const ENVIRONMENT_SHARED_POLICY_GROUP_CREATE_PERMISSION = 'environment-shared_policy_group-c' as const;
export const ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION = 'environment-shared_policy_group-u' as const;
export const ENVIRONMENT_SHARED_POLICY_GROUP_DELETE_PERMISSION = 'environment-shared_policy_group-d' as const;

/** Mirrors classic Console's `element.isKubernetesOrigin` row flag (shared-policy-groups.component.ts). */
export function isKubernetesOrigin(sharedPolicyGroup: Pick<SharedPolicyGroup, 'originContext'>): boolean {
    return sharedPolicyGroup.originContext?.origin === 'KUBERNETES';
}
