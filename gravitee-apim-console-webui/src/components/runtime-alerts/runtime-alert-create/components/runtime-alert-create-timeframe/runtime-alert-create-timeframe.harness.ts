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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class RuntimeAlertCreateTimeframeHarness extends ComponentHarness {
  static readonly hostSelector = 'runtime-alert-create-timeframe';

  private getAccordion = this.locatorFor(DivHarness.with({ selector: '.accordion__header ' }));
  private getDaysSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="days"]' }));
  private getBusinessDaysSlide = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="businessDays"]' }));
  private getOfficeHoursSlide = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="officeHours"]' }));
  private getTimeRangeInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="timeRange"]' }));

  public async clickOnAccordion() {
    return this.getAccordion()
      .then(accordion => accordion.host())
      .then(host => host.click());
  }

  public async getDaysOptions() {
    return this.getDaysSelect().then(async select => {
      await select.open();
      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public async selectDays(days: string[]) {
    return await Promise.all(days.map(day => this.getDaysSelect().then(select => select.clickOptions({ text: day }))));
  }

  public async getSelectedDays() {
    return this.getDaysSelect().then(select => select.getValueText());
  }

  public async toggleBusinessDays() {
    return this.getBusinessDaysSlide().then(slide => slide.toggle());
  }

  public async getBusinessDaysToggleValue() {
    return this.getBusinessDaysSlide().then(slide => slide.isChecked());
  }

  public async toggleOfficeHours() {
    return this.getOfficeHoursSlide().then(slide => slide.toggle());
  }

  public async getOfficeHoursToggleValue() {
    return this.getOfficeHoursSlide().then(slide => slide.isChecked());
  }

  public async getTimeRange() {
    return this.getTimeRangeInput().then(input => input.getValue());
  }

  public async setTimeRange(timeRange: string) {
    return this.getTimeRangeInput().then(input => input.setValue(timeRange));
  }
}
