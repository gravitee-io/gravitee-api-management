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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ApiHealthCheckDashboardV4Component } from './api-health-check-dashboard-v4.component';
import { ApiHealthCheckDashboardV4FiltersComponent } from './components/filters/api-health-check-dashboard-v4-filters.component';
import { GlobalResponseTimeTrendComponent } from './components/global-response-time-trend/global-response-time-trend.component';

@NgModule({
  declarations: [ApiHealthCheckDashboardV4Component],
  imports: [CommonModule, ApiHealthCheckDashboardV4FiltersComponent, GlobalResponseTimeTrendComponent],
  exports: [ApiHealthCheckDashboardV4Component],
})
export class ApiHealthCheckDashboardV4Module {}
