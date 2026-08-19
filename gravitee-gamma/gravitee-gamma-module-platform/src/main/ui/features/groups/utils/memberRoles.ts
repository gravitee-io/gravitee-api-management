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

import { isRoleLocked } from './groupPermissions';
import type { GroupMembershipRole } from '../types/group';

const PRIMARY_OWNER_MODE_USER = 'USER';

export type MemberRoleSelections = {
    apiRole: string;
    apiProductRole: string;
    applicationRole: string;
    integrationRole: string;
    clusterRole: string;
    explorerRole: string;
};

export type RoleField = keyof MemberRoleSelections;

export type MemberRoleLockFlags = {
    api: boolean;
    apiProduct: boolean;
    application: boolean;
    integration: boolean;
    cluster: boolean;
    explorer: boolean;
};

export function isPrimaryOwnerUnavailable(mode: string | undefined): boolean {
    return mode === undefined || mode.toUpperCase() === PRIMARY_OWNER_MODE_USER;
}

export function getMemberRoleLockFlags(
    locks: {
        lockApiRole: boolean;
        lockApiProductRole: boolean;
        lockApplicationRole: boolean;
    },
    canOverrideLocks: boolean,
): MemberRoleLockFlags {
    return {
        api: isRoleLocked(locks.lockApiRole, canOverrideLocks),
        apiProduct: isRoleLocked(locks.lockApiProductRole, canOverrideLocks),
        application: isRoleLocked(locks.lockApplicationRole, canOverrideLocks),
        integration: !canOverrideLocks,
        cluster: !canOverrideLocks,
        explorer: !canOverrideLocks,
    };
}

export function buildMembershipRoles(selections: MemberRoleSelections): GroupMembershipRole[] {
    const roles: GroupMembershipRole[] = [];
    if (selections.apiRole) roles.push({ scope: 'API', name: selections.apiRole });
    if (selections.apiProductRole) roles.push({ scope: 'API_PRODUCT', name: selections.apiProductRole });
    if (selections.applicationRole) roles.push({ scope: 'APPLICATION', name: selections.applicationRole });
    if (selections.integrationRole) roles.push({ scope: 'INTEGRATION', name: selections.integrationRole });
    if (selections.clusterRole) roles.push({ scope: 'CLUSTER', name: selections.clusterRole });
    if (selections.explorerRole) roles.push({ scope: 'EXPLORER', name: selections.explorerRole });
    return roles;
}
