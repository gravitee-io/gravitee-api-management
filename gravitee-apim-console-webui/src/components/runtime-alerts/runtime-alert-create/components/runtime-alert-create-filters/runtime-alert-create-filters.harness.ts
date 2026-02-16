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
import { MatButtonHarness } from '@angular/material/button/testing';

import { MetricsSimpleConditionHarness } from '../components/metrics-simple-condition/metrics-simple-condition.harness';

export class RuntimeAlertCreateFiltersHarness extends ComponentHarness {
  static readonly hostSelector = 'runtime-alert-create-filters';
  private getBanner = this.locatorForOptional(DivHarness.with({ selector: '.banner' }));
  private getAddFilterButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Add filter"]' }));
  private getDeleteFilterButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Delete filter"]' }));
  private metricsSimpleConditions = this.locatorForAll(MetricsSimpleConditionHarness);

  async isImpactBannerDisplayed(): Promise<boolean> {
    return (await this.getBanner()) !== null;
  }

  async addFilter() {
    return this.getAddFilterButton().then(btn => btn.click());
  }

  async deleteFilter() {
    return this.getDeleteFilterButton().then(btn => btn.click());
  }

  async getMetricsCondition(index: number) {
    return this.metricsSimpleConditions().then(harnesses => harnesses.at(index));
  }

  async getMetricsConditionsLength() {
    return this.metricsSimpleConditions().then(harnesses => harnesses.length);
  }
}
