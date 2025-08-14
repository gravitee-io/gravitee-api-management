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
import { Component, computed, DestroyRef, effect, input, OnInit, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import moment from 'moment';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { customTimeFrames, timeFrames } from '../../../../../../shared/utils/timeFrameRanges';
import { httpStatuses } from '../../../../../../shared/utils/httpStatuses';
import { GioSelectSearchComponent, SelectOption } from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { Plan } from '../../../../../../entities/management-api-v2';
import { GioTimeframeComponent, GioTimeframeValue } from '../../../../../../shared/components/gio-timeframe/gio-timeframe.component';

interface ApiAnalyticsProxyFilterBarForm {
  httpStatuses: FormControl<string[] | null>;
  timeframe: FormControl<GioTimeframeValue>;
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
    GioSelectSearchComponent,
    GioTimeframeComponent,
  ],
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

  form: FormGroup<ApiAnalyticsProxyFilterBarForm> = this.formBuilder.group({
    httpStatuses: this.formBuilder.control<string[] | null>(null),
    timeframe: this.formBuilder.control<GioTimeframeValue>({ period: '', from: null, to: null }, { nonNullable: true }),
    plans: this.formBuilder.control<string[] | null>(null),
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
    this.form.controls.timeframe.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((tf) => {
      const currentFilters = this.activeFilters();
      const updatedFilters: ApiAnalyticsProxyFilters = { ...currentFilters, period: tf.period };
      if (tf.period !== this.customPeriod) {
        updatedFilters.from = null;
        updatedFilters.to = null;
        this.filtersChange.emit(updatedFilters);
      }
    });

    this.form.controls.httpStatuses.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((httpStatuses) => {
      const currentFilters = this.activeFilters();
      const updatedFilters = { ...currentFilters, httpStatuses: httpStatuses };
      this.filtersChange.emit(updatedFilters);
    });

    this.form.controls.plans.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((plans) => {
      const currentFilters = this.activeFilters();
      const updatedFilters = { ...currentFilters, plans };
      this.filtersChange.emit(updatedFilters);
    });
  }

  applyCustomTimeframe() {
    const tf = this.form.controls.timeframe.value;
    const from = tf.from ? tf.from.valueOf() : null;
    const to = tf.to ? tf.to.valueOf() : null;

    const currentFilters = this.activeFilters();
    const updatedFilters: ApiAnalyticsProxyFilters = {
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

  private updateFormFromFilters(filters: ApiAnalyticsProxyFilters) {
    if (this.form) {
      const tf: GioTimeframeValue = {
        period: filters.period,
        from: filters.from ? moment(filters.from) : null,
        to: filters.to ? moment(filters.to) : null,
      };
      this.form.patchValue({
        timeframe: tf,
        plans: filters.plans,
        httpStatuses: filters.httpStatuses,
      });
    }
  }
}
