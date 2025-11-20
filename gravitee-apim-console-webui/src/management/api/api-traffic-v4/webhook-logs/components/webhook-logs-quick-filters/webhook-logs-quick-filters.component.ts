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
import { isNumber } from 'angular';

import { CommonModule, DatePipe } from '@angular/common';
import { Component, effect, EventEmitter, Input, OnDestroy, OnInit, Output, input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule, MatChipInputEvent } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { isEqual } from 'lodash';

import { WebhookLogsApplicationsFilterComponent } from './components/webhook-logs-applications-filter/webhook-logs-applications-filter.component';

import { DEFAULT_PERIOD, MultiFilter, PERIODS, SimpleFilter } from '../../../runtime-logs/models';
import {
  DEFAULT_WEBHOOK_LOGS_FILTERS,
  WebhookLogsQuickFilters,
  WebhookLogsQuickFiltersInitialValues,
  WebhookMoreFiltersForm,
} from '../../models/webhook-logs.models';
import { WebhookLogsMoreFiltersComponent } from '../webhook-logs-more-filters/webhook-logs-more-filters.component';

type QuickFiltersFormValue = {
  searchTerm: string;
  statuses: number[];
  applications: string[];
  period: SimpleFilter;
};

const DEFAULT_FORM_VALUE: QuickFiltersFormValue = {
  searchTerm: '',
  statuses: [],
  applications: [],
  period: DEFAULT_PERIOD,
};

