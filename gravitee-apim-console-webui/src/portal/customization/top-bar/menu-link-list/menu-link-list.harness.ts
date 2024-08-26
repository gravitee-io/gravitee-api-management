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
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

interface MenuLinkListHarnessData {
  name: string;
  type: string;
  target: string;
  updateButton?: MatButtonHarness;
  deleteButton?: MatButtonHarness;
}
export class MenuLinkListHarness extends ComponentHarness {
  public static hostSelector = '.menu-link-list';

  protected getTable = this.locatorFor(MatTableHarness);
  protected getAddLinkButtonHarness = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="add-link"]' }));
  async getAddLinkButton(): Promise<MatButtonHarness> {
    return this.getAddLinkButtonHarness().catch((_) => undefined);
  }

  async addLinkButtonIsActive() {
    return this.getAddLinkButtonHarness()
      .then((btn) => btn.isDisabled())
      .then((res) => !res);
  }
  async openAddLink(): Promise<void> {
    return this.getAddLinkButtonHarness().then((btn) => btn.click());
  }

  async countRows(): Promise<number> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows.length);
  }

  async getRowByIndex(index: number): Promise<MenuLinkListHarnessData> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index])
      .then((row) =>
        Promise.all([
          row.getCells({ columnName: 'name' }),
          row.getCells({ columnName: 'type' }),
          row.getCells({ columnName: 'target' }),
          row.getCells({ columnName: 'actions' }),
        ]),
      )
      .then(([names, types, target, actions]) =>
        Promise.all([
          names[0].getText(),
          types[0].getText(),
          target[0].getText(),
          actions[0].getHarness(MatButtonHarness.with({ selector: '.update-link' })).catch((_) => undefined),
          actions[0].getHarness(MatButtonHarness.with({ selector: '.delete-link' })).catch((_) => undefined),
        ]),
      )
      .then(([name, type, target, updateButton, deleteButton]) => ({
        name,
        type,
        target,
        updateButton,
        deleteButton,
      }));
  }
}
