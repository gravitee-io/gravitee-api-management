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
import { MatLegacyTableHarness as MatTableHarness } from '@angular/material/legacy-table/testing';
import { MatLegacyButtonHarness as MatButtonHarness } from '@angular/material/legacy-button/testing';

interface GioMetadataHarnessData {
  key: string;
  name: string;
  updateButton?: MatButtonHarness;
  deleteButton?: MatButtonHarness;
}
export class GioMetadataHarness extends ComponentHarness {
  public static hostSelector = '.gio-metadata';

  protected getTable = this.locatorFor(MatTableHarness);
  protected getAddMetadataButtonHarness = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="add-metadata"]' }));

  async getAddMetadataButton(): Promise<MatButtonHarness> {
    return this.getAddMetadataButtonHarness().catch((_) => undefined);
  }

  async addMetadataButtonIsActive() {
    return this.getAddMetadataButtonHarness()
      .then((btn) => btn.isDisabled())
      .then((res) => !res);
  }
  async openAddMetadata(): Promise<void> {
    return this.getAddMetadataButtonHarness().then((btn) => btn.click());
  }

  async countRows(): Promise<number> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows.length);
  }

  async getRowByIndex(index: number): Promise<GioMetadataHarnessData> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index])
      .then((row) =>
        Promise.all([row.getCells({ columnName: 'key' }), row.getCells({ columnName: 'name' }), row.getCells({ columnName: 'actions' })]),
      )
      .then(([keys, names, actions]) =>
        Promise.all([
          keys[0].getText(),
          names[0].getText(),
          actions[0].getHarness(MatButtonHarness.with({ selector: '.update-metadata' })).catch((_) => undefined),
          actions[0].getHarness(MatButtonHarness.with({ selector: '.delete-metadata' })).catch((_) => undefined),
        ]),
      )
      .then(([key, name, updateButton, deleteButton]) => ({
        key,
        name,
        updateButton,
        deleteButton,
      }));
  }
}
