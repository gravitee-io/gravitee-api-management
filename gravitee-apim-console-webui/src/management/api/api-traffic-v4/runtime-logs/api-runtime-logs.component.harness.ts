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

import { ApiRuntimeLogsListHarness, ApiRuntimeLogsQuickFiltersHarness } from './components';
import { ApiRuntimeLogsMoreFiltersHarness } from './components/api-runtime-logs-quick-filters/components';

export class ApiRuntimeLogsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs';

  public listHarness = this.locatorFor(ApiRuntimeLogsListHarness);
  public quickFiltersHarness = this.locatorFor(ApiRuntimeLogsQuickFiltersHarness);
  public moreFiltersHarness = this.locatorFor(ApiRuntimeLogsMoreFiltersHarness);
  public loader = this.locatorForOptional(DivHarness.with({ selector: '[data-testId=loader-spinner]' }));
  public banner = this.locatorForOptional(DivHarness.with({ selector: '.banner' }));

  async getQuickFiltersChips() {
    return this.quickFiltersHarness().then(quickFilters => quickFilters.getChips());
  }

  async selectPeriodQuickFilter() {
    return this.quickFiltersHarness().then(quickFilters => quickFilters.getPeriodSelectInput());
  }

  async getPeriodChip() {
    return this.quickFiltersHarness().then(quickFilters => quickFilters.getPeriodChip());
  }

  async getPeriodChipText() {
    return this.quickFiltersHarness()
      .then(quickFilters => quickFilters.getPeriodChip())
      .then(chip => chip.getText());
  }

  async removePeriodChip() {
    return this.getPeriodChip()
      .then(chip => chip.getRemoveButton())
      .then(button => button.click());
  }

  async getApplicationsTags() {
    return this.quickFiltersHarness()
      .then(harness => harness.getApplicationsTags())
      .then(input => input.getTags());
  }

  async searchApplication(appName: string) {
    return this.moreFiltersHarness()
      .then(harness => harness.getApplicationAutocomplete())
      .then(autocomplete => autocomplete.enterText(appName));
  }

  async selectedApplication(text: string) {
    return this.moreFiltersHarness()
      .then(harness => harness.getApplicationAutocomplete())
      .then(autocomplete => autocomplete.getOptions({ text }))
      .then(options => (options.length > 0 ? options[0].click() : Promise.reject('No option found')));
  }

  async getApplicationsChip() {
    return this.quickFiltersHarness().then(quickFilters => quickFilters.getApplicationsChip());
  }

  async getApplicationsChipText() {
    return this.quickFiltersHarness()
      .then(quickFilters => quickFilters.getApplicationsChip())
      .then(chip => chip.getText());
  }

  async removeApplicationsChip() {
    return this.getApplicationsChip()
      .then(chip => chip.getRemoveButton())
      .then(button => button.click());
  }

  async getSelectedPlans() {
    return this.quickFiltersHarness()
      .then(harness => harness.getPlansSelect())
      .then(select => select.getValueText());
  }

  async selectPlan(text: string) {
    return this.quickFiltersHarness()
      .then(harness => harness.getPlansSelect())
      .then(select => select.clickOptions({ text }));
  }

  async getPlanChip() {
    return this.quickFiltersHarness().then(harness => harness.getPlansChip());
  }

  async removePlanChip() {
    return this.getPlanChip()
      .then(chip => chip.getRemoveButton())
      .then(button => button.click());
  }

  async moreFiltersButtonClick() {
    return this.quickFiltersHarness()
      .then(harness => harness.getMoreButton())
      .then(btn => btn.click());
  }

  async selectPeriodFromMoreFilters() {
    return this.moreFiltersHarness().then(quickFilters => quickFilters.getPeriodSelectInput());
  }

  async setFromDate(date: string) {
    return this.moreFiltersHarness()
      .then(harness => harness.getFromInput())
      .then(input => input.setValue(date));
  }

  async setToDate(date: string) {
    return this.moreFiltersHarness()
      .then(harness => harness.getToInput())
      .then(input => input.setValue(date));
  }

  async getToDate() {
    return this.moreFiltersHarness()
      .then(harness => harness.getToInput())
      .then(input => input.getValue());
  }

  async getFromDate() {
    return this.moreFiltersHarness()
      .then(harness => harness.getFromInput())
      .then(input => input.getValue());
  }

  async moreFiltersClearAll() {
    return this.moreFiltersHarness()
      .then(harness => harness.getClearAllButton())
      .then(btn => btn.click());
  }

  async moreFiltersApply() {
    return this.moreFiltersHarness()
      .then(harness => harness.getApplyButton())
      .then(btn => btn.click());
  }

  async isMoreFiltersApplyDisabled() {
    return this.moreFiltersHarness()
      .then(harness => harness.getApplyButton())
      .then(btn => btn.isDisabled());
  }

  async closeMoreFilters() {
    this.moreFiltersHarness()
      .then(harness => harness.getCloseButton())
      .then(btn => btn.click());
  }

  async getFromChipText() {
    return this.quickFiltersHarness()
      .then(quickFilters => quickFilters.getFromChip())
      .then(chip => chip.getText());
  }

  async removeFromChip() {
    return this.quickFiltersHarness()
      .then(quickFilters => quickFilters.getFromChip())
      .then(chip => chip.getRemoveButton())
      .then(btn => btn.click());
  }

  async getFromInputValue() {
    return this.moreFiltersHarness()
      .then(quickFilters => quickFilters.getFromInput())
      .then(chip => chip.getValue());
  }

  async getToChipText() {
    return this.quickFiltersHarness()
      .then(quickFilters => quickFilters.getToChip())
      .then(chip => chip.getText());
  }

  async removeToChip() {
    return this.quickFiltersHarness()
      .then(quickFilters => quickFilters.getToChip())
      .then(chip => chip.getRemoveButton())
      .then(btn => btn.click());
  }

  async getToInputValue() {
    return this.moreFiltersHarness()
      .then(quickFilters => quickFilters.getToInput())
      .then(chip => chip.getValue());
  }

  async selectPeriodInMoreFilters(text: string) {
    return this.selectPeriodFromMoreFilters().then(select => select.clickOptions({ text }));
  }

  async moreFiltersPeriodText() {
    return this.selectPeriodFromMoreFilters().then(select => select.getValueText());
  }

  async getSelectedMethods() {
    return this.quickFiltersHarness()
      .then(harness => harness.getMethodsSelect())
      .then(select => select.getValueText());
  }

  async selectMethod(text: string) {
    return this.quickFiltersHarness()
      .then(harness => harness.getMethodsSelect())
      .then(select => select.clickOptions({ text }));
  }

  async getMethodsChip() {
    return this.quickFiltersHarness().then(harness => harness.getMethodsChip());
  }

  async getMethodsChipText() {
    return this.quickFiltersHarness()
      .then(harness => harness.getMethodsChip())
      .then(chip => chip.getText());
  }

  async removeMethodsChip() {
    return this.getMethodsChip()
      .then(chip => chip.getRemoveButton())
      .then(button => button.click());
  }

  async addInputStatusesChip(text: string) {
    return this.moreFiltersHarness()
      .then(harness => harness.getStatusesChips())
      .then(chipList => chipList.getInput())
      .then(async input => {
        await input.setValue(text);
        return input.blur();
      });
  }

  async getStatusesInputChips() {
    return this.moreFiltersHarness()
      .then(harness => harness.getStatusesChips())
      .then(chipList => chipList.getRows())
      .then(chips => Promise.all(chips.map(chip => chip.getText())));
  }

  async removeInputStatusChip(text: string) {
    return this.moreFiltersHarness()
      .then(harness => harness.getStatusesChips())
      .then(chipList => chipList.getRows({ text }))
      .then(chips => chips[0].getRemoveButton())
      .then(btn => btn.click());
  }

  async removeStatusChip() {
    return this.quickFiltersHarness()
      .then(quickFilters => quickFilters.getStatusChip())
      .then(chip => chip.getRemoveButton())
      .then(btn => btn.click());
  }

  async getStatusChip() {
    return this.quickFiltersHarness()
      .then(quickFilters => quickFilters.getStatusChip())
      .then(chip => chip.getText());
  }
}
