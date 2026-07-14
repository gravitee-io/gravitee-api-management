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

export type CodeSampleLanguage = 'curl' | 'python' | 'node' | 'javascript' | 'java' | 'go';

export const CODE_SAMPLE_LANGUAGES: readonly CodeSampleLanguage[] = [
    'curl',
    'python',
    'node',
    'javascript',
    'java',
    'go',
] as const;

export const CODE_SAMPLE_LABELS: Record<CodeSampleLanguage, string> = {
    curl: 'Shell',
    python: 'Python',
    node: 'Node',
    javascript: 'JS',
    java: 'Java',
    go: 'Go',
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
        case 'node': {
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
            return `// Node.js 18+\nconst response = await fetch('${url}', {\n  ${options.join(',\n  ')}\n});\n\nconst data = await response.json();\nconsole.log(data);`;
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
        case 'java': {
            const method = operation.method.toUpperCase();
            const escapedBody = body?.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
            const requestBuilder = [
                'HttpRequest request = HttpRequest.newBuilder()',
                `    .uri(URI.create("${url}"))`,
            ];

            if (contentType) {
                requestBuilder.push(`    .header("Content-Type", "${contentType}")`);
            }

            if (escapedBody && !['GET', 'HEAD'].includes(method)) {
                const bodyPublisher = `HttpRequest.BodyPublishers.ofString("${escapedBody}")`;
                if (method === 'POST') {
                    requestBuilder.push(`    .POST(${bodyPublisher})`);
                } else if (method === 'PUT') {
                    requestBuilder.push(`    .PUT(${bodyPublisher})`);
                } else {
                    requestBuilder.push(`    .method("${method}", ${bodyPublisher})`);
                }
            } else if (method === 'POST') {
                requestBuilder.push('    .POST(HttpRequest.BodyPublishers.noBody())');
            } else if (method === 'PUT') {
                requestBuilder.push('    .PUT(HttpRequest.BodyPublishers.noBody())');
            } else if (['GET', 'DELETE', 'HEAD'].includes(method)) {
                requestBuilder.push(`    .${method}()`);
            } else {
                requestBuilder.push(`    .method("${method}", HttpRequest.BodyPublishers.noBody())`);
            }

            requestBuilder.push('    .build();');

            return [
                'import java.net.URI;',
                'import java.net.http.HttpClient;',
                'import java.net.http.HttpRequest;',
                'import java.net.http.HttpResponse;',
                '',
                'HttpClient client = HttpClient.newHttpClient();',
                ...requestBuilder,
                'HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());',
                'System.out.println(response.body());',
            ].join('\n');
        }
        case 'go': {
            const method = operation.method.toUpperCase();
            if (body && !['GET', 'HEAD'].includes(method)) {
                return [
                    'package main',
                    '',
                    'import (',
                    '    "fmt"',
                    '    "io"',
                    '    "net/http"',
                    '    "strings"',
                    ')',
                    '',
                    'func main() {',
                    `    body := strings.NewReader(\`${body}\`)`,
                    `    req, err := http.NewRequest("${method}", "${url}", body)`,
                    '    if err != nil {',
                    '        panic(err)',
                    '    }',
                    contentType ? `    req.Header.Set("Content-Type", "${contentType}")` : undefined,
                    '    resp, err := http.DefaultClient.Do(req)',
                    '    if err != nil {',
                    '        panic(err)',
                    '    }',
                    '    defer resp.Body.Close()',
                    '    responseBody, err := io.ReadAll(resp.Body)',
                    '    if err != nil {',
                    '        panic(err)',
                    '    }',
                    '    fmt.Println(string(responseBody))',
                    '}',
                ]
                    .filter(Boolean)
                    .join('\n');
            }

            if (method === 'GET') {
                return [
                    'package main',
                    '',
                    'import (',
                    '    "fmt"',
                    '    "io"',
                    '    "net/http"',
                    ')',
                    '',
                    'func main() {',
                    `    resp, err := http.Get("${url}")`,
                    '    if err != nil {',
                    '        panic(err)',
                    '    }',
                    '    defer resp.Body.Close()',
                    '    responseBody, err := io.ReadAll(resp.Body)',
                    '    if err != nil {',
                    '        panic(err)',
                    '    }',
                    '    fmt.Println(string(responseBody))',
                    '}',
                ].join('\n');
            }

            return [
                'package main',
                '',
                'import (',
                '    "fmt"',
                '    "io"',
                '    "net/http"',
                ')',
                '',
                'func main() {',
                `    req, err := http.NewRequest("${method}", "${url}", nil)`,
                '    if err != nil {',
                '        panic(err)',
                '    }',
                contentType ? `    req.Header.Set("Content-Type", "${contentType}")` : undefined,
                '    resp, err := http.DefaultClient.Do(req)',
                '    if err != nil {',
                '        panic(err)',
                '    }',
                '    defer resp.Body.Close()',
                '    responseBody, err := io.ReadAll(resp.Body)',
                '    if err != nil {',
                '        panic(err)',
                '    }',
                '    fmt.Println(string(responseBody))',
                '}',
            ]
                .filter(Boolean)
                .join('\n');
        }
        default:
            return '';
    }
}
