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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { GioConfirmDialogModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';

import { ApiCreationV4Component } from './steps/api-creation-v4.component';
import { ApiCreationV4Step1Component } from './steps/api-creation-v4-step-1.component';
import { ApiCreationStepperComponent } from './api-creation-stepper/api-creation-stepper.component';
import { ApiCreationStepComponent } from './api-creation-stepper/api-creation-step/api-creation-step.component';

@NgModule({
  imports: [
    CommonModule,
    BrowserAnimationsModule,
    MatCardModule,
    MatButtonModule,
    GioConfirmDialogModule,
    GioIconsModule,
    MatInputModule,
    ReactiveFormsModule,
  ],
  declarations: [ApiCreationV4Component, ApiCreationStepperComponent, ApiCreationStepComponent, ApiCreationV4Step1Component],
  exports: [ApiCreationV4Component],
})
export class ApiCreationV4Module {}
