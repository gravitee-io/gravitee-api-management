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
import { organizationUserSearchKeys } from '../../../shared/utils/queryKeys';
import type { RoleScope } from '../types/role';

export const roleKeys = {
    all: ['roles'] as const,
    listByScope: (scope: RoleScope) => [...roleKeys.all, 'list', scope] as const,
    detail: (scope: RoleScope, name: string) => [...roleKeys.all, 'detail', scope, name] as const,
    permissionsByScopes: () => [...roleKeys.all, 'permissions-by-scopes'] as const,
    memberships: (scope: RoleScope, name: string) => [...roleKeys.all, 'memberships', scope, name] as const,
    /** Same cache namespace groupKeys.userSearch uses — both features search the same organization user directory. */
    userSearch: organizationUserSearchKeys.search,
} as const;
