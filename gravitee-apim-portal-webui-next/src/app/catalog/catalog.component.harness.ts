/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

export class CatalogHarness extends ComponentHarness {
  static readonly hostSelector = 'app-catalog';

  private readonly locateTable = this.locatorForOptional(MatTableHarness);
  private readonly locateRows = this.locatorForAll('.api-list__table__row');
  private readonly locateEmptyState = this.locatorForOptional('.api-list__empty-state');
  private readonly locateLoader = this.locatorForOptional('app-loader');

  async getAllRowsCellText(): Promise<Record<string, string>[]> {
    const table = await this.locateTable();
    if (!table) return [];
    return Promise.all((await table.getRows()).map(row => row.getCellTextByColumnName()));
  }

  async selectRow(index: number): Promise<void> {
    const rows = await this.locateRows();
    await rows[index].click();
  }

  async getEmptyStateText(): Promise<string | null> {
    const emptyState = await this.locateEmptyState();
    return emptyState?.text() ?? null;
  }

  async isLoading(): Promise<boolean> {
    return (await this.locateLoader()) !== null;
  }
}
