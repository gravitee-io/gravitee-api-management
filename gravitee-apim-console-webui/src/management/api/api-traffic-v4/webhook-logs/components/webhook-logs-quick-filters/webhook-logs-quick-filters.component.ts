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
import { Component, DestroyRef, effect, EventEmitter, inject, Input, OnInit, Output, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { distinctUntilChanged } from 'rxjs/operators';
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
import { httpStatuses } from '../../../../../../shared/utils/httpStatuses';

type QuickFiltersFormValue = {
  statuses: string[];
  applications: string[];
  period: SimpleFilter;
};

const DEFAULT_FORM_VALUE: QuickFiltersFormValue = {
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
    MatSelectModule,
    WebhookLogsApplicationsFilterComponent,
    WebhookLogsMoreFiltersComponent,
  ],
})
export class WebhookLogsQuickFiltersComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  @Input() availableCallbackUrls: string[] = [];
  @Input() initialValues: WebhookLogsQuickFiltersInitialValues;
  loading = input<boolean>(false);

  @Output() filtersChanged = new EventEmitter<WebhookLogsQuickFilters>();
  @Output() refresh = new EventEmitter<void>();

  readonly periods = PERIODS;
  readonly defaultFilters = DEFAULT_WEBHOOK_LOGS_FILTERS;
  readonly comparePeriods = (a: SimpleFilter, b: SimpleFilter) => a?.value === b?.value;
  readonly httpStatusChoices = [...httpStatuses];
  quickFiltersForm!: FormGroup<{
    statuses: FormControl<string[]>;
    applications: FormControl<string[]>;
    period: FormControl<SimpleFilter>;
  }>;
  currentFilters: WebhookLogsQuickFilters = DEFAULT_WEBHOOK_LOGS_FILTERS;
  showMoreFilters = false;
  moreFiltersValues!: WebhookMoreFiltersForm;
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
    this.moreFiltersValues = this.initializeMoreFiltersValues();
    this.applicationsCache = this.initialValues?.applications ?? [];
    this.quickFiltersForm = this.initializeQuickFiltersForm();
    this.onValuesChanges();
  }

  private initializeMoreFiltersValues(): WebhookMoreFiltersForm {
    const initialFrom = this.initialValues?.from;
    const initialTo = this.initialValues?.to;
    const fromValue = initialFrom != null && !isNumber(initialFrom) ? initialFrom : null;
    const toValue = initialTo != null && !isNumber(initialTo) ? initialTo : null;

    return {
      period: this.initialValues?.period ?? DEFAULT_PERIOD,
      from: fromValue,
      to: toValue,
      callbackUrls: this.initialValues?.callbackUrls ?? [],
    };
  }

  private initializeQuickFiltersForm(): FormGroup<{
    statuses: FormControl<string[]>;
    applications: FormControl<string[]>;
    period: FormControl<SimpleFilter>;
  }> {
    const initialStatuses = this.initialValues?.statuses?.map(String) ?? [];
    const initialApplications = this.initialValues?.applications?.map(app => app.value) ?? [];

    return new FormGroup({
      statuses: new FormControl<string[]>(initialStatuses, { nonNullable: true }),
      applications: new FormControl<string[]>(initialApplications, { nonNullable: true }),
      period: new FormControl<SimpleFilter>(this.initialValues?.period ?? DEFAULT_PERIOD, { nonNullable: true }),
    });
  }

  resetAllFilters() {
    this.quickFiltersForm.reset(DEFAULT_FORM_VALUE, { emitEvent: false });
    this.applyMoreFilters({ period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] });
  }

  openMoreFilters(): void {
    this.showMoreFilters = true;
  }

  applyMoreFilters(values: WebhookMoreFiltersForm) {
    this.moreFiltersValues = values;
    if (this.currentFilters?.period !== values.period && values.period) {
      this.quickFiltersForm.controls.period.setValue(values.period, { emitEvent: false, onlySelf: true });
    }
    this.updateFilters(values);
    this.showMoreFilters = false;
  }

  removeStatus(status: number): void {
    const statusString = String(status);
    const currentStatuses = this.quickFiltersForm.controls.statuses.value ?? [];
    const updated = currentStatuses.filter(value => value !== statusString);
    this.quickFiltersForm.controls.statuses.setValue(updated.length > 0 ? updated : []);
  }

  removeApplication(id: string): void {
    const updated = this.selectedApplications.filter(value => value !== id);
    this.quickFiltersForm.controls.applications.setValue(updated);
  }

  removePeriod(): void {
    this.applyMoreFilters({ ...this.moreFiltersValues, period: DEFAULT_PERIOD });
  }

  removeDateRange(): void {
    this.applyMoreFilters({ ...this.moreFiltersValues, from: null, to: null });
  }

  removeCallbackUrl(url: string): void {
    const updated = (this.moreFiltersValues.callbackUrls ?? []).filter(value => value !== url);
    this.applyMoreFilters({ ...this.moreFiltersValues, callbackUrls: updated });
  }

  get selectedApplications(): string[] {
    return this.quickFiltersForm.controls.applications.value ?? [];
  }

  getStatusLabel(status: number): string {
    const statusString = String(status);
    const statusOption = this.httpStatusChoices.find(s => s.value === statusString);
    return statusOption?.label || statusString;
  }

  onApplicationCache(cache: MultiFilter): void {
    if (!cache?.length) {
      return;
    }
    const merged: MultiFilter = [...this.applicationsCache];
    cache.forEach(item => {
      if (!merged.find(existing => existing.value === item.value)) {
        merged.push(item);
      }
    });
    this.applicationsCache = merged;
    this.updateFilters();
  }

  private onValuesChanges() {
    this.onQuickFiltersFormChanges();
    this.quickFiltersForm.updateValueAndValidity();
  }

  private onQuickFiltersFormChanges() {
    this.quickFiltersForm.valueChanges.pipe(distinctUntilChanged(isEqual), takeUntilDestroyed(this.destroyRef)).subscribe(values => {
      const formValues: QuickFiltersFormValue = values;
      if (formValues.period && formValues.period === DEFAULT_PERIOD) {
        this.moreFiltersValues = { ...this.moreFiltersValues, period: formValues.period };
      } else {
        this.moreFiltersValues = { ...this.moreFiltersValues, period: formValues.period, from: null, to: null };
      }
      this.updateFilters(this.moreFiltersValues);
    });
  }

  private updateFilters(moreFilters: WebhookMoreFiltersForm = this.moreFiltersValues): void {
    const formValues: QuickFiltersFormValue = this.quickFiltersForm.getRawValue();
    const filters = this.mapFormValues(formValues, moreFilters);

    this.currentFilters = filters;
    this.isFiltering = !isEqual(filters, DEFAULT_WEBHOOK_LOGS_FILTERS);
    this.filtersChanged.emit(filters);
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

  private mapQuickFiltersFormValues({ statuses, applications, period }: QuickFiltersFormValue) {
    return {
      statuses: statuses?.length > 0 ? statuses.map(status => Number(status)).filter(num => !Number.isNaN(num)) : undefined,
      applications: this.applicationsFromValues(applications),
      period: period && period.value !== DEFAULT_PERIOD.value ? period : undefined,
    };
  }

  private mapMoreFiltersFormValues({ from, to, callbackUrls }: WebhookMoreFiltersForm) {
    return {
      from: from?.valueOf(),
      to: to?.valueOf(),
      callbackUrls: callbackUrls && callbackUrls.length > 0 ? callbackUrls : undefined,
    };
  }

  private applicationsFromValues(ids: string[]): MultiFilter | undefined {
    if (!ids?.length) {
      return undefined;
    }
    return ids.map(id => this.applicationsCache.find(app => app.value === id) ?? { value: id, label: id });
  }
}
