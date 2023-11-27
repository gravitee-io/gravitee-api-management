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
import { EnvironmentApplicationModule } from './application/environment-application.module';
import { SettingsNavigationModule } from './configuration/settings-navigation/settings-navigation.module';
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

import { GioPermissionModule } from '../shared/components/gio-permission/gio-permission.module';
import { NotificationsModule } from '../components/notifications/notifications.module';
import { AlertsModule } from '../components/alerts/alerts.module';
import { GioSideNavModule } from '../components/gio-side-nav/gio-side-nav.module';
import { GioTopNavModule } from '../components/gio-top-nav/gio-top-nav.module';
import { ContextualDocComponentComponent } from '../components/contextual/contextual-doc.component';
import { UserComponent } from '../user/my-accout/user.component';

const managementRoutes: Routes = [
  {
    path: '',
    component: ManagementComponent,
    canActivate: [HasEnvironmentPermissionGuard],
    canActivateChild: [HasEnvironmentPermissionGuard],
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

      { path: '', pathMatch: 'full', redirectTo: 'home' },
    ],
  },
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
    EnvironmentApplicationModule,
    SettingsNavigationModule,
    InstancesModule,
    MessagesModule,
    TasksModule,
    ClientRegistrationProvidersModule,
    ApiLoggingModule,
    NotificationsModule,
    AlertsModule,
    EnvironmentNotificationSettingsModule,
    EnvironmentMetadataModule,
    GioSideNavModule,
    GioTopNavModule,
    RouterModule.forChild(managementRoutes),
  ],
  declarations: [ManagementComponent, ContextualDocComponentComponent, TicketComponent, TicketDetailComponent, TicketsListComponent],
  exports: [RouterModule],
})
export class ManagementModule {}
