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

import { OverviewComponent } from './overview/overview.component';
import { DashboardsListComponent } from './dashboards/dashboards-list/dashboards-list.component';
import { DashboardDetailComponent } from './dashboards/dashboard-detail/dashboard-detail.component';
import { EnvLogsComponent } from './env-logs/env-logs.component';
import { EnvLogsDetailsComponent } from './env-logs/components/env-logs-details/env-logs-details.component';

const routes: Routes = [
  {
    path: 'overview',
    component: OverviewComponent,
  },
  {
    path: 'dashboards',
    data: {
      permissions: {
        anyOf: ['environment-dashboard-r'],
      },
    },
    children: [
      {
        path: '',
        component: DashboardsListComponent,
      },
      {
        path: ':dashboardId',
        component: DashboardDetailComponent,
      },
    ],
  },
  {
    path: 'logs-explorer',
    component: EnvLogsComponent,
    data: {
      docs: {
        page: 'management-environment-logs-v4',
      },
    },
  },
  {
    path: 'logs-explorer/:logId',
    component: EnvLogsDetailsComponent,
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
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ObservabilityModule {}
