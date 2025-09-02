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
import { Component, input, output, effect, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';

import { GioSelectSearchComponent } from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { GioTimeframeComponent } from '../../../../../../shared/components/gio-timeframe/gio-timeframe.component';
import { Plan } from '../../../../../../entities/management-api-v2';
import { CommonFiltersComponent } from '../common/common-filters.component';
import { NativeFilters, NativeFilterForm } from '../common/common-filters.types';

export type ApiAnalyticsNativeFilters = NativeFilters;

@Component({
  selector: 'api-analytics-native-filter-bar',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    MatChipsModule,
    MatTooltipModule,
    GioSelectSearchComponent,
    GioTimeframeComponent,
  ],
  templateUrl: './api-analytics-native-filter-bar.component.html',
  styleUrl: './api-analytics-native-filter-bar.component.scss',
})
export class ApiAnalyticsNativeFilterBarComponent extends CommonFiltersComponent<NativeFilters> implements OnInit {
  activeFilters = input.required<NativeFilters>();
  filtersChange = output<NativeFilters>();
  refresh = output<void>();
  plans = input<Plan[]>([]);

  form: FormGroup<NativeFilterForm> = this.formBuilder.group({
    ...this.createCommonFormControls(),
  } as NativeFilterForm);

  constructor() {
    super();
    effect(() => {
      const filters = this.activeFilters();
      this.updateFormFromFilters(filters);
    });
  }

  ngOnInit() {
    this.setupCommonFormSubscriptions();
  }
}
