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

import { Component, computed, input, output, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, KeyValue } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { debounceTime, distinctUntilChanged, startWith } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { LogFiltersInitialValues } from '../../../runtime-logs/models';
import { PERIODS } from '../../../models';
import { WebhookLogsMoreFiltersModule } from '../webhook-logs-more-filters/webhook-logs-more-filters.module';
import { WebhookFilters, WebhookMoreFiltersForm } from '../../models';
import { httpStatuses } from '../../../../../../shared/utils/httpStatuses';

@Component({
  selector: 'webhook-logs-quick-filters',
  templateUrl: './webhook-logs-quick-filters.component.html',
  styleUrls: ['./webhook-logs-quick-filters.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    WebhookLogsMoreFiltersModule,
  ],
})
export class WebhookLogsQuickFiltersComponent implements OnInit {
  loading = input.required<boolean>();
  initialValues = input.required<LogFiltersInitialValues>();
  applications = input<{ id: string; name: string }[]>([]);
  callbackUrls = input<string[]>([]);

  refresh = output<void>();
  filtersChanged = output<WebhookFilters>();

  readonly periods = PERIODS;
  readonly defaultFilters: WebhookFilters = {
    searchTerm: '',
    status: [],
    application: [],
    timeframe: '0',
    callbackUrls: [],
  };
  readonly statusOptions = httpStatuses.filter((status) => ['200', '400', '401', '403', '404', '500', '502', '503'].includes(status.value));

  quickFiltersForm: FormGroup;
  currentFilters = signal<WebhookFilters>({});
  isFiltering = false;
  showMoreFilters = false;
  moreFiltersValues: WebhookMoreFiltersForm = {};
  moreFiltersInvalid = false;

  protected readonly Array = Array;

  filterEntries = computed(() => {
    const filters = this.currentFilters();
    return Object.entries(filters)
      .filter(([key, value]) => {
        const defaultValue = this.defaultFilters[key as keyof WebhookFilters];
        return value !== undefined && value !== defaultValue && (!Array.isArray(value) || value.length > 0);
      })
      .map(([key, value]) => ({ key, value: value as string | string[] }));
  });

  ngOnInit(): void {
    this.quickFiltersForm = new FormGroup({
      searchTerm: new FormControl(''),
      status: new FormControl([]),
      application: new FormControl([]),
      timeframe: new FormControl('0'),
      callbackUrls: new FormControl([]),
    });

    const initialValues = this.initialValues();
    if (initialValues) {
      this.quickFiltersForm.patchValue({
        searchTerm: (initialValues as any).searchTerm ?? '',
        status: initialValues.statuses ? Array.from(initialValues.statuses).map(String) : [],
        application: initialValues.applications ?? [],
        timeframe: (initialValues as any).timeframe ?? '0',
        callbackUrls: (initialValues as any).callbackUrls ?? [],
      });
    }

    this.quickFiltersForm.valueChanges
      .pipe(startWith(this.quickFiltersForm.value), debounceTime(300), distinctUntilChanged(isEqual))
      .subscribe((filters: WebhookFilters) => {
        this.currentFilters.set(filters);
        this.isFiltering = !isEqual(filters, this.defaultFilters);
        this.filtersChanged.emit(filters);
      });
  }

  removeFilter(removedFilter: KeyValue<string, string | string[]>): void {
    const defaultValue = this.defaultFilters[removedFilter.key];
    this.quickFiltersForm.get(removedFilter.key).patchValue(defaultValue);
  }

  resetAllFilters(): void {
    this.quickFiltersForm.reset(this.defaultFilters);
  }

  getFilterDisplayValue(key: string, value: string | string[]): string {
    if (key === 'status' && Array.isArray(value)) {
      return value.length > 0 ? value.join(', ') : '';
    }
    if (key === 'application' && Array.isArray(value)) {
      return value.length > 0 ? value.length + ' selected' : '';
    }
    if (key === 'timeframe') {
      const period = this.periods.find((p) => p.value === value);
      return period ? period.label : Array.isArray(value) ? value.join(', ') : value?.toString() || '';
    }
    if (key === 'callbackUrls' && Array.isArray(value)) {
      return value.length > 0 ? value.length + ' selected' : '';
    }
    return value?.toString() || '';
  }

  onMoreFiltersValuesChange(values: WebhookMoreFiltersForm): void {
    this.moreFiltersValues = values;
  }

  onMoreFiltersInvalidChange(isInvalid: boolean): void {
    this.moreFiltersInvalid = isInvalid;
  }

  resetMoreFilters(): void {
    this.moreFiltersValues = {
      period: { label: 'None', value: '0' },
      from: null,
      to: null,
      callbackUrls: [],
    };
    this.applyMoreFilters();
  }

  applyMoreFilters(): void {
    this.filtersChanged.emit({
      ...this.quickFiltersForm.value,
      ...this.moreFiltersValues,
    });
    this.showMoreFilters = false;
  }
}
