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

export type PortalSettingsSection =
    | 'homescreen'
    | 'subscription-form'
    | 'categories'
    | 'workflows'
    | 'idp-configuration'
    | 'settings';

export const PORTAL_SETTINGS_SECTION_META: Record<
    PortalSettingsSection,
    { readonly title: string; readonly description: string; readonly path: string }
> = {
    homescreen: {
        title: 'Homescreen',
        description: 'Configure the developer portal homepage content and layout.',
        path: 'homescreen',
    },
    'subscription-form': {
        title: 'Subscription Form',
        description: 'Customize the subscription request form shown to API consumers.',
        path: 'subscription-forms',
    },
    categories: {
        title: 'Categories',
        description: 'Organize APIs into categories for the developer portal catalog.',
        path: 'categories',
    },
    workflows: {
        title: 'Workflows',
        description: 'Configure approval and subscription workflows for this portal.',
        path: 'workflows',
    },
    'idp-configuration': {
        title: 'IdP configuration',
        description: 'Manage identity providers and authentication for portal users.',
        path: 'idp',
    },
    settings: {
        title: 'Settings',
        description: 'General portal settings, branding, and environment options.',
        path: 'general',
    },
};
