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
import type { EndpointDto, EndpointGroupSharedConfiguration, LoadBalancerType } from '../../../types';
import type { HealthCheckFormState } from '../../../utils/healthCheckForm';
import { healthCheckFromGroup } from '../../../utils/healthCheckForm';

export type { LoadBalancerType };

export interface EndpointFormState {
    /** Local row id for React key management — not sent to backend. */
    _id: string;
    name: string;
    target: string;
    weight: number;
    backup: boolean;
    inheritConfiguration: boolean;
    tenants: string[];
    /**
     * Original backend DTO — preserved so that fields we don't manage in the form
     * (services, secondary, etc.) survive a save round-trip.
     */
    _originalDto?: EndpointDto;
    /**
     * Per-endpoint shared config override — only applied when inheritConfiguration is false.
     * Populated from the endpoint's sharedConfigurationOverride DTO on load; updated by the
     * Configuration step in EndpointForm.
     */
    _configOverride?: SharedConfigFormState;
    healthCheck: HealthCheckFormState;
}

export interface ProxyFormState {
    enabled: boolean;
    useSystemProxy: boolean;
    host: string;
    port: string;
    username: string;
    password: string;
    type: string;
}

export interface HttpFormState {
    keepAlive: boolean;
    keepAliveTimeout: number;
    readTimeout: number;
    idleTimeout: number;
    connectTimeout: number;
    maxConcurrentConnections: number;
    useCompression: boolean;
    propagateClientAcceptEncoding: boolean;
    /** V4 schema field name (`propagateClientHost`, not `propagateClientHostHeader`). */
    propagateClientHost: boolean;
    pipelining: boolean;
    followRedirects: boolean;
    version: 'HTTP_1_1' | 'HTTP_2';
    clearTextUpgrade: boolean;
    http2MultiplexingLimit: number;
    http2ConnectionWindowSize: number;
    http2StreamWindowSize: number;
    http2MaxFrameSize: number;
}

export interface SslFormState {
    hostnameVerifier: boolean;
    trustAll: boolean;
    clientAuthentication: 'NONE' | 'REQUIRED' | 'OPTIONAL';
}

export interface HeaderEntry {
    _id: string;
    name: string;
    value: string;
}

export interface SharedConfigFormState {
    proxy: ProxyFormState;
    http: HttpFormState;
    ssl: SslFormState;
    headers: HeaderEntry[];
}

export interface EndpointGroupFormState {
    name: string;
    loadBalancerType: LoadBalancerType;
    sharedConfig: SharedConfigFormState;
    healthCheck: HealthCheckFormState;
    endpoints: EndpointFormState[];
    /** Required when creating a group — seeds the default endpoint (classic configuration.target). */
    defaultEndpointTarget?: string;
}

export const DEFAULT_HTTP: HttpFormState = {
    keepAlive: true,
    keepAliveTimeout: 30000,
    readTimeout: 10000,
    idleTimeout: 60000,
    connectTimeout: 5000,
    maxConcurrentConnections: 20,
    useCompression: true,
    propagateClientAcceptEncoding: false,
    propagateClientHost: false,
    pipelining: false,
    followRedirects: false,
    version: 'HTTP_1_1',
    clearTextUpgrade: true,
    http2MultiplexingLimit: -1,
    http2ConnectionWindowSize: -1,
    http2StreamWindowSize: -1,
    http2MaxFrameSize: 16384,
};

export const DEFAULT_PROXY: ProxyFormState = {
    enabled: false,
    useSystemProxy: false,
    host: '',
    port: '',
    username: '',
    password: '',
    type: 'HTTP',
};

export const DEFAULT_SSL: SslFormState = {
    hostnameVerifier: true,
    trustAll: false,
    clientAuthentication: 'NONE',
};

export const DEFAULT_SHARED_CONFIG: SharedConfigFormState = {
    proxy: DEFAULT_PROXY,
    http: DEFAULT_HTTP,
    ssl: DEFAULT_SSL,
    headers: [],
};

export const DEFAULT_GROUP_FORM: EndpointGroupFormState = {
    name: '',
    loadBalancerType: 'ROUND_ROBIN',
    sharedConfig: DEFAULT_SHARED_CONFIG,
    healthCheck: healthCheckFromGroup(undefined),
    endpoints: [],
};

