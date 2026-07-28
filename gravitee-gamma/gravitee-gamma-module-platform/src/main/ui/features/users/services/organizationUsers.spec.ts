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
    createOrganizationUser,
    getOrganizationUser,
    getOrganizationUserGroups,
    listEnvironmentRoles,
    listIdentityProviders,
    listOrganizationEnvironments,
    listOrganizationRoles,
    listOrganizationUsers,
    processUserRegistration,
    updateOrganizationUserRoles,
} from './organizationUsers';
import { apimFetchJsonOrg, resolveOrganizationId } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
    resolveOrganizationId: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);
const mockResolveOrganizationId = jest.mocked(resolveOrganizationId);

describe('organizationUsers service', () => {
    beforeEach(() => {
        mockApimFetchJsonOrg.mockReset();
        mockResolveOrganizationId.mockReset();
        mockResolveOrganizationId.mockResolvedValue('DEFAULT');
    });

    it('lists organization users with pagination and the search query sent verbatim', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({ data: [], page: { total_elements: 0 } });

        await listOrganizationUsers({ query: 'jane@company.com', page: 2, size: 50 });

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/users?page=2&size=50&q=jane%40company.com');
    });

    it('omits the query parameter when the search term is blank', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({ data: [], page: { total_elements: 0 } });

        await listOrganizationUsers({ query: '   ', page: 1, size: 10 });

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/users?page=1&size=10');
    });

    it('creates an organization user via POST', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({ id: 'user-1' });

        await createOrganizationUser({
            firstname: 'Jane',
            lastname: 'Doe',
            email: 'jane@company.com',
            source: 'gravitee',
            sourceId: '',
            service: false,
        });

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/users', {
            method: 'POST',
            body: JSON.stringify({
                firstname: 'Jane',
                lastname: 'Doe',
                email: 'jane@company.com',
                source: 'gravitee',
                sourceId: '',
                service: false,
            }),
        });
    });

    it('loads identity providers from organization configuration', async () => {
        mockApimFetchJsonOrg.mockResolvedValue([{ id: 'ldap', name: 'LDAP' }]);

        await expect(listIdentityProviders()).resolves.toEqual([{ id: 'ldap', name: 'LDAP' }]);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/identities');
    });

    it('loads a single organization user by id', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({ id: 'user-1', email: 'jane@company.com' });

        await expect(getOrganizationUser('user-1')).resolves.toEqual({ id: 'user-1', email: 'jane@company.com' });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/users/user-1');
    });

    it('lists organization environments', async () => {
        mockApimFetchJsonOrg.mockResolvedValue([{ id: 'DEFAULT', name: 'Default' }]);

        await expect(listOrganizationEnvironments()).resolves.toEqual([{ id: 'DEFAULT', name: 'Default' }]);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/environments');
    });

    it('loads user group memberships', async () => {
        mockApimFetchJsonOrg.mockResolvedValue([{ id: 'group-1', name: 'Platform Admins' }]);

        await expect(getOrganizationUserGroups('user-1')).resolves.toEqual([{ id: 'group-1', name: 'Platform Admins' }]);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/users/user-1/groups');
    });

    it('processes pending user registration', async () => {
        mockApimFetchJsonOrg.mockResolvedValue(undefined);

        await processUserRegistration('user-1', true);

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/users/user-1/_process', {
            method: 'POST',
            body: JSON.stringify(true),
        });
    });

    it('loads organization and environment role catalogs', async () => {
        mockApimFetchJsonOrg.mockResolvedValueOnce([{ id: 'org-user', name: 'User' }]);
        await expect(listOrganizationRoles()).resolves.toEqual([{ id: 'org-user', name: 'User' }]);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ORGANIZATION/roles');

        mockApimFetchJsonOrg.mockResolvedValueOnce([{ id: 'env-user', name: 'API_USER' }]);
        await expect(listEnvironmentRoles()).resolves.toEqual([{ id: 'env-user', name: 'API_USER' }]);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ENVIRONMENT/roles');
    });

    it('updates user roles for an organization or environment reference', async () => {
        mockApimFetchJsonOrg.mockResolvedValue(undefined);

        await updateOrganizationUserRoles('user-1', {
            referenceType: 'ORGANIZATION',
            referenceId: 'DEFAULT',
            roles: ['org-user'],
        });

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/users/user-1/roles', {
            method: 'PUT',
            body: JSON.stringify({
                user: 'user-1',
                referenceType: 'ORGANIZATION',
                referenceId: 'DEFAULT',
                roles: ['org-user'],
            }),
        });
    });
});
