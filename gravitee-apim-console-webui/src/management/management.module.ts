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

import { GioPolicyStudioRoutingModule } from './api/policy-studio/gio-policy-studio-routing.module';
import { EnvAuditModule } from './audit/env-audit.module';
import { ApiCreationModule } from './api/creation/api-creation.module';
import { EnvironmentApplicationModule } from './application/environment-application.module';
import { ApisModule } from './api/apis.module';
import { SettingsNavigationModule } from './configuration/settings-navigation/settings-navigation.module';
import { InstanceDetailsModule } from './instances/instance-details/instance-details.module';
import { ApiCreationV4Module } from './api/creation/v4/api-creation-v4.module';
import { MessagesModule } from './messages/messages.module';

import { GioPermissionModule } from '../shared/components/gio-permission/gio-permission.module';

@NgModule({
  imports: [
    CommonModule,
    BrowserAnimationsModule,
    MatSnackBarModule,
    GioPermissionModule,
    GioConfirmDialogModule,
    EnvAuditModule,
    ApiCreationModule,
    ApiCreationV4Module,
    GioPolicyStudioRoutingModule.withRouting({ stateNamePrefix: 'management.apis.detail.design.flowsNg' }),
    EnvironmentApplicationModule,
    ApisModule,
    SettingsNavigationModule,
    InstanceDetailsModule.withRouting({ stateNamePrefix: 'management.instances.detail' }),
    MessagesModule,
  ],
  declarations: [],
  entryComponents: [],
})
export class ManagementModule {}
