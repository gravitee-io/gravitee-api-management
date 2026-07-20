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

export type PortalWorkflowId =
    | 'signup'
    | 'login'
    | 'app-creation'
    | 'client-certificate-update'
    | 'api-subscription';

export interface PortalWorkflowMeta {
    readonly id: PortalWorkflowId;
    readonly title: string;
    readonly description: string;
}

export const PORTAL_WORKFLOWS: readonly PortalWorkflowMeta[] = [
    {
        id: 'signup',
        title: 'Signup',
        description: 'Configure approval steps for new user registration on the portal.',
    },
    {
        id: 'login',
        title: 'Login',
        description: 'Configure authentication and login workflow behavior.',
    },
    {
        id: 'app-creation',
        title: 'App creation',
        description: 'Configure approval steps when consumers create applications.',
    },
    {
        id: 'client-certificate-update',
        title: 'Client certificate Update',
        description: 'Configure approval steps for client certificate updates.',
    },
    {
        id: 'api-subscription',
        title: 'API subscription',
        description: 'Configure approval steps for API subscription requests.',
    },
];

export function isPortalWorkflowId(value: string): value is PortalWorkflowId {
    return PORTAL_WORKFLOWS.some(workflow => workflow.id === value);
}

export function getPortalWorkflow(id: string): PortalWorkflowMeta | undefined {
    return PORTAL_WORKFLOWS.find(workflow => workflow.id === id);
}

export type PortalIdentityProviderType = 'GOOGLE' | 'GITHUB' | 'GRAVITEEIO_AM' | 'OIDC';

export interface PortalIdpConfiguration {
    readonly clientId: string;
    readonly clientSecret: string;
    readonly serverURL: string;
    readonly domain: string;
    readonly scopes: string;
    readonly color: string;
    readonly tokenEndpoint: string;
    readonly authorizeEndpoint: string;
    readonly userInfoEndpoint: string;
    readonly userLogoutEndpoint: string;
    readonly tokenIntrospectionEndpoint: string;
}

export interface PortalIdentityProvider {
    readonly id: string;
    readonly portalId: string;
    readonly type: PortalIdentityProviderType;
    readonly name: string;
    readonly description: string;
    readonly enabled: boolean;
    readonly syncMappings: boolean;
    readonly emailRequired: boolean;
    readonly configuration: PortalIdpConfiguration;
    readonly createdAt: number;
    readonly updatedAt: number;
}

export const PORTAL_IDP_TYPE_LABELS: Record<PortalIdentityProviderType, string> = {
    GOOGLE: 'Google',
    GITHUB: 'GitHub',
    GRAVITEEIO_AM: 'Gravitee AM',
    OIDC: 'OpenID Connect',
};

export const PORTAL_IDP_TYPES: readonly PortalIdentityProviderType[] = [
    'GRAVITEEIO_AM',
    'OIDC',
    'GOOGLE',
    'GITHUB',
];

export function emptyIdpConfiguration(): PortalIdpConfiguration {
    return {
        clientId: '',
        clientSecret: '',
        serverURL: '',
        domain: '',
        scopes: '',
        color: '',
        tokenEndpoint: '',
        authorizeEndpoint: '',
        userInfoEndpoint: '',
        userLogoutEndpoint: '',
        tokenIntrospectionEndpoint: '',
    };
}

export function normalizeIdpConfiguration(
    configuration?: Partial<PortalIdpConfiguration>,
): PortalIdpConfiguration {
    return { ...emptyIdpConfiguration(), ...(configuration ?? {}) };
}

export function normalizeIdentityProvider(provider: PortalIdentityProvider): PortalIdentityProvider {
    return {
        ...provider,
        description: provider.description ?? '',
        enabled: provider.enabled ?? true,
        syncMappings: provider.syncMappings ?? false,
        emailRequired: provider.emailRequired ?? true,
        configuration: normalizeIdpConfiguration(provider.configuration),
    };
}

export type PortalIdentityProviderInput = {
    readonly type: PortalIdentityProviderType;
    readonly name: string;
    readonly description?: string;
    readonly enabled?: boolean;
    readonly syncMappings?: boolean;
    readonly emailRequired?: boolean;
    readonly configuration?: Partial<PortalIdpConfiguration>;
};

export type PortalIdentityProviderPatch = Partial<
    Pick<
        PortalIdentityProvider,
        'name' | 'description' | 'enabled' | 'syncMappings' | 'emailRequired' | 'configuration'
    >
>;
