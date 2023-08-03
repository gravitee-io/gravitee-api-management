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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

export class ApiGeneralGroupsHarness extends ComponentHarness {
  static hostSelector = 'ng-api-portal-groups';

  protected getReadOnlyGroups = this.locatorFor('.api-portal-access-groups__body__read-only-groups');
  protected getFillForm = this.locatorFor(MatFormFieldHarness);
  protected getGroupsList = this.locatorFor(MatSelectHarness);
  protected getSaveBar = this.locatorFor(GioSaveBarHarness);

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

  async isSaveBarVisible(): Promise<boolean> {
    return this.getSaveBar().then((saveBar) => saveBar.isVisible());
  }

  async isResetButtonVisible(): Promise<boolean> {
    return this.getSaveBar().then((saveBar) => saveBar.isResetButtonVisible());
  }

  async clickSubmit(): Promise<void> {
    return this.getSaveBar().then((saveBar) => saveBar.clickSubmit());
  }

  async clickReset(): Promise<void> {
    return this.getSaveBar().then((saveBar) => saveBar.clickReset());
  }
}
