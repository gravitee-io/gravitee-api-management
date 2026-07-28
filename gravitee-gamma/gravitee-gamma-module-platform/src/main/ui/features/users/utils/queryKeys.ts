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
export const organizationUserKeys = {
    all: ['organization-users'] as const,
    list: (query: string, page: number, size: number) => [...organizationUserKeys.all, 'list', query, page, size] as const,
    identityProviders: () => [...organizationUserKeys.all, 'identity-providers'] as const,
    detail: (userId: string) => [...organizationUserKeys.all, 'detail', userId] as const,
    groups: (userId: string) => [...organizationUserKeys.all, 'groups', userId] as const,
    environments: () => [...organizationUserKeys.all, 'environments'] as const,
    organizationRoles: () => [...organizationUserKeys.all, 'organization-roles'] as const,
    environmentRoles: () => [...organizationUserKeys.all, 'environment-roles'] as const,
};
