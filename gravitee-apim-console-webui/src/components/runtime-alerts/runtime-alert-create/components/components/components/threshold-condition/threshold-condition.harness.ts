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
import { MatInputHarness } from '@angular/material/input/testing';

export class ThresholdConditionHarness extends ComponentHarness {
  static readonly hostSelector = 'threshold-condition';

  private getOperatorSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="operator"]' }));
  private getThresholdInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="threshold"]' }));

  public async getOperatorOptions() {
    return this.getOperatorSelect().then(async select => {
      await select.open();
      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public async selectOperator(text: string) {
    this.getOperatorSelect().then(select => select.clickOptions({ text }));
  }

  public async getSelectedOperator() {
    return this.getOperatorSelect().then(select => select.getValueText());
  }

  public async setThresholdValue(value: string) {
    return this.getThresholdInput().then(input => input.setValue(value));
  }

  public async getThresholdValue() {
    return this.getThresholdInput().then(input => input.getValue());
  }

  public async isThresholdInvalid() {
    return this.getThresholdInput()
      .then(input => input.host())
      .then(host => host.hasClass('ng-invalid'));
  }
}
