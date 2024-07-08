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
import { MatTableHarness } from '@angular/material/table/testing';

export class ApiResourcesHarness extends ComponentHarness {
  static readonly hostSelector = 'api-resources';

  public async getDeleteButton(index: number) {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then((cells) => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Button to remove a property"]' })));
  }

  public async clickRemoveButton(rowIndex: number) {
    const deleteButton = await this.getDeleteButton(rowIndex);
    await deleteButton.click();
  }

  public async clickAddResource() {
    const addButton = await this.getAddButton();
    await addButton.click();
  }

  public async clickEditResource(rowIndex: number) {
    const editButton = await this.getEditButton(rowIndex);
    await editButton.click();
  }

  public async getEditButton(index: number) {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then((cells) => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Button to edit a Response Template"]' })));
  }

  public async editRow(index: number) {
    const editButton = await this.getEditButton(index);
    await editButton.click();
  }

  public getTable = this.locatorFor(MatTableHarness.with({ selector: '[aria-label="API Properties"]' }));
  public getAddButton = this.locatorFor(MatButtonHarness.with({ text: /Add resource/ }));
}
