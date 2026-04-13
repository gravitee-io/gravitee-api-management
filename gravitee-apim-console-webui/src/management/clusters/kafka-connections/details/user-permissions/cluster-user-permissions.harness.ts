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

import { ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { ClusterGroupMembersHarness } from './group-members/cluster-group-members.harness';

export class ClusterUserPermissionsHarness extends ComponentHarness {
  static readonly hostSelector = 'cluster-user-permissions';

  static with(options: Record<string, unknown> = {}): HarnessPredicate<ClusterUserPermissionsHarness> {
    return new HarnessPredicate(ClusterUserPermissionsHarness, options);
  }

  private getMembersTable = () => this.locatorFor(MatTableHarness)();
  private getAddMembersBtn = () => this.locatorForOptional(MatButtonHarness.with({ text: /Add members/i }))();
  private getManageGroupsBtn = () => this.locatorForOptional(MatButtonHarness.with({ text: /Manage groups/i }))();
  private getSaveBar = () => this.locatorFor(GioSaveBarHarness)();
  public getGroupMembersHarness = (groupId: string) => this.locatorFor(ClusterGroupMembersHarness.with({ groupId }))();

  /** Returns the count of data rows currently displayed in the table. */
  public async getMembersRowCount(): Promise<number> {
    const table = await this.getMembersTable();
    const rows = await table.getRows();
    return rows.length;
  }

  /** Returns the list of displayed member names from the displayName column. */
  public async getDisplayedMemberNames(): Promise<string[]> {
    const table = await this.getMembersTable();
    const rows = await table.getRows();
    const names: string[] = [];
    for (const row of rows) {
      const cell = (await row.getCells({ columnName: 'displayName' }))[0];
      names.push(await cell.getText());
    }
    return names;
  }

  /** Opens and returns the MatSelectHarness for the role column at the given row index. */
  public async getRoleSelectForRow(rowIndex: number): Promise<MatSelectHarness> {
    const row = await this.getRowAt(rowIndex);
    const roleCell = (await row.getCells({ columnName: 'role' }))[0];
    return roleCell.getHarness(MatSelectHarness);
  }

  /** Selects the provided role label for the specified row index. */
  public async selectRoleForRow(rowIndex: number, roleLabel: string | RegExp): Promise<void> {
    const select = await this.getRoleSelectForRow(rowIndex);
    await select.open();
    await select.clickOptions({ text: roleLabel });
  }

  /** Clicks the delete button for the specified row, if present. */
  public async clickDeleteForRow(rowIndex: number): Promise<void> {
    const row = await this.getRowAt(rowIndex);
    const deleteCell = (await row.getCells({ columnName: 'delete' }))[0];
    const btn = await deleteCell.getHarness(MatButtonHarness);
    await btn.click();
  }

  /** Clicks the "Add members" button if visible. */
  public async clickAddMembers(): Promise<void> {
    const btn = await this.getAddMembersBtn();
    if (btn) {
      await btn.click();
    }
  }

  /** Clicks the "Manage groups" button if visible. */
  public async clickManageGroups(): Promise<void> {
    const btn = await this.getManageGroupsBtn();
    if (btn) {
      await btn.click();
    }
  }

  /** Clicks the submit button of the gio-save-bar. */
  public async clickSave(): Promise<void> {
    const saveBar = await this.getSaveBar();
    await saveBar.clickSubmit();
  }

  private async getRowAt(index: number): Promise<any> {
    const table = await this.getMembersTable();
    const rows = await table.getRows();
    if (index < 0 || index >= rows.length) {
      throw new Error(`Row index ${index} is out of bounds (rows: ${rows.length}).`);
    }
    return rows[index];
  }
}
