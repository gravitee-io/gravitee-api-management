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
    formatSourceLabel,
    formatTruncatedRoleSummary,
    formatUserStatus,
    isDuplicateUserError,
    isValidEmail,
    sanitizeTextInput,
    statusBadgeVariant,
} from './userDisplay';

describe('userDisplay utilities', () => {
    it('sanitizes HTML from text inputs', () => {
        expect(sanitizeTextInput('<b>Jane</b>')).toBe('Jane');
    });

    it('accepts well-formed emails and rejects malformed ones', () => {
        expect(isValidEmail('jane@company.com')).toBe(true);
        expect(isValidEmail('jane@company')).toBe(false);
    });

    it('labels the archived status as deletion in progress', () => {
        expect(formatUserStatus('ARCHIVED')).toBe('Deletion In Progress');
        expect(formatUserStatus('ACTIVE')).toBe('Active');
        expect(formatUserStatus(undefined)).toBe('Unknown');
    });

    it('maps user status to a badge variant', () => {
        expect(statusBadgeVariant('ACTIVE')).toBe('success');
        expect(statusBadgeVariant('PENDING')).toBe('warning');
        expect(statusBadgeVariant('REJECTED')).toBe('destructive');
        expect(statusBadgeVariant('SOMETHING_ELSE')).toBe('secondary');
    });

    it('formats the identity provider source label', () => {
        expect(formatSourceLabel('gravitee')).toBe('Gravitee');
        expect(formatSourceLabel('ldap')).toBe('LDAP');
        expect(formatSourceLabel('openid-provider')).toBe('OpenID Provider');
        expect(formatSourceLabel(undefined)).toBe('—');
    });

    it('truncates long role lists for profile display', () => {
        expect(formatTruncatedRoleSummary([{ name: 'User' }, { name: 'ORG_TEST1' }, { name: 'ORG_TEST2' }, { name: 'ADMIN' }])).toEqual({
            display: 'User, ORG_TEST1, ORG_TEST2...',
            full: 'User, ORG_TEST1, ORG_TEST2, Admin',
            truncated: true,
        });
        expect(formatTruncatedRoleSummary([{ name: 'User' }, { name: 'ADMIN' }])).toEqual({
            display: 'User, Admin',
            full: 'User, Admin',
            truncated: false,
        });
    });

    it('detects duplicate-user API errors without matching unrelated messages', () => {
        expect(isDuplicateUserError('User cannot be created.')).toBe(true);
        expect(isDuplicateUserError('A user [jane@company.com] already exists for organization DEFAULT.')).toBe(true);
        expect(isDuplicateUserError('A dictionary with name [Airport IATA Codes] already exists in this environment.')).toBe(false);
        expect(isDuplicateUserError('Member already exists')).toBe(false);
    });
});
