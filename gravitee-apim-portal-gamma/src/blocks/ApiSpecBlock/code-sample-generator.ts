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
import type { OpenAPIV3 } from 'openapi-types';

import { getDefaultServerUrl } from './openapi-spec-utils';
import type { ParsedOperation } from './openapi-spec-utils';

export type CodeSampleLanguage = 'curl' | 'javascript' | 'python';

export const CODE_SAMPLE_LANGUAGES: readonly CodeSampleLanguage[] = ['curl', 'javascript', 'python'] as const;

export const CODE_SAMPLE_LABELS: Record<CodeSampleLanguage, string> = {
    curl: 'cURL',
    javascript: 'JavaScript',
    python: 'Python',
};

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

function getExampleBody(operation: ParsedOperation): string | undefined {
    const contentType = getJsonContentType(operation);
    if (!contentType) {
        return undefined;
    }
    const media = operation.requestBody?.content?.[contentType];
    if (!media) {
        return undefined;
    }
    if (media.example !== undefined) {
        return JSON.stringify(media.example, null, 2);
    }
    const schema = media.schema as OpenAPIV3.SchemaObject | undefined;
    if (schema?.example !== undefined) {
        return JSON.stringify(schema.example, null, 2);
    }
    return '{\n  \n}';
}

function buildUrl(serverUrl: string, path: string): string {
    const normalizedServer = serverUrl.replace(/\/$/, '');
    return `${normalizedServer}${path}`;
}

export function generateCodeSample(
    document: OpenAPIV3.Document,
    operation: ParsedOperation,
    language: CodeSampleLanguage,
    serverUrlOverride = '',
): string {
    const serverUrl = getDefaultServerUrl(document, serverUrlOverride);
    const url = buildUrl(serverUrl, operation.path);
    const body = getExampleBody(operation);
    const contentType = getJsonContentType(operation);

    switch (language) {
        case 'curl': {
            const parts = [`curl -X ${operation.method.toUpperCase()} '${url}'`];
            if (contentType) {
                parts.push(`-H 'Content-Type: ${contentType}'`);
            }
            if (body) {
                parts.push(`-d '${body.replace(/'/g, "'\\''")}'`);
            }
            return parts.join(' \\\n  ');
        }
        case 'javascript': {
            const options: string[] = [`method: '${operation.method.toUpperCase()}'`];
            const headers: string[] = [];
            if (contentType) {
                headers.push(`'Content-Type': '${contentType}'`);
            }
            if (headers.length > 0) {
                options.push(`headers: {\n    ${headers.join(',\n    ')}\n  }`);
            }
            if (body) {
                options.push(`body: JSON.stringify(${body})`);
            }
            return `const response = await fetch('${url}', {\n  ${options.join(',\n  ')}\n});\n\nconst data = await response.json();\nconsole.log(data);`;
        }
        case 'python':
            return [
                'import requests',
                '',
                `response = requests.${operation.method}(`,
                `    '${url}',`,
                contentType ? `    headers={'Content-Type': '${contentType}'},` : undefined,
                body ? `    json=${body.replace(/\n/g, '\n    ')},` : undefined,
                ')',
                'print(response.json())',
            ]
                .filter(Boolean)
                .join('\n');
        default:
            return '';
    }
}
