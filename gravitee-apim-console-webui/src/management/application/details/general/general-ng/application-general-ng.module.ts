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
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import {
  GioAvatarModule,
  GioClipboardModule,
  GioFormFilePickerModule,
  GioFormFocusInvalidModule,
  GioFormSlideToggleModule,
  GioFormTagsInputModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacyOptionModule as MatOptionModule } from '@angular/material/legacy-core';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyChipsModule as MatChipsModule } from '@angular/material/legacy-chips';
import { RouterModule } from '@angular/router';

import { ApplicationGeneralNgComponent } from './application-general-ng.component';

import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApplicationGeneralNgComponent],
  exports: [ApplicationGeneralNgComponent],
  imports: [
    CommonModule,

    GioPermissionModule,
    GioAvatarModule,
    GioClipboardModule,
    GioFormFilePickerModule,
    GioFormFocusInvalidModule,
    GioFormSlideToggleModule,
    GioFormTagsInputModule,
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
    MatChipsModule,

    ReactiveFormsModule,
    RouterModule,
  ],
})
export class ApplicationGeneralNgModule {}
