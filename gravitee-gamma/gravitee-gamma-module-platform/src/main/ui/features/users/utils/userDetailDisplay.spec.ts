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
        expect(formatCustomFieldValue({ team: 'Ops' })).toBe('{"team":"Ops"}');
    });

    it('resolves assigned role ids from user roles and the role catalog', () => {
        expect(resolveAssignedRoleIds([{ id: 'org-user', name: 'User' }], [{ id: 'org-user', name: 'User' }])).toEqual(['org-user']);
        expect(resolveAssignedRoleIds([{ name: 'User' }], [{ id: 'org-user', name: 'User' }])).toEqual(['org-user']);
    });

    it('maps role ids to display labels from the catalog', () => {
        expect(roleLabelsForIds(['org-admin'], [{ id: 'org-admin', name: 'ADMIN' }])).toEqual(['Admin']);
    });

    it('formats timestamps for profile metadata', () => {
        expect(formatUserTimestamp(undefined)).toBe('Never');
        expect(formatUserTimestamp(Date.parse('2025-07-10'))).toMatch(/Jul 10, 2025|10 Jul 2025/);
    });
});
