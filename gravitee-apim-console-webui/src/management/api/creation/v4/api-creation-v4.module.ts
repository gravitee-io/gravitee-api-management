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
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioConfirmDialogModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatListModule } from '@angular/material/list';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ApiCreationV4Component } from './api-creation-v4.component';
import { ApiCreationV4Step1Component } from './steps/step-1/api-creation-v4-step-1.component';
import { ApiCreationStepperComponent } from './components/api-creation-stepper/api-creation-stepper.component';
import { ApiCreationStepComponent } from './components/api-creation-stepper/api-creation-step/api-creation-step.component';
import { ApiCreationV4Step2Component } from './steps/step-2/api-creation-v4-step-2.component';
import { ApiCreationV4StepWipComponent } from './steps/step-wip/api-creation-v4-step-wip.component';
import { ApiCreationV4Step6Component } from './steps/step-6/api-creation-v4-step-6.component';

import { GioSelectionListOptionModule } from '../../../../shared/components/gio-selection-list-option/gio-selection-list-option.module';
@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatCardModule,
    MatButtonModule,
    MatInputModule,
    MatCheckboxModule,
    MatListModule,
    MatDialogModule,

    GioConfirmDialogModule,
    GioIconsModule,
    GioSelectionListOptionModule,
    GioConfirmDialogModule,
    MatTooltipModule,
  ],
  declarations: [
    ApiCreationV4Component,
    ApiCreationStepperComponent,
    ApiCreationStepComponent,
    ApiCreationV4StepWipComponent,
    ApiCreationV4Step1Component,
    ApiCreationV4Step2Component,
    ApiCreationV4Step6Component,
  ],
  exports: [ApiCreationV4Component],
})
export class ApiCreationV4Module {}
