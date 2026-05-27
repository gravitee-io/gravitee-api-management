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
import { buildSnippet, buildTransportConfig } from './mcp-installer.utils';
import { McpClientInstaller } from '../../../models/mcpClientInstaller';
import { McpServerSpec } from '../../../models/mcpServerSpec';

function toBase64(value: string): string {
  const encodedValue = new TextEncoder().encode(value);
  const binaryValue = Array.from(encodedValue, byte => String.fromCharCode(byte)).join('');
  return btoa(binaryValue);
}

function encodeConfig(spec: McpServerSpec): string {
  return toBase64(JSON.stringify(buildTransportConfig(spec)));
}

export const cursorInstaller: McpClientInstaller = {
  id: 'cursor',
  label: 'Cursor',
  mode: 'deep-link',
  snippetFileName: '~/.cursor/mcp.json',
  supports: () => true,
  buildSnippet: spec => buildSnippet(spec),
  buildDeepLink: spec =>
    `cursor://anysphere.cursor-deeplink/mcp/install?name=${encodeURIComponent(spec.name)}&config=${encodeURIComponent(encodeConfig(spec))}`,
  buildFallbackLink: spec =>
    `https://cursor.com/en/install-mcp?name=${encodeURIComponent(spec.name)}&config=${encodeURIComponent(encodeConfig(spec))}`,
};
