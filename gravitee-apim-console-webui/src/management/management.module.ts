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
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { GioPolicyStudioModule } from '@gravitee/ui-policy-studio-angular';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { GioPolicyStudioRoutingModule } from './api/policy-studio/gio-policy-studio-routing.module';
import { EnvAuditModule } from './audit/env-audit.module';
import { ApiCreationModule } from './api/creation/api-creation.module';

import { GioConfirmDialogModule } from '../shared/components/gio-confirm-dialog/gio-confirm-dialog.module';
import { GioPermissionModule } from '../shared/components/gio-permission/gio-permission.module';
@NgModule({
  imports: [
    CommonModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    GioPermissionModule,
    GioConfirmDialogModule,
    GioPolicyStudioModule,
    EnvAuditModule,
    ApiCreationModule,
    GioPolicyStudioRoutingModule.withRouting({ stateNamePrefix: 'management.apis.detail.design.flowsNg' }),
  ],
  declarations: [],
  entryComponents: [],
})
export class ManagementModule {}
