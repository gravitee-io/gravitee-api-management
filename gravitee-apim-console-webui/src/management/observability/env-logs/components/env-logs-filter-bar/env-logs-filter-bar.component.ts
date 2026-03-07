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

import { Component, computed, effect, input, output, signal } from '@angular/core';
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

import { EnvLogsMoreFiltersComponent } from './components/env-logs-more-filters/env-logs-more-filters.component';

import {
  createDefaultMoreFilters,
  DEFAULT_MORE_FILTERS,
  ENV_LOGS_DEFAULT_PERIOD,
  EnvLogsMoreFiltersForm,
} from '../../models/env-logs-more-filters.model';
import { SearchLogFilter, SearchLogsParam } from '../../../../../services-ngx/environment-logs.service';

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
export const MOCK_APIS = [
  { id: 'api-1', name: 'Weather API' },
  { id: 'api-2', name: 'Payment Gateway' },
  { id: 'api-3', name: 'User Service' },
];

// TODO: Replace with data from API when backend integration is implemented
export const MOCK_APPLICATIONS = [
  { id: 'app-1', name: 'Mobile App' },
  { id: 'app-2', name: 'Web Portal' },
  { id: 'app-3', name: 'Partner Integration' },
];

type EnvLogsQuickFiltersForm = {
  period: FormControl<{ label: string; value: string }>;
  apis: FormControl<string[] | null>;
  applications: FormControl<string[] | null>;
};

type FilterChip = {
  key: string;
  value: string | number;
  display: string;
};

type MoreFilterArrayKey = 'entrypoints' | 'methods' | 'plans';
const MORE_FILTER_ARRAY_KEYS: readonly MoreFilterArrayKey[] = ['entrypoints', 'methods', 'plans'];

function isMoreFilterArrayKey(key: string): key is MoreFilterArrayKey {
  return (MORE_FILTER_ARRAY_KEYS as readonly string[]).includes(key);
}

type MoreFilterStringKey = 'mcpMethod' | 'transactionId' | 'requestId' | 'uri';
const MORE_FILTER_STRING_KEYS: { key: MoreFilterStringKey; label: string }[] = [
  { key: 'mcpMethod', label: 'MCP Method' },
  { key: 'transactionId', label: 'Transaction ID' },
  { key: 'requestId', label: 'Request ID' },
  { key: 'uri', label: 'URI' },
];

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
    EnvLogsMoreFiltersComponent,
  ],
})
export class EnvLogsFilterBarComponent {
  loading = input(false);
  refresh = output<void>();
  filtersChanged = output<SearchLogsParam>();

  showMoreFilters = signal(false);
  moreFiltersValues = signal<EnvLogsMoreFiltersForm>(DEFAULT_MORE_FILTERS);

  readonly periods = ENV_LOGS_PERIODS;
  readonly apis = MOCK_APIS;
  readonly applications = MOCK_APPLICATIONS;
  readonly comparePeriod = (a: { value: string }, b: { value: string }): boolean => a?.value === b?.value;

  private readonly apisMap = new Map(this.apis.map(a => [a.id, a.name]));
  private readonly applicationsMap = new Map(this.applications.map(a => [a.id, a.name]));

  form = new FormGroup<EnvLogsQuickFiltersForm>({
    period: new FormControl(ENV_LOGS_DEFAULT_PERIOD, { nonNullable: true }),
    apis: new FormControl<string[] | null>(null),
    applications: new FormControl<string[] | null>(null),
  });

  private currentFilters = toSignal(this.form.valueChanges.pipe(distinctUntilChanged(isEqual)), { initialValue: this.form.value });

  private static readonly PERIOD_OFFSETS_MS: Record<string, number> = {
    '-5m': 5 * 60_000,
    '-30m': 30 * 60_000,
    '-1h': 60 * 60_000,
    '-3h': 3 * 60 * 60_000,
    '-6h': 6 * 60 * 60_000,
    '-12h': 12 * 60 * 60_000,
    '-1d': 24 * 60 * 60_000,
    '-3d': 3 * 24 * 60 * 60_000,
    '-7d': 7 * 24 * 60 * 60_000,
  };

  private resolveTimeRange(period: string, more: EnvLogsMoreFiltersForm): Pick<SearchLogsParam, 'timeRange'>['timeRange'] {
    if (more.from && more.to) {
      return { from: more.from.toISOString(), to: more.to.toISOString() };
    }
    if (period === '0') return undefined;
    const now = Date.now();
    const offset = EnvLogsFilterBarComponent.PERIOD_OFFSETS_MS[period] ?? 24 * 60 * 60_000;
    return { from: new Date(now - offset).toISOString(), to: new Date(now).toISOString() };
  }

  constructor() {
    effect(() => {
      this.filtersChanged.emit(this.filtersParam());
    });
  }

