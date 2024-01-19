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
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { GioFormFocusInvalidModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatLegacyDialogModule } from '@angular/material/legacy-dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacyMenuModule as MatMenuModule } from '@angular/material/legacy-menu';
import { RouterModule } from '@angular/router';

import { ApiPlanListComponent } from './list/api-plan-list.component';
import { ApiPlanEditComponent } from './edit/api-plan-edit.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { ApiPlanFormModule } from '../component/plan/api-plan-form.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';

@NgModule({
  declarations: [ApiPlanListComponent, ApiPlanEditComponent],
  exports: [ApiPlanListComponent, ApiPlanEditComponent],
  imports: [
    CommonModule,
    DragDropModule,
    ReactiveFormsModule,
    RouterModule,

    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    MatLegacyDialogModule,
    MatSnackBarModule,
    MatTableModule,
    MatTooltipModule,
    MatButtonToggleModule,

    GioIconsModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioGoBackButtonModule,
    GioFormFocusInvalidModule,

    ApiPlanFormModule,
    MatMenuModule,
  ],
})
export class ApiPlansModule {}
