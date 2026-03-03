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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MatProgressBarHarness } from '@angular/material/progress-bar/testing';
import { MatTableHarness } from '@angular/material/table/testing';

export class TopicsHarness extends ComponentHarness {
  static hostSelector = 'gke-topics';

  private readonly getTable = this.locatorFor(MatTableHarness);
  private readonly getFilterInput = this.locatorFor(MatInputHarness);
  private readonly getPaginator = this.locatorFor(MatPaginatorHarness);
  private readonly getProgressBar = this.locatorForOptional(MatProgressBarHarness);

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
    const input = await this.getFilterInput();
    await input.setValue(value);
  }

  async getPaginatorHarness() {
    return this.getPaginator();
  }

  async getRangeLabel() {
    const paginator = await this.getPaginator();
    return paginator.getRangeLabel();
  }

  async goToNextPage() {
    const paginator = await this.getPaginator();
    await paginator.goToNextPage();
  }

  async isLoading() {
    const progressBar = await this.getProgressBar();
    return progressBar !== null;
  }
}
