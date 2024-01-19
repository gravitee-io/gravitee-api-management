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
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import {
  GioConfirmDialogModule,
  GioIconsModule,
  GioSaveBarModule,
  GioFormFocusInvalidModule,
  GioClipboardModule,
} from '@gravitee/ui-particles-angular';
import { MatDialogModule } from '@angular/material/dialog';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatLegacyCheckboxModule as MatCheckboxModule } from '@angular/material/legacy-checkbox';
import { MatLegacyAutocompleteModule as MatAutocompleteModule } from '@angular/material/legacy-autocomplete';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';

import { ApiEntrypointsComponent } from './api-entrypoints.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioFormListenersContextPathModule } from '../component/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.module';
import { GioFormListenersVirtualHostModule } from '../component/gio-form-listeners/gio-form-listeners-virtual-host/gio-form-listeners-virtual-host.module';

@NgModule({
  declarations: [ApiEntrypointsComponent],
  exports: [ApiEntrypointsComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatDialogModule,
    MatTableModule,
    MatCheckboxModule,
    MatAutocompleteModule,
    MatSnackBarModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioConfirmDialogModule,
    GioIconsModule,
    GioFormFocusInvalidModule,
    GioClipboardModule,
    GioFormListenersContextPathModule,
    GioFormListenersVirtualHostModule,
  ],
})
export class ApiEntrypointsModule {}
