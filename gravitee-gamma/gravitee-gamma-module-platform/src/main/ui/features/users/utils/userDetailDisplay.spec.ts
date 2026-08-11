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
    canConvertToServiceAccount,
    canResetPassword,
    formatCustomFieldCopyValue,
    formatCustomFieldValue,
    formatUserDisplayName,
    formatUserTimestamp,
    getOrganizationRoles,
    resolveAssignedRoleIds,
    roleLabelsForIds,
} from './userDetailDisplay';

describe('userDetailDisplay utilities', () => {
    it('builds a display name from available user fields', () => {
        expect(formatUserDisplayName({ id: '1', displayName: 'Jane Doe' })).toBe('Jane Doe');
        expect(formatUserDisplayName({ id: '1', firstname: 'Jane', lastname: 'Doe' })).toBe('Jane Doe');
        expect(formatUserDisplayName({ id: '1', email: 'jane@company.com' })).toBe('jane@company.com');
    });

    it('filters organization-scoped roles', () => {
        expect(
            getOrganizationRoles([
                { name: 'User', scope: 'ORGANIZATION' },
                { name: 'API_USER', scope: 'ENVIRONMENT' },
            ]),
        ).toEqual([{ name: 'User', scope: 'ORGANIZATION' }]);
    });

    it('stringifies custom field values for display', () => {
        expect(formatCustomFieldValue('Engineering')).toBe('Engineering');
        expect(formatCustomFieldValue(null)).toBe('—');
        expect(formatCustomFieldValue(undefined)).toBe('—');
        expect(formatCustomFieldValue({ team: 'Ops' })).toBe('{"team":"Ops"}');
    });

    it('builds raw custom field clipboard values', () => {
        expect(formatCustomFieldCopyValue('Engineering')).toBe('Engineering');
        expect(formatCustomFieldCopyValue(null)).toBe('');
        expect(formatCustomFieldCopyValue({ team: 'Ops' })).toBe('{"team":"Ops"}');
    });

    it('resolves assigned role ids from user roles and the role catalog', () => {
        expect(resolveAssignedRoleIds([{ id: 'org-user', name: 'User' }], [{ id: 'org-user', name: 'User' }])).toEqual(['org-user']);
        expect(resolveAssignedRoleIds([{ name: 'User' }], [{ id: 'org-user', name: 'User' }])).toEqual(['org-user']);
    });

    it('maps role ids to display labels from the catalog', () => {
        expect(roleLabelsForIds(['org-admin'], [{ id: 'org-admin', name: 'ADMIN' }])).toEqual(['ADMIN']);
    });

    it('formats timestamps for profile metadata', () => {
        expect(formatUserTimestamp(undefined)).toBe('Never');
        const formatted = formatUserTimestamp(Date.parse('2025-07-10T15:30:00.000Z'));
        expect(formatted).toMatch(/Jul 10, 2025|10 Jul 2025/);
        expect(formatted).not.toMatch(/:\d{2}/);
    });

    it('allows service account conversion only for gravitee users without a password flag', () => {
        expect(canConvertToServiceAccount({ isServiceAccount: undefined, hasPassword: false, source: 'gravitee' })).toBe(true);
        expect(canConvertToServiceAccount({ isServiceAccount: false, hasPassword: false, source: 'gravitee' })).toBe(false);
        expect(canConvertToServiceAccount({ isServiceAccount: undefined, hasPassword: true, source: 'gravitee' })).toBe(false);
        expect(canConvertToServiceAccount({ isServiceAccount: undefined, hasPassword: false, source: 'ldap' })).toBe(false);
        expect(canConvertToServiceAccount({ isServiceAccount: true, hasPassword: false, source: 'gravitee' })).toBe(false);
    });

    it('allows password reset for active gravitee users that are not service accounts', () => {
        expect(canResetPassword({ source: 'gravitee', isServiceAccount: false, status: 'ACTIVE' })).toBe(true);
        expect(canResetPassword({ source: 'gravitee', isServiceAccount: undefined, status: 'ACTIVE' })).toBe(true);
        expect(canResetPassword({ source: 'ldap', isServiceAccount: false, status: 'ACTIVE' })).toBe(false);
        expect(canResetPassword({ source: 'gravitee', isServiceAccount: true, status: 'ACTIVE' })).toBe(false);
        expect(canResetPassword({ source: 'gravitee', isServiceAccount: false, status: 'PENDING' })).toBe(false);
    });
});
