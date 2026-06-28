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
import yaml from 'js-yaml';

import type { OpenApiSpecMetadata } from '../entities/openapi';

export interface OpenApiValidationResult {
    readonly valid: boolean;
    readonly error?: string;
    readonly metadata?: OpenApiSpecMetadata;
}

function extractMetadata(spec: Record<string, unknown>): OpenApiValidationResult {
    const info = spec.info as Record<string, unknown> | undefined;
    const paths = spec.paths as Record<string, unknown> | undefined;

    if (!info || typeof info.title !== 'string') {
        return { valid: false, error: 'Missing info.title' };
    }

    return {
        valid: true,
        metadata: {
            title: info.title,
            version: typeof info.version === 'string' ? info.version : 'unknown',
            pathsCount: paths ? Object.keys(paths).length : 0,
        },
    };
}

export function parseOpenApiContent(content: string): OpenApiValidationResult {
    const trimmed = content.trim();
    if (!trimmed) {
        return { valid: false };
    }

    try {
        const parsed = trimmed.startsWith('{')
            ? (JSON.parse(trimmed) as Record<string, unknown>)
            : (yaml.load(trimmed) as Record<string, unknown>);

        if (!parsed || typeof parsed !== 'object') {
            return { valid: false, error: 'Invalid OpenAPI document' };
        }

        if (!parsed.openapi && !parsed.swagger) {
            return { valid: false, error: 'Not an OpenAPI or Swagger document' };
        }

        return extractMetadata(parsed);
    } catch {
        return { valid: false, error: 'Failed to parse spec' };
    }
}

export function parseOpenApiSpecObject(content: string): Record<string, unknown> | undefined {
    const trimmed = content.trim();
    if (!trimmed) {
        return undefined;
    }

    try {
        return trimmed.startsWith('{')
            ? (JSON.parse(trimmed) as Record<string, unknown>)
            : (yaml.load(trimmed) as Record<string, unknown>);
    } catch {
        return undefined;
    }
}
