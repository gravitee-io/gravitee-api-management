/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

export type ElPrimitive = string | number | boolean | null | undefined;
export type ElValue = ElPrimitive | ElPrimitive[] | { [key: string]: ElValue };

export interface ElApiContext {
  name?: string;
  version?: string;
  description?: string;
  labels?: string[] | string;
  picture?: string;
  mcp?: boolean | string;
  type?: string;
  status?: string;
  owner?: string;
  metadata?: Record<string, ElPrimitive | ElPrimitive[] | undefined>;
  [key: string]: ElValue;
}

export interface ElContext {
  api?: ElApiContext;
  [key: string]: ElValue;
}

/** Matches `{#api.name}`, `{#api.metadata['key']}`, `{#api.metadata["key"]}`. */
const EL_EXPRESSION_PATTERN = /\{#([a-zA-Z_][\w]*(?:(?:\.[a-zA-Z_][\w]*)|(?:\[[^\]]+\]))*)\}/g;

/**
 * Resolve Gravitee EL-style placeholders in a GMD/Markdown template against a context object.
 * Missing paths resolve to an empty string. Arrays are joined with ", ".
 */
export function resolveElExpressions(template: string, context: ElContext = {}): string {
  if (!template) {
    return '';
  }
  return template.replace(EL_EXPRESSION_PATTERN, (_match, path: string) => formatElValue(resolveElPath(context, path)));
}

export function resolveElPath(context: ElContext, path: string): ElValue {
  const segments = tokenizeElPath(path);
  let current: ElValue = context;
  for (const segment of segments) {
    if (current === null || current === undefined || typeof current !== 'object' || Array.isArray(current)) {
      return undefined;
    }
    current = (current as Record<string, ElValue>)[segment];
  }
  return current;
}

function tokenizeElPath(path: string): string[] {
  const segments: string[] = [];
  const tokenPattern = /([a-zA-Z_][\w]*)|\[\s*['"]([^'"]+)['"]\s*\]/g;
  let match: RegExpExecArray | null;
  while ((match = tokenPattern.exec(path)) !== null) {
    segments.push(match[1] ?? match[2]);
  }
  return segments;
}

function formatElValue(value: ElValue): string {
  if (value === null || value === undefined) {
    return '';
  }
  if (Array.isArray(value)) {
    return value.filter(item => item !== null && item !== undefined && item !== '').map(String).join(', ');
  }
  if (typeof value === 'object') {
    return '';
  }
  if (typeof value === 'boolean') {
    return value ? 'true' : '';
  }
  return String(value);
}
