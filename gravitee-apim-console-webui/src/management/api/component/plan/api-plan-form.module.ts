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
import { GioBannerModule, GioFormTagsInputModule, GioIconsModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatAutocompleteModule } from '@angular/material/autocomplete';

import { PlanEditGeneralStepComponent } from './1-general-step/plan-edit-general-step.component';
import { PlanEditSecureStepComponent } from './2-secure-step/plan-edit-secure-step.component';
import { PlanEditRestrictionStepComponent } from './3-restriction-step/plan-edit-restriction-step.component';
import { ApiPlanFormComponent } from './api-plan-form.component';

import { GioSafePipeModule } from '../../../../shared/utils/safe.pipe.module';
import { GioFormJsonSchemaExtendedModule } from '../../../../shared/components/form-json-schema-extended/form-json-schema-extended.module';

@NgModule({
  declarations: [ApiPlanFormComponent, PlanEditGeneralStepComponent, PlanEditSecureStepComponent, PlanEditRestrictionStepComponent],
  exports: [ApiPlanFormComponent],
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
    MatAutocompleteModule,

    GioFormJsonSchemaExtendedModule,
    GioFormSlideToggleModule,
    GioFormTagsInputModule,
    GioIconsModule,
    GioBannerModule,
    GioSafePipeModule,
  ],
})
export class ApiPlanFormModule {}
