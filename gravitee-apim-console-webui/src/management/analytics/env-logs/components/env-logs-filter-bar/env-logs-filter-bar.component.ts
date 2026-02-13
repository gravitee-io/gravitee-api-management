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

import { Component, computed, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { toSignal } from '@angular/core/rxjs-interop';
import { distinctUntilChanged } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { HTTP_METHODS } from '../../../../../entities/management-api-v2';

export const ENV_LOGS_DEFAULT_PERIOD = { label: 'None', value: '0' };

export const ENV_LOGS_PERIODS = [
  ENV_LOGS_DEFAULT_PERIOD,
  { label: 'Last 5 Minutes', value: '-5m' },
  { label: 'Last 30 Minutes', value: '-30m' },
  { label: 'Last 1 Hour', value: '-1h' },
  { label: 'Last 3 Hours', value: '-3h' },
  { label: 'Last 6 Hours', value: '-6h' },
  { label: 'Last 12 Hours', value: '-12h' },
  { label: 'Last 1 Day', value: '-1d' },
  { label: 'Last 3 Days', value: '-3d' },
  { label: 'Last 7 Days', value: '-7d' },
];

// TODO: Replace with data from API when backend integration is implemented
export const MOCK_ENTRYPOINTS = [
  { id: 'http-proxy', name: 'HTTP Proxy' },
  { id: 'http-get', name: 'HTTP GET' },
  { id: 'http-post', name: 'HTTP POST' },
  { id: 'sse', name: 'SSE' },
  { id: 'websocket', name: 'WebSocket' },
  { id: 'webhook', name: 'Webhook' },
];

// TODO: Replace with data from API when backend integration is implemented
export const MOCK_PLANS = [
  { id: 'plan-1', name: 'Free Plan' },
  { id: 'plan-2', name: 'Gold Plan' },
  { id: 'plan-3', name: 'Enterprise Plan' },
];

type EnvLogsQuickFiltersForm = {
  period: FormControl<{ label: string; value: string }>;
  entrypoints: FormControl<string[] | null>;
  methods: FormControl<string[] | null>;
  plans: FormControl<string[] | null>;
};

type FilterChip = {
  key: string;
  value: string;
  display: string;
};

@Component({
  selector: 'env-logs-filter-bar',
  templateUrl: './env-logs-filter-bar.component.html',
  styleUrl: './env-logs-filter-bar.component.scss',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatTooltipModule,
    GioIconsModule,
  ],
})
export class EnvLogsFilterBarComponent {
  loading = input(false);
  refresh = output<void>();

  readonly periods = ENV_LOGS_PERIODS;
  readonly httpMethods = HTTP_METHODS;
  readonly entrypoints = MOCK_ENTRYPOINTS;
  readonly plans = MOCK_PLANS;

  private readonly entrypointsMap = new Map(this.entrypoints.map((e) => [e.id, e.name]));
  private readonly plansMap = new Map(this.plans.map((p) => [p.id, p.name]));

  form = new FormGroup<EnvLogsQuickFiltersForm>({
    period: new FormControl(ENV_LOGS_DEFAULT_PERIOD, { nonNullable: true }),
    entrypoints: new FormControl<string[] | null>(null),
    methods: new FormControl<string[] | null>(null),
    plans: new FormControl<string[] | null>(null),
  });

  private currentFilters = toSignal(this.form.valueChanges.pipe(distinctUntilChanged(isEqual)), { initialValue: this.form.value });

  filterChips = computed<FilterChip[]>(() => {
    const filters = this.currentFilters();
    const chips: FilterChip[] = [];

    if (filters.entrypoints?.length > 0) {
      filters.entrypoints.forEach((id: string) => {
        const name = this.entrypointsMap.get(id) ?? id;
        chips.push({ key: 'entrypoints', value: id, display: name });
      });
    }

    if (filters.methods?.length > 0) {
      filters.methods.forEach((method: string) => {
        chips.push({ key: 'methods', value: method, display: method });
      });
    }

    if (filters.plans?.length > 0) {
      filters.plans.forEach((id: string) => {
        const name = this.plansMap.get(id) ?? id;
        chips.push({ key: 'plans', value: id, display: name });
      });
    }

    return chips;
  });

  isFiltering = computed(() => this.filterChips().length > 0);

  comparePeriod = (a: { value: string }, b: { value: string }): boolean => a?.value === b?.value;

  removeChip(chip: FilterChip) {
    const control = this.form.get(chip.key);
    if (!control) return;

    const currentValue = control.value as string[] | null;
    const filtered = (currentValue ?? []).filter((v) => v !== chip.value);
    control.setValue(filtered.length > 0 ? filtered : null);
  }

  resetAllFilters() {
    this.form.reset({
      period: ENV_LOGS_DEFAULT_PERIOD,
      entrypoints: null,
      methods: null,
      plans: null,
    });
  }
}
