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
import { Component, DestroyRef, OnInit, input, effect, output, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { Moment } from 'moment';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';

import { customTimeFrames, timeFrames } from '../../../../../../shared/utils/timeFrameRanges';
import { GioSelectSearchComponent, SelectOption } from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { Plan } from '../../../../../../entities/management-api-v2';
import { GioTimeframeComponent } from '../../../../../../shared/components/gio-timeframe/gio-timeframe.component';
import { 
  ApiAnalyticsBaseFilterBarService, 
  BaseFilterBarFilters, 
  FilterChip 
} from '../api-analytics-base-filter-bar/api-analytics-base-filter-bar.service';

interface ApiAnalyticsNativeFilterBarForm {
  timeframe: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>;
  plans: FormControl<string[] | null>;
}

export interface ApiAnalyticsNativeFilters extends BaseFilterBarFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  plans: string[] | null;
}

@Component({
  selector: 'api-analytics-native-filter-bar',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    MatInputModule,
    GioSelectSearchComponent,
    MatChipsModule,
    MatTooltipModule,
    GioTimeframeComponent,
  ],
  templateUrl: './api-analytics-native-filter-bar.component.html',
  styleUrl: './api-analytics-native-filter-bar.component.scss',
})
export class ApiAnalyticsNativeFilterBarComponent implements OnInit {
  activeFilters = input.required<ApiAnalyticsNativeFilters>();
  filtersChange = output<ApiAnalyticsNativeFilters>();
  refresh = output<void>();

  plans = input<Plan[]>([]);
  
  // Inject services
  private readonly baseService = inject(ApiAnalyticsBaseFilterBarService);
  
  // Use base service properties
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  protected readonly customPeriod = 'custom';
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  
  // Define supported filters for native component: plans + timeframe only
  private readonly supportedFilters: string[] = ['period', 'from', 'to', 'plans'];

  public planOptions = computed<SelectOption[]>(() => 
    this.baseService.generatePlanOptions(this.plans())
  );

  public currentFilterChips = computed<FilterChip[]>(() => {
    const filters = this.activeFilters();
    return this.baseService.generatePlanFilterChips(filters?.plans, this.plans());
  });

  public isFiltering = computed(() => 
    this.baseService.isFiltering(this.currentFilterChips())
  );

  form: FormGroup<ApiAnalyticsNativeFilterBarForm> = this.formBuilder.group({
    ...this.baseService.createBaseForm(this.formBuilder),
  });

  constructor() {
    // Set up effect to update form when activeFilters changes
    effect(() => {
      const filters = this.activeFilters();
      this.updateFormFromFilters(filters);
    });
  }

  ngOnInit() {
    this.form.controls.timeframe.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((tf) => {
      this.baseService.handleTimeframeChange(tf, (partial) => this.emitFilters(partial));
    });

    this.form.controls.plans.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((plans) => {
      this.baseService.handlePlansChange(plans, (partial) => this.emitFilters(partial));
    });
  }

  applyCustomTimeframe() {
    this.baseService.applyCustomTimeframe(
      this.form.controls.timeframe.value,
      this.activeFilters(),
      (filters) => this.filtersChange.emit(filters)
    );
  }

  refreshFilters() {
    this.refresh.emit();
  }

  removeFilter(key: string, value: string) {
    this.baseService.handleRemoveFilter(key, value, this.form, this.supportedFilters);
  }

  resetAllFilters() {
    const resetFilters = this.baseService.getResetFiltersObject<ApiAnalyticsNativeFilters>(this.supportedFilters);
    this.emitFilters(resetFilters);
  }

  private updateFormFromFilters(filters: ApiAnalyticsNativeFilters) {
    this.baseService.updateFormFromFilters(filters, this.form, this.supportedFilters);
  }

  private emitFilters(partial: Partial<ApiAnalyticsNativeFilters>) {
    this.baseService.emitFilters(
      this.activeFilters(),
      partial,
      (filters) => this.filtersChange.emit(filters)
    );
  }
}