@Component({
  selector: 'webhook-logs-quick-filters',
  templateUrl: './webhook-logs-quick-filters.component.html',
  styleUrls: ['./webhook-logs-quick-filters.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    WebhookLogsApplicationsFilterComponent,
    WebhookLogsMoreFiltersComponent,
  ],
})
export class WebhookLogsQuickFiltersComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  // TODO: Backend Integration Required
  // - Verify that availableStatuses and availableCallbackUrls are provided from backend API response
  // - Consider fetching these dynamically from backend if not included in main logs API response
  // - Update filter options when new statuses or callback URLs are discovered in API responses
  // - Ensure filter validation matches backend API requirements

  @Input() availableStatuses: number[] = [];
  @Input() availableCallbackUrls: string[] = [];
  @Input() initialValues: WebhookLogsQuickFiltersInitialValues;
  loading = input<boolean>(false);

  @Output() filtersChanged = new EventEmitter<WebhookLogsQuickFilters>();
  @Output() refresh = new EventEmitter<void>();

  readonly periods = PERIODS;
  readonly defaultFilters = DEFAULT_WEBHOOK_LOGS_FILTERS;
  readonly comparePeriods = (a: SimpleFilter, b: SimpleFilter) => a?.value === b?.value;
  quickFiltersForm: FormGroup<{
    searchTerm: FormControl<string>;
    statuses: FormControl<number[]>;
    applications: FormControl<string[]>;
    period: FormControl<SimpleFilter>;
  }>;
  currentFilters: WebhookLogsQuickFilters;
  showMoreFilters = false;
  moreFiltersValues: WebhookMoreFiltersForm;
  isFiltering = false;
  private applicationsCache: MultiFilter = [];

  constructor() {
    effect(() => {
      const isLoading = this.loading();
      if (isLoading) {
        this.quickFiltersForm?.disable({ emitEvent: false });
      } else {
        this.quickFiltersForm?.enable({ emitEvent: false });
      }
    });
  }

  ngOnInit(): void {
    this.moreFiltersValues = {
      period: this.initialValues?.period ?? DEFAULT_PERIOD,
      from: this.initialValues?.from ? (isNumber(this.initialValues.from) ? null : this.initialValues.from) : null,
      to: this.initialValues?.to ? (isNumber(this.initialValues.to) ? null : this.initialValues.to) : null,
      callbackUrls: this.initialValues?.callbackUrls ?? [],
    };
    this.applicationsCache = this.initialValues?.applications ?? [];
    this.quickFiltersForm = new FormGroup({
      searchTerm: new FormControl<string>(this.initialValues?.searchTerm ?? '', { nonNullable: true }),
      statuses: new FormControl<number[]>(this.initialValues?.statuses ?? [], { nonNullable: true }),
      applications: new FormControl<string[]>(this.initialValues?.applications?.map((app) => app.value) ?? [], { nonNullable: true }),
      period: new FormControl<SimpleFilter>(this.initialValues?.period ?? DEFAULT_PERIOD, { nonNullable: true }),
    });
    this.onValuesChanges();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  resetAllFilters() {
    this.quickFiltersForm.reset(DEFAULT_FORM_VALUE, { emitEvent: false });
    this.applyMoreFilters({ period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] });
  }

  clearSearch(): void {
    this.quickFiltersForm.controls.searchTerm.setValue('', { emitEvent: true });
  }

  openMoreFilters(): void {
    this.showMoreFilters = true;
  }

  applyMoreFilters(values: WebhookMoreFiltersForm) {
    this.moreFiltersValues = values;
    if (this.currentFilters?.period !== values.period && values.period) {
      this.quickFiltersForm.controls.period.setValue(values.period, { emitEvent: false, onlySelf: true });
    }
    const formValues: QuickFiltersFormValue = this.quickFiltersForm.getRawValue();
    this.filtersChanged.emit(this.mapFormValues(formValues, values));
    this.currentFilters = this.mapFormValues(formValues, values);
    this.isFiltering = !isEqual(this.currentFilters, DEFAULT_WEBHOOK_LOGS_FILTERS);
    this.showMoreFilters = false;
  }

  addStatusFromInput(event: MatChipInputEvent): void {
    const inputValue = (event.value ?? '').trim();
    if (!inputValue) {
      event.chipInput?.clear();
      return;
    }
    const parsed = Number(inputValue);
    if (!Number.isNaN(parsed)) {
      this.addStatus(parsed);
    }
    event.chipInput?.clear();
  }

  removeStatus(status: number): void {
    const updated = this.selectedStatuses.filter((value) => value !== status);
    this.quickFiltersForm.controls.statuses.setValue(updated);
  }

  removeApplication(id: string): void {
    const updated = this.selectedApplications.filter((value) => value !== id);
    this.quickFiltersForm.controls.applications.setValue(updated);
  }

  removePeriod(): void {
    this.applyMoreFilters({ ...this.moreFiltersValues, period: DEFAULT_PERIOD });
  }

  removeDateRange(): void {
    this.applyMoreFilters({ ...this.moreFiltersValues, from: null, to: null });
  }

  removeCallbackUrl(url: string): void {
    const updated = (this.moreFiltersValues.callbackUrls ?? []).filter((value) => value !== url);
    this.applyMoreFilters({ ...this.moreFiltersValues, callbackUrls: updated });
  }

  get selectedStatuses(): number[] {
    return this.quickFiltersForm.controls.statuses.value ?? [];
  }

  get selectedApplications(): string[] {
    return this.quickFiltersForm.controls.applications.value ?? [];
  }

  onApplicationCache(cache: MultiFilter): void {
    if (!cache?.length) {
      return;
    }
    const merged: MultiFilter = [...this.applicationsCache];
    cache.forEach((item) => {
      if (!merged.find((existing) => existing.value === item.value)) {
        merged.push(item);
      }
    });
    this.applicationsCache = merged;
    const formValues: QuickFiltersFormValue = this.quickFiltersForm.getRawValue();
    this.filtersChanged.emit(this.mapFormValues(formValues, this.moreFiltersValues));
    this.currentFilters = this.mapFormValues(formValues, this.moreFiltersValues);
    this.isFiltering = !isEqual(this.currentFilters, DEFAULT_WEBHOOK_LOGS_FILTERS);
  }

  private onValuesChanges() {
    this.onQuickFiltersFormChanges();
    this.quickFiltersForm.updateValueAndValidity();
  }

  private onQuickFiltersFormChanges() {
    this.quickFiltersForm.valueChanges.pipe(distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$)).subscribe((values) => {
      const formValues: QuickFiltersFormValue = values;
      if (formValues.period && formValues.period === DEFAULT_PERIOD) {
        this.moreFiltersValues = { ...this.moreFiltersValues, period: formValues.period };
      } else {
        this.moreFiltersValues = { ...this.moreFiltersValues, period: formValues.period, from: null, to: null };
      }
      this.filtersChanged.emit(this.mapFormValues(formValues, this.moreFiltersValues));
      this.currentFilters = this.mapFormValues(formValues, this.moreFiltersValues);
      this.isFiltering = !isEqual(this.currentFilters, DEFAULT_WEBHOOK_LOGS_FILTERS);
    });
  }

  private mapFormValues(
    quickFilterFormValues: QuickFiltersFormValue,
    moreFiltersFormValues: WebhookMoreFiltersForm,
  ): WebhookLogsQuickFilters {
    return {
      ...this.mapMoreFiltersFormValues(moreFiltersFormValues),
      ...this.mapQuickFiltersFormValues(quickFilterFormValues),
    };
  }

  private mapQuickFiltersFormValues({ searchTerm, statuses, applications, period }: QuickFiltersFormValue) {
    const trimmedTerm = searchTerm?.trim();
    return {
      searchTerm: trimmedTerm?.length ? trimmedTerm : undefined,
      statuses: statuses?.length > 0 ? statuses : undefined,
      applications: this.applicationsFromValues(applications),
      period: period && period.value !== DEFAULT_PERIOD.value ? period : undefined,
    };
  }

  private mapMoreFiltersFormValues({ period, from, to, callbackUrls }: WebhookMoreFiltersForm) {
    return {
      period,
      from: from?.valueOf(),
      to: to?.valueOf(),
      callbackUrls: callbackUrls && callbackUrls.length > 0 ? callbackUrls : undefined,
    };
  }

  private applicationsFromValues(ids: string[]): MultiFilter | undefined {
    if (!ids?.length) {
      return undefined;
    }
    return ids.map((id) => this.applicationsCache.find((app) => app.value === id) ?? { value: id, label: id }).filter(Boolean);
  }

  private addStatus(status: number): void {
    if (this.selectedStatuses.includes(status)) {
      return;
    }
    this.quickFiltersForm.controls.statuses.setValue([...this.selectedStatuses, status]);
  }
}
