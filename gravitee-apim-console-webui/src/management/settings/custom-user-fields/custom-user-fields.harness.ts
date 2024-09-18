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
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class CustomUserFieldsHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-custom-user-fields';

  private addButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=custom-field_add_button]' }));
  private tableLocator = this.locatorForOptional(MatTableHarness);

  public getTable = async () => this.tableLocator();

  public getAddButton = async () => this.addButtonLocator();

  public rowsNumber = async (): Promise<number> => {
    return this.tableLocator()
      .then((table: MatTableHarness) => table.getRows())
      .then((rows: MatRowHarness[]) => rows.length);
  };

  private getEditRowButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index].getCells({ columnName: 'actions' }).then((cells) => {
      return cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=edit-button]' }));
    });
  };

  public editField = async (index: number) => {
    const deleteButton = await this.getEditRowButton(index);
    await deleteButton.click();
  };

  private getDeleteRowButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index].getCells({ columnName: 'actions' }).then((cells) => {
      return cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=delete-button]' }));
    });
  };

  public deleteField = async (index: number) => {
    const deleteButton = await this.getDeleteRowButton(index);
    await deleteButton.click();
  };
}
