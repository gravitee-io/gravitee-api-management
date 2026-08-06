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
import {
    hasAtLeastOneGroupMembershipRole,
    hasAtLeastOneGroupRole,
    isAddUserGroupFormValid,
    isApiRolePrimaryOwnerLocked,
    mergeGroupMembershipPayload,
    organizationUserGroupToMembershipPayload,
} from './userGroupMembership';

describe('userGroupMembership utils', () => {
    it('requires a group and at least one role', () => {
        expect(isAddUserGroupFormValid({ groupId: '', isGroupAdmin: false })).toBe(false);
        expect(isAddUserGroupFormValid({ groupId: 'group-1', isGroupAdmin: false })).toBe(false);
        expect(isAddUserGroupFormValid({ groupId: 'group-1', isGroupAdmin: true })).toBe(true);
        expect(isAddUserGroupFormValid({ groupId: 'group-1', isGroupAdmin: false, applicationRole: 'USER' })).toBe(true);
    });

    it('detects when any role field is set', () => {
        expect(hasAtLeastOneGroupRole({ groupId: '', isGroupAdmin: false, integrationRole: 'USER' })).toBe(true);
    });

    it('maps group membership rows to update payloads', () => {
        const payload = organizationUserGroupToMembershipPayload({
            id: 'group-1',
            roles: { GROUP: 'ADMIN', API: 'USER', API_PRODUCT: 'OWNER' },
        });

        expect(
            mergeGroupMembershipPayload(payload, {
                applicationRole: 'USER',
            }),
        ).toEqual({
            groupId: 'group-1',
            isGroupAdmin: true,
            apiRole: 'USER',
            apiProductRole: 'OWNER',
            applicationRole: 'USER',
            integrationRole: undefined,
        });
    });

    it('locks primary owner API roles when the user owns the API through the group', () => {
        expect(
            isApiRolePrimaryOwnerLocked({
                id: 'group-1',
                roles: { API: 'PRIMARY_OWNER' },
                isApiPrimaryOwner: true,
            }),
        ).toBe(true);
    });

    it('does not lock API roles when primary owner flag is absent', () => {
        expect(
            isApiRolePrimaryOwnerLocked({
                id: 'group-1',
                roles: { API: 'PRIMARY_OWNER' },
            }),
        ).toBe(false);
    });

    it('requires at least one membership role in update payloads', () => {
        expect(hasAtLeastOneGroupMembershipRole({ isGroupAdmin: false })).toBe(false);
        expect(hasAtLeastOneGroupMembershipRole({ isGroupAdmin: true })).toBe(true);
        expect(hasAtLeastOneGroupMembershipRole({ isGroupAdmin: false, apiRole: 'USER' })).toBe(true);
    });
});
