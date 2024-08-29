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
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatOptionHarness, OptionHarnessFilters } from '@angular/material/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class IntegrationGeneralGroupsHarness extends ComponentHarness {
  static readonly hostSelector = 'integration-general-access-groups';

  protected getReadOnlyGroups = this.locatorFor('.integration-access-groups__body__read-only-groups');
  protected getFillForm = this.locatorFor(MatFormFieldHarness);
  protected getGroupsList = this.locatorFor(MatSelectHarness);
  protected getSaveButton = this.locatorForOptional(MatButtonHarness.with({ text: 'Save' }));

  async getFillFormLabel(): Promise<string> {
    return this.getFillForm().then((form) => form.getLabel());
  }

  async isFillFormPresent(): Promise<boolean> {
    return this.getFillForm()
      .then((_) => true)
      .catch((_) => false);
  }

  async isFillFormControlDirty(): Promise<boolean> {
    return this.getFillForm().then((form) => form.isControlDirty());
  }

  async openGroupsList(): Promise<void> {
    return this.getGroupsList().then(async (selectList) => {
      if (!(await selectList.isOpen())) {
        await selectList.open();
      }
    });
  }

  async closeGroupsList(): Promise<void> {
    return this.getGroupsList().then((selectList) => selectList.close());
  }

  async getGroups(filters: OptionHarnessFilters = {}): Promise<MatOptionHarness[]> {
    await this.openGroupsList();
    return this.getGroupsList().then((selectList) => selectList.getOptions(filters));
  }

  async getSelectedGroups(): Promise<MatOptionHarness[]> {
    return this.getGroups({ isSelected: true });
  }

  async selectGroups(filters: OptionHarnessFilters = {}): Promise<void> {
    await this.openGroupsList();
    return this.getGroupsList().then((selectList) => selectList.clickOptions(filters));
  }

  async getGroupsListValueText(): Promise<string> {
    return this.getGroupsList().then((selectList) => selectList.getValueText());
  }

  async isReadOnlyGroupsPresent(): Promise<boolean> {
    return this.getReadOnlyGroups()
      .then((_) => true)
      .catch((_) => false);
  }

  async getReadOnlyGroupsText(): Promise<string> {
    return this.getReadOnlyGroups().then((readOnlyGroups) => readOnlyGroups.text());
  }

  async isSaveButtonVisible(): Promise<boolean> {
    return (await this.getSaveButton()) !== null;
  }

  async isSaveButtonDisabled(): Promise<boolean> {
    return (await this.isSaveButtonVisible()) && this.getSaveButton().then((btn) => btn.isDisabled());
  }

  async clickSave(): Promise<void> {
    return this.getSaveButton().then((btn) => btn.click());
  }
}
