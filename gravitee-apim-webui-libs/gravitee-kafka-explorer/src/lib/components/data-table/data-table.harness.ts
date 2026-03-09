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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MatProgressBarHarness } from '@angular/material/progress-bar/testing';

export class DataTableHarness extends ComponentHarness {
  static hostSelector = 'gke-data-table';

  private readonly getFilterInput = this.locatorForOptional(MatInputHarness);
  private readonly getPaginator = this.locatorForOptional(MatPaginatorHarness);
  private readonly getProgressBar = this.locatorForOptional(MatProgressBarHarness);
  private readonly getEmptyMessage = this.locatorForOptional('.data-table__empty');

  static with(options: BaseHarnessFilters = {}): HarnessPredicate<DataTableHarness> {
    return new HarnessPredicate(DataTableHarness, options);
  }

  async isLoading() {
    return (await this.getProgressBar()) !== null;
  }

  async getEmptyText() {
    const el = await this.getEmptyMessage();
    return el ? el.text() : null;
  }

  async hasFilter() {
    return (await this.getFilterInput()) !== null;
  }

  async setFilter(value: string) {
    const input = await this.getFilterInput();
    if (!input) throw new Error('Filter input not found');
    await input.setValue(value);
  }

  async hasPaginator() {
    return (await this.getPaginator()) !== null;
  }

  async getPaginatorHarness() {
    return this.getPaginator();
  }

  async getRangeLabel() {
    const paginator = await this.getPaginator();
    if (!paginator) throw new Error('Paginator not found');
    return paginator.getRangeLabel();
  }

  async goToNextPage() {
    const paginator = await this.getPaginator();
    if (!paginator) throw new Error('Paginator not found');
    await paginator.goToNextPage();
  }
}
