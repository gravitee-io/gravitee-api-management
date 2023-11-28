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

import { SettingsNavigationComponent } from '../configuration/settings-navigation/settings-navigation.component';
import { SettingsAnalyticsComponent } from '../configuration/analytics/settings-analytics.component';
import { SettingsAnalyticsDashboardComponent } from '../configuration/analytics/dashboard/settings-analytics-dashboard.component';

export const settingsRoutes: Routes = [
  {
    path: '',
    component: SettingsNavigationComponent,
    children: [
      {
        path: 'analytics',
        component: SettingsAnalyticsComponent,
        data: {
          menu: null,
          docs: {
            page: 'management-configuration-analytics',
          },
          perms: {
            only: ['environment-dashboard-r'],
            unauthorizedFallbackTo: 'management.settings.apiPortalHeader',
          },
        },
      },
      {
        path: 'analytics/dashboard/:type/new',
        component: SettingsAnalyticsDashboardComponent,
        data: {
          menu: null,
          docs: {
            page: 'management-configuration-dashboard',
          },
          perms: {
            only: ['environment-dashboard-c'],
          },
        },
      },
      {
        path: 'analytics/dashboard/:type/:dashboardId',
        component: SettingsAnalyticsDashboardComponent,
        data: {
          menu: null,
          docs: {
            page: 'management-configuration-dashboard',
          },
          perms: {
            only: ['environment-dashboard-u'],
          },
        },
      },
    ],
  },
];
@NgModule({
  imports: [RouterModule.forChild(settingsRoutes)],
  exports: [RouterModule],
})
export class SettingsRoutingModule {}
