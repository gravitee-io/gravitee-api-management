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
jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
}));

import {
    createRole,
    createRoleMembership,
    deleteRole,
    deleteRoleMembership,
    getPermissionsByScopes,
    getRole,
    listRoleMemberships,
    listRolesByScope,
    updateRole,
} from './roles';
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import type { Role, RoleScope } from '../types/role';

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

describe('roles service', () => {
    beforeEach(() => {
        mockApimFetchJsonOrg.mockReset();
    });

    it('lists roles for a scope', async () => {
        mockApimFetchJsonOrg.mockResolvedValue([{ name: 'ADMIN', scope: 'ORGANIZATION' }]);

        await expect(listRolesByScope('ORGANIZATION')).resolves.toEqual([{ name: 'ADMIN', scope: 'ORGANIZATION' }]);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ORGANIZATION/roles');
    });

    it('gets the permissions-by-scope catalog', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({ ORGANIZATION: ['ROLE'] });

        await expect(getPermissionsByScopes()).resolves.toEqual({ ORGANIZATION: ['ROLE'] });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes');
    });

    it('gets a single role', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({ name: 'USER', scope: 'ENVIRONMENT' });

        await expect(getRole('ENVIRONMENT', 'USER')).resolves.toEqual({ name: 'USER', scope: 'ENVIRONMENT' });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ENVIRONMENT/roles/USER');
    });

    it('upper-cases a lower-case scope before building the URL, mirroring Classic', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({ name: 'USER', scope: 'ENVIRONMENT' });

        await getRole('environment' as RoleScope, 'USER');
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ENVIRONMENT/roles/USER');

        mockApimFetchJsonOrg.mockResolvedValue([]);
        await listRolesByScope('environment' as RoleScope);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ENVIRONMENT/roles');
    });

    // Mirrors Classic's RoleService.list/get, which both map `scope: role.scope.toUpperCase()` — isRoleReadOnly
    // compares the server's own `role.scope` against 'ORGANIZATION', so a lowercase value would break that check.
    it('normalizes a lowercase scope in the list response', async () => {
        mockApimFetchJsonOrg.mockResolvedValue([{ name: 'ADMIN', scope: 'organization' }]);

        await expect(listRolesByScope('ORGANIZATION')).resolves.toEqual([{ name: 'ADMIN', scope: 'ORGANIZATION' }]);
    });

    it('normalizes a lowercase scope in the single-role response', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({ name: 'USER', scope: 'environment' });

        await expect(getRole('ENVIRONMENT', 'USER')).resolves.toEqual({ name: 'USER', scope: 'ENVIRONMENT' });
    });

    it('creates a role via POST against its scope', async () => {
        const role: Role = { name: 'CUSTOM', scope: 'API', permissions: { DEFINITION: ['C', 'R'] } };
        mockApimFetchJsonOrg.mockResolvedValue(role);

        await createRole(role);

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/API/roles', {
            method: 'POST',
            body: JSON.stringify(role),
        });
    });

    it('updates a role via PUT against its scope and name', async () => {
        const role: Role = { name: 'CUSTOM', scope: 'API', permissions: {} };
        mockApimFetchJsonOrg.mockResolvedValue(role);

        await updateRole(role);

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/API/roles/CUSTOM', {
            method: 'PUT',
            body: JSON.stringify(role),
        });
    });

    it('deletes a role', async () => {
        mockApimFetchJsonOrg.mockResolvedValue(undefined);

        await deleteRole('API', 'CUSTOM');

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/API/roles/CUSTOM', { method: 'DELETE' });
    });

    it('lists memberships for a role', async () => {
        mockApimFetchJsonOrg.mockResolvedValue([{ id: 'user-1', displayName: 'Jane Doe' }]);

        await expect(listRoleMemberships('ORGANIZATION', 'ADMIN')).resolves.toEqual([{ id: 'user-1', displayName: 'Jane Doe' }]);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ORGANIZATION/roles/ADMIN/users');
    });

    it('creates a role membership', async () => {
        mockApimFetchJsonOrg.mockResolvedValue(undefined);

        await createRoleMembership('ORGANIZATION', 'ADMIN', { id: 'user-1', reference: 'USER' });

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ORGANIZATION/roles/ADMIN/users', {
            method: 'POST',
            body: JSON.stringify({ id: 'user-1', reference: 'USER' }),
        });
    });

    it('deletes a role membership', async () => {
        mockApimFetchJsonOrg.mockResolvedValue(undefined);

        await deleteRoleMembership('ORGANIZATION', 'ADMIN', 'user-1');

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ORGANIZATION/roles/ADMIN/users/user-1', {
            method: 'DELETE',
        });
    });
});
