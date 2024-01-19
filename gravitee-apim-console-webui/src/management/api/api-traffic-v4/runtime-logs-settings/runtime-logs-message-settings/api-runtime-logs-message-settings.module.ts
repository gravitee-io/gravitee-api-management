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
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { GioBannerModule, GioFormSlideToggleModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyCheckboxModule as MatCheckboxModule } from '@angular/material/legacy-checkbox';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';

import { ApiRuntimeLogsMessageSettingsComponent } from './api-runtime-logs-message-settings.component';

import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApiRuntimeLogsMessageSettingsComponent],
  exports: [ApiRuntimeLogsMessageSettingsComponent],
  imports: [
    CommonModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatTooltipModule,
    ReactiveFormsModule,

    GioBannerModule,
    GioFormSlideToggleModule,
    GioIconsModule,
    GioPermissionModule,
    GioSaveBarModule,
  ],
})
export class ApiRuntimeLogsMessageSettingsModule {}
