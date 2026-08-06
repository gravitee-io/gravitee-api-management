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
import angular from 'angular';

export const DEFAULT_API_KEY_HEADER = 'X-Gravitee-Api-Key';

/**
 * gio-form-json-schema applies the apiKeyHeader schema default when the plan configuration has no
 * header set. Strip that default on create and when editing a plan without a stored header so the
 * gateway can use its environment-level configuration.
 */
export function shouldStripApiKeyHeaderSchemaDefault(mode: 'create' | 'edit', storedConfiguration: unknown): boolean {
  if (mode === 'create') {
    return true;
  }

  if (!angular.isObject(storedConfiguration) || Array.isArray(storedConfiguration)) {
    return true;
  }

  return !('apiKeyHeader' in (storedConfiguration as Record<string, unknown>));
}

/**
 * Remove apiKeyHeader from the persisted config only when the header was cleared.
 * Non-empty header values are kept.
 */
export function sanitizeApiKeySecurityConfiguration(configuration: unknown): unknown {
  if (!angular.isObject(configuration) || Array.isArray(configuration)) {
    return configuration;
  }

  const config = { ...(configuration as Record<string, unknown>) };

  if (typeof config.apiKeyHeader === 'string' && config.apiKeyHeader.trim() === '') {
    delete config.apiKeyHeader;
    delete config.enableCustomApiKeyHeader;
  }

  return config;
}

/**
 * Prevent gio-form-json-schema from injecting the schema default into an empty stored apiKeyHeader.
 */
export function stripApiKeyHeaderSchemaDefault(schema: unknown): unknown {
  if (!angular.isObject(schema) || Array.isArray(schema)) {
    return schema;
  }

  const schemaRecord = schema as Record<string, unknown>;
  const properties = schemaRecord.properties as Record<string, Record<string, unknown>> | undefined;
  const apiKeyHeader = properties?.apiKeyHeader;

  if (!apiKeyHeader || !('default' in apiKeyHeader)) {
    return schema;
  }

  const { default: _default, ...apiKeyHeaderWithoutDefault } = apiKeyHeader;

  return {
    ...schemaRecord,
    properties: {
      ...properties,
      apiKeyHeader: apiKeyHeaderWithoutDefault,
    },
  };
}
