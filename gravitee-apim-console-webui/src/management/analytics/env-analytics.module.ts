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
import { RouterModule, Routes } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { GioBannerModule, GioIconsModule } from '@gravitee/ui-particles-angular';

import { EnvAnalyticsLayoutComponent } from './env-analytics-layout.component';
import { AnalyticsDashboardComponent } from './legacy/analytics-dashboard/analytics-dashboard.component';
import { PlatformLogsComponent } from './legacy/logs/platform-logs.component';
import { PlatformLogComponent } from './legacy/logs/platform-log.component';
import { EnvLogsV4Component } from './env-logs-v4/env-logs-v4.component';
import { EnvLogsTableComponent } from './env-logs-v4/components/env-logs-table/env-logs-table.component';
import { OverviewComponent } from './overview/overview.component';
import { DashboardsListComponent } from './dashboards/dashboards-list/dashboards-list.component';

const routes: Routes = [
  {
    path: 'overview',
    component: OverviewComponent,
  },
  {
    path: 'dashboards',
    component: DashboardsListComponent,
  },
  {
    path: 'logs-explorer',
    component: EnvLogsV4Component,
    data: {
      docs: {
        page: 'management-environment-logs-v4',
      },
    },
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'overview',
  },
  {
    path: '',
    component: EnvAnalyticsLayoutComponent,
    children: [
      {
        path: 'dashboard',
        component: AnalyticsDashboardComponent,
        data: {
          docs: {
            page: 'management-dashboard-analytics',
          },
        },
      },
      {
        path: 'logs',
        component: PlatformLogsComponent,
        data: {
          docs: {
            page: 'management-api-logs',
          },
        },
      },
      {
        path: 'logs/:logId',
        component: PlatformLogComponent,
        data: {
          docs: {
            page: 'management-api-log',
          },
        },
      },
    ],
  },
];

@NgModule({
  declarations: [EnvAnalyticsLayoutComponent, AnalyticsDashboardComponent, PlatformLogsComponent, PlatformLogComponent],
  imports: [
    RouterModule.forChild(routes),
    MatTabsModule,
    MatCardModule,
    MatIconModule,
    GioIconsModule,
    GioBannerModule,
    EnvLogsTableComponent,
    EnvLogsV4Component,
  ],
  exports: [RouterModule],
})
export class EnvAnalyticsModule {}
