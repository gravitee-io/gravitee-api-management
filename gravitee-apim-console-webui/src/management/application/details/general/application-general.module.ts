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
  GioClipboardModule,
  GioFormFilePickerModule,
  GioFormFocusInvalidModule,
  GioFormHeadersModule,
  GioFormSlideToggleModule,
  GioFormTagsInputModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { RouterModule } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ApplicationGeneralComponent } from './application-general.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApplicationGeneralComponent],
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

    MatChipsModule,

    ReactiveFormsModule,
    RouterModule,
    GioFormHeadersModule,
  ],
})
export class ApplicationGeneralModule {}
