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

import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
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
import { ApiFilterService } from '../../../dashboards/ui/dashboard-viewer/filters/api-filter.service';
import { ApplicationFilterService } from '../../../dashboards/ui/dashboard-viewer/filters/application-filter.service';
import { GioSelectSearchComponent, SelectOption } from '../../../../../shared/components/gio-select-search/gio-select-search.component';

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

/** Emitted to the parent whenever any filter value changes. */
export type EnvLogsFilterValues = {
  period: string;
  apiIds: string[];
  applicationIds: string[];
  more: EnvLogsMoreFiltersForm;
};

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
    GioSelectSearchComponent,
    EnvLogsMoreFiltersComponent,
  ],
})
export class EnvLogsFilterBarComponent {
  // 1. Injections
  private readonly apiFilterService = inject(ApiFilterService);
  private readonly applicationFilterService = inject(ApplicationFilterService);

  // 2. Inputs / Outputs
  loading = input(false);
  refresh = output<void>();
  filtersChanged = output<EnvLogsFilterValues>();

  // 3. Public state
  showMoreFilters = signal(false);
  moreFiltersValues = signal<EnvLogsMoreFiltersForm>(DEFAULT_MORE_FILTERS);

  readonly periods = ENV_LOGS_PERIODS;
  readonly comparePeriod = (a: { value: string }, b: { value: string }): boolean => a?.value === b?.value;

  /** Async results loaders for gio-select-search dropdowns */
  readonly apiResultsLoader = this.apiFilterService.resultsLoader;
  readonly applicationResultsLoader = this.applicationFilterService.resultsLoader;

  /** Cache of selected option labels for chip display */
  private selectedApiLabels = new Map<string, string>();
  private selectedAppLabels = new Map<string, string>();

  form = new FormGroup<EnvLogsQuickFiltersForm>({
    period: new FormControl(ENV_LOGS_DEFAULT_PERIOD, { nonNullable: true }),
    apis: new FormControl<string[] | null>(null),
    applications: new FormControl<string[] | null>(null),
  });

  private currentFilters = toSignal(this.form.valueChanges.pipe(distinctUntilChanged(isEqual)), { initialValue: this.form.value });

  /** The IDs of the currently selected APIs — used by More Filters to load plans dynamically. */
  selectedApiIds = computed<string[]>(() => this.currentFilters().apis ?? []);

  filterChips = computed<FilterChip[]>(() => {
    const filters = this.currentFilters();
    const more = this.moreFiltersValues();
    const chips: FilterChip[] = [];

    // Period chip (only when not default)
    const period = filters.period;
    if (period && period.value !== '0') {
      chips.push({ key: 'period', value: period.value, display: period.label });
    }

    const quickFilterConfigs: { key: string; values: string[] | undefined | null; nameMap?: Map<string, string> }[] = [
      { key: 'apis', values: filters.apis, nameMap: this.selectedApiLabels },
      { key: 'applications', values: filters.applications, nameMap: this.selectedAppLabels },
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

  constructor() {
    // Emit filtersChanged whenever quick filters or more filters change
    effect(() => {
      const filters = this.currentFilters();
      const more = this.moreFiltersValues();
      this.filtersChanged.emit({
        period: filters.period?.value ?? '0',
        apiIds: filters.apis ?? [],
        applicationIds: filters.applications ?? [],
        more,
      });
    });
  }

  /** Called by gio-select-search when API options are loaded — caches labels for chip display. */
  onApiOptionsLoaded(options: SelectOption[]) {
    options.forEach(o => this.selectedApiLabels.set(o.value, o.label));
  }

  /** Called by gio-select-search when Application options are loaded — caches labels for chip display. */
  onAppOptionsLoaded(options: SelectOption[]) {
    options.forEach(o => this.selectedAppLabels.set(o.value, o.label));
  }

  removeChip(chip: FilterChip) {
    // Period chip — reset to default
    if (chip.key === 'period') {
      this.form.controls.period.setValue(ENV_LOGS_DEFAULT_PERIOD);
      return;
    }

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
