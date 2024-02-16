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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GioFormSlideToggleModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatIconModule } from '@angular/material/icon';
import { MatSortModule } from '@angular/material/sort';
import { MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { ApiQualityRulesNgComponent } from './api-quality-rules-ng.component';
import { ApiQualityRulesNgAddDialogComponent } from './api-quality-rules-ng-add-dialog/api-quality-rules-ng-add-dialog.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,

    GioFormSlideToggleModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioTableWrapperModule,
    MatButtonModule,
    MatDialogModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    MatSnackBarModule,
  ],
  declarations: [ApiQualityRulesNgComponent, ApiQualityRulesNgAddDialogComponent],
  exports: [ApiQualityRulesNgComponent, ApiQualityRulesNgAddDialogComponent],
})
export class ApiQualityRulesNgModule {}
