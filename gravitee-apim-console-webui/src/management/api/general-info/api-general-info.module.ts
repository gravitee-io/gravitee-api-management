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
  GioFormFocusInvalidModule,
  GioClipboardModule,
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
import { RouterLink } from '@angular/router';

import { ApiGeneralInfoComponent } from './api-general-info.component';
import { ApiGeneralInfoQualityComponent } from './api-general-info-quality/api-general-info-quality.component';
import { ApiGeneralInfoDangerZoneComponent } from './api-general-info-danger-zone/api-general-info-danger-zone.component';
import { ApiGeneralInfoDuplicateDialogComponent } from './api-general-info-duplicate-dialog/api-general-info-duplicate-dialog.component';
import { ApiGeneralInfoExportV2DialogComponent } from './api-general-info-export-v2-dialog/api-general-info-export-v2-dialog.component';
import { ApiGeneralInfoPromoteDialogComponent } from './api-general-info-promote-dialog/api-general-info-promote-dialog.component';
import { ApiGeneralInfoExportV4DialogComponent } from './api-general-info-export-v4-dialog/api-general-info-export-v4-dialog.component';
import { ApiGeneralInfoAgentCardComponent } from './api-general-info-agent-card/api-general-info-agent-card.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioCircularPercentageModule } from '../../../shared/components/gio-circular-percentage/gio-circular-percentage.module';
import { GioApiImportDialogModule } from '../component/gio-api-import-dialog/gio-api-import-dialog.module';
import { GioLicenseBannerModule } from '../../../shared/components/gio-license-banner/gio-license-banner.module';
import { ZeeModule } from '../../../shared/components/zee/zee.module';

@NgModule({
  declarations: [
    ApiGeneralInfoComponent,
    ApiGeneralInfoQualityComponent,
    ApiGeneralInfoDangerZoneComponent,
    ApiGeneralInfoDuplicateDialogComponent,
    ApiGeneralInfoExportV2DialogComponent,
    ApiGeneralInfoExportV4DialogComponent,
    ApiGeneralInfoPromoteDialogComponent,
  ],
  exports: [ApiGeneralInfoComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,

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
    GioLicenseBannerModule,
    ApiGeneralInfoAgentCardComponent,
    ZeeModule,
  ],
})
export class ApiGeneralInfoModule {}
