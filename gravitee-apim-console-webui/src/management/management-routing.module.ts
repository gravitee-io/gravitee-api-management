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

import { ManagementComponent } from './management.component';
import { HasEnvironmentPermissionGuard } from './has-environment-permission.guard';
import { InstanceListComponent } from './instances/instance-list/instance-list.component';
import { InstanceDetailsComponent } from './instances/instance-details/instance-details.component';
import { InstanceDetailsEnvironmentComponent } from './instances/instance-details/instance-details-environment/instance-details-environment.component';
import { InstanceDetailsMonitoringComponent } from './instances/instance-details/instance-details-monitoring/instance-details-monitoring.component';
import { EnvAuditComponent } from './audit/env-audit.component';
import { MessagesComponent } from './messages/messages.component';

import { TicketsListComponent } from '../user/support/tickets-list.component';
import { TicketDetailComponent } from '../user/support/ticket-detail.component';
import { TicketComponent } from '../user/support/ticket.component';
import { TicketComponent as ngTicketComponent } from '../user/support/ticket/ticket.component';
import { TasksComponent } from '../user/tasks/tasks.component';
import { UserComponent } from '../user/my-accout/user.component';
import { ApimFeature } from '../shared/components/gio-license/gio-license-data';
import { HasLicenseGuard } from '../shared/components/gio-license/has-license.guard';

const managementRoutes: Routes = [
  {
    path: '',
    component: ManagementComponent,
    canActivate: [HasEnvironmentPermissionGuard],
    canActivateChild: [HasEnvironmentPermissionGuard, HasLicenseGuard],
    canDeactivate: [HasEnvironmentPermissionGuard],
    children: [
      {
        path: 'home',
        loadChildren: () => import('./home/home.module').then((m) => m.HomeModule),
      },
      {
        path: 'apis',
        loadChildren: () => import('./api/apis.module').then((m) => m.ApisModule),
      },
      {
        path: 'settings',
        loadChildren: () => import('./settings/settings.module').then((m) => m.SettingsModule),
      },
      {
        path: 'my-account',
        component: UserComponent,
      },
      {
        path: 'tasks',
        component: TasksComponent,
        data: {
          docs: {
            page: 'management-tasks',
          },
        },
      },
      {
        path: 'ng-support/tickets/:ticketId',
        component: ngTicketComponent,
      },
      {
        path: 'support/new',
        component: TicketComponent,
      },
      {
        path: 'support/list',
        component: TicketsListComponent,
      },
      {
        path: 'support/:ticketId',
        component: TicketDetailComponent,
      },
      {
        path: 'applications',
        loadChildren: () => import('./application/applications.route').then((m) => m.ApplicationsRouteModule),
      },
      {
        path: 'gateways',
        component: InstanceListComponent,
        data: {
          perms: {
            only: ['environment-instance-r'],
          },
          docs: {
            page: 'management-gateways',
          },
        },
      },
      {
        path: 'gateways/:instanceId',
        component: InstanceDetailsComponent,
        data: {
          perms: {
            only: ['environment-instance-r'],
          },
        },
        children: [
          {
            path: 'environment',
            component: InstanceDetailsEnvironmentComponent,
            data: {
              docs: {
                page: 'management-gateway-environment',
              },
              useAngularMaterial: true,
            },
          },
          {
            path: 'monitoring',
            component: InstanceDetailsMonitoringComponent,
            data: {
              docs: {
                page: 'management-gateway-monitoring',
              },
              useAngularMaterial: true,
            },
          },
          {
            path: '',
            pathMatch: 'full',
            redirectTo: 'environment',
          },
        ],
      },
      {
        path: 'audit',
        component: EnvAuditComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.APIM_AUDIT_TRAIL },
            redirect: '/',
          },
          perms: {
            only: ['environment-audit-r'],
          },
          docs: {
            page: 'management-audit',
          },
        },
      },
      {
        path: 'messages',
        component: MessagesComponent,
        data: {
          docs: {
            page: 'management-messages',
          },
        },
      },
      {
        path: 'analytics',
        loadChildren: () => import('./analytics/env-analytics.module').then((m) => m.EnvAnalyticsModule),
        data: {
          perms: {
            only: ['environment-platform-r'],
          },
        },
      },
      {
        path: 'alerts',
        loadChildren: () => import('./alerts/env-alerts.module').then((m) => m.EnvAlertModule),
        data: {
          requireLicense: {
            license: { feature: ApimFeature.ALERT_ENGINE },
            redirect: '/',
          },
          perms: {
            only: ['environment-alert-r'],
          },
        },
      },

      { path: '', pathMatch: 'full', redirectTo: 'home' },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
];

@NgModule({
  imports: [RouterModule.forChild(managementRoutes)],
  exports: [RouterModule],
})
export class ManagementRoutingModule {}
