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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconHarness } from '@angular/material/icon/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export class SubscriptionApiKeysHarness extends ComponentHarness {
  static readonly hostSelector = 'subscription-api-keys';

  private getTable = this.locatorFor(MatTableHarness);
  private revokeButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[aria-label="Button to revoke an API Key"]' }));
  private renewButton = this.locatorForOptional(MatButtonHarness.with({ text: /Renew/ }));

  async computeTableCells() {
    const table = await this.getTable();

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() =>
      rows.map(async row => {
        const activeIconCell = (await row.getCells({ columnName: 'active-icon' }))[0];
        const activeIconCellIconName = await (await activeIconCell.getHarness(MatIconHarness)).getName();

        const keyCell = (await row.getCells({ columnName: 'key' }))[0];
        const keyCellText = await (await keyCell.getHarness(MatInputHarness)).getValue();

        const createdAtCell = (await row.getCells({ columnName: 'createdAt' }))[0];
        const createdAtCellText = await createdAtCell.getText();

        const endDateCell = (await row.getCells({ columnName: 'endDate' }))[0];
        const endDateCellText = await endDateCell.getText();

        const actionsCell = (await row.getCells({ columnName: 'actions' }))[0];
        const actionsCellText = (await actionsCell.getHarnessOrNull(MatButtonHarness)) === null ? undefined : 'hasRevokeButton';

        return {
          activeIcon: activeIconCellIconName,
          key: keyCellText,
          createdAt: createdAtCellText,
          endDate: endDateCellText,
          actions: actionsCellText,
        };
      }),
    );
    return { headerCells, rowCells };
  }

  public async renewApiKey(): Promise<void> {
    const button = await this.renewButton();
    return await button.click();
  }

  public async revokeApiKey(): Promise<void> {
    const button = await this.revokeButton();
    return await button.click();
  }
}
