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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { DivHarness } from '../../../testing/div.harness';

export class SubscriptionsComponentHarness extends ComponentHarness {
  public static hostSelector = 'app-subscriptions';

  private getEmptyState = this.locatorForOptional(DivHarness.with({ selector: '.subscriptions__empty-state' }));
  private getEmptyFiltered = this.locatorForOptional(DivHarness.with({ selector: '.subscriptions__empty-filtered' }));
  private getTable = this.locatorForOptional(MatTableHarness);
  private getApiFilter = this.locatorForOptional(MatSelectHarness.with({ selector: '#subscriptions-api-filter' }));
  private getApplicationFilter = this.locatorForOptional(MatSelectHarness.with({ selector: '#subscriptions-application-filter' }));
  private getStatusFilter = this.locatorForOptional(MatSelectHarness.with({ selector: '#subscriptions-status-filter' }));

  public async isEmptyStateDisplayed(): Promise<boolean> {
    return !!(await this.getEmptyState());
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

  public async selectApiFilter(index: number): Promise<void> {
    const filter = await this.getApiFilter();
    if (filter) {
      await filter.open();
      const options = await filter.getOptions();
      if (options[index]) {
        await options[index].click();
      }
    }
  }

  public async selectApplicationFilter(index: number): Promise<void> {
    const filter = await this.getApplicationFilter();
    if (filter) {
      await filter.open();
      const options = await filter.getOptions();
      if (options[index]) {
        await options[index].click();
      }
    }
  }

  public async selectStatusFilter(values: string[]): Promise<void> {
    const filter = await this.getStatusFilter();
    if (filter) {
      await filter.open();
      const options = await filter.getOptions();
      for (const option of options) {
        const text = await option.getText();
        if (values.includes(text)) {
          await option.click();
        }
      }
    }
  }
}
