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

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { isEqual } from 'lodash';
import { Subject } from 'rxjs';
import { KeyValue } from '@angular/common';

import {
  DEFAULT_FILTERS,
  DEFAULT_PERIOD,
  LogFilters,
  LogFiltersForm,
  LogFiltersInitialValues,
  MoreFiltersForm,
  MultiFilter,
  PERIODS,
} from '../../models';
import { QuickFiltersStoreService } from '../../services';
import { Plan } from '../../../../../../entities/management-api-v2';

@Component({
  selector: 'api-runtime-logs-quick-filters',
  templateUrl: './api-runtime-logs-quick-filters.component.html',
  styleUrls: ['./api-runtime-logs-quick-filters.component.scss'],
  standalone: false,
})
export class ApiRuntimeLogsQuickFiltersComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private _loading = true;

  @Input() initialValues: LogFiltersInitialValues;
  @Input() plans: Plan[];
  @Input() entrypoints: { id: string; name: string }[];
  @Output() refresh = new EventEmitter<void>();
  @Output() resetFilters = new EventEmitter<void>();

  readonly periods = PERIODS;
  readonly defaultFilters = DEFAULT_FILTERS;
  readonly httpMethods = ['CONNECT', 'DELETE', 'GET', 'HEAD', 'OPTIONS', 'PATCH', 'POST', 'PUT', 'TRACE'];
  isFiltering = false;
  quickFiltersForm: UntypedFormGroup;
  currentFilters: LogFilters;
  showMoreFilters = false;
  moreFiltersValues: MoreFiltersForm;

  constructor(private readonly quickFilterStore: QuickFiltersStoreService) {}

  ngOnInit(): void {
    this.moreFiltersValues = {
      period: DEFAULT_PERIOD,
      from: this.initialValues.from,
      to: this.initialValues.to,
      statuses: this.initialValues.statuses,
      applications: this.initialValues.applications,
    };
    this.quickFiltersForm = new UntypedFormGroup({
      period: new UntypedFormControl({ value: DEFAULT_PERIOD, disabled: true }),
      entrypoints: new UntypedFormControl({ value: this.initialValues.entrypoints, disabled: true }),
      plans: new UntypedFormControl({
        value: this.initialValues.plans?.map((plan) => plan.value) ?? DEFAULT_FILTERS.plans,
        disabled: true,
      }),
      methods: new UntypedFormControl({ value: this.initialValues.methods, disabled: true }),
    });
    this.onValuesChanges();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  removeFilter(removedFilter: KeyValue<string, LogFilters>) {
    const defaultValue = DEFAULT_FILTERS[removedFilter.key];
    if (this.moreFiltersValues[removedFilter.key]) {
      this.applyMoreFilters({ ...this.moreFiltersValues, [removedFilter.key]: defaultValue });
    } else {
      this.quickFiltersForm.get(removedFilter.key).patchValue(defaultValue);
    }
  }

  applyMoreFilters(values: MoreFiltersForm) {
    this.moreFiltersValues = values;
    if (this.currentFilters?.period !== values.period) {
      this.quickFiltersForm.get('period').setValue(values.period, { emitEvent: false, onlySelf: true });
    }
    this.quickFilterStore.next(this.mapFormValues(this.quickFiltersForm.getRawValue(), values));
    this.currentFilters = this.quickFilterStore.getFilters();
    this.isFiltering = !isEqual(this.currentFilters, DEFAULT_FILTERS);
    this.showMoreFilters = false;
  }

  resetAllFilters() {
    this.quickFiltersForm.reset(DEFAULT_FILTERS, { emitEvent: false });
    this.applyMoreFilters({ period: DEFAULT_FILTERS.period, from: null, to: null, statuses: null });
  }

  @Input()
  get loading() {
    return this._loading;
  }
  set loading(value: boolean) {
    this._loading = value;
    if (value) {
      this.quickFiltersForm?.disable({ emitEvent: false });
    } else {
      this.quickFiltersForm?.enable({ emitEvent: false });
    }
  }

  private onValuesChanges() {
    this.onQuickFiltersFormChanges();
    this.quickFiltersForm.updateValueAndValidity();
  }

  private onQuickFiltersFormChanges() {
    this.quickFiltersForm.valueChanges.pipe(distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$)).subscribe((values) => {
      if (values.period && values.period === DEFAULT_PERIOD) {
        this.moreFiltersValues = { ...this.moreFiltersValues, period: values.period };
      } else {
        this.moreFiltersValues = { ...this.moreFiltersValues, period: values.period, from: null, to: null };
      }
      this.quickFilterStore.next(this.mapFormValues(values, this.moreFiltersValues));
      this.currentFilters = this.quickFilterStore.getFilters();
      this.isFiltering = !isEqual(this.currentFilters, DEFAULT_FILTERS);
    });
  }

  private mapFormValues(quickFilterFormValues: LogFiltersForm, moreFilersFormValues: MoreFiltersForm): LogFilters {
    return {
      ...this.mapMoreFiltersFormValues(moreFilersFormValues),
      ...this.mapQuickFiltersFormValues(quickFilterFormValues),
    };
  }

  private mapQuickFiltersFormValues({ period, entrypoints, plans, methods }: LogFiltersForm) {
    return {
      period,
      entrypoints,
      plans: this.plansFromValues(plans),
      methods: methods?.length > 0 ? methods : undefined,
    };
  }

  private mapMoreFiltersFormValues({ period, from, to, statuses, applications }: MoreFiltersForm) {
    return { period, from: from?.valueOf(), to: to?.valueOf(), applications, statuses };
  }

  private plansFromValues(ids: string[]): MultiFilter {
    return ids?.length > 0
      ? ids.map((id) => {
          const plan = this.plans.find((p) => p.id === id);
          return { label: plan.name, value: plan.id };
        })
      : DEFAULT_FILTERS.plans;
  }
}
