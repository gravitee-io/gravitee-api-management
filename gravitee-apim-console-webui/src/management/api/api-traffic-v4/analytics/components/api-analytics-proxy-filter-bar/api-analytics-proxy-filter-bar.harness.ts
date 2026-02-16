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
import { MatChipHarness } from '@angular/material/chips/testing';

import { GioSelectSearchHarness } from '../../../../../../shared/components/gio-select-search/gio-select-search.harness';
import { BaseFilterBarHarness } from '../base-analytics-filter-bar/base-filter-bar.harness';

export class ApiAnalyticsProxyFilterBarHarness extends BaseFilterBarHarness {
  static hostSelector = 'api-analytics-proxy-filter-bar';

  async getSelectedHttpStatuses() {
    const select = await this.getHttpStatusesSelect();
    await select.open();
    return await select.getSelectedValues();
  }

  private async getHttpStatusesSelect() {
    return await this.locatorForOptional(GioSelectSearchHarness.with({ formControlName: 'httpStatuses' }))();
  }

  async selectHttpStatus(optionText: string) {
    const select = await this.getHttpStatusesSelect();
    if (select) {
      await select.open();
      await select.checkOptionByLabel(optionText);
      await select.close();
    }
  }

  async getFiltersAppliedText() {
    const filtersApplied = await this.locatorForOptional('.applied-filters-text')();
    return filtersApplied?.text();
  }

  async getNoFiltersAppliedText() {
    const noFilters = await this.locatorForOptional('.no-filters-applied')();
    return noFilters?.text();
  }

  async getApplicationsSelect(): Promise<GioSelectSearchHarness | null> {
    return await this.locatorForOptional(GioSelectSearchHarness.with({ formControlName: 'applications' }))();
  }

  async getSelectedApplications(): Promise<string[] | null> {
    const select = await this.getApplicationsSelect();
    return await select.getSelectedValues();
  }

  async selectApplication(optionText: string): Promise<void> {
    const select = await this.getApplicationsSelect();
    await select.checkOptionByLabel(optionText);
  }

  async openApplicationsSelect(): Promise<void> {
    const select = await this.getApplicationsSelect();
    await select.open();
  }

  async closeApplicationsSelect(): Promise<void> {
    const select = await this.getApplicationsSelect();
    if (select) {
      await select.close();
    }
  }

  async getFilterChips(): Promise<MatChipHarness[]> {
    return this.locatorForAll(MatChipHarness)();
  }

  async showsChipByText(text: string): Promise<boolean> {
    return this.locatorForOptional(MatChipHarness.with({ text }))().then(chip => !!chip);
  }

  async getFilterChipCount(): Promise<number> {
    return this.getFilterChips().then(chips => chips.length);
  }
}
