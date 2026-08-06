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
import type { NavGroup, NavItem } from '@gravitee/graphene-core';
import {
    AppWindowIcon,
    BellIcon,
    BookOpenIcon,
    BoxesIcon,
    CloudIcon,
    DatabaseIcon,
    GlobeIcon,
    GroupIcon,
    KeyIcon,
    LayersIcon,
    MailIcon,
    RadioIcon,
    ScrollTextIcon,
    ServerIcon,
    SettingsIcon,
    ShieldIcon,
    UsersIcon,
} from '@gravitee/graphene-core/icons';
import type { ElementType } from 'react';

import { ROUTES } from './routes';

export type PlatformNavSectionKey = 'organization' | 'environment' | 'team';

export interface PlatformNavSection {
    readonly key: PlatformNavSectionKey;
    readonly title: string;
    readonly icon: ElementType;
    readonly groups: NavGroup[];
}

export const NAV_SECTIONS: PlatformNavSection[] = [
    {
        key: 'organization',
        title: 'Organization',
        icon: GlobeIcon,
        groups: [
            {
                label: 'Assets',
                items: [
                    {
                        key: 'tenants',
                        title: ROUTES.tenants.label,
                        icon: BoxesIcon,
                    },
                    {
                        key: 'entrypoints-and-sharding-tags',
                        title: ROUTES['entrypoints-and-sharding-tags'].label,
                        icon: RadioIcon,
                    },
                ],
            },
            {
                label: 'System & Security',
                items: [
                    { key: 'access-management', title: ROUTES['access-management'].label, icon: ShieldIcon },
                    { key: 'management-and-schedulers', title: ROUTES['management-and-schedulers'].label, icon: SettingsIcon },
                    { key: 'cors', title: ROUTES.cors.label, icon: GlobeIcon },
                    { key: 'smtp', title: ROUTES.smtp.label, icon: MailIcon },
                    { key: 'organization-audit', title: ROUTES['organization-audit'].label, icon: ScrollTextIcon },
                ],
            },
        ],
    },
    {
        key: 'environment',
        title: 'Environment',
        icon: CloudIcon,
        groups: [
            {
                label: 'APIs & Assets',
                items: [
                    { key: 'applications', title: ROUTES.applications.label, icon: AppWindowIcon },
                    { key: 'metadata', title: ROUTES.metadata.label, icon: DatabaseIcon },
                    { key: 'dictionaries', title: ROUTES.dictionaries.label, icon: BookOpenIcon },
                    { key: 'shared-policy-groups', title: ROUTES['shared-policy-groups'].label, icon: LayersIcon },
                ],
            },
            {
                label: 'System & Security',
                items: [
                    { key: 'gateways', title: ROUTES.gateways.label, icon: ServerIcon },
                    { key: 'alerts', title: ROUTES.alerts.label, icon: BellIcon },
                    { key: 'security-plan-types', title: ROUTES['security-plan-types'].label, icon: KeyIcon },
                    { key: 'environment-audit', title: ROUTES['environment-audit'].label, icon: ScrollTextIcon },
                ],
            },
        ],
    },
    {
        key: 'team',
        title: 'Team',
        icon: UsersIcon,
        groups: [
            {
                label: 'Team',
                items: [
                    { key: 'users', title: ROUTES.users.label, icon: UsersIcon },
                    { key: 'user-groups', title: ROUTES['user-groups'].label, icon: GroupIcon },
                    { key: 'organization-groups', title: ROUTES['organization-groups'].label, icon: GroupIcon },
                ],
            },
        ],
    },
];

export function platformPrimaryNavItems(sections: readonly PlatformNavSection[]): NavItem[] {
    return sections.map(section => ({ key: section.key, title: section.title, icon: section.icon }));
}

export function findNavSectionKey(sections: readonly PlatformNavSection[], itemKey: string): PlatformNavSectionKey | undefined {
    return sections.find(section => section.groups.some(group => group.items.some(item => item.key === itemKey)))?.key;
}

export function firstNavItemKey(section: PlatformNavSection): string | undefined {
    for (const group of section.groups) {
        const firstItem = group.items[0];
        if (firstItem) {
            return firstItem.key;
        }
    }
    return undefined;
}

export function filterNavSections(sections: readonly PlatformNavSection[], isVisible: (itemKey: string) => boolean): PlatformNavSection[] {
    return sections
        .map(section => ({
            ...section,
            groups: section.groups
                .map(group => ({
                    ...group,
                    items: group.items.filter(item => isVisible(item.key)),
                }))
                .filter(group => group.items.length > 0),
        }))
        .filter(section => section.groups.length > 0);
}
