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
    APPLICATION_IMPLEMENTED_DETAIL_PATHS,
    APPLICATION_NAV_GROUPS,
    filterApplicationDetailNavGroups,
    getApplicationDetailTabPermissions,
    getFirstAccessibleApplicationDetailPath,
    getFirstAccessibleImplementedApplicationDetailPath,
    resolveApplicationDetailLandingPath,
} from './applicationDetailNavigation';

describe('normalizeCrudMapRecord (application permissions)', () => {
    it('expands CRUD strings into per-letter tokens', async () => {
        const { normalizeCrudMapRecord } = await import('../shared/gamma-modules-sdk');
        expect(normalizeCrudMapRecord('application', { DEFINITION: 'CRUD' })).toEqual([
            'application-definition-c',
            'application-definition-r',
            'application-definition-u',
            'application-definition-d',
        ]);
    });
});

describe('applicationDetailNavigation permissions', () => {
    const hasDefinitionRead = (permissions: string[]) => permissions.includes('application-definition-r');
    const hasMemberRead = (permissions: string[]) => permissions.includes('application-member-r');

    // An Application is not an API object, so it skips the canonical API groups — but it follows the
    // same vocabulary (FOUND-304): no `Security` group of its own, no group named after its only item,
    // and the entity's own settings page is called Settings.
    it('groups the sidebar as General / Access / Monitoring', () => {
        expect(APPLICATION_NAV_GROUPS.map(group => group.label)).toEqual(['General', 'Access', 'Monitoring']);
    });

    it('keeps User Permissions in General and names the settings page Settings', () => {
        const general = APPLICATION_NAV_GROUPS.find(group => group.label === 'General');
        expect(general?.items.map(item => item.label)).toEqual(['Overview', 'Settings', 'User Permissions']);
    });

    it('labels the notifications tab Notifications, under Monitoring', () => {
        const monitoring = APPLICATION_NAV_GROUPS.find(group => group.label === 'Monitoring');
        expect(monitoring?.items.find(item => item.path === 'notifications')?.label).toBe('Notifications');
    });

    it('maps general tab to application-definition-r', () => {
        expect(getApplicationDetailTabPermissions('general')).toEqual(['application-definition-r']);
    });

    it('maps subscriptions tab to application-subscription-r', () => {
        expect(getApplicationDetailTabPermissions('subscriptions')).toEqual(['application-subscription-r']);
    });

    it('filters nav groups by permission', () => {
        const filtered = filterApplicationDetailNavGroups(APPLICATION_NAV_GROUPS, hasDefinitionRead);
        expect(filtered.map(g => g.label)).toEqual(['General']);
        expect(filtered[0].items.map(i => i.path)).toEqual(['overview', 'general']);
        // `user-permissions` now shares the General group but keeps its own application-member-r gate.
    });

    it('returns first accessible path for the user', () => {
        expect(getFirstAccessibleApplicationDetailPath(APPLICATION_NAV_GROUPS, hasMemberRead)).toBe('user-permissions');
        expect(getFirstAccessibleApplicationDetailPath(APPLICATION_NAV_GROUPS, hasDefinitionRead)).toBe('overview');
    });

    it('returns first accessible implemented path', () => {
        expect(
            getFirstAccessibleImplementedApplicationDetailPath(
                APPLICATION_NAV_GROUPS,
                APPLICATION_IMPLEMENTED_DETAIL_PATHS,
                hasDefinitionRead,
            ),
        ).toBe('overview');
        expect(
            getFirstAccessibleImplementedApplicationDetailPath(APPLICATION_NAV_GROUPS, APPLICATION_IMPLEMENTED_DETAIL_PATHS, hasMemberRead),
        ).toBe('user-permissions');
    });

    it('returns null when user has no tab permissions', () => {
        expect(getFirstAccessibleApplicationDetailPath(APPLICATION_NAV_GROUPS, () => false)).toBeNull();
    });

    it('resolveApplicationDetailLandingPath prefers implemented tabs then any permitted tab', () => {
        expect(resolveApplicationDetailLandingPath(hasDefinitionRead)).toBe('overview');
        expect(resolveApplicationDetailLandingPath(hasMemberRead)).toBe('user-permissions');
        expect(resolveApplicationDetailLandingPath(() => false)).toBeNull();
    });
});
