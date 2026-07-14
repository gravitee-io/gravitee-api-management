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

import type { ParsedOperation } from '../../ApiSpecBlock/openapi-spec-utils';
import { dereferenceSchema } from '../../ApiSpecBlock/openapi-spec-utils';

export function getOperationSectionId(operationId: string): string {
    return `operation-${operationId}`;
}

export interface ResponseMediaInfo {
    readonly status: string;
    readonly contentType: string;
    readonly description?: string;
    readonly media: OpenAPIV3.MediaTypeObject;
}

function isSuccessStatus(status: string): boolean {
    return status === 'default' || (status.startsWith('2') && !Number.isNaN(Number(status)));
}

export function getPrimaryResponseMedia(operation: ParsedOperation): ResponseMediaInfo | undefined {
    const entries = Object.entries(operation.responses);
    const successEntry =
        entries.find(([status]) => status.startsWith('2')) ??
        entries.find(([status]) => isSuccessStatus(status)) ??
        entries[0];

    if (!successEntry) {
        return undefined;
    }

    const [status, responseRef] = successEntry as [
        string,
        OpenAPIV3.ResponseObject | OpenAPIV3.ReferenceObject,
    ];
    if ('$ref' in responseRef) {
        return undefined;
    }

    const content = responseRef.content;
    if (!content) {
        return {
            status,
            contentType: 'application/json',
            description: responseRef.description,
            media: {},
        };
    }

    const contentType = content['application/json'] ? 'application/json' : Object.keys(content)[0];
    const media = content[contentType];
    if (!media) {
        return undefined;
    }

    return {
        status,
        contentType,
        description: responseRef.description,
        media,
    };
}

export function getResponseSchema(
    document: OpenAPIV3.Document,
    media: OpenAPIV3.MediaTypeObject,
): OpenAPIV3.SchemaObject | undefined {
    if (!media.schema) {
        return undefined;
    }
    if ('$ref' in media.schema) {
        return dereferenceSchema(document, media.schema);
    }
    return dereferenceSchema(document, media.schema as OpenAPIV3.SchemaObject);
}

function schemaToExample(schema: OpenAPIV3.SchemaObject): unknown {
    if (schema.example !== undefined) {
        return schema.example;
    }
    if (schema.enum?.length) {
        return schema.enum[0];
    }
    if (schema.type === 'string') {
        return schema.format === 'date-time' ? '2024-01-01T00:00:00Z' : 'string';
    }
    if (schema.type === 'integer' || schema.type === 'number') {
        return schema.minimum ?? 0;
    }
    if (schema.type === 'boolean') {
        return true;
    }
    if (schema.type === 'array' && schema.items && !('$ref' in schema.items)) {
        return [schemaToExample(schema.items as OpenAPIV3.SchemaObject)];
    }
    if (schema.properties) {
        const result: Record<string, unknown> = {};
        for (const [key, value] of Object.entries(schema.properties)) {
            if ('$ref' in value) {
                result[key] = key;
            } else {
                result[key] = schemaToExample(value as OpenAPIV3.SchemaObject);
            }
        }
        return result;
    }
    return {};
}

export function getExampleResponse(
    document: OpenAPIV3.Document,
    operation: ParsedOperation,
): string | undefined {
    const responseMedia = getPrimaryResponseMedia(operation);
    if (!responseMedia) {
        return undefined;
    }

    const { media } = responseMedia;

    if (media.example !== undefined) {
        return JSON.stringify(media.example, null, 2);
    }

    const firstExample = media.examples ? Object.values(media.examples)[0] : undefined;
    if (firstExample && !('$ref' in firstExample) && firstExample.value !== undefined) {
        return JSON.stringify(firstExample.value, null, 2);
    }

    const schema = getResponseSchema(document, media);
    if (schema) {
        return JSON.stringify(schemaToExample(schema), null, 2);
    }

    return undefined;
}

export function getParameterType(parameter: OpenAPIV3.ParameterObject): string {
    const schema = parameter.schema;
    if (!schema || '$ref' in schema) {
        return 'string';
    }
    if (schema.type) {
        if (Array.isArray(schema.type)) {
            return schema.type.join(' | ');
        }
        if (schema.format) {
            return `${schema.type} (${schema.format})`;
        }
        return String(schema.type);
    }
    if (schema.enum) {
        return `enum`;
    }
    return 'string';
}

