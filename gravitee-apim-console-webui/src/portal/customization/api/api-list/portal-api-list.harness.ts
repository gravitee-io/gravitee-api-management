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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatIconHarness } from '@angular/material/icon/testing';

interface PortalApiListHarnessData {
  name: string;
  value: string;
  updateButton?: MatButtonHarness;
  deleteButton?: MatButtonHarness;
}

export class PortalApiListHarness extends ComponentHarness {
  static hostSelector = 'app-portal-api-list';

  private apiKeyHeaderLocator = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="apiKeyHeader"]' }));
  private addButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: 'button[data-testid="add-information-button"]' }));
  private tableLocator = this.locatorFor(MatTableHarness);
  private resetButtonLocator = this.locatorFor(MatButtonHarness.with({ text: /Discard/i }));
  private editButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: 'button[data-testid="edit-button"]' }));
  private deleteButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: 'button[data-testid="delete-button"]' }));
  private badgeWarningLocator1 = this.locatorFor('.gio-badge-warning[data-testid="badge-warning-1"]');
  private badgeWarningLocator2 = this.locatorFor('.gio-badge-warning[data-testid="badge-warning-2"]');
  private badgeIconLocator1 = this.locatorFor(
    MatIconHarness.with({ selector: 'mat-icon[svgIcon="gio:chat-lines"][data-testid="badge-icon-1"]' }),
  );
  private badgeIconLocator2 = this.locatorFor(
    MatIconHarness.with({ selector: 'mat-icon[svgIcon="gio:chat-lines"][data-testid="badge-icon-2"]' }),
  );
  private noDataRowLocator = this.locatorFor('tr[data-testid="no-data-row"]');
  private rows = this.locatorForAll(MatRowHarness);

  async setApiKeyHeader(value: string): Promise<void> {
    const input = await this.apiKeyHeaderLocator();
    await input.setValue(value);
  }

  async getBadgeWarningText1(): Promise<string> {
    const badge = await this.badgeWarningLocator1();
    return badge.text();
  }

  async getBadgeWarningText2(): Promise<string> {
    const badge = await this.badgeWarningLocator2();
    return badge.text();
  }

  async getBadgeIconName1(): Promise<string> {
    const icon = await this.badgeIconLocator1();
    return icon.getName();
  }

  async getBadgeIconName2(): Promise<string> {
    const icon = await this.badgeIconLocator2();
    return icon.getName();
  }

  public async getAddButton() {
    return this.addButtonLocator();
  }

  async getTableRows(): Promise<any[]> {
    const table = await this.tableLocator();
    return table.getRows();
  }

  async getApiKeyHeader(): Promise<string> {
    const input = await this.apiKeyHeaderLocator();
    return input.getValue();
  }

  async getDeleteButton(): Promise<MatButtonHarness | undefined> {
    return await this.deleteButtonLocator();
  }

  async getEditButton(): Promise<MatButtonHarness | undefined> {
    return await this.editButtonLocator();
  }

  async getNoDataRowText(): Promise<string> {
    const noDataRow = await this.noDataRowLocator();
    return noDataRow.text();
  }

  async hasRows(): Promise<boolean> {
    const rows = await this.rows();
    return rows.length > 0;
  }

  async getRowByIndex(index: number): Promise<PortalApiListHarnessData> {
    return this.tableLocator()
      .then((table) => table.getRows())
      .then((rows) => rows[index])
      .then((row) =>
        Promise.all([row.getCells({ columnName: 'name' }), row.getCells({ columnName: 'value' }), row.getCells({ columnName: 'actions' })]),
      )
      .then(([names, value, actions]) =>
        Promise.all([
          names[0].getText(),
          value[0].getText(),
          actions[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="edit-button"]' })),
          actions[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="delete-button"]' })),
        ]),
      )
      .then(([name, value, updateButton, deleteButton]) => ({
        name,
        value,
        updateButton,
        deleteButton,
      }));
  }
}
