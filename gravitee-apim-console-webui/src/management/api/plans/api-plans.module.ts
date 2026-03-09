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
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { GioFormFocusInvalidModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { ReactiveFormsModule } from '@angular/forms';
import { MatMenuModule } from '@angular/material/menu';
import { RouterModule } from '@angular/router';

import { ApiPlanListComponent } from './list/api-plan-list.component';
import { ApiPlanEditComponent } from './edit/api-plan-edit.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { ApiPlanFormModule } from '../component/plan/api-plan-form.module';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { PlanListComponent } from '../component/plan/plan-list/plan-list.component';

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
    PlanListComponent,
  ],
})
export class ApiPlansModule {}
