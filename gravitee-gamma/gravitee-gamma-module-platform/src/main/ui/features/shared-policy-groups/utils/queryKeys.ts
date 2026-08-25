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

export const sharedPolicyGroupKeys = {
    all: ['environment-shared-policy-groups'] as const,
    list: (envId: string, query: string, page: number, perPage: number, sortBy: string | undefined) =>
        [...sharedPolicyGroupKeys.all, 'list', envId, query, page, perPage, sortBy] as const,
    detail: (envId: string, sharedPolicyGroupId: string) => [...sharedPolicyGroupKeys.all, 'detail', envId, sharedPolicyGroupId] as const,
    histories: (envId: string, sharedPolicyGroupId: string, page: number, perPage: number, sortBy: string | undefined) =>
        [...sharedPolicyGroupKeys.all, 'histories', envId, sharedPolicyGroupId, page, perPage, sortBy] as const,
    policies: () => [...sharedPolicyGroupKeys.all, 'policies'] as const,
} as const;
