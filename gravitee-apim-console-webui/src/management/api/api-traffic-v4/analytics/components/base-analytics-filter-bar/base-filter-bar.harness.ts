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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import moment from 'moment';

import { GioSelectSearchHarness } from '.././../../../../../shared/components/gio-select-search/gio-select-search.harness';

export abstract class BaseFilterBarHarness extends ComponentHarness {
  async getPeriodSelect(): Promise<MatSelectHarness | null> {
    return this.locatorForOptional(MatSelectHarness)();
  }

  async selectPeriod(period: string): Promise<void> {
    const select = await this.getPeriodSelect();
    if (select) {
      await select.open();
      await select.clickOptions({ text: period });
    }
  }

  async getSelectedPeriod(): Promise<string | null> {
    const select = await this.getPeriodSelect();
    if (select) {
      return await select.getValueText();
    }
    return null;
  }

  async getPlanSelect(): Promise<GioSelectSearchHarness | null> {
    return await this.locatorForOptional(GioSelectSearchHarness.with({ formControlName: 'plans' }))();
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

  async getApplyButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ text: /apply/i }))();
  }

  async getRefreshButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ text: /refresh/i }))();
  }

  async clickApply(): Promise<void> {
    const button = await this.getApplyButton();
    if (button) {
      await button.click();
    }
  }

  async clickRefresh(): Promise<void> {
    const button = await this.getRefreshButton();
    if (button) {
      await button.click();
    }
  }

  async isApplyButtonDisabled(): Promise<boolean> {
    const button = await this.getApplyButton();
    return await button.isDisabled();
  }

  async isApplyButtonEnabled(): Promise<boolean> {
    const button = await this.getApplyButton();
    return !(await button.isDisabled());
  }

  async getFromDateField(): Promise<MatFormFieldHarness | null> {
    return this.locatorForOptional(MatFormFieldHarness)();
  }

  async getToDateField(): Promise<MatFormFieldHarness | null> {
    return this.locatorForOptional(MatFormFieldHarness)();
  }

  async getCustomDateInputs(): Promise<TestElement[]> {
    return this.locatorForAll('.custom-date input')();
  }

  async setCustomDateRange(fromDate: moment.Moment, toDate: moment.Moment): Promise<void> {
    // For date inputs, we'll use the native input elements directly
    const inputs = await this.getCustomDateInputs();

    if (inputs.length >= 2) {
      // Set from date
      await inputs[0].sendKeys(fromDate.format('MM/DD/YYYY HH:mm'));
      // Set to date
      await inputs[1].sendKeys(toDate.format('MM/DD/YYYY HH:mm'));
    }
  }

  async isCustomPeriodVisible(): Promise<boolean> {
    const fromField = await this.getFromDateField();
    return fromField !== null;
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

  async hasDateRangeError(): Promise<boolean> {
    const errors = await this.getErrorMessages();
    return errors.some(error => error.toLowerCase().includes('date range') || error.toLowerCase().includes('earlier'));
  }

  async getFormValidationState(): Promise<{ isValid: boolean; errors: string[] }> {
    const errors = await this.getErrorMessages();
    return {
      isValid: errors.length === 0,
      errors,
    };
  }
}
