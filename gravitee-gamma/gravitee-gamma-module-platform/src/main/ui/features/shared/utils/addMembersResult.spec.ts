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
import { formatAddMembersResultMessage, summarizeAddMembersResults } from './addMembersResult';

describe('addMembersResult', () => {
    const alice = { reference: 'ref-a', displayName: 'Alice' };
    const bob = { reference: 'ref-b', displayName: 'Bob' };
    const carol = { reference: 'ref-c', displayName: 'Carol' };

    describe('summarizeAddMembersResults', () => {
        it('maps each rejection to its user and stringifies non-Error reasons', () => {
            const results: PromiseSettledResult<unknown>[] = [
                { status: 'fulfilled', value: undefined },
                { status: 'rejected', reason: new Error('boom') },
                { status: 'rejected', reason: 'plain string' },
            ];

            expect(summarizeAddMembersResults([alice, bob, carol], results)).toEqual({
                succeededCount: 1,
                failed: [
                    { user: bob, reason: 'boom' },
                    { user: carol, reason: 'plain string' },
                ],
            });
        });

        it('returns no failures when every add succeeds', () => {
            const results: PromiseSettledResult<unknown>[] = [
                { status: 'fulfilled', value: undefined },
                { status: 'fulfilled', value: undefined },
            ];

            expect(summarizeAddMembersResults([alice, bob], results)).toEqual({ succeededCount: 2, failed: [] });
        });
    });

    describe('formatAddMembersResultMessage', () => {
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

        it('names the member when a single-member failure has no reason', () => {
            expect(formatAddMembersResultMessage(1, 0, [{ user: alice, reason: '' }])).toBe('Failed to add Alice.');
        });

        it('falls back to a generic message when a single-member failure has no failed entry', () => {
            expect(formatAddMembersResultMessage(1, 0, [])).toBe('Failed to add member.');
        });

        it('labels a member by email, then by reference, when the display name is empty', () => {
            expect(
                formatAddMembersResultMessage(2, 0, [
                    { user: { reference: 'ref-a', displayName: '', email: 'a@x.io' }, reason: 'X' },
                    { user: { reference: 'ref-b', displayName: '' }, reason: 'Y' },
                ]),
            ).toBe('Failed to add 2 members: a@x.io (X), ref-b (Y).');
        });
    });
});
