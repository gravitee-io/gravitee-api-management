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
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { GioSaveBarModule, GioConfirmDialogModule, GioIconsModule, GioLicenseModule } from '@gravitee/ui-particles-angular';
import { RouterModule } from '@angular/router';

import { GioPolicyStudioLayoutComponent } from './gio-policy-studio-layout.component';
import { PolicyStudioDesignModule } from './design/policy-studio-design.module';
import { PolicyStudioConfigModule } from './config/policy-studio-config.module';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { DebugModeModule } from '../debug-mode/debug-mode.module';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

    MatTabsModule,
    MatSnackBarModule,

    GioSaveBarModule,
    GioPermissionModule,
    GioConfirmDialogModule,

    PolicyStudioDesignModule,
    PolicyStudioConfigModule,
    DebugModeModule,
    GioLicenseModule,
    GioIconsModule,
  ],
  declarations: [GioPolicyStudioLayoutComponent],
})
export class GioPolicyStudioRoutingModule {}
