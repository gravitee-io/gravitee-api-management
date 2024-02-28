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

import { ThresholdConditionHarness } from '../components/threshold-condition/threshold-condition.harness';
import { MetricsSimpleConditionHarness } from '../metrics-simple-condition/metrics-simple-condition.harness';
import { MissingDataConditionHarness } from '../missing-data-condition/missing-data-condition.harness';

export class RequestMetricsRateConditionHarness extends ComponentHarness {
  static readonly hostSelector = 'request-metrics-rate-condition';

  public metricsSimpleConditionForm = this.locatorFor(MetricsSimpleConditionHarness);
  public getThresholdHarness = this.locatorFor(ThresholdConditionHarness);
  public durationHarness = this.locatorFor(MissingDataConditionHarness);
}
