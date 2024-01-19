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
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { GioBannerModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { RouterModule } from '@angular/router';
import { A11yModule } from '@angular/cdk/a11y';

import { SignUpConfirmComponent } from './sign-up-confirm.component';

@NgModule({
  declarations: [SignUpConfirmComponent],
  exports: [SignUpConfirmComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    A11yModule,

    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatButtonModule,
    MatSnackBarModule,

    GioIconsModule,
    GioBannerModule,
  ],
})
export class SignUpConfirmModule {}
