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
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatRadioButtonHarness } from '@angular/material/radio/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { GioTableWrapperHarness } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

export class AddApiToCategoryDialogHarness extends MatDialogHarness {
  static override hostSelector = 'add-api-to-category-dialog';
  protected getGioTable = this.locatorFor(GioTableWrapperHarness);
  protected getMatTable = this.locatorFor(MatTableHarness);
  protected getAddApiButton = this.locatorFor(MatButtonHarness.with({ text: 'Add API' }));

  public async getRows(): Promise<MatRowHarness[]> {
    return await this.getMatTable().then((table) => table.getRows());
  }

  public async searchApis(query: string): Promise<void> {
    return await this.getGioTable().then((table) => table.setSearchValue(query));
  }

  public async getNameByIndex(index: number): Promise<string> {
    return await this.getMatTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index].getCells({ columnName: 'name' }))
      .then((cells) => cells[0].getText());
  }

  public async getSelectApiByIndex(index: number): Promise<MatRadioButtonHarness> {
    return await this.getMatTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index])
      .then((row) => row.getCells({ columnName: 'selectApi' }))
      .then((cells) => cells[0].getHarnessOrNull(MatRadioButtonHarness));
  }

  public async getSubmitButton(): Promise<MatButtonHarness> {
    return await this.getAddApiButton();
  }
}
