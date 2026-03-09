/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { MatExpansionPanelHarness } from '@angular/material/expansion/testing';

export class McpToolHarness extends ComponentHarness {
  static hostSelector = 'app-mcp-tool';

  protected locatePanel = this.locatorFor(MatExpansionPanelHarness);
  protected locatePanelDescription = this.locatorForOptional('[aria-label="MCP tool description"]');

  async getTextContent(): Promise<string> {
    return this.locatePanel().then(panel => panel.getTextContent());
  }

  async getTitleContent(): Promise<string | null> {
    const panel = await this.locatePanel();
    return panel.getTitle();
  }

  async open(): Promise<void> {
    return this.locatePanel().then(panel => panel.expand());
  }

  async getDescriptionContent(): Promise<string | null> {
    const description = await this.locatePanelDescription();
    if (!description) {
      return null;
    }
    return description.text();
  }
}
