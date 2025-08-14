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
import { MatButtonHarness } from '@angular/material/button/testing';

import { GioSelectSearchHarness } from '../../../../../../shared/components/gio-select-search/gio-select-search.harness';

export class ApiAnalyticsProxyFilterBarHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-proxy-filter-bar';

  async getPeriodSelect(): Promise<MatSelectHarness | null> {
    return this.locatorForOptional(MatSelectHarness)();
  }

  async getPlanSelect(): Promise<GioSelectSearchHarness | null> {
    return await this.locatorForOptional(GioSelectSearchHarness.with({ formControlName: 'plans' }))();
  }

  async getApplyButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ text: /apply/i }))();
  }

  async getRefreshButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ text: /refresh/i }))();
  }

  async getErrorMessages(): Promise<string[]> {
    const errorElements = await this.locatorForAll('mat-error')();
    const messages: string[] = [];

    for (const element of errorElements) {
      const text = await element.text();
      if (text) {
        messages.push(text);
      }
    }

    return messages;
  }

  async getSelectedPlans(): Promise<string[] | null> {
    const select = await this.getPlanSelect();
    await select.open();
    return await select.getSelectedValues();
  }

  async selectPlan(optionText: string): Promise<void> {
    const select = await this.getPlanSelect();
    if (select) {
      await select.open();
      await select.checkOptionByLabel(optionText);
      await select.close();
    }
  }

  async searchPlan(query: string): Promise<void> {
    const select = await this.getPlanSelect();
    if (select) {
      await select.open();
      await select.setSearchValue(query);
    }
  }

  async clickRefresh(): Promise<void> {
    const button = await this.getRefreshButton();
    if (button) {
      await button.click();
    }
  }

  async isApplyButtonEnabled(): Promise<boolean> {
    const button = await this.getApplyButton();
    if (!button) {
      // If the apply button is not present, it's not enabled.
      return false;
    }
    return !(await button.isDisabled());
  }

  async getSelectedPeriod(): Promise<string | null> {
    const select = await this.getPeriodSelect();
    if (select) {
      return await select.getValueText();
    }
    return null;
  }

  async hasDateRangeError(): Promise<boolean> {
    const errors = await this.getErrorMessages();
    return errors.some((error) => error.toLowerCase().includes('date range') || error.toLowerCase().includes('earlier'));
  }

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
}
