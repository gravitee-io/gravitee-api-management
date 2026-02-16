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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class MissingDataConditionHarness extends ComponentHarness {
  static readonly hostSelector = 'missing-data-condition';

  private getDurationInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="duration"]' }));
  private getTimeUnitSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="timeUnit"]' }));

  public async getDurationValue() {
    return this.getDurationInput().then(input => input.getValue());
  }

  public async setDurationValue(value: string) {
    return this.getDurationInput().then(input => input.setValue(value));
  }

  public async getTimeUnitOptions() {
    return this.getTimeUnitSelect().then(async select => {
      await select.open();
      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public async selectTimeUnit(text: string) {
    return this.getTimeUnitSelect().then(select => select.clickOptions({ text }));
  }

  public async getSelectedTimeUnit() {
    return this.getTimeUnitSelect().then(select => select.getValueText());
  }
}
