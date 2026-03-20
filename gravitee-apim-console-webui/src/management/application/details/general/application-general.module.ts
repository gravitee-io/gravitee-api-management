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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import {
  GioAvatarModule,
  GioBannerModule,
  GioClipboardModule,
  GioFormFilePickerModule,
  GioFormFocusInvalidModule,
  GioFormHeadersModule,
  GioFormSlideToggleModule,
  GioFormTagsInputModule,
  GioLoaderModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { RouterModule } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';

import { ApplicationGeneralComponent } from './application-general.component';
import { AddCertificateDialogComponent } from './add-certificate-dialog/add-certificate-dialog.component';
import { CertificateDetailDialogComponent } from './certificate-detail-dialog/certificate-detail-dialog.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { DaysLeftPipe } from '../../../../shared/pipes/days-left.pipe';
import { DATE_TIME_FORMATS } from '../../../../shared/utils/timeFrameRanges';

@NgModule({
  declarations: [ApplicationGeneralComponent, AddCertificateDialogComponent, CertificateDetailDialogComponent, DaysLeftPipe],
  exports: [ApplicationGeneralComponent],
  imports: [
    CommonModule,

    GioPermissionModule,
    GioAvatarModule,
    GioClipboardModule,
    GioFormFilePickerModule,
    GioFormFocusInvalidModule,
    GioFormSlideToggleModule,
    GioFormTagsInputModule,
    GioLoaderModule,
    GioSaveBarModule,

    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatOptionModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatTableModule,
    MatDialogModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,

    MatChipsModule,

    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    GioBannerModule,
    GioFormHeadersModule,
  ],
  providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
})
export class ApplicationGeneralModule {}
