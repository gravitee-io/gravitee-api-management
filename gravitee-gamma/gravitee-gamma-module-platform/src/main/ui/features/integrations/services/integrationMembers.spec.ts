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
    addIntegrationMember,
    listIntegrationMembers,
    listIntegrationRoles,
    removeIntegrationMember,
    updateIntegrationMemberRole,
} from './integrationMembers';
import { apimFetchJsonOrg, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { IntegrationMember } from '../types/integrationMembers';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);
const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('integration members service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('requests up to 100 members of the integration for the environment and returns them in response order', async () => {
        const members: IntegrationMember[] = [
            { id: 'user-2', displayName: 'Charlie', roles: [{ name: 'USER', scope: 'INTEGRATION' }] },
            { id: 'user-1', displayName: 'Bob', roles: [{ name: 'OWNER', scope: 'INTEGRATION' }] },
        ];
        mockApimFetchJsonV2.mockResolvedValueOnce({ data: members });

        const result = await listIntegrationMembers('env-1', 'a/b c');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/a%2Fb%20c/members?page=1&perPage=100');
        expect(result).toEqual(members);
    });

    it('returns no members when the response has no data', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({});

        const result = await listIntegrationMembers('env-1', 'int-1');

        expect(result).toEqual([]);
    });

    it('posts the new member to the integration members of the environment', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({});

        await addIntegrationMember('env-1', 'a/b c', { userId: 'user-1', externalReference: 'ref-1', roleName: 'USER' });

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/a%2Fb%20c/members', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: 'user-1', externalReference: 'ref-1', roleName: 'USER' }),
        });
    });

    it("puts the member's new role to that member of the integration of the environment", async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({});

        await updateIntegrationMemberRole('env-1', 'a/b c', 'user/1', 'OWNER');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/a%2Fb%20c/members/user%2F1', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ memberId: 'user/1', roleName: 'OWNER' }),
        });
    });

    it('deletes that member of the integration of the environment', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce(undefined);

        await removeIntegrationMember('env-1', 'a/b c', 'user/1');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/a%2Fb%20c/members/user%2F1', { method: 'DELETE' });
    });

    it('lists the Integration-scoped roles of the organization', async () => {
        const roles = [{ name: 'PRIMARY_OWNER' }, { name: 'OWNER' }, { name: 'USER' }];
        mockApimFetchJsonOrg.mockResolvedValueOnce(roles);

        const result = await listIntegrationRoles();

        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/INTEGRATION/roles');
        expect(result).toEqual(roles);
    });
});
