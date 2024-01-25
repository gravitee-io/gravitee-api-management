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
import { CommonModule } from '@angular/common';
import { MatInputModule } from '@angular/material/input';
import { GioConfirmDialogModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { ApiPortalHeaderComponent } from './api-portal-header.component';
import { ApiPortalHeaderEditDialogComponent } from './api-portal-header-edit-dialog/api-portal-header-edit-dialog.component';

import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApiPortalHeaderComponent, ApiPortalHeaderEditDialogComponent],
  imports: [
    CommonModule,
    FormsModule,
    MatInputModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatTableModule,
    MatSortModule,
    MatIconModule,
    MatOptionModule,
    MatSelectModule,
    MatCardModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatButtonModule,
    GioConfirmDialogModule,
    GioFormSlideToggleModule,
    GioTableWrapperModule,
    GioPermissionModule,
  ],
  exports: [ApiPortalHeaderComponent],
})
export class ApiPortalHeaderModule {}
