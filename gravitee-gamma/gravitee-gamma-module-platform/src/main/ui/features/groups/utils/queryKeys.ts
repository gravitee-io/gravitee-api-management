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

import type { GroupMembershipType } from '../types/group';

export const groupKeys = {
    all: ['environment-groups'] as const,
    list: (envId: string, query: string, page: number, size: number) => [...groupKeys.all, 'list', envId, query, page, size] as const,
    detail: (envId: string, groupId: string) => [...groupKeys.all, 'detail', envId, groupId] as const,
    members: (envId: string, groupId: string) => [...groupKeys.all, 'members', envId, groupId] as const,
    memberships: (envId: string, groupId: string, type: GroupMembershipType) =>
        [...groupKeys.all, 'memberships', envId, groupId, type] as const,
    roles: (scope: 'API' | 'APPLICATION' | 'API_PRODUCT') => [...groupKeys.all, 'roles', scope] as const,
    apiRoles: () => groupKeys.roles('API'),
    applicationRoles: () => groupKeys.roles('APPLICATION'),
    apiProductRoles: () => groupKeys.roles('API_PRODUCT'),
} as const;
