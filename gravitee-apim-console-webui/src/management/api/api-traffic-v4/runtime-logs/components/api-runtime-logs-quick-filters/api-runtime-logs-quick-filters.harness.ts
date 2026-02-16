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
import { GioFormTagsInputHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';

export class ApiRuntimeLogsQuickFiltersHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-quick-filters';

  public getPeriodSelectInput = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="period"]' }));
  public getChips = this.locatorForOptional(MatChipListboxHarness.with({ selector: '[class="quick-filters__chip_list"]' }));
  public getPeriodChip = this.locatorFor(MatChipHarness.with({ text: /^period:/ }));
  public getApplicationsTags = this.locatorFor(GioFormTagsInputHarness);
  public getApplicationsChip = this.locatorFor(MatChipHarness.with({ text: /^applications:/ }));
  public getPlansSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="plans"]' }));
  public getPlansChip = this.locatorFor(MatChipHarness.with({ text: /^plans:/ }));
  public getRefreshButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testId=refresh-button]' }));
  public getResetFiltersButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testId=reset-filters-button]' }));
  public getMoreButton = this.locatorFor(MatButtonHarness.with({ text: /More/ }));
  public getFromChip = this.locatorFor(MatChipHarness.with({ text: /^from:/ }));
  public getToChip = this.locatorFor(MatChipHarness.with({ text: /^to:/ }));
  public getMethodsSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="methods"]' }));
  public getMethodsChip = this.locatorFor(MatChipHarness.with({ text: /^methods:/ }));
  public getStatusChip = this.locatorFor(MatChipHarness.with({ text: /^statuses:/ }));

  public async moreFiltersButtonClick() {
    return this.getMoreButton().then(btn => btn.click());
  }

  public clickRefresh() {
    return this.getRefreshButton().then(button => button.click());
  }

  public clickResetFilters() {
    return this.getResetFiltersButton().then(button => button.click());
  }
}
