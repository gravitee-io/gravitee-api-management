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
    formatGroupScopeRole,
    formatResourceVisibility,
    groupMembershipStatusLabel,
    isGroupAdmin,
    isPublicResourceVisibility,
} from './userGroupDisplay';

describe('userGroupDisplay', () => {
    it('formats group admin and scope roles', () => {
        const roles = {
            GROUP: 'ADMIN',
            API: 'API_USER',
            API_PRODUCT: 'USER',
            APPLICATION: 'USER',
        };

        expect(isGroupAdmin(roles)).toBe(true);
        expect(formatGroupScopeRole(roles, 'GROUP')).toBe('ADMIN');
        expect(formatGroupScopeRole(roles, 'API')).toBe('API_USER');
        expect(formatGroupScopeRole(roles, 'API_PRODUCT')).toBe('USER');
        expect(formatGroupScopeRole(roles, 'INTEGRATION')).toBeUndefined();
    });

    it('describes group membership status labels', () => {
        expect(groupMembershipStatusLabel({ id: 'group-1', roles: { GROUP: 'ADMIN', API: 'USER' } })).toBe('Group Admin');
        expect(groupMembershipStatusLabel({ id: 'group-2', roles: { API: 'USER' } })).toBe('Member');
        expect(groupMembershipStatusLabel({ id: 'group-3', roles: {} })).toBeUndefined();
    });

    it('formats resource visibility labels and detects public resources', () => {
        expect(formatResourceVisibility('PUBLIC')).toBe('Public');
        expect(formatResourceVisibility('PRIVATE')).toBe('Private');
        expect(isPublicResourceVisibility('PUBLIC')).toBe(true);
        expect(isPublicResourceVisibility('PRIVATE')).toBe(false);
    });
});
