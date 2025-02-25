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
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioAvatarModule, GioBannerModule, GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { Route, RouterModule } from '@angular/router';

import { HomeApiHealthCheckComponent } from './home-api-health-check/home-api-health-check.component';
import { HomeLayoutComponent } from './home-layout/home-layout.component';
import { HomeOverviewComponent } from './home-overview/home-overview.component';
import { HealthAvailabilityTimeFrameModule } from './home-api-health-check/health-availability-time-frame/health-availability-time-frame.module';
import { GioQuickTimeRangeModule } from './components/gio-quick-time-range/gio-quick-time-range.module';
import { GioRequestStatsModule } from './components/gio-request-stats/gio-request-stats.module';
import { GioTopApisTableModule } from './components/gio-top-apis-table/gio-top-apis-table.module';
import { GioApiResponseStatusModule } from './components/gio-api-response-status/gio-api-response-status.module';
import { GioApiStateModule } from './components/gio-api-state/gio-api-state.module';
import { GioApiLifecycleStateModule } from './components/gio-api-lifecycle-state/gio-api-lifecycle-state.module';
import { GioApiEventsTableModule } from './components/gio-api-events-table/gio-api-events-table.module';
import { DashboardV4ApiRequestStats } from './components/dashboard-v4-api-request-stats/dashboard-v4-api-request-stats';
import { DashboardFiltersBarComponent } from './components/dashboard-filters-bar/dashboard-filters-bar.component';
import { V2ApiCallsWithNoContextPathComponent } from './components/v2-api-calls-with-no-contex-path/v2-api-calls-with-no-context-path.component';
import { V2ResponseTimesComponent } from './components/v2-response-times/v2-response-times.component';
import { V2ResponseStatusComponent } from './components/v2-response-status/v2-response-status.component';
import { V4ResponseStatusComponent } from './components/v4-response-status/v4-response-status.component';
import { V4ResponseTimesComponent } from './components/v4-response-times/v4-response-times.component';
import { TopApplicationsByRequestsComponent } from './components/top-applications-by-requests/top-applications-by-requests.component';

import { GioCircularPercentageModule } from '../../shared/components/gio-circular-percentage/gio-circular-percentage.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { TasksComponent } from '../../user/tasks/tasks.component';
import { MessagesComponent } from '../messages/messages.component';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { ApiAnalyticsResponseStatusRangesComponent } from '../../shared/components/api-analytics-response-status-ranges/api-analytics-response-status-ranges.component';
import { TopApisWidgetComponent } from '../../shared/components/top-apis-widget/top-apis-widget.component';

const homeRoutes: Route[] = [
  {
    path: '',
    component: HomeLayoutComponent,
    children: [
      {
        path: 'overview',
        component: HomeOverviewComponent,
      },
      {
        path: 'apiHealthCheck',
        component: HomeApiHealthCheckComponent,
      },
      {
        path: 'tasks',
        component: TasksComponent,
      },
      {
        path: 'broadcasts',
        component: MessagesComponent,
      },
      {
        path: '',
        redirectTo: 'overview',
        pathMatch: 'full',
      },
    ],
  },
];

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(homeRoutes),

    MatTabsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCardModule,

    GioAvatarModule,
    GioTableWrapperModule,
    GioCircularPercentageModule,
    GioBannerModule,
    GioIconsModule,
    GioLoaderModule,
    GioRequestStatsModule,
    HealthAvailabilityTimeFrameModule,
    GioQuickTimeRangeModule,
    GioTopApisTableModule,
    GioApiResponseStatusModule,
    GioApiStateModule,
    GioApiLifecycleStateModule,
    GioApiEventsTableModule,
    GioPermissionModule,
    ApiAnalyticsResponseStatusRangesComponent,
    TopApisWidgetComponent,
    DashboardV4ApiRequestStats,

    DashboardFiltersBarComponent,
    V2ApiCallsWithNoContextPathComponent,
    V2ResponseTimesComponent,
    V2ResponseStatusComponent,
    V4ResponseStatusComponent,
    V4ResponseTimesComponent,
    TopApplicationsByRequestsComponent,
  ],
  declarations: [HomeLayoutComponent, HomeOverviewComponent, HomeApiHealthCheckComponent],
})
export class HomeModule {}
