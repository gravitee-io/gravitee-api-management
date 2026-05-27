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
import { InjectionToken } from '@angular/core';

import { claudeDesktopInstaller } from './installers/claude-desktop.installer';
import { cursorInstaller } from './installers/cursor.installer';
import { vscodeInstaller } from './installers/vscode.installer';
import { McpClientInstaller } from '../../models/mcpClientInstaller';

export const DEFAULT_INSTALLERS: McpClientInstaller[] = [cursorInstaller, vscodeInstaller, claudeDesktopInstaller];

export const GMD_MCP_INSTALLERS = new InjectionToken<McpClientInstaller[]>('GMD_MCP_INSTALLERS', {
  providedIn: 'root',
  factory: () => DEFAULT_INSTALLERS,
});
