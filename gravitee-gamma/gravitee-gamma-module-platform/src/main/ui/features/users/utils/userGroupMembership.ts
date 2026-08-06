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
import type { AddUserGroupMembershipPayload, OrganizationUserGroup } from '../types/user';

export interface AddUserGroupFormState {
    groupId: string;
    isGroupAdmin: boolean;
    apiRole?: string;
    apiProductRole?: string;
    applicationRole?: string;
    integrationRole?: string;
}

export const EMPTY_ADD_USER_GROUP_FORM: AddUserGroupFormState = {
    groupId: '',
    isGroupAdmin: false,
};

export function hasAtLeastOneGroupMembershipRole(payload: Omit<AddUserGroupMembershipPayload, 'groupId'>): boolean {
    return (
        payload.isGroupAdmin ||
        Boolean(payload.apiRole) ||
        Boolean(payload.apiProductRole) ||
        Boolean(payload.applicationRole) ||
        Boolean(payload.integrationRole)
    );
}

export function hasAtLeastOneGroupRole(form: AddUserGroupFormState): boolean {
    return hasAtLeastOneGroupMembershipRole(form);
}

export function organizationUserGroupToMembershipPayload(group: OrganizationUserGroup): AddUserGroupMembershipPayload {
    return {
        groupId: group.id,
        isGroupAdmin: group.roles?.GROUP === 'ADMIN',
        apiRole: group.roles?.API,
        apiProductRole: group.roles?.API_PRODUCT,
        applicationRole: group.roles?.APPLICATION,
        integrationRole: group.roles?.INTEGRATION,
    };
}

export function mergeGroupMembershipPayload(
    base: AddUserGroupMembershipPayload,
    patch: Partial<Omit<AddUserGroupMembershipPayload, 'groupId'>>,
): AddUserGroupMembershipPayload {
    return { ...base, ...patch };
}

export function isApiRolePrimaryOwnerLocked(group: OrganizationUserGroup): boolean {
    return group.roles?.API === 'PRIMARY_OWNER' && Boolean(group.isApiPrimaryOwner);
}

export function isAddUserGroupFormValid(form: AddUserGroupFormState): boolean {
    return Boolean(form.groupId) && hasAtLeastOneGroupRole(form);
}
