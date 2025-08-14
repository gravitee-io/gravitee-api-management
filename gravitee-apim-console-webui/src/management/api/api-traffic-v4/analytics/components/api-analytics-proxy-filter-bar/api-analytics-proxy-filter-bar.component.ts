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
import { Component, DestroyRef, OnInit, input, effect, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import moment, { Moment } from 'moment';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';

import { customTimeFrames, DATE_TIME_FORMATS, timeFrames } from '../../../../../../shared/utils/timeFrameRanges';
import { httpStatuses } from '../../../../../../shared/utils/httpStatuses';
import { GioSelectSearchComponent, SelectOption } from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { Plan } from '../../../../../../entities/management-api-v2';

interface ApiAnalyticsProxyFilterBarForm {
  httpStatuses: FormControl<string[] | null>;
  period: FormControl<string>;
  from: FormControl<Moment | null>;
  to: FormControl<Moment | null>;
  plans: FormControl<string[] | null>;
}

export interface ApiAnalyticsProxyFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  httpStatuses: string[] | null;
  plans: string[] | null;
  hosts: string[] | null;
  applications: string[] | null;
}

interface FilterChip {
  key: string;
  value: string;
  display: string;
}

@Component({
  selector: 'api-analytics-proxy-filter-bar',
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
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    GioSelectSearchComponent,
    MatChipsModule,
    MatTooltipModule,
  ],
  providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
  templateUrl: './api-analytics-proxy-filter-bar.component.html',
  styleUrl: './api-analytics-proxy-filter-bar.component.scss',
})
export class ApiAnalyticsProxyFilterBarComponent implements OnInit {
  activeFilters = input.required<ApiAnalyticsProxyFilters>();
  filtersChange = output<ApiAnalyticsProxyFilters>();
  refresh = output<void>();

  protected readonly httpStatuses = [...httpStatuses];
  plans = input<Plan[]>([]);
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  public planOptions = computed<SelectOption[]>(() => {
    const plans = this.plans() || [];
    return plans.map((plan) => ({ value: plan.id, label: plan.name }));
  });

  public currentFilterChips = computed<FilterChip[]>(() => {
    const filters = this.activeFilters();
    const chips: FilterChip[] = [];

    if (filters?.httpStatuses?.length) {
      filters.httpStatuses.forEach((status) => {
        const statusOption = this.httpStatuses?.find((opt) => opt.value === status);
        chips.push({
          key: 'httpStatuses',
          value: status,
          display: statusOption?.label || status,
        });
      });
    }

    if (filters?.plans?.length) {
      const plans = this.plans();
      filters.plans.forEach((planId) => {
        const plan = plans?.find((p) => p.id === planId);
        const display = plan ? plan.name : planId;
        chips.push({
          key: 'plans',
          value: planId,
          display: display,
        });
      });
    }

    return chips;
  });

  public isFiltering = computed(() => this.currentFilterChips().length > 0);

  form: FormGroup<ApiAnalyticsProxyFilterBarForm> = this.formBuilder.group(
    {
      httpStatuses: [null],
      period: [''],
      from: [null],
      to: [null],
      plans: [null],
    },
    { validators: this.dateRangeValidator },
  );
  minDate: Moment;
  nowDate: Moment = moment().add(1, 'd');
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
    this.form.controls.period.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((period) => {
      if (period !== this.customPeriod) {
        this.emitFilters({ period });
      }
    });

    this.form.controls.from.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((from) => {
      this.minDate = from;
      this.form.updateValueAndValidity();
    });

    this.form.controls.to.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.updateValueAndValidity();
    });

    this.form.controls.httpStatuses.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((httpStatuses) => {
      this.emitFilters({ httpStatuses: httpStatuses });
    });

    this.form.controls.plans.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((plans) => {
      this.emitFilters({ plans });
    });
  }

  applyCustomTimeframe() {
    const from = this.form.controls.from.value.valueOf();
    const to = this.form.controls.to.value.valueOf();

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
    const filteredList = (currentList || []).filter((item) => item !== value);
    formControl.setValue(filteredList.length > 0 ? filteredList : null);
  }

  removeFilter(key: string, value: string) {
    if (key === 'httpStatuses') {
      this.removeValueFromFilter(this.form.controls.httpStatuses.value, value, this.form.controls.httpStatuses);
    } else if (key === 'plans') {
      this.removeValueFromFilter(this.form.controls.plans.value, value, this.form.controls.plans);
    }
  }

  resetAllFilters() {
    this.emitFilters({ httpStatuses: null, plans: null });
  }

  private dateRangeValidator(group: FormGroup): { [key: string]: any } | null {
    const from = group.get('from')?.value;
    const to = group.get('to')?.value;

    if (from && to && from.isAfter(to)) {
      return { dateRange: true };
    }

    return null;
  }

  private updateFormFromFilters(filters: ApiAnalyticsProxyFilters) {
    if (this.form) {
      this.form.patchValue({
        period: filters.period,
        from: filters.from ? moment(filters.from) : null,
        to: filters.to ? moment(filters.to) : null,
        plans: filters.plans,
        httpStatuses: filters.httpStatuses,
      });
    }
  }

  private emitFilters(partial: Partial<ApiAnalyticsProxyFilters>) {
    this.filtersChange.emit({
      ...this.activeFilters(),
      ...partial,
    });
  }
}
