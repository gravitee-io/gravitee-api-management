/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { DataTableHarness } from '../../../components/data-table/data-table.harness';

export class ConsumerGroupsHarness extends ComponentHarness {
  static hostSelector = 'gke-consumer-groups';

  private readonly getTable = this.locatorFor(MatTableHarness);
  private readonly getDataTable = this.locatorFor(DataTableHarness);

  async getRows() {
    const table = await this.getTable();
    return table.getRows();
  }

  async getRowsData() {
    const rows = await this.getRows();
    return parallel(() => rows.map(row => row.getCellTextByColumnName()));
  }

  async getRowCount() {
    const rows = await this.getRows();
    return rows.length;
  }

  async setFilter(value: string) {
    const dataTable = await this.getDataTable();
    await dataTable.setFilter(value);
  }

  async getRangeLabel() {
    const dataTable = await this.getDataTable();
    return dataTable.getRangeLabel();
  }

  async goToNextPage() {
    const dataTable = await this.getDataTable();
    await dataTable.goToNextPage();
  }

  async isLoading() {
    const dataTable = await this.getDataTable();
    return dataTable.isLoading();
  }
}
