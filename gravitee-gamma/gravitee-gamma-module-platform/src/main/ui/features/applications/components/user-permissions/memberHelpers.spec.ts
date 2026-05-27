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
    formatAddMembersResultMessage,
    formatRoleLabel,
    getApplicationRole,
    getGroupMemberRole,
    isMemberPrimaryOwner,
    isSameUser,
} from './memberHelpers';
import type { ApplicationUiMember } from '../../types/applicationMembers.types';

describe('memberHelpers', () => {
    describe('formatRoleLabel', () => {
        it('formats underscore-separated roles', () => {
            expect(formatRoleLabel('PRIMARY_OWNER')).toBe('Primary Owner');
            expect(formatRoleLabel('USER')).toBe('User');
        });
    });

    describe('getApplicationRole', () => {
        it('prefers APPLICATION scope role', () => {
            const member: ApplicationUiMember = {
                id: 'u1',
                displayName: 'User',
                roles: [
                    { name: 'USER', scope: 'GROUP' },
                    { name: 'OWNER', scope: 'APPLICATION' },
                ],
            };
            expect(getApplicationRole(member)).toBe('OWNER');
        });

        it('falls back to first role when APPLICATION scope is missing', () => {
            const member: ApplicationUiMember = {
                id: 'u1',
                displayName: 'User',
                roles: [{ name: 'USER', scope: 'GROUP' }],
            };
            expect(getApplicationRole(member)).toBe('USER');
        });
    });

    describe('isMemberPrimaryOwner', () => {
        it('returns true when member has PRIMARY_OWNER role', () => {
            expect(
                isMemberPrimaryOwner({
                    id: 'u1',
                    displayName: 'Owner',
                    roles: [{ name: 'PRIMARY_OWNER', scope: 'APPLICATION' }],
                }),
            ).toBe(true);
        });

        it('returns false for other roles', () => {
            expect(
                isMemberPrimaryOwner({
                    id: 'u1',
                    displayName: 'User',
                    roles: [{ name: 'USER', scope: 'APPLICATION' }],
                }),
            ).toBe(false);
        });
    });

    describe('isSameUser', () => {
        it('matches by id when both ids are set', () => {
            expect(isSameUser({ id: 'a', reference: 'ref-a' }, { id: 'a', reference: 'ref-b' })).toBe(true);
            expect(isSameUser({ id: 'a', reference: 'ref-a' }, { id: 'b', reference: 'ref-a' })).toBe(false);
        });

        it('falls back to reference when ids are missing', () => {
            expect(isSameUser({ id: null, reference: 'ref-1' }, { id: undefined, reference: 'ref-1' })).toBe(true);
        });
    });

    describe('formatAddMembersResultMessage', () => {
        const alice = { reference: 'ref-a', displayName: 'Alice' };
        const bob = { reference: 'ref-b', displayName: 'Bob' };

        it('describes partial success with failed member names', () => {
            expect(
                formatAddMembersResultMessage(5, 2, [
                    { user: alice, reason: 'Conflict' },
                    { user: bob, reason: 'Forbidden' },
                ]),
            ).toBe('Added 2 of 5 members. Failed to add: Alice (Conflict), Bob (Forbidden).');
        });

        it('describes total failure for multiple members', () => {
            expect(
                formatAddMembersResultMessage(2, 0, [
                    { user: alice, reason: 'Conflict' },
                    { user: bob, reason: 'Forbidden' },
                ]),
            ).toBe('Failed to add 2 members: Alice (Conflict), Bob (Forbidden).');
        });

        it('describes single-member failure with API reason', () => {
            expect(formatAddMembersResultMessage(1, 0, [{ user: alice, reason: 'Member already exists' }])).toBe(
                'Failed to add member: Member already exists',
            );
        });
    });

    describe('getGroupMemberRole', () => {
        it('prefers GROUP scope role', () => {
            expect(getGroupMemberRole({ roles: { GROUP: 'OWNER', APPLICATION: 'USER' } })).toBe('OWNER');
        });

        it('falls back to APPLICATION then first value', () => {
            expect(getGroupMemberRole({ roles: { APPLICATION: 'USER' } })).toBe('USER');
            expect(getGroupMemberRole({ roles: { OTHER: 'ADMIN' } })).toBe('ADMIN');
        });

        it('returns em dash when roles are empty', () => {
            expect(getGroupMemberRole({ roles: {} })).toBe('—');
        });
    });
});
