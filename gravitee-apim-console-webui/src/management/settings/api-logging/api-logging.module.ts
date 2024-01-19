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
import {
  GioAvatarModule,
  GioBannerModule,
  GioFormFocusInvalidModule,
  GioFormSlideToggleModule,
  GioFormTagsInputModule,
  GioIconsModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatLegacyOptionModule as MatOptionModule } from '@angular/material/legacy-core';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacyCheckboxModule as MatCheckboxModule } from '@angular/material/legacy-checkbox';

import { ApiLoggingComponent } from './api-logging.component';

import { GioFormCardGroupModule } from '../../../shared/components/gio-form-card-group/gio-form-card-group.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioTableOfContentsModule } from '../../../shared/components/gio-table-of-contents/gio-table-of-contents.module';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    MatOptionModule,
    MatSelectModule,
    MatCheckboxModule,

    GioFormCardGroupModule,
    GioIconsModule,
    GioBannerModule,
    GioFormSlideToggleModule,
    GioAvatarModule,
    GioGoBackButtonModule,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
    GioFormTagsInputModule,
    GioPermissionModule,
    GioTableOfContentsModule,
  ],
  declarations: [ApiLoggingComponent],
  exports: [ApiLoggingComponent],
})
export class ApiLoggingModule {}
