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

import { parseOpenApiSpecObject } from '../../features/editor/utils/parse-openapi-spec';

export const UNTAGGED_OPERATIONS_TAG = 'General';

export type HttpMethod = 'get' | 'put' | 'post' | 'delete' | 'options' | 'head' | 'patch' | 'trace';

export const HTTP_METHODS: readonly HttpMethod[] = [
    'get',
    'put',
    'post',
    'delete',
    'options',
    'head',
    'patch',
    'trace',
] as const;

export interface ParsedOperation {
    readonly operationId: string;
    readonly method: HttpMethod;
    readonly path: string;
    readonly tags: readonly string[];
    readonly summary?: string;
    readonly description?: string;
    readonly operation: OpenAPIV3.OperationObject;
    readonly parameters: readonly OpenAPIV3.ParameterObject[];
    readonly requestBody?: OpenAPIV3.RequestBodyObject;
    readonly responses: OpenAPIV3.ResponsesObject;
    readonly security?: readonly OpenAPIV3.SecurityRequirementObject[];
}

export interface ParsedOpenApiSpec {
    readonly document: OpenAPIV3.Document;
    readonly operations: readonly ParsedOperation[];
    readonly tags: readonly string[];
}

function isHttpMethod(method: string): method is HttpMethod {
    return (HTTP_METHODS as readonly string[]).includes(method);
}

function resolveRef<T>(document: OpenAPIV3.Document, ref: string, seen = new Set<string>()): T | undefined {
    if (!ref.startsWith('#/')) {
        return undefined;
    }
    if (seen.has(ref)) {
        return undefined;
    }
    seen.add(ref);

    const parts = ref.slice(2).split('/');
    let current: unknown = document;
    for (const part of parts) {
        if (!current || typeof current !== 'object') {
            return undefined;
        }
        current = (current as Record<string, unknown>)[part];
    }

    if (current && typeof current === 'object' && '$ref' in current) {
        const nestedRef = (current as { $ref?: string }).$ref;
        if (typeof nestedRef === 'string') {
            return resolveRef<T>(document, nestedRef, seen);
        }
    }

    return current as T | undefined;
}

export function dereferenceSchema(
    document: OpenAPIV3.Document,
    schema: OpenAPIV3.SchemaObject | OpenAPIV3.ReferenceObject | undefined,
    seen = new Set<string>(),
): OpenAPIV3.SchemaObject | undefined {
    if (!schema) {
        return undefined;
    }

    if ('$ref' in schema && typeof schema.$ref === 'string') {
        if (seen.has(schema.$ref)) {
            return { type: 'object', description: schema.$ref };
        }
        seen.add(schema.$ref);
        const resolved = resolveRef<OpenAPIV3.SchemaObject>(document, schema.$ref, new Set(seen));
        return resolved ? dereferenceSchema(document, resolved, seen) : undefined;
    }

    const objectSchema = schema as OpenAPIV3.SchemaObject;
    const result: OpenAPIV3.SchemaObject = { ...objectSchema };

    if (objectSchema.properties) {
        const properties: Record<string, OpenAPIV3.SchemaObject | OpenAPIV3.ReferenceObject> = {};
        for (const [key, value] of Object.entries(objectSchema.properties)) {
            const resolved = dereferenceSchema(document, value, new Set(seen));
            if (resolved) {
                properties[key] = resolved;
            }
        }
        result.properties = properties;
    }

    if ('items' in objectSchema && objectSchema.items) {
        const resolvedItems = dereferenceSchema(document, objectSchema.items, new Set(seen));
        if (resolvedItems) {
            (result as OpenAPIV3.ArraySchemaObject).items = resolvedItems;
        }
    }

    if (objectSchema.allOf) {
        result.allOf = objectSchema.allOf
            .map(item => dereferenceSchema(document, item, new Set(seen)))
            .filter((item): item is OpenAPIV3.SchemaObject => Boolean(item));
    }

    if (objectSchema.oneOf) {
        result.oneOf = objectSchema.oneOf
            .map(item => dereferenceSchema(document, item, new Set(seen)))
            .filter((item): item is OpenAPIV3.SchemaObject => Boolean(item));
    }

    if (objectSchema.anyOf) {
        result.anyOf = objectSchema.anyOf
            .map(item => dereferenceSchema(document, item, new Set(seen)))
            .filter((item): item is OpenAPIV3.SchemaObject => Boolean(item));
    }

    return result;
}

function resolveParameter(
    document: OpenAPIV3.Document,
    parameter: OpenAPIV3.ParameterObject | OpenAPIV3.ReferenceObject,
): OpenAPIV3.ParameterObject | undefined {
    if ('$ref' in parameter && typeof parameter.$ref === 'string') {
        return resolveRef<OpenAPIV3.ParameterObject>(document, parameter.$ref);
    }
    return parameter as OpenAPIV3.ParameterObject;
}

function resolveRequestBody(
    document: OpenAPIV3.Document,
    requestBody: OpenAPIV3.RequestBodyObject | OpenAPIV3.ReferenceObject | undefined,
): OpenAPIV3.RequestBodyObject | undefined {
    if (!requestBody) {
        return undefined;
    }
    if ('$ref' in requestBody && typeof requestBody.$ref === 'string') {
        return resolveRef<OpenAPIV3.RequestBodyObject>(document, requestBody.$ref);
    }
    return requestBody as OpenAPIV3.RequestBodyObject;
}

function collectOperationTags(operation: OpenAPIV3.OperationObject): readonly string[] {
    const tags = operation.tags ?? [];
    return tags.length > 0 ? tags : [UNTAGGED_OPERATIONS_TAG];
}

