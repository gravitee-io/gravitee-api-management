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
import { MatInputHarness } from '@angular/material/input/testing';

export interface ApiResourcesEditDialogHarnessOptions extends BaseHarnessFilters {}

export class ApiResourcesEditDialogHarness extends ComponentHarness {
  public static readonly hostSelector = `api-resources-edit-dialog`;

  public static with(options: ApiResourcesEditDialogHarnessOptions): HarnessPredicate<ApiResourcesEditDialogHarness> {
    return new HarnessPredicate(ApiResourcesEditDialogHarness, options);
  }

  protected _title = this.locatorForOptional(MatDialogSection.TITLE);
  protected _content = this.locatorForOptional(MatDialogSection.CONTENT);
  protected _actions = this.locatorForOptional(MatDialogSection.ACTIONS);

  public async getTitleText(): Promise<string> {
    return (await this._title())?.text() ?? '';
  }

  public async getContentText(): Promise<string> {
    return (await this._content())?.text() ?? '';
  }

  public async getActionsText(): Promise<string> {
    return (await this._actions())?.text() ?? '';
  }

  public async close(): Promise<void> {
    const closeButton = await this.locatorFor(MatButtonHarness.with({ text: /Close/ }))();
    await closeButton.click();
  }

  public async setResourceName(resourceName: string): Promise<void> {
    const input = await this.locatorFor(MatInputHarness.with({ selector: '[formControlName="name"]' }))();
    await input.setValue(resourceName);
  }

  public async save(): Promise<void> {
    const confirmButton = await this.locatorFor(MatButtonHarness.with({ text: /Save/ }))();
    await confirmButton.click();
  }
}
