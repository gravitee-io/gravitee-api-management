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

import { HarnessLoader } from '@angular/cdk/testing';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export async function getTableRows(harnessLoader: HarnessLoader): Promise<MatRowHarness[]> {
  return await harnessLoader.getHarness(MatTableHarness).then((table) => table.getRows());
}

export async function getNameByRowIndex(harnessLoader: HarnessLoader, index: number): Promise<string> {
  return await getTextByColumnNameAndRowIndex(harnessLoader, 'name', index);
}

export async function getApiCountByRowIndex(harnessLoader: HarnessLoader, index: number): Promise<string> {
  return await getTextByColumnNameAndRowIndex(harnessLoader, 'count', index);
}

export async function getDescriptionByRowIndex(harnessLoader: HarnessLoader, index: number): Promise<string> {
  return await getTextByColumnNameAndRowIndex(harnessLoader, 'description', index);
}

export async function getTextByColumnNameAndRowIndex(harnessLoader: HarnessLoader, columnName: string, index: number): Promise<string> {
  return await getTableRows(harnessLoader)
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
