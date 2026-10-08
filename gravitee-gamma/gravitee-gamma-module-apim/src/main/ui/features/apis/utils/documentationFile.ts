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
import type { PortalPageContentType } from '../types/apiDocumentation';

/** The files the classic portal editor imports too: Markdown, and OpenAPI or AsyncAPI as YAML or JSON. */
export const DOCUMENTATION_FILE_ACCEPT = {
    'text/markdown': ['.md'],
    'application/x-yaml': ['.yaml', '.yml'],
    'application/json': ['.json'],
};

export const MAX_DOCUMENTATION_FILE_SIZE_MB = 10;

// A root key starts a line, unindented, optionally quoted. Matching it avoids pulling a YAML parser into the module.
const ROOT_SPEC_KEYS = /^(["']?)(asyncapi|openapi|swagger)\1[ \t]*:/gm;

export function detectPageContentType(fileName: string, content: string): PortalPageContentType | null {
    if (fileName.toLowerCase().endsWith('.md')) {
        return 'GRAVITEE_MARKDOWN';
    }
    const rootKeys = jsonRootKeys(content) ?? yamlSpecRootKeys(content);
    if (rootKeys.includes('asyncapi')) {
        return 'ASYNCAPI';
    }
    return rootKeys.includes('openapi') || rootKeys.includes('swagger') ? 'OPENAPI' : null;
}

export function titleFromFileName(fileName: string): string {
    const extensionStart = fileName.lastIndexOf('.');
    return extensionStart > 0 ? fileName.slice(0, extensionStart) : fileName;
}

function jsonRootKeys(content: string): string[] | null {
    let document: unknown;
    try {
        document = JSON.parse(content);
    } catch {
        return null;
    }
    return typeof document === 'object' && document !== null && !Array.isArray(document) ? Object.keys(document) : [];
}

function yamlSpecRootKeys(content: string): string[] {
    return Array.from(content.matchAll(ROOT_SPEC_KEYS), match => match[2] ?? '');
}
