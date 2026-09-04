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
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatTableHarness } from '@angular/material/table/testing';

export class CatalogHarness extends ComponentHarness {
  static readonly hostSelector = 'app-catalog';

  private readonly locateTable = this.locatorForOptional(MatTableHarness);
  private readonly locateRows = this.locatorForAll('.api-list__table__row');
  private readonly locateEmptyState = this.locatorForOptional('.api-list__empty-state');
  private readonly locateLoader = this.locatorForOptional('app-loader');
  private readonly locateKinds = this.locatorForAll('.api-list__kinds button');
  private readonly locateFilterSections = this.locatorForAll('.catalog-filters__name');
  private readonly locateTally = this.locatorForOptional('.api-list__tally');
  private readonly locateClear = this.locatorForOptional('.api-list__clear');
  private readonly locateSort = this.locatorFor('[data-testid="catalog-sort"]');

  async getAllRowsCellText(): Promise<Record<string, string>[]> {
    const table = await this.locateTable();
    if (!table) return [];
    return Promise.all((await table.getRows()).map(row => row.getCellTextByColumnName()));
  }

  async selectRow(index: number): Promise<void> {
    const rows = await this.locateRows();
    await rows[index].click();
  }

  async selectSort(value: 'name' | 'newest' | 'updated'): Promise<void> {
    const sort = await this.locateSort();
    await sort.setInputValue(value);
    await sort.dispatchEvent('change');
  }

  async getEmptyStateText(): Promise<string | null> {
    const emptyState = await this.locateEmptyState();
    return emptyState?.text() ?? null;
  }

  async selectKind(label: 'Agents' | 'APIs'): Promise<void> {
    const kinds = await this.locateKinds();
    const texts = await Promise.all(kinds.map(kind => kind.text()));
    const index = texts.findIndex(text => text.startsWith(label));
    await kinds[index].click();
  }

  async getKinds(): Promise<string[]> {
    return Promise.all((await this.locateKinds()).map(kind => kind.text({ exclude: '.api-list__count' })));
  }

  async getKindCounts(): Promise<string[]> {
    return Promise.all(
      (await this.locateKinds()).map(async kind => (await kind.text()).replace(await kind.text({ exclude: '.api-list__count' }), '')),
    );
  }

  async getFilters(): Promise<string[]> {
    return Promise.all((await this.locateFilterSections()).map(section => section.text()));
  }

  async getFilterValues(key: string): Promise<string[]> {
    const labels = await this.locatorForAll(`[data-testid="section-${key}"] .catalog-filters__value`)();
    const counts = await this.locatorForAll(`[data-testid="section-${key}"] .catalog-filters__count`)();
    return Promise.all(labels.map(async (label, index) => `${await label.text()} ${await counts[index].text()}`));
  }

  async isFilterPicked(key: string, value: string): Promise<boolean> {
    const checkbox = await this.locatorFor(MatCheckboxHarness.with({ ancestor: `[data-testid="section-${key}"]`, label: value }))();
    return checkbox.isChecked();
  }

  async pickFilterValue(key: string, value: string): Promise<void> {
    const checkbox = await this.locatorFor(MatCheckboxHarness.with({ ancestor: `[data-testid="section-${key}"]`, label: value }))();
    await checkbox.toggle();
  }

  async getTally(): Promise<string | null> {
    const tally = await this.locateTally();
    return tally?.text() ?? null;
  }

  async hasClearFilters(): Promise<boolean> {
    return (await this.locateClear()) !== null;
  }

  async clearFilters(): Promise<void> {
    await (await this.locateClear())?.click();
  }

  async isLoading(): Promise<boolean> {
    return (await this.locateLoader()) !== null;
  }
}
