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
import { MatSelectHarness } from '@angular/material/select/testing';

/** Host for the “Manage groups” dialog body (`MatDialog.open` → `ApiProductGroupsComponent`). */
export class ApiProductGroupsDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'api-product-groups';

  async getHeadingTitle(): Promise<string> {
    const el = await this.locatorFor('[data-testid="api_product_groups_dialog_title"]')();
    return (await el.text()).trim();
  }

  async getGroupsSelect(): Promise<MatSelectHarness | null> {
    return this.locatorForOptional(MatSelectHarness.with({ selector: '[formControlName="selectedGroups"]' }))();
  }

  async getSaveButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="api_product_groups_dialog_save"]' }))();
  }

  /** No-op when read-only (Save is hidden); throws if editable but button is missing. */
  async clickSave(): Promise<void> {
    const btn = await this.getSaveButton();
    if (!btn) {
      throw new Error('Save API Product groups button not found (dialog may be read-only).');
    }
    await btn.click();
  }

  async selectGroupsByOptionTexts(texts: string[]): Promise<void> {
    const select = await this.getGroupsSelect();
    if (!select) {
      throw new Error('Groups mat-select not found (read-only mode?).');
    }
    await select.open();
    for (const text of texts) {
      await select.clickOptions({ text });
    }
    await select.close();
  }
}
