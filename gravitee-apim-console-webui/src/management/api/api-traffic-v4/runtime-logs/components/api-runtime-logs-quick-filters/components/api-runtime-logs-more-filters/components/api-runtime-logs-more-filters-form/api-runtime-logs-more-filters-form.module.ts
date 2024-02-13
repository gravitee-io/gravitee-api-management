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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatChipsModule } from '@angular/material/chips';

import { ApiRuntimeLogsMoreFiltersFormComponent } from './api-runtime-logs-more-filters-form.component';
import { ApplicationsFilterModule } from './components';

import { DATE_TIME_FORMATS } from '../../../../../../models';

@NgModule({
  declarations: [ApiRuntimeLogsMoreFiltersFormComponent],
  exports: [ApiRuntimeLogsMoreFiltersFormComponent],
  imports: [
    CommonModule,
    FormsModule,
    MatChipsModule,
    MatDatepickerModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    ReactiveFormsModule,

    ApplicationsFilterModule,
    GioIconsModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
  ],
  providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
})
export class ApiRuntimeLogsMoreFiltersFormModule {}
