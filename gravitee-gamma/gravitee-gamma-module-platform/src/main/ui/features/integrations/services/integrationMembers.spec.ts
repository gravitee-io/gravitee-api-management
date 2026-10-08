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

import { listIntegrationMembers } from './integrationMembers';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { IntegrationMember } from '../types/integrationMembers';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

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
});
