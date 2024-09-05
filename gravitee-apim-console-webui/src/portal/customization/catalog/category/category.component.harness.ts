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
import { HarnessLoader } from '@angular/cdk/testing';

export async function getNameInput(harnessLoader: HarnessLoader): Promise<MatInputHarness> {
  return await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
}

export async function getDescriptionInput(harnessLoader: HarnessLoader): Promise<MatInputHarness> {
  return await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
}

export async function getSaveBar(rootLoader: HarnessLoader): Promise<GioSaveBarHarness> {
  return await rootLoader.getHarness(GioSaveBarHarness);
}

export async function getTableRows(harnessLoader: HarnessLoader): Promise<MatRowHarness[]> {
  return await harnessLoader.getHarness(MatTableHarness).then((table) => table.getRows());
}

export async function getNameByRowIndex(harnessLoader: HarnessLoader, index: number): Promise<string> {
  return await getTextByColumnNameAndRowIndex(harnessLoader, 'name', index);
}

export async function getTextByColumnNameAndRowIndex(harnessLoader: HarnessLoader, columnName: string, index: number): Promise<string> {
  return await harnessLoader
    .getHarness(MatTableHarness)
    .then((table) => table.getRows())
    .then((rows) => rows[index])
    .then((row) => row.getCellTextByIndex({ columnName }).then((cell) => cell[0]));
}

export async function getActionButtonByRowIndexAndTooltip(
  harnessLoader: HarnessLoader,
  rowIndex: number,
  tooltipText: string,
): Promise<MatButtonHarness | null> {
  return await getTableRows(harnessLoader)
    .then((rows) => rows[rowIndex].getCells({ columnName: 'actions' }))
    .then((cells) => cells[0])
    .then((actionCell) => actionCell.getHarnessOrNull(MatButtonHarness.with({ selector: `[mattooltip="${tooltipText}"]` })));
}

export async function addApiToCategory(harnessLoader: HarnessLoader): Promise<void> {
  return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.add-button' })).then((btn) => btn.click());
}
