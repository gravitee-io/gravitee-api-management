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
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { GioFormFilePickerModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacyDialogModule } from '@angular/material/legacy-dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacyTabsModule as MatTabsModule } from '@angular/material/legacy-tabs';
import { MatLegacyRadioModule as MatRadioModule } from '@angular/material/legacy-radio';

import { ApiPathMappingsComponent } from './api-path-mappings.component';
import { ApiPathMappingsEditDialogComponent } from './api-path-mappings-edit-dialog/api-path-mappings-edit-dialog.component';
import { ApiPathMappingsAddDialogComponent } from './api-path-mappings-add-dialog/api-path-mappings-add-dialog.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApiPathMappingsComponent, ApiPathMappingsAddDialogComponent, ApiPathMappingsEditDialogComponent],
  exports: [ApiPathMappingsComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,

    GioFormFilePickerModule,
    GioIconsModule,
    GioPermissionModule,

    MatButtonModule,
    MatDialogModule,
    MatLegacyDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
    MatSnackBarModule,
    MatTableModule,
    MatTooltipModule,
    MatTabsModule,
  ],
})
export class ApiPathMappingsModule {}
