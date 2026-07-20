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
    | 'designer'
    | 'subscription-form'
    | 'categories'
    | 'workflows'
    | 'idp-configuration'
    | 'settings';

export interface MappedApi {
    readonly id: string;
    readonly name: string;
}

export interface PortalCategory {
    readonly id: string;
    readonly portalId: string;
    readonly name: string;
    readonly description: string;
    readonly createdAt: number;
    readonly enabled: boolean;
    readonly mappedApis: readonly MappedApi[];
}

export type FormFieldType = 'text' | 'textarea' | 'dropdown' | 'radio' | 'checkbox';

export interface FormField {
    readonly id: string;
    readonly type: FormFieldType;
    readonly label: string;
    /** When true, the field must be filled in on the subscription form. */
    readonly required: boolean;
    /** Used by dropdown and radio fields (static / fallback options). */
    readonly options: readonly string[];
    /** Regex validation for text and textarea fields. */
    readonly validation: string;
    /** Expression language for dropdown and radio options (API metadata), e.g. {#api.metadata['key']}. */
    readonly expression: string;
}

export type FormFieldPatch = Partial<Pick<FormField, 'label' | 'required' | 'options' | 'validation' | 'expression'>>;

export interface SubscriptionForm {
    readonly id: string;
    readonly portalId: string;
    readonly name: string;
    readonly description: string;
    readonly createdAt: number;
    readonly mappedApis: readonly MappedApi[];
    readonly fields: readonly FormField[];
}

export const FIELD_TYPE_LABELS: Record<FormFieldType, string> = {
    text: 'Text box',
    textarea: 'Text area',
    dropdown: 'Drop down',
    radio: 'Radio button',
    checkbox: 'Checkbox',
};

export const FIELD_PALETTE: readonly FormFieldType[] = ['text', 'textarea', 'dropdown', 'radio', 'checkbox'];

export function normalizeFormField(field: FormField): FormField {
    return {
        ...field,
        required: field.required ?? false,
        options: field.options ?? [],
        validation: field.validation ?? '',
        expression: field.expression ?? '',
    };
}

export type PortalSettingsSectionMeta = {
    readonly title: string;
    readonly description: string;
    /** Settings sub-route segment; omit for sections that open the portal designer instead. */
    readonly path?: string;
};

export const PORTAL_SETTINGS_SECTION_META: Record<PortalSettingsSection, PortalSettingsSectionMeta> = {
    designer: {
        title: 'Portal designer',
        description: 'Edit portal pages, navigation, theme, and layout.',
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
