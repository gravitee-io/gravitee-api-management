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
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import {
  GioFormTagsInputModule,
  GioSaveBarModule,
  GioFormSlideToggleModule,
  GioFormFocusInvalidModule,
} from '@gravitee/ui-particles-angular';

import { ApiFailoverComponent } from './api-failover.component';

@NgModule({
  declarations: [ApiFailoverComponent],
  exports: [ApiFailoverComponent],
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
export class ApiFailoverModule {}
