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

import type { Group } from '../types/group';

/** Mirrors classic settings-routing.module.ts groups route guard + groups.component.ts permission checks. */
export const ENVIRONMENT_GROUP_READ_PERMISSION = 'environment-group-r' as const;
export const ENVIRONMENT_GROUP_CREATE_PERMISSION = 'environment-group-c' as const;
export const ENVIRONMENT_GROUP_UPDATE_PERMISSION = 'environment-group-u' as const;
export const ENVIRONMENT_GROUP_DELETE_PERMISSION = 'environment-group-d' as const;

/** Gates the "Require a group on applications" org-wide toggle on the groups list — mirrors classic
 *  groups.component.ts's `hasAnyMatching(['organization-settings-r'])` check (read-only gate; the save
 *  itself relies on the backend's own CREATE/UPDATE/DELETE check on this same permission, same as classic). */
export const ORGANIZATION_SETTINGS_READ_PERMISSION = 'organization-settings-r' as const;

/** Org-wide, cross-environment group list (GET .../organizations/{orgId}/groups) — mirrors backend
 *  OrganizationGroupsResource's `RolePermission.ORGANIZATION_TAG` READ requirement. */
export const ORGANIZATION_TAG_READ_PERMISSION = 'organization-tag-r' as const;

/**
 * Mirrors classic Console's `apiPrimaryOwner || apiProductPrimaryOwner` badge/delete-guard condition
 * (groups.component.html / groups.component.ts) — `primary_owner` alone misses groups that are only
 * an API Product primary owner, since the backend only sets `primary_owner` from API-scope PO state.
 */
export function isPrimaryOwnerGroup(group: Pick<Group, 'primary_owner' | 'apiPrimaryOwner' | 'apiProductPrimaryOwner'>): boolean {
    return Boolean(group.primary_owner || group.apiPrimaryOwner || group.apiProductPrimaryOwner);
}
