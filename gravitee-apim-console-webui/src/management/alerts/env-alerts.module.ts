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
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { EnvAlertsLayoutComponent } from './env-alerts-layout.component';
import { AlertsActivityComponent } from './activity/alerts-activity.component';

import { AlertComponent } from '../../components/alerts/alert/alert.component';
import { AlertsComponent } from '../../components/alerts/alerts.component';
import { AlertsModule } from '../../components/alerts/alerts.module';

const routes: Routes = [
  {
    path: '',
    component: EnvAlertsLayoutComponent,
    children: [
      {
        path: 'list/new',
        component: AlertComponent,
        data: {
          perms: {
            only: ['environment-alert-c'],
          },
          docs: {
            page: 'management-alerts',
          },
        },
      },
      {
        path: 'list/:alertId',
        component: AlertComponent,
        data: {
          docs: {
            page: 'management-alerts',
          },
        },
      },
      {
        path: 'list',
        component: AlertsComponent,
        data: {
          docs: {
            page: 'management-alerts',
          },
        },
      },
      {
        path: 'activity',
        component: AlertsActivityComponent,
        data: {
          docs: {
            page: 'management-dashboard-alerts',
          },
        },
      },

      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'list',
      },
    ],
  },
];

@NgModule({
  declarations: [EnvAlertsLayoutComponent, AlertsActivityComponent],
  imports: [RouterModule.forChild(routes), AlertsModule, MatTabsModule, GioIconsModule],
  exports: [RouterModule],
})
export class EnvAlertModule {}
