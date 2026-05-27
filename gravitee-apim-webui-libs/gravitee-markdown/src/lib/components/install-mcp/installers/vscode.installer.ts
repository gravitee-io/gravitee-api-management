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
import { buildSnippet, buildTransportConfig, buildVscodeInstallConfig } from './mcp-installer.utils';
import { McpClientInstaller } from '../../../models/mcpClientInstaller';
import { McpServerSpec } from '../../../models/mcpServerSpec';

function encodeDeepLinkConfig(spec: McpServerSpec): string {
  return encodeURIComponent(JSON.stringify(buildVscodeInstallConfig(spec)));
}

function encodeFallbackConfig(spec: McpServerSpec): string {
  return encodeURIComponent(JSON.stringify(buildTransportConfig(spec)));
}

export const vscodeInstaller: McpClientInstaller = {
  id: 'vscode',
  label: 'VS Code',
  mode: 'deep-link',
  snippetFileName: 'mcp.json',
  supports: () => true,
  buildSnippet: spec => buildSnippet(spec),
  buildDeepLink: spec => `vscode:mcp/install?${encodeDeepLinkConfig(spec)}`,
  buildFallbackLink: spec =>
    `https://insiders.vscode.dev/redirect/mcp/install?name=${encodeURIComponent(spec.name)}&config=${encodeFallbackConfig(spec)}`,
};
