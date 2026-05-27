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
import { McpServerSpec } from '../../../models/mcpServerSpec';

export function buildSnippet(spec: McpServerSpec): string {
  return JSON.stringify(
    {
      mcpServers: {
        [spec.name]: buildTransportConfig(spec),
      },
    },
    null,
    2,
  );
}

export function buildTransportConfig(spec: McpServerSpec): Record<string, unknown> {
  if (spec.transport === 'stdio') {
    return {
      command: spec.command,
      ...(spec.args?.length ? { args: spec.args } : {}),
      ...(spec.env && Object.keys(spec.env).length > 0 ? { env: spec.env } : {}),
    };
  }

  return {
    ...(spec.transport === 'sse' ? { type: 'sse' } : {}),
    url: spec.url,
    ...(getHeaders(spec) ? { headers: getHeaders(spec) } : {}),
  };
}

export function buildVscodeInstallConfig(spec: McpServerSpec): Record<string, unknown> {
  if (spec.transport === 'stdio') {
    return {
      name: spec.name,
      command: spec.command,
      ...(spec.args?.length ? { args: spec.args } : {}),
      ...(spec.env && Object.keys(spec.env).length > 0 ? { env: spec.env } : {}),
    };
  }

  return {
    name: spec.name,
    type: spec.transport,
    url: spec.url,
    ...(getHeaders(spec) ? { headers: getHeaders(spec) } : {}),
  };
}

export function getHeaders(spec: McpServerSpec): Record<string, string> | undefined {
  if (spec.transport === 'stdio') {
    return undefined;
  }

  return spec.headers;
}
