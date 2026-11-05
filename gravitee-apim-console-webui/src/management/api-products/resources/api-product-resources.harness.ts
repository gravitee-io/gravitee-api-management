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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTableHarness } from '@angular/material/table/testing';

export class ApiProductResourcesHarness extends ComponentHarness {
  static readonly hostSelector = 'api-product-resources';

  public getTable = this.locatorFor(MatTableHarness.with({ selector: '[aria-label="API Product Resources"]' }));
  public getAddButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="api_product_resources_add_button"]' }));

  public async clickAddResource(): Promise<void> {
    const addButton = await this.getAddButton();
    if (!addButton) {
      throw new Error('Add resource button is not available');
    }
    await addButton.click();
  }

  public async clickToggle(rowIndex: number): Promise<void> {
    await (await this.actionButton(rowIndex, 'api_product_resources_toggle_button')).click();
  }

  public async clickEdit(rowIndex: number): Promise<void> {
    await (await this.actionButton(rowIndex, 'api_product_resources_edit_button')).click();
  }

  public async clickRemove(rowIndex: number): Promise<void> {
    await (await this.actionButton(rowIndex, 'api_product_resources_remove_button')).click();
  }

  public async getToggleButton(rowIndex: number) {
    return this.optionalActionButton(rowIndex, 'api_product_resources_toggle_button');
  }

  public async getEditButton(rowIndex: number) {
    return this.optionalActionButton(rowIndex, 'api_product_resources_edit_button');
  }

  public async getRemoveButton(rowIndex: number) {
    return this.optionalActionButton(rowIndex, 'api_product_resources_remove_button');
  }

  private async actionButton(rowIndex: number, testId: string) {
    const button = await this.optionalActionButton(rowIndex, testId);
    if (!button) {
      throw new Error(`Missing ${testId} on row ${rowIndex}`);
    }
    return button;
  }

  private async optionalActionButton(rowIndex: number, testId: string) {
    const table = await this.getTable();
    const rows = await table.getRows();
    const cells = await rows[rowIndex].getCells({ columnName: 'actions' });
    return cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: `[data-testid="${testId}"]` }));
  }
}
