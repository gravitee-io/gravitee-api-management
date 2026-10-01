/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export type CloudSettingsNavKey =
    | 'general'
    | 'custom-reporters'
    | 'account-tokens'
    | 'cloud-tokens'
    | 'sso'
    | 'private-networks'
    | 'members'
    | 'invite-member';

export interface CloudSettingsNavItem {
    readonly key: CloudSettingsNavKey;
    readonly label: string;
    readonly locked?: boolean;
}

export interface CloudSettingsNavGroup {
    readonly label: string;
    readonly items: readonly CloudSettingsNavItem[];
}

export const CLOUD_SETTINGS_NAV_LABELS: Record<CloudSettingsNavKey, string> = {
    general: 'General',
    'custom-reporters': 'Custom Reporters',
    'account-tokens': 'Account Tokens',
    'cloud-tokens': 'Cloud Tokens',
    sso: 'Single Sign On',
    'private-networks': 'Private Networks',
    members: 'Members',
    'invite-member': 'Invite a member',
};

/** Cockpit Cloud account settings submenu (matches gravitee-cockpit-ui settings sidebar). */
export const CLOUD_SETTINGS_NAV_GROUPS: readonly CloudSettingsNavGroup[] = [
    {
        label: 'General settings',
        items: [
            { key: 'general', label: CLOUD_SETTINGS_NAV_LABELS.general },
            { key: 'custom-reporters', label: CLOUD_SETTINGS_NAV_LABELS['custom-reporters'] },
        ],
    },
    {
        label: 'Security',
        items: [
            { key: 'account-tokens', label: CLOUD_SETTINGS_NAV_LABELS['account-tokens'] },
            { key: 'cloud-tokens', label: CLOUD_SETTINGS_NAV_LABELS['cloud-tokens'] },
            { key: 'sso', label: CLOUD_SETTINGS_NAV_LABELS.sso },
            { key: 'private-networks', label: CLOUD_SETTINGS_NAV_LABELS['private-networks'], locked: true },
        ],
    },
    {
        label: 'User settings',
        items: [
            { key: 'members', label: CLOUD_SETTINGS_NAV_LABELS.members },
            { key: 'invite-member', label: CLOUD_SETTINGS_NAV_LABELS['invite-member'] },
        ],
    },
];

export function cloudSettingsNavPath(envHrid: string, key: CloudSettingsNavKey): string {
    return `/environments/${envHrid}/cloud/settings/${key}`;
}

export function resolveCloudSettingsNavKey(pathname: string, envHrid: string): CloudSettingsNavKey {
    const prefix = `/environments/${envHrid}/cloud/settings/`;
    if (!pathname.startsWith(prefix)) return 'general';
    const segment = pathname.slice(prefix.length).split('/')[0];
    if (segment && segment in CLOUD_SETTINGS_NAV_LABELS) {
        return segment as CloudSettingsNavKey;
    }
    return 'general';
}
