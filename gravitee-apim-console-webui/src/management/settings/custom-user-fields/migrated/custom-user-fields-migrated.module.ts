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
import { MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSortModule } from '@angular/material/sort';
import { MatCardModule } from '@angular/material/card';
import { GioFormSlideToggleModule, GioFormTagsInputModule } from '@gravitee/ui-particles-angular';

import { CustomUserFieldsMigratedComponent } from './custom-user-fields-migrated.component';
import { CustomUserFieldsDialogComponent } from './dialog/custom-user-fields-dialog.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,

    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatIconModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatDialogModule,
    MatAutocompleteModule,

    GioFormSlideToggleModule,
    GioTableWrapperModule,
    GioFormTagsInputModule,
    GioPermissionModule,
  ],
  declarations: [CustomUserFieldsMigratedComponent, CustomUserFieldsDialogComponent],
  exports: [CustomUserFieldsMigratedComponent],
})
export class CustomUserFieldsModule {}
