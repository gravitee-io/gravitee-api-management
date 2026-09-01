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
import { NAV_SECTIONS } from './navigation';
import {
    firstVisibleNavItemKey,
    isNavItemVisible,
    landingNavItemKey,
    modulePathFor,
    NAV_ITEM_PERMISSIONS,
    pageGuardForNavItem,
    requiresOrganizationSettingsGate,
    visibleNavItemKeys,
    type NavVisibilityInput,
} from './navVisibility';

const ORGANIZATION_USER = [
    'organization-environment-r',
    'organization-role-r',
    'organization-tag-r',
    'organization-tenant-r',
    'organization-entrypoint-r',
] as const;

const ENVIRONMENT_USER = [
    'environment-api-r',
    'environment-application-c',
    'environment-application-r',
    'environment-application-u',
    'environment-application-d',
    'environment-group-r',
    'environment-documentation-r',
    'environment-integration-r',
    'environment-api_product-r',
    'environment-shared_policy_group-r',
] as const;

const FEDERATION_AGENT = [
    'environment-integration-c',
    'environment-integration-r',
    'environment-integration-u',
    'environment-integration-d',
] as const;

const GAMMA_IDENTITY_ADMIN = [
    'environment-am_configuration-c',
    'environment-am_configuration-r',
    'environment-am_configuration-u',
    'environment-agent_identity-c',
    'environment-agent_identity-r',
    'environment-agent_identity-u',
    'environment-agent_identity-d',
] as const;

const ENVIRONMENT_ADMIN = [
    ...ENVIRONMENT_USER,
    'environment-metadata-r',
    'environment-dictionary-r',
    'environment-instance-r',
    'environment-alert-r',
    'environment-settings-r',
    'environment-audit-r',
    'environment-am_configuration-r',
] as const;

const ORGANIZATION_ADMIN = [
    ...ORGANIZATION_USER,
    'organization-settings-r',
    'organization-settings-u',
    'organization-identity_provider-r',
    'organization-audit-r',
    'organization-policies-r',
    'organization-policies-c',
    'organization-policies-d',
    'organization-policies-u',
    'organization-user-c',
    'organization-user-r',
    'organization-user-u',
    'organization-user-d',
    'organization-notification_templates-r',
] as const;

function hasOf(granted: readonly string[]): (permission: string) => boolean {
    const set = new Set(granted);
    return permission => set.has(permission);
}

function visibility(granted: readonly string[], extra: Partial<NavVisibilityInput> = {}): NavVisibilityInput {
    return {
        permissionsReady: true,
        has: hasOf(granted),
        ...extra,
    };
}

