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

import { getIntegrationGroupMembership } from './integrationGroupMembers';
import { ApimApiError, apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
    ApimApiError: jest.requireActual('../../../shared/api/apimClient').ApimApiError,
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

const GROUP_PERMISSIONS_PATH = '/groups/a%2Fb/permissions';
const GROUP_MEMBERS_PATH = '/groups/a%2Fb/members?page=1&perPage=100';

function givenResponses(groupPermissions: Record<string, string | string[]>, membersResponse: () => Promise<unknown>) {
    mockApimFetchJsonV2.mockImplementation(async (_environmentId: string, path: string) =>
        path === GROUP_PERMISSIONS_PATH ? groupPermissions : membersResponse(),
    );
}

describe('integration group membership service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it("returns the group's members with their roles by scope when the viewer may read the group's members", async () => {
        givenResponses({ MEMBER: 'R' }, async () => ({
            data: [{ id: 'user-1', displayName: 'Alice', roles: [{ name: 'OWNER', scope: 'INTEGRATION' }] }],
        }));

        const membership = await getIntegrationGroupMembership('env-1', 'a/b');

        expect(mockApimFetchJsonV2.mock.calls.map(([environmentId, path]) => [environmentId, path])).toEqual([
            ['env-1', GROUP_PERMISSIONS_PATH],
            ['env-1', GROUP_MEMBERS_PATH],
        ]);
        expect(membership).toEqual({
            canViewMembers: true,
            members: [{ id: 'user-1', displayName: 'Alice', roles: { INTEGRATION: 'OWNER' } }],
        });
    });

    it("reports the members as not viewable, without requesting them, when the viewer's group permissions lack member read", async () => {
        givenResponses({ MEMBER: ['C', 'U', 'D'], DEFINITION: 'R' }, async () => ({ data: [] }));

        const membership = await getIntegrationGroupMembership('env-1', 'a/b');

        expect(membership).toEqual({ canViewMembers: false });
        expect(mockApimFetchJsonV2).not.toHaveBeenCalledWith('env-1', GROUP_MEMBERS_PATH);
    });

    it('reports the members as not viewable when the group members request is forbidden', async () => {
        givenResponses({ MEMBER: 'R' }, () => Promise.reject(new ApimApiError(403, 'Forbidden')));

        const membership = await getIntegrationGroupMembership('env-1', 'a/b');

        expect(membership).toEqual({ canViewMembers: false });
    });

    it('fails with the error of a group members request that fails for a reason other than a missing permission', async () => {
        const serverError = new ApimApiError(500, 'Internal Server Error');
        givenResponses({ MEMBER: 'R' }, () => Promise.reject(serverError));

        await expect(getIntegrationGroupMembership('env-1', 'a/b')).rejects.toBe(serverError);
    });

    it("fails with the error of a failed group permissions request, without requesting the group's members", async () => {
        const serverError = new ApimApiError(500, 'Internal Server Error');
        mockApimFetchJsonV2.mockImplementation(async (_environmentId: string, path: string) => {
            if (path === GROUP_PERMISSIONS_PATH) {
                throw serverError;
            }
            return { data: [] };
        });

        await expect(getIntegrationGroupMembership('env-1', 'a/b')).rejects.toBe(serverError);
        expect(mockApimFetchJsonV2).not.toHaveBeenCalledWith('env-1', GROUP_MEMBERS_PATH);
    });
});
