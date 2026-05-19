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
import type { ApiProxyDraft, ValidationErrors } from '../types/apiCreation';

const CONTEXT_PATH_PATTERN = /^\/[/.a-zA-Z0-9\-_]*$/;

function validateTargetUrl(value: string): string | null {
    const trimmed = value.trim();
    if (!trimmed) return 'Target URL is required.';
    try {
        const url = new URL(trimmed);
        if (url.protocol !== 'http:' && url.protocol !== 'https:') return 'Target URL must start with http:// or https://.';
    } catch {
        return 'Target URL must be a valid URL (e.g. https://api.example.com).';
    }
    return null;
}

export function validateContextPath(value: string): string | null {
    if (!value.trim()) return 'Context path is required.';
    if (value.includes('//')) return 'Context path is not valid.';
    if (!CONTEXT_PATH_PATTERN.test(value)) return 'Context path is not valid.';
    if (value.length <= 3) return 'Context path has to be more than 3 characters long.';
    return null;
}

export function validateDetails(form: ApiProxyDraft): ValidationErrors {
    const errors: ValidationErrors = {};
    if (!form.apiName.trim()) errors['apiName'] = 'API name is required.';
    if (!form.apiVersion.trim()) errors['apiVersion'] = 'Version is required.';
    return errors;
}

export function validateEntrypoints(form: ApiProxyDraft): ValidationErrors {
    const errors: ValidationErrors = {};
    const urlError = validateTargetUrl(form.targetUrl);
    if (urlError) errors['targetUrl'] = urlError;
    if (form.virtualHostsEnabled) {
        const hasEmptyHost = form.virtualHosts.some(vh => !vh.host.trim());
        if (hasEmptyHost) errors['virtualHosts'] = 'All virtual hosts must have a host value.';
    } else {
        const pathError = validateContextPath(form.contextPath);
        if (pathError) errors['contextPath'] = pathError;
    }
    return errors;
}

export function validateSecurity(form: ApiProxyDraft): ValidationErrors {
    const errors: ValidationErrors = {};
    if (form.authType === 'api-key' && !form.apiKeyPlanName.trim()) errors['apiKeyPlanName'] = 'Plan name is required.';
    if (form.authType === 'jwt' && !form.jwtPlanName.trim()) errors['jwtPlanName'] = 'Plan name is required.';
    if (form.authType === 'oauth2' && !form.oauth2PlanName.trim()) errors['oauth2PlanName'] = 'Plan name is required.';
    if (form.authType === 'mtls' && !form.mtlsPlanName.trim()) errors['mtlsPlanName'] = 'Plan name is required.';
    return errors;
}

export function validateEssentials(form: ApiProxyDraft): ValidationErrors {
    const errors: ValidationErrors = {};
    if (!form.apiName.trim()) errors['apiName'] = 'API name is required.';
    if (!form.apiVersion.trim()) errors['apiVersion'] = 'Version is required.';
    const pathError = validateContextPath(form.contextPath);
    if (pathError) errors['contextPath'] = pathError;
    const urlError = validateTargetUrl(form.targetUrl);
    if (urlError) errors['targetUrl'] = urlError;
    return errors;
}
