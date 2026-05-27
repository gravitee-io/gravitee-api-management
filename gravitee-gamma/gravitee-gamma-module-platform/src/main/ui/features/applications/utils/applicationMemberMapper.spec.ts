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
import { mapApplicationMemberToUiMember, toApplicationMemberEntity } from './applicationMemberMapper';

describe('applicationMemberMapper', () => {
    it('maps API member to UI member with APPLICATION scope', () => {
        expect(mapApplicationMemberToUiMember({ id: 'u1', displayName: 'Alice', role: 'USER' })).toEqual({
            id: 'u1',
            displayName: 'Alice',
            roles: [{ name: 'USER', scope: 'APPLICATION' }],
        });
    });

    it('uses id as displayName when displayName is missing', () => {
        expect(mapApplicationMemberToUiMember({ id: 'u1', role: 'USER' }).displayName).toBe('u1');
    });

    it('maps UI member back to API entity', () => {
        expect(
            toApplicationMemberEntity({ id: 'u1', displayName: 'Alice', roles: [{ name: 'OWNER', scope: 'APPLICATION' }] }, 'OWNER'),
        ).toEqual({ id: 'u1', displayName: 'Alice', role: 'OWNER' });
    });
});
