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
import { formatRoleLabel, getGroupMemberRole } from './memberRoles';

describe('memberRoles', () => {
    describe('formatRoleLabel', () => {
        it('formats underscore-separated roles', () => {
            expect(formatRoleLabel('PRIMARY_OWNER')).toBe('Primary Owner');
            expect(formatRoleLabel('USER')).toBe('User');
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
