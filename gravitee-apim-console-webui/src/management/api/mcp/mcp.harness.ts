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
import { ComponentHarness } from '@angular/cdk/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { NoMcpEntrypointHarness } from './no-mcp-entrypoint/no-mcp-entrypoint.harness';
import { ConfigureMcpEntrypointHarness } from './components/configure-mcp-entrypoint/configure-mcp-entrypoint.harness';
import { ToolDisplayHarness } from './components/tool-display/tool-display.harness';

export class McpHarness extends ComponentHarness {
  static readonly hostSelector = 'mcp';

  protected locateMcpEntryPointNotFound = this.locatorForOptional(NoMcpEntrypointHarness);
  protected locateConfigureMcpEntrypoint = this.locatorForOptional(ConfigureMcpEntrypointHarness);
  protected locateSaveBar = this.locatorForOptional(GioSaveBarHarness);
  protected locateToolDisplays = this.locatorForAll(ToolDisplayHarness);

  async getMcpEntryPointNotFound(): Promise<NoMcpEntrypointHarness | null> {
    return this.locateMcpEntryPointNotFound();
  }

  async getConfigureMcpEntrypoint(): Promise<ConfigureMcpEntrypointHarness> {
    return this.locateConfigureMcpEntrypoint();
  }

  async getSaveBar(): Promise<GioSaveBarHarness | null> {
    return this.locateSaveBar();
  }

  async getToolDisplays(): Promise<ToolDisplayHarness[]> {
    return this.locateToolDisplays();
  }
}
