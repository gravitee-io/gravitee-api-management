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
import { GioMonacoEditorHarness } from '@gravitee/ui-particles-angular';

import { ToolDisplayHarness } from '../tool-display/tool-display.harness';

export class OpenApiToMcpToolsHarness extends ComponentHarness {
  static hostSelector = 'open-api-to-mcp-tools';

  protected locateGioMonacoEditor = this.locatorFor(GioMonacoEditorHarness);
  protected locateToolDisplays = this.locatorForAll(ToolDisplayHarness);
  protected locateErrors = this.locatorForAll('.open-api-to-mcp-tools__error');

  public async setValue(value: string): Promise<void> {
    const monacoEditor = await this.locateGioMonacoEditor();
    await monacoEditor.setValue(value);
  }

  public async getValue(): Promise<string> {
    const monacoEditor = await this.locateGioMonacoEditor();
    return monacoEditor.getValue();
  }

  public async getToolCount(): Promise<number> {
    return this.locateToolDisplays().then(toolsDisplays => toolsDisplays.length);
  }

  public async getErrors(): Promise<string[]> {
    const errorElements = await this.locateErrors();
    if (errorElements.length > 0) {
      return Promise.all(errorElements.map(errorElement => errorElement.text()));
    }
    return [];
  }
}
