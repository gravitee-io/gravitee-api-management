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
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class RuntimeAlertListHarness extends ComponentHarness {
  static hostSelector = 'runtime-alert-list';

  private getAlertTable = () => this.locatorFor(MatTableHarness)();
  private getDeleteButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Button to delete an alert"]' }));

  async computeTableCells() {
    const table = await this.getAlertTable();

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  async deleteAlert(index: number) {
    return this.getDeleteButtons().then(btn => btn[index].click());
  }
}
