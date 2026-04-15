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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatDialogSection } from '@angular/material/dialog/testing';

export interface ClusterManageGroupsDialogHarnessOptions extends BaseHarnessFilters {}

export class ClusterManageGroupsDialogHarness extends ComponentHarness {
  public static readonly hostSelector = `cluster-manage-groups-dialog`;

  public static with(options: ClusterManageGroupsDialogHarnessOptions): HarnessPredicate<ClusterManageGroupsDialogHarness> {
    return new HarnessPredicate(ClusterManageGroupsDialogHarness, options);
  }

  protected _title = this.locatorForOptional(MatDialogSection.TITLE);
  protected getGroupsSelect = this.locatorForOptional(MatSelectHarness);

  public async getTitleText(): Promise<string | null> {
    const title = await this._title();
    return title ? title.text() : null;
  }

  public async cancel(): Promise<void> {
    const cancelButton = await this.locatorFor(MatButtonHarness.with({ text: /Cancel/ }))();
    await cancelButton.click();
  }

  public async save(): Promise<void> {
    const saveButton = await this.locatorFor(MatButtonHarness.with({ text: /Save/ }))();
    await saveButton.click();
  }

  public async getAvailableGroups(): Promise<string[]> {
    const select = await this.getGroupsSelect();
    if (!select) {
      return [];
    }
    await select.open();
    const options = await select.getOptions();
    return Promise.all(options.map(o => o.getText()));
  }

  public async selectGroups(groupsToSelect: string[]): Promise<void[]> {
    const select = await this.getGroupsSelect();
    if (!select) {
      return [];
    }
    await select.open();
    return Promise.all(groupsToSelect.map(g => select.clickOptions({ text: g })));
  }

  public async getSelectedGroupsText(): Promise<string | null> {
    const select = await this.getGroupsSelect();
    if (!select) {
      return null;
    }
    return select.getValueText();
  }

  public async getReadOnlyGroupsText(): Promise<string | null> {
    // When user lacks permission, the dialog shows a read-only list at '.read-only-groups .mat-body'
    const readOnlyEl = await this.locatorForOptional('.read-only-groups .mat-body')();
    return readOnlyEl ? readOnlyEl.text() : null;
  }
}
