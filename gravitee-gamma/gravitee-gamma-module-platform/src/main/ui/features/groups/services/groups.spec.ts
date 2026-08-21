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
import { associateGroupToExisting, deleteGroupInvitation, inviteGroupMember, listGroupInvitations, removeGroupMember } from './groups';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { GroupInvitationPayload } from '../types/group';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

const INVITATION: GroupInvitationPayload = {
    reference_type: 'GROUP',
    reference_id: 'group/1',
    email: 'user@example.com',
    api_role: 'USER',
    application_role: 'USER',
};

describe('groups service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('returns invitation-created when the backend persists an invitation', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue({ id: 'invitation-1' });

        await expect(inviteGroupMember('DEFAULT', 'group/1', INVITATION)).resolves.toEqual({ outcome: 'invitation-created' });
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/groups/group%2F1/invitations', {
            method: 'POST',
            body: JSON.stringify(INVITATION),
        });
    });

    it.each([null, undefined])('returns member-added when the backend response body is %s', async responseBody => {
        mockApimFetchJsonV1Env.mockResolvedValue(responseBody);

        await expect(inviteGroupMember('DEFAULT', 'group-1', INVITATION)).resolves.toEqual({ outcome: 'member-added' });
    });

    it('treats the backend user list response as an ambiguous email match', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue([{ id: 'user-1' }, { id: 'user-2' }]);

        await expect(inviteGroupMember('DEFAULT', 'group-1', INVITATION)).resolves.toEqual({ outcome: 'ambiguous' });
    });

    it('rejects an unexpected invitation response shape', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue({});

        await expect(inviteGroupMember('DEFAULT', 'group-1', INVITATION)).rejects.toThrow('Unexpected group invitation response');
    });

    it('lists group invitations', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue([]);

        await expect(listGroupInvitations('DEFAULT', 'group/1')).resolves.toEqual([]);
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/groups/group%2F1/invitations');
    });

    it('deletes an invitation using encoded identifiers', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);

        await deleteGroupInvitation('DEFAULT', 'group/1', 'invitation/1');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/groups/group%2F1/invitations/invitation%2F1', {
            method: 'DELETE',
        });
    });

    it('removes a member using encoded identifiers', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);

        await removeGroupMember('DEFAULT', 'group/1', 'member/1');

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/groups/group%2F1/members/member%2F1', {
            method: 'DELETE',
        });
    });

    it.each(['api', 'api_product', 'application'] as const)('associates a group with all existing %s resources', async type => {
        mockApimFetchJsonV1Env.mockResolvedValue({ id: 'group/1' });

        await associateGroupToExisting('DEFAULT', 'group/1', type);

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', `/configuration/groups/group%2F1/memberships?type=${type}`, {
            method: 'POST',
            body: JSON.stringify({}),
        });
    });
});
