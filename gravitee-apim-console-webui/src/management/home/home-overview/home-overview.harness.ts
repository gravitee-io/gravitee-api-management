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

import { DashboardFiltersBarHarness } from '../components/dashboard-filters-bar/dashboard-filters-bar.harness';
import { GioRequestStatsHarness } from '../components/gio-request-stats/gio-request-stats.harness';
import { DashboardV4ApiRequestStatsHarness } from '../components/dashboard-v4-api-request-stats/dashboard-v4-api-request-stats.harness';

export class HomeOverviewHarness extends ComponentHarness {
  static readonly hostSelector: string = 'home-overview';

  getDashboardFiltersBarHarness = this.locatorForOptional(DashboardFiltersBarHarness);
  getGioRequestStatsHarness = this.locatorForOptional(GioRequestStatsHarness);
  getDashboardV4ApiRequestStatsHarness = this.locatorForOptional(DashboardV4ApiRequestStatsHarness);
}
