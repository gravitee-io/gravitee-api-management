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

import { ApiHealthCheckDashboardV4FiltersHarness } from './components/filters/api-health-check-dashboard-v4-filters.harness';
import { AvailabilityHarness } from './components/global-availability/global-availability.harness';
import { AverageResponseTimeHarness } from './components/global-average-response-time/global-average-response-time.harness';
import { AvailabilityPerFieldHarness } from './components/availability-per-field/availability-per-field.harness';

export class ApiHealthCheckDashboardV4Harness extends ComponentHarness {
  static hostSelector = 'app-health-check-dashboard-v4';

  getFiltersHarness = this.locatorForOptional(ApiHealthCheckDashboardV4FiltersHarness);

  getAvailabilityWidgetHarness = this.locatorForOptional(AvailabilityHarness);

  getAverageResponseTimeWidgetHarness = this.locatorForOptional(AverageResponseTimeHarness);

  getAvailabilityPerEndpointWidgetHarness = this.locatorForOptional(
    AvailabilityPerFieldHarness.with({ title: 'Availability Per Endpoint' }),
  );

  getAvailabilityPerGatewayWidgetHarness = this.locatorForOptional(AvailabilityPerFieldHarness.with({ title: 'Availability Per Gateway' }));
}
