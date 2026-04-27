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
import { createContext, useContext, useMemo, type ReactNode } from 'react';

export type ApimRuntimeConfig = {
    /**
     * Management REST root without version segment, e.g. `https://host:8083/management`.
     * v2 calls append `/v2/...` per Management API OpenAPI (`/management/v2/environments/...`, `/management/v2/organizations/.../plugins/...`).
     */
    readonly managementBaseURL: string;
    readonly organizationId: string;
    readonly environmentId: string;
};

declare global {
    interface Window {
        __GAMMA_APIM_RUNTIME__?: ApimRuntimeConfig;
    }
}

const ApimRuntimeContext = createContext<ApimRuntimeConfig | null>(null);

/** Standalone / federated-without-host default: Management API on local dev port (no dev-server proxy). */
export const DEFAULT_STANDALONE_MANAGEMENT_BASE_URL = 'http://localhost:8083/management';

function stripTrailingSlash(url: string): string {
    return url.replace(/\/+$/, '');
}

/**
 * Environment-scoped Management API v2 base (`openapi-apis.yaml` server `/management/v2` + paths `/environments/{envId}/...`).
 * Example: `{managementBaseURL}/v2/environments/DEFAULT` → POST `.../apis`, `.../apis/_verify/paths`, etc.
 */
export function getEnvironmentV2BaseUrl(config: ApimRuntimeConfig): string {
    const base = stripTrailingSlash(config.managementBaseURL);
    return `${base}/v2/environments/${config.environmentId}`;
}

/**
 * Organization-scoped Management API v2 base for plugins (`openapi-plugins.yaml` server `/management/v2/organizations/{orgId}`).
 * Example: `{managementBaseURL}/v2/organizations/DEFAULT` → GET `.../plugins/entrypoints`, `.../plugins/endpoints/{id}`, etc.
 */
export function getOrganizationV2BaseUrl(config: ApimRuntimeConfig): string {
    const base = stripTrailingSlash(config.managementBaseURL);
    return `${base}/v2/organizations/${config.organizationId}`;
}

/**
 * Standalone dev default: direct Management API (`DEFAULT_STANDALONE_MANAGEMENT_BASE_URL`).
 * Host apps should pass `ApimRuntimeProvider` `value` or set `window.__GAMMA_APIM_RUNTIME__`.
 */
export function getDefaultStandaloneApimRuntime(): ApimRuntimeConfig {
    if (typeof window !== 'undefined' && window.__GAMMA_APIM_RUNTIME__) {
        return {
            ...window.__GAMMA_APIM_RUNTIME__,
            managementBaseURL: stripTrailingSlash(window.__GAMMA_APIM_RUNTIME__.managementBaseURL),
        };
    }
    return {
        managementBaseURL: stripTrailingSlash(DEFAULT_STANDALONE_MANAGEMENT_BASE_URL),
        organizationId: 'DEFAULT',
        environmentId: 'DEFAULT',
    };
}

export function ApimRuntimeProvider({
    value,
    children,
}: Readonly<{ value?: ApimRuntimeConfig; children: ReactNode }>) {
    const resolved = useMemo(
        () =>
            value ?? {
                ...getDefaultStandaloneApimRuntime(),
            },
        [value],
    );
    const normalized = useMemo(
        () => ({
            ...resolved,
            managementBaseURL: stripTrailingSlash(resolved.managementBaseURL),
        }),
        [resolved],
    );
    return <ApimRuntimeContext.Provider value={normalized}>{children}</ApimRuntimeContext.Provider>;
}

export function useApimRuntime(): ApimRuntimeConfig {
    const ctx = useContext(ApimRuntimeContext);
    if (ctx) return ctx;
    return getDefaultStandaloneApimRuntime();
}
