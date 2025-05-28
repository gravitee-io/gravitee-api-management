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
import {MatExpansionPanelHarness} from "@angular/material/expansion/testing";

export class ToolsDisplayHarness extends ComponentHarness {
  static hostSelector = 'tools-display';

  private getEmptyStateText = this.locatorFor('.empty-state');
  private getToolPanels = this.locatorForAll(MatExpansionPanelHarness);

  /**
   * Gets the empty state message when no tools are configured
   */
  async getEmptyStateMessage(): Promise<string | null> {
    try {
      const emptyState = await this.getEmptyStateText();
      return emptyState.text();
    } catch {
      return null;
    }
  }

  /**
   * Gets all tool elements displayed in the component
   */
  async getTools(): Promise<string[]> {
    const tools = await this.getToolPanels();
    return Promise.all(tools.map(async (tool) => {
      return await tool.getTextContent()
    })
    );
  }

  /**
   * Checks if the component is in empty state
   */
  async isEmptyState(): Promise<boolean> {
    const message = await this.getEmptyStateMessage();
    return message !== null && message.includes('No tools are configured');
  }

  /**
   * Gets the number of tools displayed
   */
  async getToolCount(): Promise<number> {
    const tools = await this.getTools();
    return tools.length;
  }
}
