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
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { GioFormHeadersModule, GioIconsModule, GioSaveBarModule, GioFormFocusInvalidModule } from '@gravitee/ui-particles-angular';
import { MatLegacyAutocompleteModule as MatAutocompleteModule } from '@angular/material/legacy-autocomplete';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacyDialogModule } from '@angular/material/legacy-dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { RouterModule } from '@angular/router';

import { ApiResponseTemplatesEditComponent } from './edit/api-response-templates-edit.component';
import { ApiResponseTemplatesListComponent } from './list/api-response-templates-list.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';

@NgModule({
  declarations: [ApiResponseTemplatesListComponent, ApiResponseTemplatesEditComponent],
  exports: [ApiResponseTemplatesListComponent, ApiResponseTemplatesEditComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,

    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatSnackBarModule,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatDialogModule,
    MatLegacyDialogModule,

    GioPermissionModule,
    GioIconsModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioFormHeadersModule,
    GioFormFocusInvalidModule,
    GioGoBackButtonModule,
  ],
})
export class ApiResponseTemplatesModule {}
