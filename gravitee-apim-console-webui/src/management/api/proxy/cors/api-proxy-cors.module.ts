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
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { GioFormTagsInputModule, GioSaveBarModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';

import { ApiProxyCorsComponent } from './api-proxy-cors.component';

import { GioFormFocusInvalidModule } from '../../../../shared/components/gio-form-focus-first-invalid/gio-form-focus-first-invalid.module';

@NgModule({
  declarations: [ApiProxyCorsComponent],
  exports: [ApiProxyCorsComponent],
  imports: [
    CommonModule,

    ReactiveFormsModule,

    MatFormFieldModule,
    MatSelectModule,
    MatCardModule,
    MatDialogModule,
    MatSlideToggleModule,
    MatDividerModule,
    MatSnackBarModule,
    MatInputModule,
    GioFormSlideToggleModule,
    GioFormTagsInputModule,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
  ],
})
export class ApiProxyCorsModule {}
