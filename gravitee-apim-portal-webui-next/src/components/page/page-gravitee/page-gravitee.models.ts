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
import { readYaml } from '../../../app/helpers/yaml-parser';

export type HttpMethod = 'GET' | 'PUT' | 'POST' | 'DELETE' | 'OPTIONS' | 'HEAD' | 'PATCH' | 'TRACE';

const HTTP_METHODS = new Set(['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace']);

export interface GraviteeParameter {
  name: string;
  in: 'path' | 'query' | 'header' | 'cookie' | string;
  required: boolean;
  description?: string;
  type: string;
  defaultValue?: string;
  enumValues?: string[];
}

export interface GraviteeResponseInfo {
  status: string;
  contentType: string;
  description?: string;
  example?: string;
}

export interface GraviteeOperation {
  id: string;
  method: HttpMethod;
  path: string;
  summary: string;
  description?: string;
  tag: string;
  parameters: GraviteeParameter[];
  hasRequestBody: boolean;
  requestBodyDescription?: string;
  response?: GraviteeResponseInfo;
}

export interface GraviteeSpecView {
  title: string;
  version: string;
  description: string;
  servers: string[];
  schemaNames: string[];
  operations: GraviteeOperation[];
  operationsByTag: Array<{ tag: string; operations: GraviteeOperation[] }>;
  rawContent: string;
}

type JsonSchema = {
  type?: string | string[];
  format?: string;
  default?: unknown;
  example?: unknown;
  enum?: unknown[];
  properties?: Record<string, JsonSchema | { $ref?: string }>;
  items?: JsonSchema | { $ref?: string };
  $ref?: string;
};

type OpenApiLike = {
  info?: { title?: string; version?: string; description?: string };
  servers?: Array<{ url?: string }>;
  paths?: Record<string, Record<string, unknown>>;
  components?: { schemas?: Record<string, unknown>; parameters?: Record<string, unknown> };
};

function asRecord(value: unknown): Record<string, unknown> | undefined {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, unknown>) : undefined;
}

function resolveRef(document: OpenApiLike, ref: string): unknown {
  if (!ref.startsWith('#/')) {
    return undefined;
  }
  let current: unknown = document;
  for (const part of ref.slice(2).split('/')) {
    const record = asRecord(current);
    if (!record) {
      return undefined;
    }
    current = record[part];
  }
  return current;
}

function schemaType(schema: JsonSchema | undefined): string {
  if (!schema) {
    return 'string';
  }
  if (schema.enum?.length) {
    return 'enum';
  }
  const type = Array.isArray(schema.type) ? schema.type.join(' | ') : schema.type;
  if (!type) {
    return 'string';
  }
  return schema.format ? `${type} (${schema.format})` : type;
}

function schemaToExample(schema: JsonSchema | undefined): unknown {
  if (!schema) {
    return {};
  }
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
    return 0;
  }
  if (schema.type === 'boolean') {
    return true;
  }
  if (schema.type === 'array') {
    const items = schema.items && !('$ref' in schema.items) ? (schema.items as JsonSchema) : undefined;
    return [schemaToExample(items)];
  }
  if (schema.properties) {
    const result: Record<string, unknown> = {};
    Object.entries(schema.properties).forEach(([key, value]) => {
      if (value && '$ref' in value) {
        result[key] = key;
      } else {
        result[key] = schemaToExample(value as JsonSchema);
      }
    });
    return result;
  }
  return {};
}

function normalizeParameter(document: OpenApiLike, parameter: unknown): GraviteeParameter | undefined {
  let param = asRecord(parameter);
  if (!param) {
    return undefined;
  }
  if (typeof param['$ref'] === 'string') {
    param = asRecord(resolveRef(document, param['$ref'] as string));
    if (!param) {
      return undefined;
    }
  }

  const schemaRaw = param['schema'];
  let schema = asRecord(schemaRaw) as JsonSchema | undefined;
  if (schema?.$ref) {
    schema = asRecord(resolveRef(document, schema.$ref)) as JsonSchema | undefined;
  }

  return {
    name: String(param['name'] ?? ''),
    in: String(param['in'] ?? 'query'),
    required: Boolean(param['required']),
    description: typeof param['description'] === 'string' ? param['description'] : undefined,
    type: schemaType(schema),
    defaultValue:
      schema?.default !== undefined
        ? String(schema.default)
        : param['example'] !== undefined
          ? String(param['example'])
          : undefined,
    enumValues: schema?.enum?.map(String),
  };
}

