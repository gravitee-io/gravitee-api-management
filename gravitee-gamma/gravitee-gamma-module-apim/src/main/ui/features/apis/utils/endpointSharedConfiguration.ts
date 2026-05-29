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
import type { HttpFormState, ProxyFormState, SharedConfigFormState, SslFormState } from '../pages/detail/endpoints/types';
import type {
    EndpointGroupHeader,
    EndpointGroupHttp,
    EndpointGroupProxy,
    EndpointGroupSharedConfiguration,
    EndpointGroupSsl,
} from '../types';

/** Serializes HTTP client options for V4 shared configuration (plugin httpClientOptions oneOf schema). */
export function serializeHttpClientOptions(http: HttpFormState): EndpointGroupHttp {
    const base = {
        version: http.version,
        keepAlive: http.keepAlive,
        keepAliveTimeout: http.keepAliveTimeout,
        connectTimeout: http.connectTimeout,
        pipelining: http.pipelining,
        readTimeout: http.readTimeout,
        useCompression: http.useCompression,
        propagateClientAcceptEncoding: http.propagateClientAcceptEncoding,
        propagateClientHost: http.propagateClientHost,
        idleTimeout: http.idleTimeout,
        followRedirects: http.followRedirects,
        maxConcurrentConnections: http.maxConcurrentConnections,
    };

    if (http.version === 'HTTP_2') {
        return {
            ...base,
            clearTextUpgrade: http.clearTextUpgrade,
            http2MultiplexingLimit: http.http2MultiplexingLimit,
            http2ConnectionWindowSize: http.http2ConnectionWindowSize,
            http2StreamWindowSize: http.http2StreamWindowSize,
            http2MaxFrameSize: http.http2MaxFrameSize,
        };
    }

    return base;
}

/**
 * Serializes HTTP proxy options for V4 shared configuration (gravitee-plugin-common-configurations oneOf schema).
 * When disabled or using system proxy, only `enabled` and `useSystemProxy` are sent (additionalProperties: false).
 */
export function serializeHttpProxyOptions(proxy: ProxyFormState): EndpointGroupProxy {
    if (!proxy.enabled) {
        return { enabled: false, useSystemProxy: false };
    }
    if (proxy.useSystemProxy) {
        return { enabled: true, useSystemProxy: true };
    }

    const port = proxy.port !== '' ? parseInt(proxy.port, 10) : undefined;
    return {
        enabled: true,
        useSystemProxy: false,
        type: proxy.type,
        host: proxy.host.trim(),
        port: port !== undefined && !Number.isNaN(port) ? port : undefined,
        ...(proxy.username.trim() ? { username: proxy.username.trim() } : {}),
        ...(proxy.password ? { password: proxy.password } : {}),
    };
}

/** Serializes SSL options — only schema-allowed top-level fields; preserves trust/key stores from existing DTO. */
export function serializeSslOptions(ssl: SslFormState, existing?: EndpointGroupSsl): EndpointGroupSsl {
    return {
        hostnameVerifier: ssl.hostnameVerifier,
        trustAll: ssl.trustAll,
        ...(existing?.trustStore ? { trustStore: existing.trustStore } : {}),
        ...(existing?.keyStore ? { keyStore: existing.keyStore } : {}),
    };
}

export function validateHttpProxyOptions(proxy: ProxyFormState): string | null {
    if (!proxy.enabled || proxy.useSystemProxy) return null;
    if (!proxy.host.trim()) return 'Proxy host is required when proxy is enabled.';
    const port = proxy.port !== '' ? parseInt(proxy.port, 10) : NaN;
    if (proxy.port === '' || Number.isNaN(port) || port <= 0) return 'Proxy port is required when proxy is enabled.';
    return null;
}

export function serializeSharedConfiguration(
    config: SharedConfigFormState,
    existing?: EndpointGroupSharedConfiguration,
): EndpointGroupSharedConfiguration {
    return {
        http: serializeHttpClientOptions(config.http),
        ssl: serializeSslOptions(config.ssl, existing?.ssl),
        headers: config.headers.filter(h => h.name.trim()).map(h => ({ name: h.name, value: h.value }) as EndpointGroupHeader),
        proxy: serializeHttpProxyOptions(config.proxy),
    };
}

export function serializeSharedConfigurationOverride(
    config: SharedConfigFormState | undefined,
    existing?: EndpointGroupSharedConfiguration,
): Record<string, unknown> {
    if (!config) return {};
    return serializeSharedConfiguration(config, existing) as Record<string, unknown>;
}
