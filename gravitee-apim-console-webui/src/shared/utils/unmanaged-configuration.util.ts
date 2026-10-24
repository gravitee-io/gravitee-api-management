/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { isPlainObject, omit } from 'lodash';

type JsonSchema = Record<string, unknown>;

/**
 * A plugin schema form only emits the properties its schema declares, so saving its value as the whole
 * configuration deletes properties written by other clients (such as the tools of an MCP Studio). Those are copied
 * from the stored configuration. Declared properties are taken from the form as a whole and never merged: a merge
 * would bring back values the user removed, such as a credential hidden after changing the authentication type.
 */
export function preserveUnmanagedConfiguration(schema: unknown, storedConfiguration: unknown, formConfiguration: unknown): unknown {
  if (!isPlainObject(schema) || !isPlainObject(storedConfiguration) || !isPlainObject(formConfiguration)) {
    return formConfiguration;
  }
  const declaredProperties = collectDeclaredProperties(schema as JsonSchema, schema as JsonSchema, new Set());
  if (declaredProperties === null) {
    return formConfiguration;
  }
  return { ...omit(storedConfiguration as object, [...declaredProperties]), ...(formConfiguration as object) };
}

// null when the declared properties cannot be known for sure, and the form value is then kept as the whole
// configuration, as before: the schema constrains undeclared properties itself (allowed by the form, or rejected and
// stripped by the Management API), or a $ref cannot be resolved.
function collectDeclaredProperties(schema: JsonSchema, root: JsonSchema, visitedRefs: Set<string>): Set<string> | null {
  const ref = schema.$ref;
  if (typeof ref === 'string') {
    // visitedRefs is per path, so a definition can be reused by sibling sub-schemas while a cycle still stops.
    if (visitedRefs.has(ref)) {
      return new Set();
    }
    const target = resolveLocalRef(ref, root);
    return target ? collectDeclaredProperties(target, root, new Set([...visitedRefs, ref])) : null;
  }
  if (schema.additionalProperties !== undefined || schema.patternProperties !== undefined) {
    return null;
  }

  const declaredProperties = new Set(isPlainObject(schema.properties) ? Object.keys(schema.properties as object) : []);
  const subSchemas = [
    ...['allOf', 'anyOf', 'oneOf'].flatMap(keyword => (Array.isArray(schema[keyword]) ? (schema[keyword] as unknown[]) : [])),
    ...['if', 'then', 'else'].map(keyword => schema[keyword]),
  ].filter(isPlainObject) as JsonSchema[];

  for (const subSchema of subSchemas) {
    const subSchemaProperties = collectDeclaredProperties(subSchema, root, visitedRefs);
    if (subSchemaProperties === null) {
      return null;
    }
    subSchemaProperties.forEach(property => declaredProperties.add(property));
  }
  return declaredProperties;
}

function resolveLocalRef(ref: string, root: JsonSchema): JsonSchema | undefined {
  if (!ref.startsWith('#/')) {
    return undefined;
  }
  const target = ref
    .slice(2)
    .split('/')
    .reduce<unknown>((current, segment) => (isPlainObject(current) ? (current as JsonSchema)[segment] : undefined), root);
  return isPlainObject(target) ? (target as JsonSchema) : undefined;
}