describe('platform nav visibility', () => {
    it('declares a permission map for every nav item and none for organization-groups', () => {
        const keys = NAV_SECTIONS.flatMap(section => section.groups.flatMap(group => group.items.map(item => item.key)));
        expect(keys).not.toContain('organization-groups');
        expect(Object.keys(NAV_ITEM_PERMISSIONS).sort()).toEqual([...keys].sort());
        expect(Object.keys(NAV_ITEM_PERMISSIONS)).toEqual(expect.arrayContaining(['groups']));
        expect(Object.keys(NAV_ITEM_PERMISSIONS)).not.toContain('user-groups');
    });

    it('hides every item until environment permissions are ready', () => {
        const admin = visibility([...ORGANIZATION_ADMIN, ...ENVIRONMENT_ADMIN], { permissionsReady: false });
        expect(visibleNavItemKeys(admin)).toEqual([]);
    });

    it('hides unknown keys', () => {
        expect(isNavItemVisible('organization-groups', visibility([...ORGANIZATION_ADMIN, ...ENVIRONMENT_ADMIN]))).toBe(false);
        expect(isNavItemVisible('missing', visibility(['environment-application-r']))).toBe(false);
    });

    it('shows Applications, Shared Policy Groups, and Groups for ENVIRONMENT:USER with org USER', () => {
        const keys = visibleNavItemKeys(visibility([...ORGANIZATION_USER, ...ENVIRONMENT_USER]));
        expect(keys.sort()).toEqual(['applications', 'groups', 'shared-policy-groups']);
        expect(
            firstVisibleNavItemKey(NAV_SECTIONS, itemKey =>
                isNavItemVisible(itemKey, visibility([...ORGANIZATION_USER, ...ENVIRONMENT_USER])),
            ),
        ).toBe('applications');
    });

    it('shows no Platform items for FEDERATION_AGENT with org USER', () => {
        const input = visibility([...ORGANIZATION_USER, ...FEDERATION_AGENT]);
        expect(visibleNavItemKeys(input)).toEqual([]);
        expect(firstVisibleNavItemKey(NAV_SECTIONS, itemKey => isNavItemVisible(itemKey, input))).toBeUndefined();
        expect(landingNavItemKey(input)).toBeUndefined();
    });

    it('lands on Applications when that item is visible, even if Organization is first in the nav', () => {
        expect(landingNavItemKey(visibility([...ORGANIZATION_ADMIN, ...ENVIRONMENT_ADMIN]))).toBe('applications');
        expect(landingNavItemKey(visibility([...ORGANIZATION_USER, ...ENVIRONMENT_USER]))).toBe('applications');
    });

    it('lands on the first remaining item when Applications is not visible', () => {
        expect(landingNavItemKey(visibility([...ORGANIZATION_ADMIN]))).toBe('tenants');
        expect(landingNavItemKey(visibility(['environment-group-r']))).toBe('groups');
        expect(landingNavItemKey(visibility([]))).toBeUndefined();
    });

    // A locked item stays in the sidebar but its route redirects away, so landing on it would bounce
    // straight back out. It must never be chosen as the landing target.
    it('never lands on a locked item, even when it is the only visible one', () => {
        const alertsOnly = visibility(['environment-alert-r'], { lockedItemKeys: ['alerts'] });
        expect(visibleNavItemKeys(alertsOnly)).toEqual(['alerts']);
        expect(landingNavItemKey(alertsOnly)).toBeUndefined();
        expect(landingNavItemKey(visibility(['environment-alert-r']))).toBe('alerts');
    });

    // A 403 on an organization-scoped page strips nothing from the environment permissions, so without
    // the denial the item stays visible and stays the landing key the page keeps redirecting back to.
    it('hides a denied item and lands elsewhere, even while its permission is still granted', () => {
        const denied = visibility([...ORGANIZATION_ADMIN], { deniedItemKeys: new Set(['tenants']) });
        expect(visibleNavItemKeys(denied)).not.toContain('tenants');
        expect(landingNavItemKey(denied)).not.toBe('tenants');
        expect(landingNavItemKey(visibility([...ORGANIZATION_ADMIN]))).toBe('tenants');
    });

    it('skips a locked item and lands on the next visible one', () => {
        const input = visibility(['environment-alert-r', 'environment-group-r'], { lockedItemKeys: ['alerts'] });
        expect(landingNavItemKey(input)).toBe('groups');
    });

    it('hides Organization and Users for ENVIRONMENT:ADMIN who is still org USER', () => {
        const keys = visibleNavItemKeys(visibility([...ORGANIZATION_USER, ...ENVIRONMENT_ADMIN]));
        expect(keys).toEqual(
            expect.arrayContaining([
                'applications',
                'metadata',
                'dictionaries',
                'shared-policy-groups',
                'gateways',
                'alerts',
                'security-plan-types',
                'environment-audit',
                'access-management',
                'groups',
            ]),
        );
        expect(keys).not.toContain('tenants');
        expect(keys).not.toContain('users');
        expect(keys).not.toContain('cors');
        expect(keys).not.toContain('policy-studio');
    });

    it('shows Access Management for GAMMA_IDENTITY_ADMIN or env ADMIN without org settings', () => {
        expect(requiresOrganizationSettingsGate('access-management')).toBe(false);
        expect(pageGuardForNavItem('access-management')).toEqual({ anyOf: ['environment-am_configuration-r'] });
        expect(visibleNavItemKeys(visibility([...ORGANIZATION_USER, ...GAMMA_IDENTITY_ADMIN]))).toEqual(['access-management']);
        expect(isNavItemVisible('access-management', visibility(['environment-am_configuration-r']))).toBe(true);
        expect(isNavItemVisible('access-management', visibility([...ORGANIZATION_USER, ...ENVIRONMENT_USER]))).toBe(false);
        expect(isNavItemVisible('access-management', visibility([...ORGANIZATION_USER, ...FEDERATION_AGENT]))).toBe(false);
    });

    it('hides Management, CORS, and SMTP when the user has organization-settings-u without -r', () => {
        const onlyUpdate = visibility(['organization-settings-u']);
        expect(isNavItemVisible('management-and-schedulers', onlyUpdate)).toBe(false);
        expect(isNavItemVisible('cors', onlyUpdate)).toBe(false);
        expect(isNavItemVisible('smtp', onlyUpdate)).toBe(false);
        expect(isNavItemVisible('users', visibility(['organization-settings-u', 'organization-user-r']))).toBe(true);
    });

    it('shows Management, CORS, and SMTP when the user has organization-settings-r', () => {
        const canRead = visibility(['organization-settings-r']);
        expect(isNavItemVisible('management-and-schedulers', canRead)).toBe(true);
        expect(isNavItemVisible('cors', canRead)).toBe(true);
        expect(isNavItemVisible('smtp', canRead)).toBe(true);
        expect(isNavItemVisible('templates', canRead)).toBe(false);
    });

    it('gates Templates on organization-notification_templates-r after the org settings gate', () => {
        expect(requiresOrganizationSettingsGate('templates')).toBe(true);
        expect(isNavItemVisible('templates', visibility(['organization-settings-r']))).toBe(false);
        expect(isNavItemVisible('templates', visibility(['organization-notification_templates-r']))).toBe(false);
        expect(isNavItemVisible('templates', visibility(['organization-settings-r', 'organization-notification_templates-r']))).toBe(true);
    });

    it('shows Organization items for org ADMIN after the outer gate plus item-level -r', () => {
        const keys = visibleNavItemKeys(visibility([...ORGANIZATION_ADMIN, ...ENVIRONMENT_ADMIN]));
        expect(keys).toEqual(
            expect.arrayContaining([
                'tenants',
                'entrypoints-and-sharding-tags',
                'policy-studio',
                'access-management',
                'authentication',
                'management-and-schedulers',
                'cors',
                'smtp',
                'templates',
                'organization-audit',
                'users',
            ]),
        );
    });

    it('gates Policy Studio on organization-policies read/create/delete/update after the org settings gate', () => {
        expect(requiresOrganizationSettingsGate('policy-studio')).toBe(true);
        expect(isNavItemVisible('policy-studio', visibility(['organization-settings-r', 'organization-policies-r']))).toBe(true);
        expect(isNavItemVisible('policy-studio', visibility(['organization-policies-u']))).toBe(false);
        expect(isNavItemVisible('policy-studio', visibility(['organization-settings-r', 'organization-policies-u']))).toBe(true);
        expect(isNavItemVisible('policy-studio', visibility(['organization-settings-r']))).toBe(false);
    });

    it('does not show Tenants without organization-tenant-r even with the org settings gate', () => {
        expect(isNavItemVisible('tenants', visibility(['organization-settings-r', 'organization-tag-r']))).toBe(false);
        expect(isNavItemVisible('tenants', visibility(['organization-settings-r', 'environment-tenant-r']))).toBe(false);
        expect(isNavItemVisible('tenants', visibility(['organization-settings-r', 'organization-tenant-r']))).toBe(true);
    });

    it('gates Entrypoints on organization-tag-r after the org settings gate', () => {
        expect(
            isNavItemVisible('entrypoints-and-sharding-tags', visibility(['organization-settings-r', 'organization-entrypoint-r'])),
        ).toBe(false);
        expect(isNavItemVisible('entrypoints-and-sharding-tags', visibility(['organization-settings-r', 'organization-tag-r']))).toBe(true);
    });

    it('requires the org settings gate for Users even when the user has organization-user-r', () => {
        expect(requiresOrganizationSettingsGate('users')).toBe(true);
        expect(isNavItemVisible('users', visibility(['organization-user-r']))).toBe(false);
        expect(isNavItemVisible('users', visibility(['organization-settings-r', 'organization-user-r']))).toBe(true);
    });

    it('builds page guards from the same map as nav visibility', () => {
        expect(pageGuardForNavItem('applications')).toEqual({ anyOf: ['environment-application-r'] });
        expect(pageGuardForNavItem('access-management')).toEqual({ anyOf: ['environment-am_configuration-r'] });
        expect(pageGuardForNavItem('cors')).toEqual({
            anyOf: ['organization-settings-r'],
            alsoAnyOf: ['organization-settings-r', 'organization-settings-u'],
        });
        expect(pageGuardForNavItem('tenants')).toEqual({
            anyOf: ['organization-tenant-r'],
            alsoAnyOf: ['organization-settings-r', 'organization-settings-u'],
        });
    });

    it('does not require the org settings gate for env Groups', () => {
        expect(requiresOrganizationSettingsGate('groups')).toBe(false);
        expect(isNavItemVisible('groups', visibility(['environment-group-r']))).toBe(true);
    });

    it('fails closed for an unmapped page-guard key', () => {
        expect(pageGuardForNavItem('user-groups')).toEqual({ anyOf: [] });
        expect(pageGuardForNavItem('missing')).toEqual({ anyOf: [] });
    });

    it('hides metadata and dictionaries when the list API 403s', () => {
        const granted = visibility(['environment-metadata-r', 'environment-dictionary-r'], {
            metadataForbidden: true,
            dictionariesForbidden: true,
        });
        expect(isNavItemVisible('metadata', granted)).toBe(false);
        expect(isNavItemVisible('dictionaries', granted)).toBe(false);
    });

    it('rewrites a pasted module URL onto another leaf without dropping the host prefix', () => {
        expect(modulePathFor('/alerts', 'applications')).toBe('/applications');
        expect(modulePathFor('/environments/dev/platform/alerts', 'applications')).toBe('/environments/dev/platform/applications');
        expect(modulePathFor('/groups/group-1', 'applications')).toBe('/applications');
        expect(modulePathFor('/environments/dev/platform/applications/app-1/subscriptions/sub-1', 'groups')).toBe(
            '/environments/dev/platform/groups',
        );
        expect(modulePathFor('/environments/dev/platform', 'no-access')).toBe('/environments/dev/platform/no-access');
        expect(modulePathFor('/', 'no-access')).toBe('/no-access');
    });
});
