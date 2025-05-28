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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatDialogSection } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import {OpenApiToMcpToolsHarness} from "../open-api-to-mcp-tools/open-api-to-mcp-tools.harness";

export interface ImportMcpToolsDialogHarnessOptions extends BaseHarnessFilters {}

export class ImportMcpToolsDialogHarness extends ComponentHarness {
  public static readonly hostSelector = `import-mcp-tools-dialog`;

  public static with(options: ImportMcpToolsDialogHarnessOptions): HarnessPredicate<ImportMcpToolsDialogHarness> {
    return new HarnessPredicate(ImportMcpToolsDialogHarness, options);
  }

  protected _title = this.locatorForOptional(MatDialogSection.TITLE);
  protected _openApiToMcpTools = this.locatorFor(OpenApiToMcpToolsHarness);

  public async setOpenApiValue(value: string): Promise<void> {
    const openApiToMcpTools = await this._openApiToMcpTools();
    await openApiToMcpTools.setValue(value);
  }

  public async getOpenApiValue(): Promise<string> {
    const openApiToMcpTools = await this._openApiToMcpTools();
    return openApiToMcpTools.getValue();
  }

  public async close(): Promise<void> {
    const closeButton = await this.locatorFor(MatButtonHarness.with({ text: /Close/ }))();
    await closeButton.click();
  }

  public async importTools(): Promise<void> {
    const confirmButton = await this.locatorFor(MatButtonHarness.with({ text: /Import Tools/ }))();
    await confirmButton.click();
  }
}
