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

export class ToolsDisplayHarness extends ComponentHarness {
  static readonly hostSelector = 'tools-display';

  private locateTitle = this.locatorFor('h5');
  private locateToolElements = this.locatorForAll('div:not(:empty)');
  private locateEmptyMessage = this.locatorFor('div:contains("No tools are configured")');

  async getTitle(): Promise<string> {
    const title = await this.locateTitle();
    return title.text();
  }

  async getTools(): Promise<string[]> {
    const toolElements = await this.locateToolElements();
    const tools: string[] = [];

    for (const element of toolElements) {
      const text = await element.text();
      if (!text.includes('No tools are configured')) {
        tools.push(text.trim());
      }
    }

    return tools;
  }

  async hasTools(): Promise<boolean> {
    const tools = await this.getTools();
    return tools.length > 0;
  }

  async getEmptyMessage(): Promise<string | null> {
    try {
      const emptyMessage = await this.locateEmptyMessage();
      return emptyMessage.text();
    } catch {
      return null;
    }
  }
}
