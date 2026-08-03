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
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { ComponentHarness, HarnessLoader } from '@angular/cdk/testing';

export class CategoryHarness extends ComponentHarness {
  static hostSelector = 'category';

  async getTitleInput(harnessLoader: HarnessLoader): Promise<MatInputHarness> {
    return await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="title"]' }));
  }

  async getDescriptionInput(harnessLoader: HarnessLoader): Promise<MatInputHarness> {
    return await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
  }

  async getVisibleToggle(harnessLoader: HarnessLoader): Promise<MatSlideToggleHarness> {
    return await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="visible"]' }));
  }

  async getAddApiButton(harnessLoader: HarnessLoader): Promise<MatButtonHarness | null> {
    return await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.add-button' }));
  }

  async getSaveBar(rootLoader: HarnessLoader): Promise<GioSaveBarHarness> {
    return await rootLoader.getHarness(GioSaveBarHarness);
  }

  async getTableRows(harnessLoader: HarnessLoader): Promise<MatRowHarness[]> {
    return await harnessLoader.getHarness(MatTableHarness).then(table => table.getRows());
  }

  async getNameByRowIndex(harnessLoader: HarnessLoader, index: number): Promise<string> {
    return await this.getTextByColumnNameAndRowIndex(harnessLoader, 'name', index);
  }

  async getTextByColumnNameAndRowIndex(harnessLoader: HarnessLoader, columnName: string, index: number): Promise<string> {
    return await harnessLoader
      .getHarness(MatTableHarness)
      .then(table => table.getRows())
      .then(rows => rows[index])
      .then(row => row.getCellTextByIndex({ columnName }).then(cell => cell[0]));
  }
}
