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
import { CloudIcon, GlobeIcon, GroupIcon, UsersIcon } from '@gravitee/graphene-core/icons';

import { filterNavSections, findNavSectionKey, firstNavItemKey, NAV_SECTIONS, platformPrimaryNavItems } from './navigation';
import { PLATFORM_ROUTE_CONFIG, ROUTES } from './routes';

function sectionKeys(label: string, groupLabel: string): string[] {
    const section = NAV_SECTIONS.find(candidate => candidate.title === label);
    const group = section?.groups.find(candidate => candidate.label === groupLabel);
    return group?.items.map(item => item.key) ?? [];
}

describe('platform navigation config', () => {
    it('exposes Organization, Environment, and Team as the only primary sections', () => {
        expect(NAV_SECTIONS.map(section => section.key)).toEqual(['organization', 'environment', 'team']);
        expect(NAV_SECTIONS.map(section => section.title)).toEqual(['Organization', 'Environment', 'Team']);
        expect(NAV_SECTIONS[0]?.icon).toBe(GlobeIcon);
        expect(NAV_SECTIONS[1]?.icon).toBe(CloudIcon);
        expect(NAV_SECTIONS[2]?.icon).toBe(UsersIcon);
    });

    it('does not include a General primary section', () => {
        expect(NAV_SECTIONS.some(section => section.key === 'general' || section.title === 'General')).toBe(false);
    });

    it('places Tenants then Entrypoints & Sharding Tags under Organization / Assets', () => {
        expect(sectionKeys('Organization', 'Assets')).toEqual(['tenants', 'entrypoints-and-sharding-tags']);
    });

    it('places Access Management under Organization / System & Security', () => {
        expect(sectionKeys('Organization', 'System & Security')).toEqual(['access-management']);
    });

    it('places Applications, Metadata, and Dictionaries under Environment / APIs & Assets', () => {
        expect(sectionKeys('Environment', 'APIs & Assets')).toEqual(['applications', 'metadata', 'dictionaries']);
    });

    it('places Gateways, Alerts, and Security Plan Types under Environment / System & Security', () => {
        expect(sectionKeys('Environment', 'System & Security')).toEqual(['gateways', 'alerts', 'security-plan-types']);
    });

    it('places Users and Groups under Team', () => {
        const teamGroup = NAV_SECTIONS.find(section => section.key === 'team')?.groups.find(group => group.label === 'Team');
        expect(teamGroup?.items.map(item => item.key)).toEqual(['users', 'user-groups']);
        expect(teamGroup?.items[0]?.title).toBe('Users');
        expect(teamGroup?.items[0]?.icon).toBe(UsersIcon);
        expect(teamGroup?.items[1]?.title).toBe('Groups');
        expect(teamGroup?.items[1]?.icon).toBe(GroupIcon);
    });

    it('builds unlabeled primary items from visible sections', () => {
        const items = platformPrimaryNavItems(NAV_SECTIONS);
        expect(items.map(item => item.key)).toEqual(['organization', 'environment', 'team']);
        expect(items.map(item => item.title)).toEqual(['Organization', 'Environment', 'Team']);
        expect(items.some(item => item.title === 'Platform')).toBe(false);
    });

    it('resolves the section that owns a nav item', () => {
        expect(findNavSectionKey(NAV_SECTIONS, 'applications')).toBe('environment');
        expect(findNavSectionKey(NAV_SECTIONS, 'users')).toBe('team');
        expect(findNavSectionKey(NAV_SECTIONS, 'access-management')).toBe('organization');
        expect(findNavSectionKey(NAV_SECTIONS, 'tenants')).toBe('organization');
        expect(findNavSectionKey(NAV_SECTIONS, 'missing')).toBeUndefined();
    });

    it('returns the first visible item in a section', () => {
        const organization = NAV_SECTIONS.find(section => section.key === 'organization');
        expect(organization).toBeDefined();
        expect(firstNavItemKey(organization!)).toBe('tenants');
    });

    it('filters hidden items and drops empty groups and sections', () => {
        const filtered = filterNavSections(NAV_SECTIONS, key => key === 'users');

        expect(filtered.map(section => section.key)).toEqual(['team']);
        expect(filtered[0]?.groups).toEqual([
            expect.objectContaining({
                label: 'Team',
                items: [expect.objectContaining({ key: 'users' })],
            }),
        ]);
    });

    it('declares the users route in platform routing config', () => {
        expect(PLATFORM_ROUTE_CONFIG.routeKeys).toContain('users');
        expect(ROUTES.users).toEqual({ path: 'users', label: 'Users' });
    });

    it('declares the user-groups route in platform routing config', () => {
        expect(PLATFORM_ROUTE_CONFIG.routeKeys).toContain('user-groups');
        expect(ROUTES['user-groups']).toEqual({ path: 'user-groups', label: 'Groups' });
    });

    it('declares the tenants route in platform routing config', () => {
        expect(PLATFORM_ROUTE_CONFIG.routeKeys).toContain('tenants');
        expect(ROUTES.tenants).toEqual({ path: 'tenants', label: 'Tenants' });
    });
});
