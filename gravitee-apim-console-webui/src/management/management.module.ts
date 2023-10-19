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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { GioConfirmDialogModule } from '@gravitee/ui-particles-angular';

import { EnvAuditModule } from './audit/env-audit.module';
import { EnvironmentApplicationModule } from './application/environment-application.module';
import { ApisModule } from './api/apis.module';
import { SettingsNavigationModule } from './configuration/settings-navigation/settings-navigation.module';
import { InstanceDetailsModule } from './instances/instance-details/instance-details.module';
import { MessagesModule } from './messages/messages.module';
import { HomeModule } from './home/home.module';
import { TasksModule } from './tasks/tasks.module';
import { ClientRegistrationProvidersModule } from './configuration/client-registration-providers/client-registration-providers.module';
import { EnvironmentNotificationSettingsModule } from './configuration/notifications/notification-settings/environment-notification-settings.module';

import { GioPermissionModule } from '../shared/components/gio-permission/gio-permission.module';
import { NotificationsModule } from '../components/notifications/notifications.module';
import { AlertsModule } from '../components/alerts/alerts.module';

@NgModule({
  imports: [
    CommonModule,
    BrowserAnimationsModule,
    MatSnackBarModule,
    GioPermissionModule,
    GioConfirmDialogModule,
    EnvAuditModule,
    HomeModule,
    ApisModule,
    EnvironmentApplicationModule,
    SettingsNavigationModule,
    InstanceDetailsModule.withRouting({ stateNamePrefix: 'management.instances.detail' }),
    MessagesModule,
    TasksModule,
    ClientRegistrationProvidersModule,
    NotificationsModule,
    AlertsModule,
    EnvironmentNotificationSettingsModule,
  ],
  declarations: [],
  entryComponents: [],
})
export class ManagementModule {}
