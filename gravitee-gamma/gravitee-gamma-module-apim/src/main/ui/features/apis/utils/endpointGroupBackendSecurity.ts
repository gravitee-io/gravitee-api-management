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
import type { ApiDetailDto, EndpointGroupSharedConfiguration } from '../types';

/** First endpoint group in the API definition — used as the default upstream group. */
export function getDefaultEndpointGroup(api: ApiDetailDto | null) {
    return api?.endpointGroups?.[0];
}

/**
 * True when the default endpoint group's shared configuration includes backend security
 * settings beyond factory defaults (proxy auth, SSL/TLS, or upstream headers).
 */
export function hasDefaultEndpointGroupBackendSecurityConfigured(api: ApiDetailDto | null): boolean {
    return hasBackendSecurityConfiguration(getDefaultEndpointGroup(api)?.sharedConfiguration);
}

function storeConfigured(store: unknown): boolean {
    if (!store || typeof store !== 'object') return false;
    const type = String((store as { type?: unknown }).type ?? '').toUpperCase();
    return type !== '' && type !== 'NONE';
}

export function hasBackendSecurityConfiguration(sharedConfiguration: EndpointGroupSharedConfiguration | undefined): boolean {
    if (!sharedConfiguration || Object.keys(sharedConfiguration).length === 0) {
        return false;
    }

    const proxy = (sharedConfiguration.proxy ?? {}) as {
        enabled?: boolean;
        host?: string;
        username?: string;
        password?: string;
    };
    const ssl = (sharedConfiguration.ssl ?? {}) as {
        hostnameVerifier?: boolean;
        trustAll?: boolean;
        trustStore?: unknown;
        keyStore?: unknown;
    };
    const headers = sharedConfiguration.headers ?? [];

    if (proxy.enabled && ((proxy.host ?? '').trim() || (proxy.username ?? '').trim() || (proxy.password ?? '').trim())) {
        return true;
    }

    if (storeConfigured(ssl.trustStore) || storeConfigured(ssl.keyStore)) {
        return true;
    }

    if (ssl.trustAll === true || ssl.hostnameVerifier === false) {
        return true;
    }

    return headers.some(h => (h.name ?? '').trim() && (h.value ?? '').trim());
}
