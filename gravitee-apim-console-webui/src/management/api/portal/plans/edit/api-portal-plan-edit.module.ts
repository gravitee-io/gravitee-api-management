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
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatStepperModule } from '@angular/material/stepper';
import {
  GioBannerModule,
  GioFormTagsInputModule,
  GioIconsModule,
  GioSaveBarModule,
  GioFormSlideToggleModule,
} from '@gravitee/ui-particles-angular';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';

import { PlanEditGeneralStepComponent } from './1-general-step/plan-edit-general-step.component';
import { PlanEditSecureStepComponent } from './2-secure-step/plan-edit-secure-step.component';
import { PlanEditRestrictionStepComponent } from './3-restriction-step/plan-edit-restriction-step.component';
import { ApiPortalPlanEditComponent } from './api-portal-plan-edit.component';

import { GioFormFocusInvalidModule } from '../../../../../shared/components/gio-form-focus-first-invalid/gio-form-focus-first-invalid.module';
import { GioGoBackButtonModule } from '../../../../../shared/components/gio-go-back-button/gio-go-back-button.module';

@NgModule({
  declarations: [ApiPortalPlanEditComponent, PlanEditGeneralStepComponent, PlanEditSecureStepComponent, PlanEditRestrictionStepComponent],
  exports: [ApiPortalPlanEditComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatCardModule,
    MatIconModule,
    MatStepperModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatDividerModule,
    MatSnackBarModule,

    GioFormSlideToggleModule,
    GioFormTagsInputModule,
    GioIconsModule,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
    GioBannerModule,
    GioGoBackButtonModule,
  ],
})
export class ApiPortalPlanEditModule {}
