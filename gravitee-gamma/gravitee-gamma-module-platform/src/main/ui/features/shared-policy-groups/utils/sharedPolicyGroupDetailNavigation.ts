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

/** Child routes under `shared-policy-groups/:sharedPolicyGroupId`. */
export const SHARED_POLICY_GROUP_DETAIL_TABS = [
    { path: 'overview', label: 'Overview' },
    { path: 'studio', label: 'Studio' },
    { path: 'history', label: 'History' },
] as const;

export type SharedPolicyGroupDetailTabPath = (typeof SHARED_POLICY_GROUP_DETAIL_TABS)[number]['path'];

export const SHARED_POLICY_GROUP_DEFAULT_TAB: SharedPolicyGroupDetailTabPath = 'studio';

export function isSharedPolicyGroupDetailTabPath(value: string): value is SharedPolicyGroupDetailTabPath {
    return SHARED_POLICY_GROUP_DETAIL_TABS.some(tab => tab.path === value);
}

export function sharedPolicyGroupDetailHref(
    sharedPolicyGroupId: string,
    tab: SharedPolicyGroupDetailTabPath = SHARED_POLICY_GROUP_DEFAULT_TAB,
): string {
    return `${sharedPolicyGroupId}/${tab}`;
}
