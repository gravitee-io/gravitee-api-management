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

export const integrationKeys = {
    all: ['environment-integrations'] as const,
    list: (envId: string, page: number, perPage: number) => [...integrationKeys.all, 'list', envId, page, perPage] as const,
    detail: (envId: string, integrationId: string) => [...integrationKeys.all, 'detail', envId, integrationId] as const,
    permissions: (envId: string, integrationId: string) => [...integrationKeys.all, 'permissions', envId, integrationId] as const,
    federatedApis: (envId: string, integrationId: string) => [...integrationKeys.all, 'federated-apis', envId, integrationId] as const,
    members: (envId: string, integrationId: string) => [...integrationKeys.all, 'members', envId, integrationId] as const,
    groupMembership: (envId: string, integrationId: string, groupId: string) =>
        [...integrationKeys.all, 'group-membership', envId, integrationId, groupId] as const,
    groups: (envId: string, groupIdsKey: string) => [...integrationKeys.all, 'groups', envId, groupIdsKey] as const,
    environmentGroups: (envId: string) => [...integrationKeys.all, 'environment-groups', envId] as const,
    roles: () => [...integrationKeys.all, 'roles'] as const,
} as const;
