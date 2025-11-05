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
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

import { WebhookLogsListHarness } from './components/webhook-logs-list/webhook-logs-list.component.harness';
import { WebhookLogsQuickFiltersHarness } from './components/webhook-logs-quick-filters/webhook-logs-quick-filters.component.harness';

export class WebhookLogsHarness extends ComponentHarness {
  static hostSelector = 'webhook-logs';

  public listHarness = this.locatorFor(WebhookLogsListHarness);
  public quickFiltersHarness = this.locatorFor(WebhookLogsQuickFiltersHarness);
  public banner = this.locatorForOptional(DivHarness.with({ selector: '.banner-warning' }));

  async getQuickFiltersChips() {
    const quickFilters = await this.quickFiltersHarness();
    return quickFilters.getChips();
  }

  async selectPeriodQuickFilter() {
    const quickFilters = await this.quickFiltersHarness();
    return quickFilters.getPeriodSelectInput();
  }

  async getSelectedStatus() {
    const harness = await this.quickFiltersHarness();
    const select = await harness.getStatusSelect();
    return select.getValueText();
  }

  async selectStatus(text: string) {
    const harness = await this.quickFiltersHarness();
    const select = await harness.getStatusSelect();
    return select.clickOptions({ text });
  }

  async getSelectedApplications() {
    const harness = await this.quickFiltersHarness();
    const select = await harness.getApplicationSelect();
    return select.getValueText();
  }

  async selectApplication(text: string) {
    const harness = await this.quickFiltersHarness();
    const select = await harness.getApplicationSelect();
    return select.clickOptions({ text });
  }

  async moreFiltersButtonClick() {
    const harness = await this.quickFiltersHarness();
    const btn = await harness.getMoreButton();
    return btn.click();
  }

  async refreshButtonClick() {
    const harness = await this.quickFiltersHarness();
    const btn = await harness.getRefreshButton();
    return btn.click();
  }

  async resetFiltersClick() {
    const harness = await this.quickFiltersHarness();
    const btn = await harness.getResetFiltersButton();
    return btn.click();
  }

  async removeStatusChip() {
    const quickFilters = await this.quickFiltersHarness();
    const chip = await quickFilters.getStatusChip();
    const btn = await chip.getRemoveButton();
    return btn.click();
  }

  async removeApplicationChip() {
    const quickFilters = await this.quickFiltersHarness();
    const chip = await quickFilters.getApplicationChip();
    const btn = await chip.getRemoveButton();
    return btn.click();
  }
}
