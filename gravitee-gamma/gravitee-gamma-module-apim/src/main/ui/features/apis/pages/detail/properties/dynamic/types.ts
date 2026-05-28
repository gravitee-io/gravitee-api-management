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

import type { DynamicPropertyConfig, DynamicPropertySslStore } from '../../../../types';

// ─── HTTP method ──────────────────────────────────────────────────────────────

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS';

// ─── Header entry ─────────────────────────────────────────────────────────────

export interface HeaderEntry {
    _id: string;
    name: string;
    value: string;
}

// ─── HTTP client options ──────────────────────────────────────────────────────

export interface HttpClientFormState {
    connectTimeout: number;
    readTimeout: number;
    keepAliveTimeout: number;
    idleTimeout: number;
    maxConcurrentConnections: number;
    keepAlive: boolean;
    pipelining: boolean;
    useCompression: boolean;
    propagateClientAcceptEncoding: boolean;
    followRedirects: boolean;
}

// ─── Proxy ────────────────────────────────────────────────────────────────────

export interface ProxyFormState {
    enabled: boolean;
    useSystemProxy: boolean;
    type: 'HTTP' | 'SOCKS4' | 'SOCKS5';
    host: string;
    port: string;
    username: string;
    password: string;
}

// ─── SSL — truststore ─────────────────────────────────────────────────────────

export type TrustStoreType = 'NONE' | 'PEM' | 'JKS' | 'PKCS12';
export type KeyStoreType = 'NONE' | 'PEM' | 'JKS' | 'PKCS12';

export interface TrustStorePem {
    type: 'PEM';
    /** Supports EL, e.g. /path/to/cert.pem or {#api.properties['cert']} */
    path: string;
    content?: string;
}

export interface TrustStoreJks {
    type: 'JKS';
    path: string;
    password: string;
    alias: string;
}

export interface TrustStorePkcs12 {
    type: 'PKCS12';
    path: string;
    password: string;
    alias: string;
}

export interface TrustStoreNone {
    type: 'NONE';
}

export type TrustStoreFormState = TrustStorePem | TrustStoreJks | TrustStorePkcs12 | TrustStoreNone;

// ─── SSL — keystore ───────────────────────────────────────────────────────────

export interface KeyStorePem {
    type: 'PEM';
    /** Cert path — supports EL */
    certPath: string;
    /** Key path — supports EL */
    keyPath: string;
    keyPassword: string;
    certContent?: string;
    keyContent?: string;
}

export interface KeyStoreJks {
    type: 'JKS';
    path: string;
    password: string;
    alias: string;
}

export interface KeyStorePkcs12 {
    type: 'PKCS12';
    path: string;
    password: string;
    alias: string;
}

export interface KeyStoreNone {
    type: 'NONE';
}

export type KeyStoreFormState = KeyStorePem | KeyStoreJks | KeyStorePkcs12 | KeyStoreNone;

// ─── SSL top-level ────────────────────────────────────────────────────────────

export interface SslFormState {
    hostnameVerifier: boolean;
    trustAll: boolean;
    trustStore: TrustStoreFormState;
    keyStore: KeyStoreFormState;
}

// ─── Full form state ──────────────────────────────────────────────────────────

export interface DynamicPropertiesFormState {
    enabled: boolean;
    schedule: string;
    method: HttpMethod;
    url: string;
    headers: HeaderEntry[];
    body: string;
    specification: string;
    useSystemProxy: boolean;
    httpClient: HttpClientFormState;
    proxy: ProxyFormState;
    ssl: SslFormState;
}

// ─── Validation ───────────────────────────────────────────────────────────────

export interface FormErrors {
    url?: string;
}

export function validateForm(state: DynamicPropertiesFormState): FormErrors {
    const errors: FormErrors = {};
    if (state.enabled && !state.url.trim()) errors.url = 'URL is required.';
    return errors;
}

export function hasErrors(errors: FormErrors): boolean {
    return Object.values(errors).some(Boolean);
}

// ─── Defaults ─────────────────────────────────────────────────────────────────

export const DEFAULT_JOLT = `[
    {
        "operation": "default",
        "spec": {}
    }
]`;

export const DEFAULT_SCHEDULE = '0 */5 * * * *';

export const DEFAULT_HTTP_CLIENT: HttpClientFormState = {
    connectTimeout: 5000,
    readTimeout: 10000,
    keepAliveTimeout: 30000,
    idleTimeout: 60000,
    maxConcurrentConnections: 10,
    keepAlive: true,
    pipelining: false,
    useCompression: false,
    propagateClientAcceptEncoding: false,
    followRedirects: false,
};

