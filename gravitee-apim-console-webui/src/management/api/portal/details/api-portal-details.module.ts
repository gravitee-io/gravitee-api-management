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
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import {
  GioAvatarModule,
  GioBannerModule,
  GioFormFilePickerModule,
  GioFormTagsInputModule,
  GioSaveBarModule,
  GioFormSlideToggleModule,
} from '@gravitee/ui-particles-angular';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { A11yModule } from '@angular/cdk/a11y';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { ApiPortalDetailsComponent } from './api-portal-details.component';
import { ApiPortalDetailsQualityComponent } from './api-portal-details-quality/api-portal-details-quality.component';
import { ApiPortalDetailsDangerZoneComponent } from './api-portal-details-danger-zone/api-portal-details-danger-zone.component';
import { ApiPortalDetailsDuplicateDialogComponent } from './api-portal-details-duplicate-dialog/api-portal-details-duplicate-dialog.component';
import { ApiPortalDetailsExportDialogComponent } from './api-portal-details-export-dialog/api-portal-details-export-dialog.component';
import { ApiPortalDetailsPromoteDialogComponent } from './api-portal-details-promote-dialog/api-portal-details-promote-dialog.component';

import { GioFormFocusInvalidModule } from '../../../../shared/components/gio-form-focus-first-invalid/gio-form-focus-first-invalid.module';
import { GioClipboardModule } from '../../../../shared/components/gio-clipboard/gio-clipboard.module';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioCircularPercentageModule } from '../../../../shared/components/gio-circular-percentage/gio-circular-percentage.module';
import { GioApiImportDialogModule } from '../../../../shared/components/gio-api-import-dialog/gio-api-import-dialog.module';

@NgModule({
  declarations: [
    ApiPortalDetailsComponent,
    ApiPortalDetailsQualityComponent,
    ApiPortalDetailsDangerZoneComponent,
    ApiPortalDetailsDuplicateDialogComponent,
    ApiPortalDetailsExportDialogComponent,
    ApiPortalDetailsPromoteDialogComponent,
  ],
  exports: [ApiPortalDetailsComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    A11yModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatIconModule,
    MatSelectModule,
    MatButtonModule,
    MatDialogModule,
    MatSlideToggleModule,
    MatCheckboxModule,
    MatTooltipModule,
    MatTabsModule,
    MatProgressBarModule,

    GioFormFocusInvalidModule,
    GioAvatarModule,
    GioFormFilePickerModule,
    GioSaveBarModule,
    GioFormTagsInputModule,
    GioClipboardModule,
    GioPermissionModule,
    GioFormSlideToggleModule,
    GioCircularPercentageModule,
    GioApiImportDialogModule,
    GioBannerModule,
  ],
})
export class ApiPortalDetailsModule {}