export function getParameterDefault(parameter: OpenAPIV3.ParameterObject): string | undefined {
    const schema = parameter.schema;
    if (!schema || '$ref' in schema) {
        return parameter.example !== undefined ? String(parameter.example) : undefined;
    }
    if (schema.default !== undefined) {
        return String(schema.default);
    }
    if (schema.example !== undefined) {
        return String(schema.example);
    }
    return undefined;
}

export function getParameterEnum(parameter: OpenAPIV3.ParameterObject): readonly string[] | undefined {
    const schema = parameter.schema;
    if (!schema || '$ref' in schema || !schema.enum) {
        return undefined;
    }
    return schema.enum.map(String);
}

export function downloadSpecContent(specContent: string, title: string): void {
    const trimmed = specContent.trim();
    const isYaml = !trimmed.startsWith('{');
    const extension = isYaml ? 'yaml' : 'json';
    const mimeType = isYaml ? 'application/yaml' : 'application/json';
    const blob = new Blob([specContent], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${title.replace(/\s+/g, '-').toLowerCase() || 'openapi'}.${extension}`;
    anchor.click();
    URL.revokeObjectURL(url);
}

export async function copyToClipboard(text: string): Promise<void> {
    await navigator.clipboard.writeText(text);
}

export function isScrollableElement(element: HTMLElement): boolean {
    const style = getComputedStyle(element);
    return (
        /(auto|scroll|overlay)/.test(style.overflowY) ||
        /(auto|scroll)/.test(style.overflow)
    );
}

export function getDocsScrollContainer(docsRoot: HTMLElement): HTMLElement | null {
    let parent = docsRoot.parentElement;
    const scrollables: HTMLElement[] = [];

    while (parent) {
        if (isScrollableElement(parent)) {
            scrollables.push(parent);
        }
        parent = parent.parentElement;
    }

    const scrollingAncestor = [...scrollables]
        .reverse()
        .find(element => element.scrollHeight > element.clientHeight + 1);

    if (scrollingAncestor) {
        return scrollingAncestor;
    }

    const outermostScrollable = scrollables.at(-1);
    if (outermostScrollable) {
        return outermostScrollable;
    }

    return document.scrollingElement instanceof HTMLElement ? document.scrollingElement : null;
}

export function getDocsScrollParent(element: HTMLElement): HTMLElement | null {
    const docsRoot = element.closest('[data-testid="gravitee-docs-renderer"]');
    if (docsRoot instanceof HTMLElement) {
        return getDocsScrollContainer(docsRoot);
    }

    let parent = element.parentElement;
    const scrollables: HTMLElement[] = [];

    while (parent) {
        if (isScrollableElement(parent)) {
            scrollables.push(parent);
        }
        parent = parent.parentElement;
    }

    return scrollables.at(-1) ?? null;
}

export function getDocsStickyHeaderOffset(element: HTMLElement): number {
    const shell = element.closest('[data-testid="gravitee-docs-renderer"]');
    if (!(shell instanceof HTMLElement)) {
        return 0;
    }

    const raw = getComputedStyle(shell).getPropertyValue('--docs-sticky-header-offset').trim();
    const parsed = Number.parseFloat(raw);
    return Number.isFinite(parsed) ? parsed : 0;
}

export function findOperationSectionElement(
    docsRoot: HTMLElement | null | undefined,
    operationId: string,
): HTMLElement | null {
    const sectionId = getOperationSectionId(operationId);
    const scoped = docsRoot?.querySelector<HTMLElement>(`#${CSS.escape(sectionId)}`);
    return scoped ?? document.getElementById(sectionId);
}

export function scrollOperationSectionIntoView(
    element: HTMLElement,
    scrollContainer?: HTMLElement | null,
): void {
    const container = scrollContainer ?? getDocsScrollParent(element);
    const headerOffset = getDocsStickyHeaderOffset(element);

    if (container) {
        const top =
            element.getBoundingClientRect().top -
            container.getBoundingClientRect().top +
            container.scrollTop -
            headerOffset;
        container.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
        return;
    }

    element.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
}

export function groupOperationsByTag(
    operations: readonly ParsedOperation[],
    tags: readonly string[],
): ReadonlyArray<{ tag: string; operations: readonly ParsedOperation[] }> {
    return tags.map(tag => ({
        tag,
        operations: operations.filter(operation => operation.tags.includes(tag)),
    }));
}
