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
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { GioConfirmDialogModule } from '@gravitee/ui-particles-angular';
import { RouterModule, Routes } from '@angular/router';

import { EnvAuditModule } from './audit/env-audit.module';
import { MessagesModule } from './messages/messages.module';
import { TasksModule } from './tasks/tasks.module';
import { ClientRegistrationProvidersModule } from './configuration/client-registration-providers/client-registration-providers.module';
import { ApiLoggingModule } from './configuration/api-logging/api-logging.module';
import { EnvironmentNotificationSettingsModule } from './configuration/notifications/notification-settings/environment-notification-settings.module';
import { EnvironmentMetadataModule } from './configuration/metadata/environment-metadata.module';
import { ManagementComponent } from './management.component';
import { HasEnvironmentPermissionGuard } from './has-environment-permission.guard';
import { EnvironmentResolver } from './environement.resolver';
import { TasksComponent } from './tasks/tasks.component';
import { TicketComponent } from './support/ticket.component';
import { TicketDetailComponent } from './support/ticket-detail.component';
import { TicketsListComponent } from './support/tickets-list.component';
import { InstancesModule } from './instances/instances.module';
import { InstanceListComponent } from './instances/instance-list/instance-list.component';
import { InstanceDetailsComponent } from './instances/instance-details/instance-details.component';
import { InstanceDetailsEnvironmentComponent } from './instances/instance-details/instance-details-environment/instance-details-environment.component';
import { InstanceDetailsMonitoringComponent } from './instances/instance-details/instance-details-monitoring/instance-details-monitoring.component';
import { EnvAuditComponent } from './audit/env-audit.component';
import { MessagesComponent } from './messages/messages.component';
import { AnalyticsDashboardComponent } from './analytics/analytics-dashboard/analytics-dashboard.component';
import { PlatformLogsComponent } from './analytics/logs/platform-logs.component';
import { PlatformLogComponent } from './analytics/logs/platform-log.component';

import { GioPermissionModule } from '../shared/components/gio-permission/gio-permission.module';
import { NotificationsModule } from '../components/notifications/notifications.module';
import { GioSideNavModule } from '../components/gio-side-nav/gio-side-nav.module';
import { GioTopNavModule } from '../components/gio-top-nav/gio-top-nav.module';
import { ContextualDocComponentComponent } from '../components/contextual/contextual-doc.component';
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
    resolve: {
      environmentResolver: EnvironmentResolver,
    },
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
        path: 'analytics/dashboard',
        component: AnalyticsDashboardComponent,
        data: {
          perms: {
            only: ['environment-platform-r'],
          },
          docs: {
            page: 'management-dashboard-analytics',
          },
        },
      },
      {
        path: 'analytics/logs',
        component: PlatformLogsComponent,
        data: {
          perms: {
            only: ['environment-platform-r'],
          },
          docs: {
            page: 'management-api-logs',
          },
        },
      },
      {
        path: 'analytics/logs/:logId',
        component: PlatformLogComponent,
        data: {
          perms: {
            only: ['environment-platform-r'],
          },
          docs: {
            page: 'management-api-log',
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
  imports: [
    CommonModule,
    MatSnackBarModule,
    GioPermissionModule,
    GioConfirmDialogModule,
    EnvAuditModule,
    // HomeModule,
    // ApisModule,
    // ApplicationsModule,
    // SettingsNavigationModule,
    InstancesModule,
    MessagesModule,
    TasksModule,
    ClientRegistrationProvidersModule,
    ApiLoggingModule,
    NotificationsModule,
    EnvironmentNotificationSettingsModule,
    EnvironmentMetadataModule,
    GioSideNavModule,
    GioTopNavModule,
    RouterModule.forChild(managementRoutes),
  ],
  declarations: [
    ManagementComponent,
    ContextualDocComponentComponent,
    TicketComponent,
    TicketDetailComponent,
    TicketsListComponent,
    AnalyticsDashboardComponent,
    PlatformLogsComponent,
    PlatformLogComponent,
  ],
  exports: [RouterModule],
})
export class ManagementModule {}
