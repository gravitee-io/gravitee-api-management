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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatRadioModule } from '@angular/material/radio';
import { MatSortModule } from '@angular/material/sort';
import { FormsModule } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { EnvApplicationListComponent } from './list/env-application-list.component';

import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { GioAvatarModule } from '../../shared/components/gio-avatar/gio-avatar.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioConfirmDialogModule } from '../../shared/components/gio-confirm-dialog/gio-confirm-dialog.module';

@NgModule({
  imports: [
    CommonModule,

    MatButtonModule,
    MatDialogModule,
    MatSnackBarModule,
    MatIconModule,
    MatInputModule,
    MatTooltipModule,
    MatTableModule,
    MatPaginatorModule,
    MatRadioModule,
    MatSortModule,

    GioPermissionModule,
    GioAvatarModule,
    GioConfirmDialogModule,
    GioTableWrapperModule,
    GioIconsModule,
    FormsModule,
  ],
  declarations: [EnvApplicationListComponent],
  exports: [EnvApplicationListComponent],
  entryComponents: [EnvApplicationListComponent],
  providers: [],
})
export class EnvironmentApplicationModule {}
