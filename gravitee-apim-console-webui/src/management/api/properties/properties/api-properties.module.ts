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
import { GioBannerModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';

import { ApiPropertiesComponent } from './api-properties.component';
import { PropertiesAddDialogModule } from './properties-add-dialog/properties-add-dialog.module';
import { PropertiesImportDialogModule } from './properties-import-dialog/properties-import-dialog.module';

import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { ApiDynamicPropertiesV2Module } from '../components/dynamic-properties-v2/api-dynamic-properties-v2.module';
import { ApiDynamicPropertiesV4Module } from '../components/dynamic-properties-v4/api-dynamic-properties-v4.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,

    GioSaveBarModule,
    GioTableWrapperModule,
    GioPermissionModule,
    GioIconsModule,
    PropertiesAddDialogModule,
    PropertiesImportDialogModule,

    MatSlideToggleModule,
    MatTableModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatDialogModule,
    GioBannerModule,

    ApiDynamicPropertiesV2Module,
    ApiDynamicPropertiesV4Module,
  ],
  declarations: [ApiPropertiesComponent],
  exports: [ApiPropertiesComponent],
})
export class ApiPropertiesModule {}
