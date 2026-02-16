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
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class AggregationConditionHarness extends ComponentHarness {
  static readonly hostSelector = 'aggregation-condition';
  private getAccordion = this.locatorFor(DivHarness.with({ selector: '.accordion__header' }));
  private getPropertySelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="property"]' }));

  public async accordionClick() {
    return this.getAccordion()
      .then(accordion => accordion.host())
      .then(host => host.click());
  }

  public async getPropertyOptions() {
    return this.getPropertySelect().then(async select => {
      await select.open();
      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public async selectProperty(text: string) {
    return this.getPropertySelect().then(select => select.clickOptions({ text }));
  }

  public async getSelectedProperty() {
    return this.getPropertySelect().then(select => select.getValueText());
  }
}