/** Convert a DTO shared-configuration object (group's or endpoint-override's) to form state. */
export function parseSharedConfigDto(sc: EndpointGroupSharedConfiguration): SharedConfigFormState {
    const http = sc.http ?? {};
    const proxy = sc.proxy ?? {};
    const ssl = sc.ssl ?? {};
    return {
        http: {
            keepAlive: http.keepAlive ?? true,
            keepAliveTimeout: http.keepAliveTimeout ?? 30000,
            readTimeout: http.readTimeout ?? 10000,
            idleTimeout: http.idleTimeout ?? 60000,
            connectTimeout: http.connectTimeout ?? 5000,
            maxConcurrentConnections: http.maxConcurrentConnections ?? 20,
            useCompression: http.useCompression ?? true,
            propagateClientAcceptEncoding: http.propagateClientAcceptEncoding ?? false,
            propagateClientHost:
                http.propagateClientHost ?? (http as { propagateClientHostHeader?: boolean }).propagateClientHostHeader ?? false,
            pipelining: http.pipelining ?? false,
            followRedirects: http.followRedirects ?? false,
            version: http.version ?? 'HTTP_1_1',
            clearTextUpgrade: http.clearTextUpgrade ?? true,
            http2MultiplexingLimit: http.http2MultiplexingLimit ?? -1,
            http2ConnectionWindowSize: http.http2ConnectionWindowSize ?? -1,
            http2StreamWindowSize: http.http2StreamWindowSize ?? -1,
            http2MaxFrameSize: http.http2MaxFrameSize ?? 16384,
        },
        proxy: {
            enabled: proxy.enabled ?? false,
            useSystemProxy: proxy.useSystemProxy ?? false,
            host: proxy.host ?? '',
            port: proxy.port !== undefined ? String(proxy.port) : '',
            username: proxy.username ?? '',
            password: proxy.password ?? '',
            type: proxy.type ?? 'HTTP',
        },
        ssl: {
            hostnameVerifier: ssl.hostnameVerifier ?? true,
            trustAll: ssl.trustAll ?? false,
            clientAuthentication: ssl.clientAuthentication ?? 'NONE',
        },
        headers: (sc.headers ?? []).map(h => ({
            _id: Math.random().toString(36).slice(2, 10),
            name: h.name,
            value: h.value,
        })),
    };
}

export function newEndpointRow(groupHealthCheck?: HealthCheckFormState): EndpointFormState {
    const groupHc = groupHealthCheck ?? healthCheckFromGroup(undefined);
    return {
        _id: Math.random().toString(36).slice(2, 10),
        name: '',
        target: '',
        weight: 1,
        backup: false,
        inheritConfiguration: true,
        tenants: [],
        healthCheck: {
            enabled: groupHc.enabled,
            inherit: true,
            configuration: { ...groupHc.configuration },
        },
    };
}

export function newHeaderRow(): HeaderEntry {
    return { _id: Math.random().toString(36).slice(2, 10), name: '', value: '' };
}

/** Validate group name — classic console: required, no colons (Validators.pattern /^[^:]*$/). */
export function validateGroupName(name: string): string | null {
    if (!name.trim()) return 'Name is required.';
    if (name.includes(':')) return 'Name must not contain colons.';
    return null;
}

/** Validate endpoint name — classic console: required, no colons. */
export function validateEndpointName(name: string): string | null {
    if (!name.trim()) return 'Name is required.';
    if (name.includes(':')) return 'Name must not contain colons.';
    return null;
}

/** http-proxy endpoint configuration requires a non-empty target (plugin schema). */
export function validateEndpointTarget(value: string): string | null {
    if (!value.trim()) return 'Target URL is required.';
    if (/\s/.test(value)) return 'Target URL must not contain whitespace.';
    return null;
}

/** Default endpoint for a new group — classic adds `configuration: { target }` on create. */
export function buildDefaultEndpointForGroup(groupName: string, target: string, type = 'http-proxy'): EndpointDto {
    const cleanName = groupName.trim();
    return {
        name: `${cleanName} default endpoint`,
        type,
        inheritConfiguration: true,
        weight: 1,
        configuration: { target: target.trim() },
    };
}
