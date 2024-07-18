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
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';

export interface EnvironmentFlowsAddEditDialogHarnessOptions extends BaseHarnessFilters {}

export class EnvironmentFlowsAddEditDialogHarness extends ComponentHarness {
  public static readonly hostSelector = `environment-flows-add-edit-dialog`;

  public static with(options: EnvironmentFlowsAddEditDialogHarnessOptions): HarnessPredicate<EnvironmentFlowsAddEditDialogHarness> {
    return new HarnessPredicate(EnvironmentFlowsAddEditDialogHarness, options);
  }

  protected _title = this.locatorForOptional(MatDialogSection.TITLE);
  protected _content = this.locatorForOptional(MatDialogSection.CONTENT);
  protected _actions = this.locatorForOptional(MatDialogSection.ACTIONS);
  protected nameInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName="name"]' }));
  protected descriptionInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName="description"]' }));
  protected phaseToggleGroup = this.locatorForOptional(MatButtonToggleGroupHarness.with({ selector: '[formControlName="phase"]' }));

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

  public async confirmMyAction(): Promise<void> {
    const confirmButton = await this.locatorFor(MatButtonHarness.with({ text: /My action/ }))();
    await confirmButton.click();
  }

  public async setName(text: string) {
    await this.nameInput().then((input) => input.setValue(text));
  }
  public async getName() {
    return await this.nameInput().then((input) => input.getValue());
  }

  public async setDescription(text: string) {
    await this.descriptionInput().then((input) => input.setValue(text));
  }
  public async getDescription() {
    return await this.descriptionInput().then((input) => input.getValue());
  }

  public async setPhase(text: string) {
    const toggleGroup = await this.phaseToggleGroup().then((group) => group.getToggles({ text }));

    if (toggleGroup.length === 1) {
      await toggleGroup[0].check();
    }
  }
  public async getPhase() {
    const toggle = await this.phaseToggleGroup().then((group) => group.getToggles({ checked: true }));
    return toggle.length === 1 ? toggle[0].getText() : '';
  }

  async save() {
    const saveButton = await this.locatorFor(MatButtonHarness.with({ text: /Save/ }))();
    await saveButton.click();
  }
}
