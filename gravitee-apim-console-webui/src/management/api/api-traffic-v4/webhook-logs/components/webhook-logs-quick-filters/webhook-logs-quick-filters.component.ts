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
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule, MatChipInputEvent } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { distinctUntilChanged } from 'rxjs/operators';

import { ApplicationsFilterModule } from '../../../runtime-logs/components/api-runtime-logs-quick-filters/components/api-runtime-logs-more-filters/components/api-runtime-logs-more-filters-form/components/applications-filter';
import { DEFAULT_PERIOD, MultiFilter, PERIODS, SimpleFilter } from '../../../runtime-logs/models';
import { DEFAULT_WEBHOOK_LOGS_FILTERS, WebhookLogsQuickFilters, WebhookLogsQuickFiltersInitialValues } from '../../models';

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
    ReactiveFormsModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    ApplicationsFilterModule,
  ],
})
export class WebhookLogsQuickFiltersComponent implements OnInit, OnChanges {
  private _loading = false;
  readonly periods = PERIODS;
  readonly defaultPeriod = DEFAULT_PERIOD;
  readonly comparePeriods = (a: SimpleFilter, b: SimpleFilter) => a?.value === b?.value;

  @Input() availableStatuses: number[] = [];
  @Input() initialValues: WebhookLogsQuickFiltersInitialValues = DEFAULT_WEBHOOK_LOGS_FILTERS;
  @Input()
  get loading(): boolean {
    return this._loading;
  }
  set loading(value: boolean) {
    this._loading = value;
    if (value) {
      this.filtersForm?.disable({ emitEvent: false });
    } else {
      this.filtersForm?.enable({ emitEvent: false });
    }
  }

  @Output() filtersChanged = new EventEmitter<WebhookLogsQuickFilters>();
  @Output() refresh = new EventEmitter<void>();
  @Output() moreFilters = new EventEmitter<void>();

  filtersForm = new UntypedFormGroup({
    searchTerm: new UntypedFormControl(DEFAULT_FORM_VALUE.searchTerm),
    statuses: new UntypedFormControl(DEFAULT_FORM_VALUE.statuses),
    applications: new UntypedFormControl(DEFAULT_FORM_VALUE.applications),
    period: new UntypedFormControl(DEFAULT_FORM_VALUE.period),
  });
  currentFilters: WebhookLogsQuickFilters = DEFAULT_WEBHOOK_LOGS_FILTERS;
  private applicationsCache: MultiFilter = [];

  ngOnInit(): void {
    this.patchInitialValues();
    this.filtersForm.valueChanges.pipe(distinctUntilChanged((a, b) => this.areFormValuesEqual(a, b))).subscribe((values) => {
      this.emitFilters(values as QuickFiltersFormValue);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.initialValues && !changes.initialValues.firstChange) {
      this.patchInitialValues();
    }
  }

  resetFilters(): void {
    this.filtersForm.reset(DEFAULT_FORM_VALUE, { emitEvent: false });
    this.applicationsCache = [];
    this.emitFilters(DEFAULT_FORM_VALUE);
  }

  clearSearch(): void {
    this.filtersForm.get('searchTerm')?.setValue('', { emitEvent: true });
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
    this.filtersForm.get('statuses')?.setValue(updated);
  }

  removeApplication(id: string): void {
    const updated = this.selectedApplications.filter((value) => value !== id);
    this.filtersForm.get('applications')?.setValue(updated);
  }

  openMoreFilters(): void {
    this.moreFilters.emit();
  }

  removePeriod(): void {
    this.filtersForm.get('period')?.setValue(this.defaultPeriod);
  }

  hasActiveFilters(): boolean {
    return (
      Boolean(this.currentFilters.searchTerm) ||
      !!this.currentFilters.statuses?.length ||
      !!this.currentFilters.applications?.length ||
      !!this.currentFilters.period
    );
  }

  get selectedStatuses(): number[] {
    return this.filtersForm.get('statuses')?.value ?? [];
  }

  get selectedApplications(): string[] {
    return this.filtersForm.get('applications')?.value ?? [];
  }

  private patchInitialValues(): void {
    this.applicationsCache = this.initialValues?.applications ?? [];
    const initialFormValue: QuickFiltersFormValue = {
      searchTerm: this.initialValues?.searchTerm ?? DEFAULT_FORM_VALUE.searchTerm,
      statuses: this.initialValues?.statuses ?? DEFAULT_FORM_VALUE.statuses,
      applications: this.initialValues?.applications?.map((app) => app.value) ?? DEFAULT_FORM_VALUE.applications,
      period: this.initialValues?.period ?? this.defaultPeriod,
    };
    this.filtersForm.setValue(initialFormValue, { emitEvent: false });
    this.emitFilters(initialFormValue);
  }

  private emitFilters(formValue: QuickFiltersFormValue): void {
    const normalized = this.normalizeFormValue(formValue);
    this.currentFilters = normalized;
    this.filtersChanged.emit(normalized);
  }

  private normalizeFormValue({ searchTerm, statuses, applications, period }: QuickFiltersFormValue): WebhookLogsQuickFilters {
    const trimmedTerm = searchTerm?.trim();
    const normalizedPeriod = this.normalizePeriod(period);
    const normalized: WebhookLogsQuickFilters = {
      searchTerm: trimmedTerm?.length ? trimmedTerm : undefined,
      statuses: statuses?.length ? [...statuses] : undefined,
      applications: this.applicationsFromValues(applications),
    };
    if (normalizedPeriod) {
      normalized.period = normalizedPeriod;
    }
    return normalized;
  }

  private areFormValuesEqual(a: QuickFiltersFormValue, b: QuickFiltersFormValue): boolean {
    return (
      a.searchTerm === b.searchTerm &&
      this.areArraysEqual(a.statuses ?? [], b.statuses ?? []) &&
      this.areArraysEqual(a.applications ?? [], b.applications ?? []) &&
      this.arePeriodsEqual(a.period, b.period)
    );
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
    this.emitFilters(this.filtersForm.getRawValue() as QuickFiltersFormValue);
  }

  private addStatus(status: number): void {
    if (this.selectedStatuses.includes(status)) {
      return;
    }
    this.filtersForm.get('statuses')?.setValue([...this.selectedStatuses, status]);
  }

  private applicationsFromValues(ids: string[]): MultiFilter | undefined {
    if (!ids?.length) {
      return undefined;
    }
    return ids.map((id) => this.applicationsCache.find((app) => app.value === id) ?? { value: id, label: id }).filter(Boolean);
  }

  private areArraysEqual<T>(a: T[], b: T[]): boolean {
    if (a.length !== b.length) {
      return false;
    }
    const setB = new Set(b);
    return a.every((value) => setB.has(value));
  }

  private normalizePeriod(period?: SimpleFilter): SimpleFilter | undefined {
    if (!period) {
      return undefined;
    }
    const match = this.periods.find((available) => available.value === period.value);
    return match && match.value !== this.defaultPeriod.value ? match : undefined;
  }

  private arePeriodsEqual(a?: SimpleFilter, b?: SimpleFilter): boolean {
    if (!a && !b) {
      return true;
    }
    if (!a || !b) {
      return false;
    }
    return a.value === b.value;
  }
}
