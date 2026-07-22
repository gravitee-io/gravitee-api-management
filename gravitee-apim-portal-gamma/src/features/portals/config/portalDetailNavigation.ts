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
import {
    FileTextIcon,
    FolderOpenIcon,
    KeyIcon,
    SettingsIcon,
    UsersIcon,
    WorkflowIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';

export const PORTAL_BREADCRUMB_LABEL_MAX_LENGTH = 40;

export function truncatePortalBreadcrumbLabel(
    label: string,
    maxLength = PORTAL_BREADCRUMB_LABEL_MAX_LENGTH,
): string {
    if (label.length <= maxLength) {
        return label;
    }
    return `${label.slice(0, Math.max(0, maxLength - 1)).trimEnd()}…`;
}

export type PortalDetailNavItem = {
    readonly path: string;
    readonly label: string;
    readonly icon: LucideIcon;
    /** When false, matches any sub-path (prefix match). Defaults to true (exact match). */
    readonly end?: boolean;
};

export type PortalDetailNavGroup = {
    readonly label: string;
    readonly items: readonly PortalDetailNavItem[];
};

export const PORTAL_DETAIL_NAV_GROUPS: readonly PortalDetailNavGroup[] = [
    {
        label: 'Portal Configuration',
        items: [
            { path: 'general', label: 'General', icon: SettingsIcon },
            { path: 'categories', label: 'Categories', icon: FolderOpenIcon },
            {
                path: 'subscription-forms',
                label: 'Subscription Forms',
                icon: FileTextIcon,
                end: false,
            },
        ],
    },
    {
        label: 'Access Management',
        items: [
            { path: 'idp', label: 'Identity Providers', icon: KeyIcon },
            { path: 'tenants', label: 'Tenants', icon: UsersIcon, end: false },
        ],
    },
    {
        label: 'Automation',
        items: [{ path: 'workflows', label: 'Workflows', icon: WorkflowIcon, end: false }],
    },
];

const SECTION_LABELS: Record<string, string> = {
    general: 'General',
    categories: 'Categories',
    'subscription-forms': 'Subscription Forms',
    idp: 'Identity Providers',
    tenants: 'Tenants',
    workflows: 'Workflows',
};

/**
 * Resolves the active settings section breadcrumb label from a pathname that
 * includes `/settings/...`.
 */
export function resolvePortalSettingsSectionLabel(pathname: string): string {
    const match = pathname.match(/\/settings\/([^/]+)/);
    if (!match) {
        return 'General';
    }
    return SECTION_LABELS[match[1]] ?? match[1];
}
