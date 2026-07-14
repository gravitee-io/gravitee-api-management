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
import type { ParsedOperation } from './openapi-spec-utils';

export interface TryItResponse {
    readonly status: number;
    readonly statusText: string;
    readonly durationMs: number;
    readonly body: string;
    readonly ok: boolean;
}

export function buildRequestUrl(
    operation: ParsedOperation,
    serverUrl: string,
    pathValues: Record<string, string>,
    queryValues: Record<string, string>,
): string {
    let path = operation.path;
    for (const [name, value] of Object.entries(pathValues)) {
        path = path.replace(`{${name}}`, encodeURIComponent(value));
    }

    const query = new URLSearchParams();
    for (const [name, value] of Object.entries(queryValues)) {
        if (value) {
            query.set(name, value);
        }
    }

    const normalizedServer = serverUrl.replace(/\/$/, '');
    const queryString = query.toString();
    return `${normalizedServer}${path}${queryString ? `?${queryString}` : ''}`;
}

function getJsonContentType(operation: ParsedOperation): string | undefined {
    const content = operation.requestBody?.content;
    if (!content) {
        return undefined;
    }
    if (content['application/json']) {
        return 'application/json';
    }
    return Object.keys(content)[0];
}

export async function executeTryItRequest(
    operation: ParsedOperation,
    serverUrl: string,
    pathValues: Record<string, string>,
    queryValues: Record<string, string>,
    headerValues: Record<string, string>,
    body: string,
    authType: string,
    authValue: string,
): Promise<TryItResponse> {
    const url = buildRequestUrl(operation, serverUrl, pathValues, queryValues);
    const headers = new Headers(headerValues);

    const contentType = getJsonContentType(operation);
    if (contentType && body.trim()) {
        headers.set('Content-Type', contentType);
    }

    if (authType === 'bearer' && authValue.trim()) {
        headers.set('Authorization', `Bearer ${authValue.trim()}`);
    }
    if (authType === 'apiKey' && authValue.trim()) {
        headers.set('X-API-Key', authValue.trim());
    }

    const startedAt = performance.now();
    const response = await fetch(url, {
        method: operation.method.toUpperCase(),
        headers,
        body: ['GET', 'HEAD'].includes(operation.method.toUpperCase()) ? undefined : body || undefined,
    });
    const durationMs = Math.round(performance.now() - startedAt);
    const text = await response.text();

    return {
        status: response.status,
        statusText: response.statusText,
        durationMs,
        body: text,
        ok: response.ok,
    };
}
