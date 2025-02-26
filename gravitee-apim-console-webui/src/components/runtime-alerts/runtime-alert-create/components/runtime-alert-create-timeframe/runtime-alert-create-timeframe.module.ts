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
import { OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { CdkAccordionModule } from '@angular/cdk/accordion';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { GioFormSlideToggleModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from "@angular/material/card";

import { RuntimeAlertCreateTimeframeComponent } from './runtime-alert-create-timeframe.component';
import { MatButton } from "@angular/material/button";

@NgModule({
  declarations: [RuntimeAlertCreateTimeframeComponent],
  exports: [RuntimeAlertCreateTimeframeComponent],
  imports: [
    CommonModule,
    CdkAccordionModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    GioIconsModule,
    GioFormSlideToggleModule,
    MatCardModule,
    MatButton
  ]
})
export class RuntimeAlertCreateTimeframeModule {}
