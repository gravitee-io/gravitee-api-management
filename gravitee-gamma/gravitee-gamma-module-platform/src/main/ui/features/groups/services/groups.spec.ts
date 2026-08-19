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
import { deleteGroupInvitation, inviteGroupMember, listGroupInvitations } from './groups';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { GroupInvitationPayload } from '../types/group';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
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

describe('groups invitation service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('posts the invitation and treats an invitation response as unambiguous', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue({ id: 'invitation-1' });

        await expect(inviteGroupMember('DEFAULT', 'group/1', INVITATION)).resolves.toEqual({ ambiguous: false });
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/groups/group%2F1/invitations', {
            method: 'POST',
            body: JSON.stringify(INVITATION),
        });
    });

    it('treats the backend user list response as an ambiguous email match', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue([{ id: 'user-1' }, { id: 'user-2' }]);

        await expect(inviteGroupMember('DEFAULT', 'group-1', INVITATION)).resolves.toEqual({ ambiguous: true });
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
});
