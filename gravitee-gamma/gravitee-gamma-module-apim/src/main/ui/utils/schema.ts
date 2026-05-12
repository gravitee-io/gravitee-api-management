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
import type { ApiCreationState, SecurityConfig } from '../features/apis/types/models';

export type StepId = 'api-details' | 'configure-proxy' | 'secure' | 'review-deploy' | 'essentials';

export type ValidationErrors = Record<string, string>;

export type ValidationResult = { success: true; errors: {} } | { success: false; errors: ValidationErrors };

const isBlank = (value: unknown) => typeof value !== 'string' || value.trim().length === 0;

const maxLen = (value: string, max: number) => value.length <= max;

export function detailsSchema(details: ApiCreationState['details']): ValidationResult {
    const errors: ValidationErrors = {};

    if (isBlank(details.name)) errors['details.name'] = 'API name is required.';
    if (isBlank(details.version)) errors['details.version'] = 'Version is required.';
    if (!isBlank(details.description) && typeof details.description === 'string' && !maxLen(details.description, 250)) {
        errors['details.description'] = 'Max 250 characters.';
    }

    return Object.keys(errors).length ? { success: false, errors } : { success: true, errors: {} };
}

export function proxySchema(proxy: ApiCreationState['proxy']): ValidationResult {
    const errors: ValidationErrors = {};

    if (proxy.enableVirtualHosts) {
        const any = Array.isArray(proxy.virtualHosts) ? proxy.virtualHosts : [];
        if (any.length === 0) errors['proxy.virtualHosts'] = 'At least one virtual host is required.';
        if (any.some(virtualHost => isBlank(virtualHost.host))) errors['proxy.virtualHosts'] = 'Virtual host is required.';
        if (any.some(virtualHost => isBlank(virtualHost.path))) errors['proxy.virtualHosts'] = 'Context-path is required.';
    } else {
        if (isBlank(proxy.contextPath)) errors['proxy.contextPath'] = 'Context path is required.';
    }
    if (isBlank(proxy.targetUrl)) errors['proxy.targetUrl'] = 'Target URL is required.';

    return Object.keys(errors).length ? { success: false, errors } : { success: true, errors: {} };
}

function securityTypeSchema(security: SecurityConfig): ValidationResult {
    const errors: ValidationErrors = {};

    if (security.type === 'keyless') return { success: true, errors: {} };

    if ('planName' in security && isBlank(security.planName)) errors['security.planName'] = 'Plan name is required.';

    if (security.type === 'jwt') {
        if (isBlank(security.signature)) errors['security.signature'] = 'Signature is required.';
        if (isBlank(security.jwksResolver)) errors['security.jwksResolver'] = 'JWKS resolver is required.';
        if (isBlank(security.resolverParam)) errors['security.resolverParam'] = 'Resolver parameter is required.';
    }

    if (security.type === 'oauth2') {
        if (isBlank(security.resource)) errors['security.resource'] = 'Resource is required.';
    }

    return Object.keys(errors).length ? { success: false, errors } : { success: true, errors: {} };
}

export function securitySchema(security: ApiCreationState['security']): ValidationResult {
    return securityTypeSchema(security);
}

export function essentialsSchema(data: ApiCreationState): ValidationResult {
    const d = detailsSchema(data.details);
    const p = proxySchema(data.proxy);
    const errors: ValidationErrors = { ...d.errors, ...p.errors };

    if (data.security.type === 'jwt' && isBlank(data.security.resolverParam)) {
        errors['security.resolverParam'] = 'JWKS URL is required.';
    }
    if (data.security.type === 'oauth2' && isBlank(data.security.resource)) {
        errors['security.resource'] = 'OAuth2 resource is required.';
    }

    return Object.keys(errors).length ? { success: false, errors } : { success: true, errors: {} };
}

export function validateStep(stepId: StepId, data: ApiCreationState): ValidationResult {
    switch (stepId) {
        case 'essentials':
            return essentialsSchema(data);
        case 'api-details':
            return detailsSchema(data.details);
        case 'configure-proxy':
            return proxySchema(data.proxy);
        case 'secure':
            return securitySchema(data.security);
        case 'review-deploy':
            return securitySchema(data.security);
    }
}
