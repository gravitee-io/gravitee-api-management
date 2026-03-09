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

import { DropdownSearchComponentHarness } from '../../../components/dropdown-search/dropdown-search.component.harness';
import { DivHarness } from '../../../testing/div.harness';

export class SubscriptionsComponentHarness extends ComponentHarness {
  public static hostSelector = 'app-subscriptions';

  private getEmptyState = this.locatorForOptional(DivHarness.with({ selector: '.subscriptions__empty-state' }));
  private getEmptyFiltered = this.locatorForOptional(DivHarness.with({ selector: '.subscriptions__empty-filtered' }));
  private getTable = this.locatorForOptional(MatTableHarness);
  private getAllDropdownFilters = this.locatorForAll(DropdownSearchComponentHarness);

  public async isEmptyStateDisplayed(): Promise<boolean> {
    return (await this.getEmptyState()) !== null;
  }

  public async isEmptyFilteredStateDisplayed(): Promise<boolean> {
    return !!(await this.getEmptyFiltered());
  }

  public async isTableDisplayed(): Promise<boolean> {
    return !!(await this.getTable());
  }

  public async getTableRowCount(): Promise<number> {
    const table = await this.getTable();
    if (!table) return 0;
    const rows = await table.getRows();
    return rows.length;
  }

  public async getTableHeaderCells(): Promise<string[]> {
    const table = await this.getTable();
    if (!table) return [];
    const headerRows = await table.getHeaderRows();
    if (headerRows.length === 0) return [];
    const cells = await headerRows[0].getCells();
    return Promise.all(cells.map(cell => cell.getText()));
  }

  /** API filter dropdown (first app-dropdown-search). */
  public async getApiFilter(): Promise<DropdownSearchComponentHarness> {
    const dropdowns = await this.getAllDropdownFilters();
    return dropdowns[0];
  }

  /** Application filter dropdown (second app-dropdown-search). */
  public async getApplicationFilter(): Promise<DropdownSearchComponentHarness> {
    const dropdowns = await this.getAllDropdownFilters();
    return dropdowns[1];
  }

  /** Status filter dropdown (third app-dropdown-search). */
  public async getStatusFilter(): Promise<DropdownSearchComponentHarness> {
    const dropdowns = await this.getAllDropdownFilters();
    return dropdowns[2];
  }

  /** Opens the API filter and selects the option at the given index. */
  public async selectApiFilter(index: number): Promise<void> {
    const filter = await this.getApiFilter();
    const overlay = await filter.getOverlayHarness();
    await overlay.selectOptionByIndex(index);
  }

  /** Opens the Application filter and selects the option at the given index. */
  public async selectApplicationFilter(index: number): Promise<void> {
    const filter = await this.getApplicationFilter();
    const overlay = await filter.getOverlayHarness();
    await overlay.selectOptionByIndex(index);
  }

  /** Opens the Status filter and selects the options whose labels match the given values. */
  public async selectStatusFilter(labels: string[]): Promise<void> {
    const filter = await this.getStatusFilter();
    const overlay = await filter.getOverlayHarness();
    await overlay.selectOptionsByLabels(labels);
  }
}