function createOperationId(method: string, path: string, operation: OpenAPIV3.OperationObject): string {
    if (operation.operationId) {
        return operation.operationId;
    }
    return `${method}_${path.replace(/[{}]/g, '').replace(/\//g, '_').replace(/^_/, '')}`;
}

export function extractOperations(document: OpenAPIV3.Document): ParsedOperation[] {
    const operations: ParsedOperation[] = [];

    for (const [path, pathItemRef] of Object.entries(document.paths ?? {})) {
        if (!pathItemRef) {
            continue;
        }

        const pathItem = pathItemRef as OpenAPIV3.PathItemObject;
        const pathParameters = (pathItem.parameters ?? [])
            .map(parameter => resolveParameter(document, parameter))
            .filter((parameter): parameter is OpenAPIV3.ParameterObject => Boolean(parameter));

        for (const method of HTTP_METHODS) {
            const operation = pathItem[method];
            if (!operation) {
                continue;
            }

            const operationParameters = (operation.parameters ?? [])
                .map(parameter => resolveParameter(document, parameter))
                .filter((parameter): parameter is OpenAPIV3.ParameterObject => Boolean(parameter));

            const mergedParameters = [...pathParameters];
            for (const parameter of operationParameters) {
                const existingIndex = mergedParameters.findIndex(
                    existing => existing.name === parameter.name && existing.in === parameter.in,
                );
                if (existingIndex >= 0) {
                    mergedParameters[existingIndex] = parameter;
                } else {
                    mergedParameters.push(parameter);
                }
            }

            operations.push({
                operationId: createOperationId(method, path, operation),
                method,
                path,
                tags: collectOperationTags(operation),
                summary: operation.summary,
                description: operation.description,
                operation,
                parameters: mergedParameters,
                requestBody: resolveRequestBody(document, operation.requestBody),
                responses: operation.responses ?? {},
                security: operation.security,
            });
        }
    }

    return operations.sort((left, right) => {
        const tagCompare = (left.tags[0] ?? '').localeCompare(right.tags[0] ?? '');
        if (tagCompare !== 0) {
            return tagCompare;
        }
        const pathCompare = left.path.localeCompare(right.path);
        if (pathCompare !== 0) {
            return pathCompare;
        }
        return left.method.localeCompare(right.method);
    });
}

export function extractTags(document: OpenAPIV3.Document, operations: readonly ParsedOperation[]): string[] {
    const tagSet = new Set<string>();

    for (const tag of document.tags ?? []) {
        if (tag.name) {
            tagSet.add(tag.name);
        }
    }

    for (const operation of operations) {
        for (const tag of operation.tags) {
            tagSet.add(tag);
        }
    }

    return [...tagSet].sort((left, right) => {
        if (left === UNTAGGED_OPERATIONS_TAG) {
            return 1;
        }
        if (right === UNTAGGED_OPERATIONS_TAG) {
            return -1;
        }
        return left.localeCompare(right);
    });
}

export function getOperationsByTag(
    operations: readonly ParsedOperation[],
    tag: string,
    operationId?: string,
): ParsedOperation[] {
    const filtered = operations.filter(operation => operation.tags.includes(tag));
    if (!operationId) {
        return filtered;
    }
    return filtered.filter(operation => operation.operationId === operationId);
}

function collectSchemaRefs(value: unknown, refs: Set<string>): void {
    if (!value || typeof value !== 'object') {
        return;
    }

    if ('$ref' in value && typeof (value as { $ref: unknown }).$ref === 'string') {
        refs.add((value as { $ref: string }).$ref);
        return;
    }

    if (Array.isArray(value)) {
        for (const item of value) {
            collectSchemaRefs(item, refs);
        }
        return;
    }

    for (const nested of Object.values(value)) {
        collectSchemaRefs(nested, refs);
    }
}

export function getSchemaNameFromRef(ref: string): string | undefined {
    const match = ref.match(/^#\/components\/schemas\/(.+)$/);
    return match?.[1];
}

export function getSchemasForOperations(
    document: OpenAPIV3.Document,
    operations: readonly ParsedOperation[],
): Record<string, OpenAPIV3.SchemaObject> {
    const refs = new Set<string>();

    for (const operation of operations) {
        collectSchemaRefs(operation.parameters, refs);
        collectSchemaRefs(operation.requestBody, refs);
        collectSchemaRefs(operation.responses, refs);
    }

    const schemas: Record<string, OpenAPIV3.SchemaObject> = {};

    for (const ref of refs) {
        const name = getSchemaNameFromRef(ref);
        if (!name) {
            continue;
        }
        const resolved = resolveRef<OpenAPIV3.SchemaObject>(document, ref);
        const dereferenced = dereferenceSchema(document, resolved);
        if (dereferenced) {
            schemas[name] = dereferenced;
        }
    }

    return schemas;
}

export function parseOpenApiDocument(content: string): ParsedOpenApiSpec | undefined {
    const raw = parseOpenApiSpecObject(content);
    if (!raw) {
        return undefined;
    }

    const document = raw as unknown as OpenAPIV3.Document;
    const operations = extractOperations(document);
    const tags = extractTags(document, operations);

    return {
        document,
        operations,
        tags,
    };
}

export function getDefaultServerUrl(document: OpenAPIV3.Document, override?: string): string {
    if (override?.trim()) {
        return override.trim();
    }
    const server = document.servers?.[0];
    return server?.url ?? '';
}
