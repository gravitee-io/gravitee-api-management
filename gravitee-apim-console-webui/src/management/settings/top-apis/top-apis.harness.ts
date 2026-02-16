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

export class TopApisHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-top-apis';

  private getTable = this.locatorForOptional(MatTableHarness);
  private addTopApisLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=add-top-api-button]' }));

  private getDeleteRowButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index].getCells({ columnName: 'actions' }).then(cells => {
      return cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=delete-top-api-button]' }));
    });
  };

  public getAddButton = () => {
    return this.addTopApisLocator();
  };

  private getUpButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then(cells => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=move-up-top-api-button]' })));
  };

  private getDownButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then(cells => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=move-down-top-api-button]' })));
  };

  public rowsNumber = async (): Promise<number> => {
    return this.getTable()
      .then((table: MatTableHarness) => table.getRows())
      .then((rows: MatRowHarness[]) => rows.length);
  };

  public deleteTopApi = async (index: number) => {
    const deleteButton = await this.getDeleteRowButton(index);
    await deleteButton.click();
  };

  public moveTopApiUp = async (index: number) => {
    const deleteButton = await this.getUpButton(index);
    await deleteButton.click();
  };

  public moveTopApiDown = async (index: number) => {
    const deleteButton = await this.getDownButton(index);
    await deleteButton.click();
  };
}
