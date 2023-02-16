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
import { MatCellHarness, MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatOptionHarness, OptionHarnessFilters } from '@angular/material/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class ApiPortalMembersHarness extends ComponentHarness {
  static hostSelector = 'api-portal-members';

  protected getMemberTableElement = this.locatorFor(MatTableHarness);
  protected getNotificationsCheckbox = this.locatorFor(MatCheckboxHarness);
  protected getSaveBarElement = this.locatorFor(GioSaveBarHarness);

  async getTableRows(): Promise<MatRowHarness[]> {
    return this.getMemberTableElement().then((table) => table.getRows());
  }

  async getMembersName(): Promise<string[]> {
    const rows = await this.getTableRows();
    return Promise.all(
      rows.map(async (row) => {
        return await row.getCells().then(async (cells) => {
          return await cells[1].getText();
        });
      }),
    );
  }

  async getMemberRoleSelectForRowIndex(i: number): Promise<MatSelectHarness> {
    const rows = await this.getTableRows();
    expect(rows.length).toBeGreaterThan(i);
    const roleCell = await rows[i].getCells({ columnName: 'role' });
    expect(roleCell.length).toEqual(1);
    return roleCell[0].getHarness(MatSelectHarness);
  }

  async getMemberRoleSelectOptions(rowIndex: number, options: OptionHarnessFilters = {}): Promise<MatOptionHarness[]> {
    return this.getMemberRoleSelectForRowIndex(rowIndex).then(async (select) => {
      await select.open();
      return await select.getOptions(options);
    });
  }

  async isMemberRoleSelectDisabled(rowIndex: number): Promise<boolean> {
    return this.getMemberRoleSelectForRowIndex(rowIndex).then((select) => select.isDisabled());
  }

  async getMemberDeleteButton(rowIndex: number): Promise<MatButtonHarness> {
    const deleteCell = await this.getMemberDeleteCell(rowIndex);
    return deleteCell.getHarness(MatButtonHarness);
  }

  async getMemberDeleteCell(rowIndex: number): Promise<MatCellHarness> {
    const rows = await this.getTableRows();
    expect(rows.length).toBeGreaterThan(rowIndex);
    const deleteCell = await rows[rowIndex].getCells({ columnName: 'delete' });
    expect(deleteCell.length).toEqual(1);
    return deleteCell[0];
  }

  async isMemberDeleteButtonVisible(rowIndex: number): Promise<boolean> {
    return this.getMemberDeleteCell(rowIndex).then(async (cell) => {
      const harnesses = await cell.getAllHarnesses(MatButtonHarness);
      return harnesses !== null && harnesses.length > 0;
    });
  }

  async isNotificationsCheckboxChecked(): Promise<boolean> {
    return this.getNotificationsCheckbox().then((cb) => cb.isChecked());
  }

  async toggleNotificationCheckbox(): Promise<void> {
    return this.getNotificationsCheckbox().then((cb) => cb.toggle());
  }

  async isSaveBarVisible(): Promise<boolean> {
    return this.getSaveBarElement().then((sb) => sb.isVisible());
  }

  async clickOnSave(): Promise<void> {
    return this.getSaveBarElement().then((sb) => sb.clickSubmit());
  }
}
