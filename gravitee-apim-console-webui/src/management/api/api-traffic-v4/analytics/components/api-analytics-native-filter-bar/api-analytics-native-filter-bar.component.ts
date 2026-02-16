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
import { Component, computed, DestroyRef, effect, input, InputSignal, OnInit, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import moment, { Moment } from 'moment';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';

import { customTimeFrames, timeFrames } from '../../../../../../shared/utils/timeFrameRanges';
import { GioSelectSearchComponent, SelectOption } from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { BaseApplication, Plan } from '../../../../../../entities/management-api-v2';
import { GioTimeframeComponent } from '../../../../../../shared/components/gio-timeframe/gio-timeframe.component';

interface ApiAnalyticsNativeFilterBarForm {
  timeframe: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>;
  plans: FormControl<string[] | null>;
  applications: FormControl<string[] | null>;
}

export interface ApiAnalyticsNativeFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  plans: string[] | null;
  applications: string[] | null;
}

interface FilterChip {
  key: string;
  value: string;
  display: string;
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
  applications: InputSignal<BaseApplication[]> = input<BaseApplication[]>();
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];

  public planOptions = computed<SelectOption[]>(() => {
    const plans = this.plans() || [];
    return plans.map(plan => ({ value: plan.id, label: plan.name }));
  });

  public applicationOptions = computed<SelectOption[]>(() => {
    const applications = this.applications() || [];
    return applications.map(app => ({ value: app.id, label: app.name }));
  });

  public currentFilterChips = computed<FilterChip[]>(() => {
    const filters = this.activeFilters();
    const chips: FilterChip[] = [];

    if (filters?.plans?.length) {
      const plans = this.plans();
      filters.plans.forEach(planId => {
        const plan = plans?.find(p => p.id === planId);
        const display = plan ? plan.name : planId;
        chips.push({
          key: 'plans',
          value: planId,
          display: display,
        });
      });
    }

    if (filters?.applications?.length) {
      const applications = this.applications();
      for (const appId of filters.applications) {
        const application = applications?.find(p => p.id === appId);
        const display = application ? application.name : appId;
        chips.push({
          key: 'applications',
          value: appId,
          display: display,
        });
      }
    }

    return chips;
  });

  public isFiltering = computed(() => this.currentFilterChips().length > 0);

  form: FormGroup<ApiAnalyticsNativeFilterBarForm> = this.formBuilder.group({
    timeframe: this.formBuilder.control<{ period: string; from: Moment | null; to: Moment | null } | null>(null),
    plans: [null],
    applications: [null],
  });
  customPeriod: string = 'custom';

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly destroyRef: DestroyRef,
  ) {
    // Set up effect to update form when activeFilters changes
    effect(() => {
      const filters = this.activeFilters();
      this.updateFormFromFilters(filters);
    });
  }

  ngOnInit() {
    this.form.controls.timeframe.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(tf => {
      if (tf?.period && tf.period !== this.customPeriod) {
        this.emitFilters({ period: tf.period, from: null, to: null });
      }
    });

    this.form.controls.plans.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(plans => {
      this.emitFilters({ plans });
    });

    this.form.controls.applications.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(applications => {
      this.emitFilters({ applications });
    });
  }

  applyCustomTimeframe() {
    const tf = this.form.controls.timeframe.value;
    const from = tf?.from?.valueOf() ?? null;
    const to = tf?.to?.valueOf() ?? null;

    const currentFilters = this.activeFilters();
    const updatedFilters = {
      ...currentFilters,
      from,
      to,
      period: this.customPeriod,
    };

    this.filtersChange.emit(updatedFilters);
  }

  refreshFilters() {
    this.refresh.emit();
  }

  private removeValueFromFilter(currentList: string[] | null, value: string, formControl: FormControl<string[] | null>): void {
    const filteredList = (currentList || []).filter(item => item !== value);
    formControl.setValue(filteredList.length > 0 ? filteredList : null);
  }

  removeFilter(key: string, value: string) {
    if (key === 'plans') {
      this.removeValueFromFilter(this.form.controls.plans.value, value, this.form.controls.plans);
    }

    if (key === 'applications') {
      this.removeValueFromFilter(this.form.controls.applications.value, value, this.form.controls.applications);
    }
  }

  resetAllFilters() {
    this.emitFilters({ plans: null, applications: null });
  }

  private updateFormFromFilters(filters: ApiAnalyticsNativeFilters) {
    if (this.form) {
      this.form.patchValue({
        timeframe: {
          period: filters.period,
          from: filters.from ? moment(filters.from) : null,
          to: filters.to ? moment(filters.to) : null,
        },
        plans: filters.plans,
        applications: filters.applications,
      });
    }
  }

  private emitFilters(partial: Partial<ApiAnalyticsNativeFilters>) {
    this.filtersChange.emit({
      ...this.activeFilters(),
      ...partial,
    });
  }
}
