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

import { MissingDataConditionHarness } from '../missing-data-condition/missing-data-condition.harness';
import { ThresholdConditionHarness } from '../components/threshold-condition/threshold-condition.harness';
import { AggregationConditionHarness } from '../components/aggegation-condition/aggregation-condition.harness';

export class RequestMetricsAggregationConditionHarness extends ComponentHarness {
  static readonly hostSelector = 'request-metrics-aggregation-condition';
  private getFunctionSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="function"]' }));
  private getMetricSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="metric"]' }));
  public getThresholdHarness = this.locatorFor(ThresholdConditionHarness);
  public durationHarness = this.locatorFor(MissingDataConditionHarness);
  public aggregationForm = this.locatorFor(AggregationConditionHarness);

  public async getFunctionOptions() {
    return this.getFunctionSelect().then(async select => {
      await select.open();
      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public async selectFunction(text: string) {
    return this.getFunctionSelect().then(select => select.clickOptions({ text }));
  }

  public async getSelectedFunction() {
    return this.getFunctionSelect().then(select => select.getValueText());
  }

  public async getMetricOptions() {
    return this.getMetricSelect().then(async select => {
      await select.open();
      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public async selectMetric(text: string) {
    return this.getMetricSelect().then(select => select.clickOptions({ text }));
  }

  public async getSelectedMetric() {
    return this.getMetricSelect().then(select => select.getValueText());
  }
}
