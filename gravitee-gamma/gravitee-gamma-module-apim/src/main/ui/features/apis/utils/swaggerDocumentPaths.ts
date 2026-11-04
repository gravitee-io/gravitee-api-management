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
import { PATH_MAPPING_PATTERN } from './pathMappings';
import { parseSpecDocument } from './specDocument';

/** `{param}` becomes `:param`. Paths outside `^/[a-zA-Z0-9:/._-]+$` are left out. */
export function swaggerDocumentPaths(content: string | undefined | null): string[] {
    if (!content?.trim()) {
        return [];
    }
    const parsed = parseSpecDocument('OPENAPI', content);
    if (parsed.status !== 'valid' || !isPathMap(parsed.document.paths)) {
        return [];
    }
    return Object.keys(parsed.document.paths).flatMap(rawPath => {
        const template = rawPath.replaceAll(/\{([^}]+)\}/g, ':$1');
        return PATH_MAPPING_PATTERN.test(template) ? [template] : [];
    });
}

function isPathMap(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}
