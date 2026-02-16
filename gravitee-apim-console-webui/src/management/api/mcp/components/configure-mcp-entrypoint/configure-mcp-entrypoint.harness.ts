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
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ToolDisplayHarness } from '../tool-display/tool-display.harness';

export class ConfigureMcpEntrypointHarness extends ComponentHarness {
  static readonly hostSelector = 'configure-mcp-entrypoint';

  protected locateMcpPathFormField = this.locatorFor(
    MatFormFieldHarness.with({ selector: '.configure-mcp-entrypoint__content__mcp-path' }),
  );
  protected locateMcpPathInput = this.locatorFor(MatInputHarness.with({ selector: 'input[formControlName="mcpPath"]' }));
  protected locateToolDisplays = this.locatorForAll(ToolDisplayHarness);
  protected locateImportToolsButton = this.locatorFor(MatButtonHarness.with({ text: /Generate Tools from OpenAPI/ }));

  async getMcpPathFormField(): Promise<MatFormFieldHarness> {
    return this.locateMcpPathFormField();
  }

  async getMcpPathInput(): Promise<MatInputHarness> {
    return this.locateMcpPathInput();
  }

  async getMcpPathValue(): Promise<string> {
    const input = await this.getMcpPathInput();
    return input.getValue();
  }

  async setMcpPathValue(value: string): Promise<void> {
    const input = await this.getMcpPathInput();
    await input.setValue(value);
  }

  async getMcpPathError(): Promise<string | null> {
    const formField = await this.getMcpPathFormField();
    const errors = await formField.getTextErrors();
    return errors.length > 0 ? errors[0] : null;
  }

  async hasTools(): Promise<boolean> {
    return this.locateToolDisplays().then(tools => tools.length > 0);
  }

  async openImportToolsDialog(): Promise<void> {
    const importToolsButton = await this.locateImportToolsButton();
    await importToolsButton.click();
  }
}
