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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatChipHarness, MatChipListboxHarness } from '@angular/material/chips/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export class WebhookLogsQuickFiltersHarness extends ComponentHarness {
  static hostSelector = 'webhook-logs-quick-filters';

  public getPeriodSelectInput = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="timeframe"]' }));
  public getChips = this.locatorForOptional(MatChipListboxHarness.with({ selector: '[class="quick-filters__chip_list"]' }));
  public getPeriodChip = this.locatorFor(MatChipHarness.with({ text: /^timeframe:/ }));
  public getStatusChip = this.locatorFor(MatChipHarness.with({ text: /^status:/ }));
  public getApplicationChip = this.locatorFor(MatChipHarness.with({ text: /^application:/ }));
  public getRefreshButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testId=refresh-button]' }));
  public getResetFiltersButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testId=reset-filters-button]' }));
  public getMoreButton = this.locatorFor(MatButtonHarness.with({ text: /More/ }));
  public getSearchInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="searchTerm"]' }));
  public getStatusSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="status"]' }));
  public getApplicationSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="application"]' }));

  public async moreFiltersButtonClick() {
    return this.getMoreButton().then((btn) => btn.click());
  }

  public clickRefresh() {
    return this.getRefreshButton().then((button) => button.click());
  }

  public clickResetFilters() {
    return this.getResetFiltersButton().then((button) => button.click());
  }

  public async setSearchTerm(text: string) {
    const input = await this.getSearchInput();
    return input.setValue(text);
  }
}