  filtersParam = computed<SearchLogsParam>(() => {
    const quick = this.currentFilters();
    const more = this.moreFiltersValues();
    const filters: SearchLogFilter[] = [];

    if (quick.apis?.length) filters.push({ name: 'API', operator: 'IN', value: quick.apis });
    if (quick.applications?.length) filters.push({ name: 'APPLICATION', operator: 'IN', value: quick.applications });
    if (more.entrypoints?.length) filters.push({ name: 'ENTRYPOINT', operator: 'IN', value: more.entrypoints });
    if (more.methods?.length) filters.push({ name: 'HTTP_METHOD', operator: 'IN', value: more.methods });
    if (more.plans?.length) filters.push({ name: 'PLAN', operator: 'IN', value: more.plans });
    if (more.statuses?.size > 0) filters.push({ name: 'HTTP_STATUS', operator: 'IN', value: Array.from(more.statuses) });
    if (more.mcpMethod) filters.push({ name: 'MCP_METHOD', operator: 'EQ', value: more.mcpMethod });
    if (more.transactionId) filters.push({ name: 'TRANSACTION_ID', operator: 'EQ', value: more.transactionId });
    if (more.requestId) filters.push({ name: 'REQUEST_ID', operator: 'EQ', value: more.requestId });
    if (more.uri) filters.push({ name: 'URI', operator: 'EQ', value: more.uri });
    if (more.errorKeys?.length) filters.push({ name: 'ERROR_KEY', operator: 'IN', value: more.errorKeys });
    if (more.responseTime != null) filters.push({ name: 'RESPONSE_TIME', operator: 'GTE', value: more.responseTime });

    return { filters, timeRange: this.resolveTimeRange(quick.period.value, more) };
  });

  filterChips = computed<FilterChip[]>(() => {
    const filters = this.currentFilters();
    const more = this.moreFiltersValues();
    const chips: FilterChip[] = [];

    const quickFilterConfigs: { key: string; values: string[] | undefined | null; nameMap?: Map<string, string> }[] = [
      { key: 'apis', values: filters.apis, nameMap: this.apisMap },
      { key: 'applications', values: filters.applications, nameMap: this.applicationsMap },
    ];

    for (const { key, values, nameMap } of quickFilterConfigs) {
      if (values && values.length > 0) {
        values.forEach(v => chips.push({ key, value: v, display: nameMap?.get(v) ?? v }));
      }
    }

    for (const key of MORE_FILTER_ARRAY_KEYS) {
      const values = more[key];
      if (values && values.length > 0) {
        values.forEach(v => chips.push({ key, value: v, display: v }));
      }
    }

    if (more.from) {
      chips.push({ key: 'from', value: 'from', display: more.from.format('YYYY-MM-DD HH:mm:ss') });
    }
    if (more.to) {
      chips.push({ key: 'to', value: 'to', display: more.to.format('YYYY-MM-DD HH:mm:ss') });
    }
    if (more.statuses.size > 0) {
      more.statuses.forEach((status: number) => {
        chips.push({ key: 'statuses', value: status, display: `${status}` });
      });
    }

    for (const { key, label } of MORE_FILTER_STRING_KEYS) {
      const value = more[key];
      if (value) {
        chips.push({ key, value, display: `${label}: ${value}` });
      }
    }

    if (more.responseTime != null) {
      chips.push({ key: 'responseTime', value: more.responseTime, display: `Response time: >${more.responseTime}ms` });
    }

    return chips;
  });

  isFiltering = computed(() => this.filterChips().length > 0);

  removeChip(chip: FilterChip) {
    // eslint-disable-next-line angular/typecheck-number -- angular.isNumber is an AngularJS 1.x API; typeof provides proper TypeScript type narrowing
    if (chip.key === 'statuses' && typeof chip.value === 'number') {
      const current = this.moreFiltersValues();
      const statuses = new Set(current.statuses);
      statuses.delete(chip.value);
      this.moreFiltersValues.set({ ...current, statuses });
      return;
    }

    if (isMoreFilterArrayKey(chip.key)) {
      const current = this.moreFiltersValues();
      const currentArray = current[chip.key] ?? [];
      const filtered = currentArray.filter(v => v !== chip.value);
      this.moreFiltersValues.set({ ...current, [chip.key]: filtered.length > 0 ? filtered : null });
      return;
    }

    const isScalarMoreFilter =
      chip.key === 'from' || chip.key === 'to' || chip.key === 'responseTime' || MORE_FILTER_STRING_KEYS.some(s => s.key === chip.key);

    if (isScalarMoreFilter) {
      const current = this.moreFiltersValues();
      this.moreFiltersValues.set({ ...current, [chip.key]: null });
      return;
    }

    const control = this.form.get(chip.key);
    if (!control) return;

    const currentValue: string[] | null = control.value;
    const filtered = (currentValue ?? []).filter(v => v !== chip.value);
    control.setValue(filtered.length > 0 ? filtered : null);
  }

  applyMoreFilters(values: EnvLogsMoreFiltersForm) {
    this.moreFiltersValues.set(values);
    this.showMoreFilters.set(false);
  }

  resetAllFilters() {
    this.form.reset({
      period: ENV_LOGS_DEFAULT_PERIOD,
      apis: null,
      applications: null,
    });
    this.moreFiltersValues.set(createDefaultMoreFilters());
  }
}
