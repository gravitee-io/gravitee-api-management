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
import { sortOrganizationUsers } from './userListSort';
import type { OrganizationUser } from '../types/user';

const USERS: OrganizationUser[] = [
    { id: '2', displayName: 'Zara', status: 'PENDING', source: 'ldap', lastConnectionAt: 100 },
    { id: '1', displayName: 'Alice', status: 'ACTIVE', source: 'gravitee', lastConnectionAt: 200 },
];

describe('sortOrganizationUsers', () => {
    it('sorts users by display name ascending', () => {
        expect(sortOrganizationUsers(USERS, [{ id: 'user', desc: false }]).map(user => user.displayName)).toEqual(['Alice', 'Zara']);
    });

    it('sorts users by last login descending', () => {
        expect(sortOrganizationUsers(USERS, [{ id: 'lastActivity', desc: true }]).map(user => user.id)).toEqual(['1', '2']);
    });

    it('returns the original order when sorting is inactive', () => {
        expect(sortOrganizationUsers(USERS, [])).toEqual(USERS);
    });
});
