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

import { ApiRuntimeLogsListHarness, ApiRuntimeLogsQuickFiltersHarness } from './components';

export class ApiRuntimeLogsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs';

  public listHarness = this.locatorFor(ApiRuntimeLogsListHarness);
  public quickFiltersHarness = this.locatorFor(ApiRuntimeLogsQuickFiltersHarness);

  async isEmptyPanelDisplayed(): Promise<boolean> {
    return this.listHarness().then((list) => list.isEmptyPanelDisplayed());
  }

  async isImpactBannerDisplayed(): Promise<boolean> {
    return this.listHarness().then((list) => list.isImpactBannerDisplayed());
  }

  async clickOpenSettings(): Promise<void> {
    return this.listHarness().then((list) => list.clickOpenSettings());
  }

  async getRows() {
    return this.listHarness().then((list) => list.rows());
  }

  async getPaginator() {
    return this.listHarness().then((list) => list.paginator());
  }

  async getQuickFiltersChips() {
    return this.quickFiltersHarness().then((quickFilters) => quickFilters.getChips());
  }

  async selectPeriodQuickFilter() {
    return this.quickFiltersHarness().then((quickFilters) => quickFilters.getPeriodSelectInput());
  }

  async getPeriodChip() {
    return this.quickFiltersHarness().then((quickFilters) => quickFilters.getPeriodChip());
  }

  async removePeriodChip() {
    return this.getPeriodChip()
      .then((chip) => chip.getRemoveButton())
      .then((button) => button.click());
  }

  async getApplicationsTags() {
    return this.quickFiltersHarness()
      .then((harness) => harness.getApplicationsTags())
      .then((input) => input.getTags());
  }

  async searchApplication(appName: string) {
    return this.quickFiltersHarness()
      .then((harness) => harness.getApplicationAutocomplete())
      .then((autocomplete) => autocomplete.enterText(appName));
  }

  async selectedApplication(text: string) {
    return this.quickFiltersHarness()
      .then((harness) => harness.getApplicationAutocomplete())
      .then((autocomplete) => autocomplete.selectOption({ text }));
  }

  async getApplicationsChip() {
    return this.quickFiltersHarness().then((quickFilters) => quickFilters.getApplicationsChip());
  }

  async removeApplicationsChip() {
    return this.getApplicationsChip()
      .then((chip) => chip.getRemoveButton())
      .then((button) => button.click());
  }

  async getSelectedPlans() {
    return this.quickFiltersHarness()
      .then((harness) => harness.getPlansSelect())
      .then((select) => select.getValueText());
  }

  async selectPlan(text: string) {
    return this.quickFiltersHarness()
      .then((harness) => harness.getPlansSelect())
      .then((select) => select.clickOptions({ text }));
  }

  async getPlanChip() {
    return this.quickFiltersHarness().then((harness) => harness.getPlansChip());
  }

  async removePlanChip() {
    return this.getPlanChip()
      .then((chip) => chip.getRemoveButton())
      .then((button) => button.click());
  }

  async goToNextPage() {
    return this.getPaginator().then((paginator) => paginator.goToNextPage());
  }
}
