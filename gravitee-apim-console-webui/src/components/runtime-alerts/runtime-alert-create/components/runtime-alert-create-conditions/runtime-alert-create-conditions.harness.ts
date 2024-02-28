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

import { MissingDataConditionHarness } from './components/missing-data-condition/missing-data-condition.harness';
import { MetricsSimpleConditionHarness } from './components/metrics-simple-condition/metrics-simple-condition.harness';
import { RequestMetricsAggregationConditionHarness } from './components/request-metrics-aggregation-condition/request-metrics-aggregation-condition.harness';
import { RequestMetricsRateConditionHarness } from './components/request-metrics-rate-condition/request-metrics-rate-condition.harness';
import { EndpointHealthCheckConditionHarness } from './components/endpoint-health-check-condition/endpoint-health-check-condition.harness';

export class RuntimeAlertCreateConditionsHarness extends ComponentHarness {
  static readonly hostSelector = 'runtime-alert-create-conditions';
  public missingDataConditionForm = this.locatorFor(MissingDataConditionHarness);
  public metricsSimpleConditionForm = this.locatorFor(MetricsSimpleConditionHarness);
  public requestMetricsAggregationConditionForm = this.locatorFor(RequestMetricsAggregationConditionHarness);
  public requestMetricsRateConditionForm = this.locatorFor(RequestMetricsRateConditionHarness);
  public endpointHealthCheckConditionForm = this.locatorFor(EndpointHealthCheckConditionHarness);
}
