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
import type { PageContentType } from '../portals/types';
import {
    emptyIdpConfiguration,
    normalizeIdpConfiguration,
    PORTAL_IDP_TYPE_LABELS,
    PORTAL_IDP_TYPES,
    type PortalIdentityProviderType,
    type PortalIdpConfiguration,
} from '../settings/types';

export type ModuleConfigSection =
    | 'identity-providers'
    | 'domains'
    | 'templates'
    | 'google-analytics'
    | 'webhooks'
    | 'third-party-apps'
    | 'dashboards'
    | 'logs';

export type ModuleConfigSectionMeta = {
    readonly title: string;
    readonly description: string;
};

export const MODULE_CONFIG_SECTION_META: Record<ModuleConfigSection, ModuleConfigSectionMeta> = {
    'identity-providers': {
        title: 'Identity Providers',
        description: 'Manage SSO integrations, social login, and secure user directories',
    },
    domains: {
        title: 'Domains',
        description: 'Map custom hostnames to developer portals.',
    },
    templates: {
        title: 'Templates',
        description: 'Default and custom page templates for portal navigation items.',
    },
    'google-analytics': {
        title: 'Google Analytics',
        description: 'Configure Google Analytics tracking across your developer portals.',
    },
    webhooks: {
        title: 'Webhooks',
        description: 'Notify external systems when portal events occur.',
    },
    'third-party-apps': {
        title: 'Third-Party Apps',
        description: 'Connect external applications and OAuth integrations.',
    },
    dashboards: {
        title: 'Dashboards',
        description: 'Monitor portal traffic, authentication, and subscription activity.',
    },
    logs: {
        title: 'Logs',
        description: 'Browse audit and access logs across your developer portals.',
    },
};

export {
    emptyIdpConfiguration,
    normalizeIdpConfiguration,
    PORTAL_IDP_TYPE_LABELS,
    PORTAL_IDP_TYPES,
    type PortalIdentityProviderType,
    type PortalIdpConfiguration,
};

export interface TransversalIdentityProvider {
    readonly id: string;
    readonly type: PortalIdentityProviderType;
    readonly name: string;
    readonly description: string;
    readonly enabled: boolean;
    readonly syncMappings: boolean;
    readonly emailRequired: boolean;
    readonly configuration: PortalIdpConfiguration;
    /** Tenant IDs this provider can land users into. */
    readonly tenantIds: readonly string[];
    readonly createdAt: number;
    readonly updatedAt: number;
}

export type TransversalIdentityProviderInput = {
    readonly type: PortalIdentityProviderType;
    readonly name: string;
    readonly description?: string;
    readonly enabled?: boolean;
    readonly syncMappings?: boolean;
    readonly emailRequired?: boolean;
    readonly configuration?: Partial<PortalIdpConfiguration>;
    readonly tenantIds?: readonly string[];
};

export type TransversalIdentityProviderPatch = Partial<
    Pick<
        TransversalIdentityProvider,
        | 'name'
        | 'description'
        | 'enabled'
        | 'syncMappings'
        | 'emailRequired'
        | 'configuration'
        | 'tenantIds'
    >
>;

type LegacyTransversalIdentityProvider = Omit<TransversalIdentityProvider, 'tenantIds'> & {
    readonly tenantIds?: readonly string[];
    /** @deprecated Replaced by tenantIds; ignored on read. */
    readonly portalIds?: readonly string[];
};

export function normalizeTransversalIdentityProvider(
    provider: LegacyTransversalIdentityProvider | TransversalIdentityProvider,
): TransversalIdentityProvider {
    const { portalIds: _legacyPortalIds, ...rest } = provider as LegacyTransversalIdentityProvider;
    return {
        ...rest,
        description: rest.description ?? '',
        enabled: rest.enabled ?? true,
        syncMappings: rest.syncMappings ?? false,
        emailRequired: rest.emailRequired ?? true,
        // Legacy portalIds are not tenant IDs — drop them rather than migrate incorrectly.
        tenantIds: rest.tenantIds ?? [],
        configuration: normalizeIdpConfiguration(rest.configuration),
    };
}

export type DomainStatus = 'Pending' | 'Active' | 'Failed';

export interface PortalDomain {
    readonly id: string;
    readonly hostname: string;
    readonly portalId: string;
    readonly status: DomainStatus;
    readonly primary: boolean;
    readonly createdAt: number;
    readonly updatedAt: number;
}

export type PortalDomainInput = {
    readonly hostname: string;
    readonly portalId: string;
    readonly primary?: boolean;
    readonly status?: DomainStatus;
};

export type PortalDomainPatch = Partial<Pick<PortalDomain, 'hostname' | 'portalId' | 'status' | 'primary'>>;

export function normalizePortalDomain(domain: PortalDomain): PortalDomain {
    return {
        ...domain,
        hostname: domain.hostname.trim().toLowerCase(),
        status: domain.status ?? 'Pending',
        primary: domain.primary ?? false,
    };
}

export type PageTemplateKind =
    | 'home'
    | 'catalog'
    | 'documentation'
    | 'api-reference'
    | 'asyncapi'
    | 'html-blank'
    | 'login'
    | 'signup'
    | 'custom';

export interface PageTemplate {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly contentType: PageContentType;
    readonly kind: PageTemplateKind;
    /** When true, the template is seeded by the system and cannot be deleted. */
    readonly system: boolean;
    /** Stub body / placeholder content — not a full page editor payload. */
    readonly bodyStub: string;
    readonly createdAt: number;
    readonly updatedAt: number;
}

export type PageTemplateInput = {
    readonly name: string;
    readonly description?: string;
    readonly contentType: PageContentType;
    readonly kind?: PageTemplateKind;
    readonly bodyStub?: string;
};

export type PageTemplatePatch = Partial<Pick<PageTemplate, 'name' | 'description' | 'bodyStub' | 'contentType'>>;

export function normalizePageTemplate(template: PageTemplate): PageTemplate {
    return {
        ...template,
        description: template.description ?? '',
        bodyStub: template.bodyStub ?? '',
        system: template.system ?? false,
        kind: template.kind ?? 'custom',
    };
}

export const PAGE_TEMPLATE_CONTENT_TYPE_LABELS: Record<PageContentType, string> = {
    BLOCK: 'Block',
    OPENAPI: 'OpenAPI',
    HTML: 'HTML',
    ASYNCAPI: 'AsyncAPI',
};

export const DOMAIN_STATUS_LABELS: Record<DomainStatus, string> = {
    Pending: 'Pending',
    Active: 'Active',
    Failed: 'Failed',
};
