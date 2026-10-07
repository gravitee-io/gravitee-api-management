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
import { validateDuplicateHost } from './duplicateDialogValidation';
import { isTcpForm } from './protocol';
import type { ApiProxyDraft, TcpHostEntry, ValidationErrors } from '../types/apiCreation';

const CONTEXT_PATH_PATTERN = /^\/[/.a-zA-Z0-9\-_]*$/;

/** Matches the `maxLength` on the name and version inputs in the creation steps. */
export const API_NAME_MAX_LENGTH = 50;
export const API_VERSION_MAX_LENGTH = 32;

/** Classic accepts any non-blank target, including expression language and secrets. */
function validateTargetUrl(value: string): string | null {
    if (!value.trim()) return 'Target URL is required.';
    return null;
}

export function validateContextPath(value: string): string | null {
    const pathError = validateVirtualHostPath(value);
    if (pathError) return pathError;
    if (value.length <= 3) return 'Context path has to be more than 3 characters long.';
    return null;
}

/** Classic `virtualHostModePathPathSyncValidator`: same pattern, no minimum length. */
export function validateVirtualHostPath(value: string): string | null {
    if (!value.trim()) return 'Context path is required.';
    if (value.includes('//')) return 'Context path is not valid.';
    if (!CONTEXT_PATH_PATTERN.test(value)) return 'Context path is not valid.';
    return null;
}

/** True once the user has typed something the Cancel button should confirm before discarding. */
export function isCreationDraftDirty(form: ApiProxyDraft): boolean {
    if (form.apiName.trim() || form.apiDescription.trim()) return true;
    if (form.apiVersion.trim() !== '1.0.0') return true;
    if (form.targetUrl.trim()) return true;
    if (isTcpForm(form)) {
        return form.tcpHosts.some(host => host.host.trim() !== '') || form.tcpTargetHost.trim() !== '' || form.tcpTargetPort.trim() !== '';
    }
    if (form.virtualHostsEnabled) {
        return form.virtualHosts.some(vh => vh.host.trim() !== '' || vh.path !== '/');
    }
    return form.contextPath !== '/';
}

export function validateDetails(form: ApiProxyDraft): ValidationErrors {
    const errors: ValidationErrors = {};
    if (!form.apiName.trim()) errors['apiName'] = 'API name is required.';
    else if (form.apiName.length > API_NAME_MAX_LENGTH) errors['apiName'] = `API name must be ${API_NAME_MAX_LENGTH} characters or fewer.`;
    if (!form.apiVersion.trim()) errors['apiVersion'] = 'Version is required.';
    else if (form.apiVersion.length > API_VERSION_MAX_LENGTH)
        errors['apiVersion'] = `Version must be ${API_VERSION_MAX_LENGTH} characters or fewer.`;
    return errors;
}

export function validateTcpHosts(hosts: TcpHostEntry[]): string | null {
    // Extra rows added via "Add host" are allowed to sit blank until filled in — only rows
    // the user has actually typed into are validated as hostnames. At least one non-blank
    // host is still required overall.
    const nonEmptyHosts = hosts.filter(h => h.host.trim() !== '');
    if (nonEmptyHosts.length === 0) return 'Host is required.';

    const firstError = nonEmptyHosts.map(h => validateDuplicateHost(h.host)).find(message => message !== null);
    if (firstError) return firstError;

    const hostValues = nonEmptyHosts.map(h => h.host.trim());
    if (new Set(hostValues).size !== hostValues.length) return 'Duplicated hosts not allowed';
    return null;
}

export function validateTcpPort(value: string): string | null {
    const trimmed = value.trim();
    if (!trimmed) return 'Port is required.';
    if (!/^\d+$/.test(trimmed)) return 'Port must be a number between 0 and 65535.';
    const port = Number(trimmed);
    if (port < 0 || port > 65535) return 'Port must be a number between 0 and 65535.';
    return null;
}

function validateTcpEntrypoint(form: ApiProxyDraft): ValidationErrors {
    const errors: ValidationErrors = {};
    const hostsError = validateTcpHosts(form.tcpHosts);
    if (hostsError) errors['tcpHosts'] = hostsError;
    const targetHostError = validateDuplicateHost(form.tcpTargetHost);
    if (targetHostError) errors['tcpTargetHost'] = targetHostError;
    const portError = validateTcpPort(form.tcpTargetPort);
    if (portError) errors['tcpTargetPort'] = portError;
    return errors;
}

export function validateEntrypoints(form: ApiProxyDraft): ValidationErrors {
    if (isTcpForm(form)) return validateTcpEntrypoint(form);

    const errors: ValidationErrors = {};
    const urlError = validateTargetUrl(form.targetUrl);
    if (urlError) errors['targetUrl'] = urlError;
    if (form.virtualHostsEnabled) {
        const invalidPath = form.virtualHosts.map(vh => validateVirtualHostPath(vh.path)).find(message => message !== null);
        if (invalidPath) errors['virtualHosts'] = invalidPath;
        else if (form.virtualHosts.some(vh => !vh.host.trim())) errors['virtualHosts'] = 'All virtual hosts must have a host value.';
    } else {
        const pathError = validateContextPath(form.contextPath);
        if (pathError) errors['contextPath'] = pathError;
    }
    return errors;
}

function validateOAuth2Plan(form: ApiProxyDraft, errors: ValidationErrors): void {
    if (!form.oauth2PlanName.trim()) errors['oauth2PlanName'] = 'Plan name is required.';
    if (!form.oauth2ResourceType) errors['oauth2ResourceType'] = 'Select an OAuth2 provider.';
    else if (!form.oauth2ResourceValid) errors['oauth2ResourceConfig'] = 'Complete the OAuth2 provider configuration.';
}

export function validateSecurity(form: ApiProxyDraft): ValidationErrors {
    const errors: ValidationErrors = {};
    if (form.authType === 'api-key' && !form.apiKeyPlanName.trim()) errors['apiKeyPlanName'] = 'Plan name is required.';
    if (form.authType === 'jwt' && !form.jwtPlanName.trim()) errors['jwtPlanName'] = 'Plan name is required.';
    if (form.authType === 'jwt' && form.jwtJwksResolver !== 'GATEWAY_KEYS' && !form.jwtResolverParameter.trim()) {
        errors['jwtResolverParameter'] = form.jwtJwksResolver === 'JWKS_URL' ? 'JWKS URL is required.' : 'Public key is required.';
    }
    if (form.authType === 'oauth2') validateOAuth2Plan(form, errors);
    if (form.authType === 'mtls' && !form.mtlsPlanName.trim()) errors['mtlsPlanName'] = 'Plan name is required.';
    return errors;
}

export function validateEssentials(form: ApiProxyDraft): ValidationErrors {
    const errors: ValidationErrors = { ...validateSecurity(form) };
    if (!form.apiName.trim()) errors['apiName'] = 'API name is required.';
    if (!form.apiVersion.trim()) errors['apiVersion'] = 'Version is required.';
    const pathError = validateContextPath(form.contextPath);
    if (pathError) errors['contextPath'] = pathError;
    const urlError = validateTargetUrl(form.targetUrl);
    if (urlError) errors['targetUrl'] = urlError;
    return errors;
}