export const DEFAULT_PROXY: ProxyFormState = {
    enabled: false,
    useSystemProxy: false,
    type: 'HTTP',
    host: '',
    port: '',
    username: '',
    password: '',
};

export const DEFAULT_SSL: SslFormState = {
    hostnameVerifier: true,
    trustAll: false,
    trustStore: { type: 'NONE' },
    keyStore: { type: 'NONE' },
};

export const DEFAULT_FORM: DynamicPropertiesFormState = {
    enabled: false,
    schedule: DEFAULT_SCHEDULE,
    method: 'GET',
    url: '',
    headers: [],
    body: '',
    specification: DEFAULT_JOLT,
    useSystemProxy: false,
    httpClient: DEFAULT_HTTP_CLIENT,
    proxy: DEFAULT_PROXY,
    ssl: DEFAULT_SSL,
};

// ─── DTO → form state ─────────────────────────────────────────────────────────

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function parseTrustStore(raw: any): TrustStoreFormState {
    if (!raw) return { type: 'NONE' };
    const t = (raw.type as string)?.toUpperCase();
    if (t === 'PEM') return { type: 'PEM', path: raw.path ?? '', content: raw.content };
    if (t === 'JKS') return { type: 'JKS', path: raw.path ?? '', password: raw.password ?? '', alias: raw.alias ?? '' };
    if (t === 'PKCS12') return { type: 'PKCS12', path: raw.path ?? '', password: raw.password ?? '', alias: raw.alias ?? '' };
    return { type: 'NONE' };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function parseKeyStore(raw: any): KeyStoreFormState {
    if (!raw) return { type: 'NONE' };
    const t = (raw.type as string)?.toUpperCase();
    if (t === 'PEM')
        return {
            type: 'PEM',
            certPath: raw.certPath ?? raw.certFile ?? '',
            keyPath: raw.keyPath ?? raw.keyFile ?? '',
            keyPassword: raw.keyPassword ?? '',
            certContent: raw.certContent,
            keyContent: raw.keyContent,
        };
    if (t === 'JKS') return { type: 'JKS', path: raw.path ?? '', password: raw.password ?? '', alias: raw.alias ?? '' };
    if (t === 'PKCS12') return { type: 'PKCS12', path: raw.path ?? '', password: raw.password ?? '', alias: raw.alias ?? '' };
    return { type: 'NONE' };
}

export function fromDtoToFormState(dto: DynamicPropertyConfig): DynamicPropertiesFormState {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const cfg: any = dto.configuration ?? {};
    const httpClientOpts = cfg.httpClientOptions ?? {};
    const httpProxyOpts = cfg.httpProxyOptions ?? {};
    const sslOpts = cfg.sslOptions ?? {};

    const headers: HeaderEntry[] = (cfg.headers ?? []).map(
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (h: any) => ({
            _id: crypto.randomUUID(),
            name: typeof h === 'object' ? (h.name ?? '') : '',
            value: typeof h === 'object' ? (h.value ?? '') : '',
        }),
    );

    return {
        enabled: dto.enabled,
        schedule: dto.schedule ?? DEFAULT_SCHEDULE,
        method: (cfg.method as HttpMethod) ?? 'GET',
        url: cfg.url ?? '',
        headers,
        body: cfg.body ?? '',
        specification: cfg.specification ?? DEFAULT_JOLT,
        useSystemProxy: cfg.useSystemProxy ?? false,
        httpClient: {
            connectTimeout: httpClientOpts.connectTimeout ?? DEFAULT_HTTP_CLIENT.connectTimeout,
            readTimeout: httpClientOpts.readTimeout ?? DEFAULT_HTTP_CLIENT.readTimeout,
            keepAliveTimeout: httpClientOpts.keepAliveTimeout ?? DEFAULT_HTTP_CLIENT.keepAliveTimeout,
            idleTimeout: httpClientOpts.idleTimeout ?? DEFAULT_HTTP_CLIENT.idleTimeout,
            maxConcurrentConnections: httpClientOpts.maxConcurrentConnections ?? DEFAULT_HTTP_CLIENT.maxConcurrentConnections,
            keepAlive: httpClientOpts.keepAlive ?? DEFAULT_HTTP_CLIENT.keepAlive,
            pipelining: httpClientOpts.pipelining ?? DEFAULT_HTTP_CLIENT.pipelining,
            useCompression: httpClientOpts.useCompression ?? DEFAULT_HTTP_CLIENT.useCompression,
            propagateClientAcceptEncoding:
                httpClientOpts.propagateClientAcceptEncoding ?? DEFAULT_HTTP_CLIENT.propagateClientAcceptEncoding,
            followRedirects: httpClientOpts.followRedirects ?? DEFAULT_HTTP_CLIENT.followRedirects,
        },
        proxy: {
            enabled: httpProxyOpts.enabled ?? false,
            useSystemProxy: httpProxyOpts.useSystemProxy ?? false,
            type: httpProxyOpts.type ?? 'HTTP',
            host: httpProxyOpts.host ?? '',
            port: httpProxyOpts.port !== undefined ? String(httpProxyOpts.port) : '',
            username: httpProxyOpts.username ?? '',
            password: httpProxyOpts.password ?? '',
        },
        ssl: {
            hostnameVerifier: sslOpts.hostnameVerifier ?? true,
            trustAll: sslOpts.trustAll ?? false,
            trustStore: parseTrustStore(sslOpts.trustStore),
            keyStore: parseKeyStore(sslOpts.keyStore),
        },
    };
}

// ─── Form state → DTO ─────────────────────────────────────────────────────────

function serializeTrustStore(ts: TrustStoreFormState): DynamicPropertySslStore | undefined {
    if (ts.type === 'NONE') return undefined;
    if (ts.type === 'PEM') return { type: 'PEM', path: ts.path || undefined, content: ts.content || undefined };
    return { type: ts.type, path: ts.path || undefined, password: ts.password || undefined, alias: ts.alias || undefined };
}

function serializeKeyStore(ks: KeyStoreFormState): DynamicPropertySslStore | undefined {
    if (ks.type === 'NONE') return undefined;
    if (ks.type === 'PEM')
        return {
            type: 'PEM',
            certPath: ks.certPath || undefined,
            keyPath: ks.keyPath || undefined,
            keyPassword: ks.keyPassword || undefined,
            certContent: ks.certContent || undefined,
            keyContent: ks.keyContent || undefined,
        };
    return { type: ks.type, path: ks.path || undefined, password: ks.password || undefined, alias: ks.alias || undefined };
}

export function fromFormStateToDto(state: DynamicPropertiesFormState): DynamicPropertyConfig {
    const trustStore = serializeTrustStore(state.ssl.trustStore);
    const keyStore = serializeKeyStore(state.ssl.keyStore);

    return {
        enabled: state.enabled,
        provider: 'HTTP',
        schedule: state.schedule,
        configuration: {
            method: state.method,
            url: state.url,
            headers: state.headers.filter(h => h.name.trim()).map(h => ({ name: h.name, value: h.value })),
            body: state.body || undefined,
            specification: state.specification,
            useSystemProxy: state.useSystemProxy,
            httpClientOptions: {
                connectTimeout: state.httpClient.connectTimeout,
                readTimeout: state.httpClient.readTimeout,
                keepAliveTimeout: state.httpClient.keepAliveTimeout,
                idleTimeout: state.httpClient.idleTimeout,
                maxConcurrentConnections: state.httpClient.maxConcurrentConnections,
                keepAlive: state.httpClient.keepAlive,
                pipelining: state.httpClient.pipelining,
                useCompression: state.httpClient.useCompression,
                propagateClientAcceptEncoding: state.httpClient.propagateClientAcceptEncoding,
                followRedirects: state.httpClient.followRedirects,
            },
            httpProxyOptions: state.proxy.enabled
                ? {
                      enabled: true,
                      useSystemProxy: state.proxy.useSystemProxy,
                      type: state.proxy.useSystemProxy ? undefined : state.proxy.type,
                      host: state.proxy.useSystemProxy ? undefined : state.proxy.host || undefined,
                      port: state.proxy.useSystemProxy ? undefined : state.proxy.port ? Number(state.proxy.port) : undefined,
                      username: state.proxy.useSystemProxy ? undefined : state.proxy.username || undefined,
                      password: state.proxy.useSystemProxy ? undefined : state.proxy.password || undefined,
                  }
                : { enabled: false },
            sslOptions: {
                hostnameVerifier: state.ssl.hostnameVerifier,
                trustAll: state.ssl.trustAll,
                trustStore: trustStore ?? null,
                keyStore: keyStore ?? null,
            },
        },
    };
}

export function newHeaderRow(): HeaderEntry {
    return { _id: crypto.randomUUID(), name: '', value: '' };
}
