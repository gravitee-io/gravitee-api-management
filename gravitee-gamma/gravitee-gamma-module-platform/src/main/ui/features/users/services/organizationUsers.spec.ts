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
import { createOrganizationUser, listIdentityProviders, listOrganizationUsers } from './organizationUsers';
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

describe('organizationUsers service', () => {
    beforeEach(() => {
        mockApimFetchJsonOrg.mockReset();
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
});
