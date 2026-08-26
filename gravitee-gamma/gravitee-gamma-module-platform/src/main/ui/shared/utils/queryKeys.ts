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

export const environmentPermissionKeys = {
    all: ['environment-permissions'] as const,
    detail: (envId: string) => [...environmentPermissionKeys.all, envId] as const,
} as const;

export const currentUserKeys = {
    all: ['current-user'] as const,
    detail: () => [...currentUserKeys.all, 'detail'] as const,
} as const;

export const organizationGroupKeys = {
    all: ['organization-groups'] as const,
    list: () => [...organizationGroupKeys.all, 'list'] as const,
} as const;

/** The installed policy plugins: one catalog for every Policy Studio host, so one cache entry. */
export const policyPluginKeys = {
    all: ['policy-plugins'] as const,
    list: () => [...policyPluginKeys.all, 'list'] as const,
} as const;