function getResponseInfo(document: OpenApiLike, responses: Record<string, unknown> | undefined): GraviteeResponseInfo | undefined {
  if (!responses) {
    return undefined;
  }
  const status =
    Object.keys(responses).find(key => key.startsWith('2')) ??
    Object.keys(responses).find(key => key === 'default') ??
    Object.keys(responses)[0];
  if (!status) {
    return undefined;
  }

  let response = asRecord(responses[status]);
  if (response && typeof response['$ref'] === 'string') {
    response = asRecord(resolveRef(document, response['$ref'] as string));
  }
  if (!response) {
    return { status, contentType: 'application/json' };
  }

  const content = asRecord(response['content']) ?? {};
  const contentType = content['application/json'] ? 'application/json' : Object.keys(content)[0] ?? 'application/json';
  const media = asRecord(content[contentType]) ?? {};
  let example: string | undefined;

  if (media['example'] !== undefined) {
    example = JSON.stringify(media['example'], null, 2);
  } else {
    const examples = asRecord(media['examples']);
    const firstExample = examples ? Object.values(examples)[0] : undefined;
    const firstExampleRecord = asRecord(firstExample);
    if (firstExampleRecord && firstExampleRecord['value'] !== undefined) {
      example = JSON.stringify(firstExampleRecord['value'], null, 2);
    } else {
      let schema = asRecord(media['schema']) as JsonSchema | undefined;
      if (schema?.$ref) {
        schema = asRecord(resolveRef(document, schema.$ref)) as JsonSchema | undefined;
      }
      if (schema) {
        example = JSON.stringify(schemaToExample(schema), null, 2);
      }
    }
  }

  return {
    status,
    contentType,
    description: typeof response['description'] === 'string' ? response['description'] : undefined,
    example,
  };
}

function createOperationId(method: string, path: string, operation: Record<string, unknown>): string {
  if (typeof operation['operationId'] === 'string' && operation['operationId']) {
    return operation['operationId'];
  }
  return `${method}_${path}`.replace(/[{}/:]/g, '_');
}

export function parseGraviteeSpec(content: string): GraviteeSpecView | null {
  try {
    const trimmed = content.trim();
    const parsed = (trimmed.startsWith('{') || trimmed.startsWith('[') ? JSON.parse(trimmed) : readYaml(trimmed)) as OpenApiLike;
    if (!parsed || typeof parsed !== 'object') {
      return null;
    }

    const operations: GraviteeOperation[] = [];
    Object.entries(parsed.paths ?? {}).forEach(([path, pathItem]) => {
      const pathRecord = asRecord(pathItem);
      if (!pathRecord) {
        return;
      }

      const pathParameters = ((pathRecord['parameters'] as unknown[]) ?? [])
        .map(parameter => normalizeParameter(parsed, parameter))
        .filter((parameter): parameter is GraviteeParameter => Boolean(parameter?.name));

      Object.entries(pathRecord).forEach(([method, operationValue]) => {
        if (!HTTP_METHODS.has(method.toLowerCase())) {
          return;
        }
        const operation = asRecord(operationValue);
        if (!operation) {
          return;
        }

        const operationParameters = ((operation['parameters'] as unknown[]) ?? [])
          .map(parameter => normalizeParameter(parsed, parameter))
          .filter((parameter): parameter is GraviteeParameter => Boolean(parameter?.name));

        const merged = [...pathParameters];
        operationParameters.forEach(parameter => {
          const index = merged.findIndex(existing => existing.name === parameter.name && existing.in === parameter.in);
          if (index >= 0) {
            merged[index] = parameter;
          } else {
            merged.push(parameter);
          }
        });

        const tags = Array.isArray(operation['tags']) ? (operation['tags'] as string[]) : [];
        const tag = tags[0] ?? 'Endpoints';
        const requestBody = asRecord(operation['requestBody']);
        const methodUpper = method.toUpperCase() as HttpMethod;

        operations.push({
          id: createOperationId(method, path, operation),
          method: methodUpper,
          path,
          summary: String(operation['summary'] || operation['operationId'] || path),
          description: typeof operation['description'] === 'string' ? operation['description'] : undefined,
          tag,
          parameters: merged,
          hasRequestBody: Boolean(requestBody),
          requestBodyDescription: typeof requestBody?.['description'] === 'string' ? requestBody['description'] : undefined,
          response: getResponseInfo(parsed, asRecord(operation['responses']) as Record<string, unknown> | undefined),
        });
      });
    });

    const tags = [...new Set(operations.map(operation => operation.tag))];
    const schemaNames = Object.keys(parsed.components?.schemas ?? {});
    const servers = (parsed.servers ?? []).map(server => server.url).filter((url): url is string => Boolean(url));

    return {
      title: parsed.info?.title ?? 'OpenAPI',
      version: parsed.info?.version ?? '',
      description: parsed.info?.description ?? '',
      servers,
      schemaNames,
      operations,
      operationsByTag: tags.map(tag => ({
        tag,
        operations: operations.filter(operation => operation.tag === tag),
      })),
      rawContent: content,
    };
  } catch {
    return null;
  }
}

