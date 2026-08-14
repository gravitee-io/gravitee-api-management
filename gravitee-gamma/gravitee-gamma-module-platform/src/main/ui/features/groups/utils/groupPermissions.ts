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

export const ENVIRONMENT_GROUP_READ_PERMISSION = 'environment-group-r' as const;
export const ENVIRONMENT_GROUP_CREATE_PERMISSION = 'environment-group-c' as const;
export const ENVIRONMENT_GROUP_UPDATE_PERMISSION = 'environment-group-u' as const;
export const ENVIRONMENT_GROUP_DELETE_PERMISSION = 'environment-group-d' as const;

export const ORGANIZATION_SETTINGS_READ_PERMISSION = 'organization-settings-r' as const;

export function isPrimaryOwnerGroup(group: Pick<Group, 'primary_owner' | 'apiPrimaryOwner' | 'apiProductPrimaryOwner'>): boolean {
    return Boolean(group.primary_owner || group.apiPrimaryOwner || group.apiProductPrimaryOwner);
}

export function canInviteToGroup(group: Pick<Group, 'manageable' | 'system_invitation' | 'email_invitation'>): boolean {
    return Boolean(group.manageable) && Boolean(group.system_invitation || group.email_invitation);
}

export function isRoleLocked(locked: boolean, canOverrideLocks: boolean): boolean {
    return locked && !canOverrideLocks;
}
