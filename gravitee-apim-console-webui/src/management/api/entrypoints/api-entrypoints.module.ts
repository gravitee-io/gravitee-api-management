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
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import {
  GioConfirmDialogModule,
  GioIconsModule,
  GioSaveBarModule,
  GioFormFocusInvalidModule,
  GioClipboardModule,
} from '@gravitee/ui-particles-angular';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatLegacySnackBarModule } from '@angular/material/legacy-snack-bar';

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
    MatLegacySnackBarModule,
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