export function buildRequestUrl(
  operation: GraviteeOperation,
  serverUrl: string,
  pathValues: Record<string, string>,
  queryValues: Record<string, string>,
): string {
  let path = operation.path;
  Object.entries(pathValues).forEach(([name, value]) => {
    path = path.replace(`{${name}}`, encodeURIComponent(value));
  });

  const query = new URLSearchParams();
  Object.entries(queryValues).forEach(([name, value]) => {
    if (value) {
      query.set(name, value);
    }
  });

  const normalizedServer = (serverUrl || '').replace(/\/$/, '');
  const queryString = query.toString();
  return `${normalizedServer}${path}${queryString ? `?${queryString}` : ''}`;
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
  anchor.download = `${title.replace(/[^a-z0-9-_]+/gi, '-').toLowerCase() || 'openapi'}.${extension}`;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function getOperationSectionId(operationId: string): string {
  return `operation-${operationId}`;
}

function isScrollableElement(element: HTMLElement): boolean {
  const style = getComputedStyle(element);
  const overflowY = style.overflowY;
  return overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay';
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

  const scrollingAncestor = [...scrollables].reverse().find(element => element.scrollHeight > element.clientHeight + 1);
  if (scrollingAncestor) {
    return scrollingAncestor;
  }

  const outermostScrollable = scrollables.at(-1);
  if (outermostScrollable) {
    return outermostScrollable;
  }

  return document.scrollingElement instanceof HTMLElement ? document.scrollingElement : null;
}

export function getDocsStickyHeaderOffset(element: HTMLElement): number {
  const shell = element.closest('[data-testid="gravitee-renderer"]');
  if (!(shell instanceof HTMLElement)) {
    return 0;
  }

  const raw = getComputedStyle(shell).getPropertyValue('--docs-sticky-header-offset').trim();
  const parsed = Number.parseFloat(raw);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function findOperationSectionElement(docsRoot: HTMLElement | null | undefined, operationId: string): HTMLElement | null {
  const sectionId = getOperationSectionId(operationId);
  const scoped = docsRoot?.querySelector<HTMLElement>(`#${CSS.escape(sectionId)}`);
  return scoped ?? document.getElementById(sectionId);
}

export function scrollOperationSectionIntoView(element: HTMLElement, scrollContainer?: HTMLElement | null): void {
  const container = scrollContainer ?? getDocsScrollContainer(element);
  const headerOffset = getDocsStickyHeaderOffset(element);

  if (container) {
    const top =
      element.getBoundingClientRect().top - container.getBoundingClientRect().top + container.scrollTop - headerOffset;
    container.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
    return;
  }

  element.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
}
