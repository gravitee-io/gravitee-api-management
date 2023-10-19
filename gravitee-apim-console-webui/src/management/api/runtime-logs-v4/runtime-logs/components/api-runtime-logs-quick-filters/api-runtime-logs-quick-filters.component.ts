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
import { FormControl, FormGroup } from '@angular/forms';
import { distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { isEqual } from 'lodash';
import { Subject } from 'rxjs';
import { KeyValue } from '@angular/common';

import { CacheEntry } from './components';

import { DEFAULT_FILTERS, DEFAULT_PERIOD, LogFilters, LogFiltersForm, LogFiltersInitialValues, MultiFilter, PERIODS } from '../../models';
import { QuickFiltersStoreService } from '../../services';
import { Plan } from '../../../../../../entities/management-api-v2';

@Component({
  selector: 'api-runtime-logs-quick-filters',
  template: require('./api-runtime-logs-quick-filters.component.html'),
  styles: [require('./api-runtime-logs-quick-filters.component.scss')],
})
export class ApiRuntimeLogsQuickFiltersComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() initialValues: LogFiltersInitialValues;
  @Input() plans: Plan[];
  @Output() refresh = new EventEmitter<void>();

  readonly periods = PERIODS;
  isFiltering = false;
  quickFiltersForm: FormGroup;
  currentFilter: LogFilters;
  applicationsCache: CacheEntry[];
  readonly defaultFilters = DEFAULT_FILTERS;

  private _loading = true;

  constructor(private readonly quickFilterStore: QuickFiltersStoreService) {}

  ngOnInit(): void {
    this.applicationsCache = this.initialValues.applications;
    this.quickFiltersForm = new FormGroup({
      period: new FormControl({ value: DEFAULT_PERIOD, disabled: true }),
      applications: new FormControl({
        value: this.initialValues.applications?.map((application) => application.value) ?? null,
        disabled: true,
      }),
      plans: new FormControl({ value: this.initialValues.plans?.map((plan) => plan.value) ?? DEFAULT_FILTERS.plans, disabled: true }),
    });
    this.onValuesChanges();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  removeFilter(removedFilter: KeyValue<string, LogFilters>) {
    const defaultValue = DEFAULT_FILTERS[removedFilter.key];
    this.quickFiltersForm.get(removedFilter.key).patchValue(defaultValue);
    this.currentFilter[removedFilter.key] = defaultValue;
    this.isFiltering = !isEqual(this.currentFilter, DEFAULT_FILTERS);
  }

  @Input()
  get loading() {
    return this._loading;
  }
  set loading(value: boolean) {
    this._loading = value;
    if (value) {
      this.quickFiltersForm?.disable();
    } else {
      this.quickFiltersForm?.enable();
    }
  }

  private onValuesChanges() {
    this.quickFiltersForm.valueChanges.pipe(distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$)).subscribe((values) => {
      this.quickFilterStore.next(this.mapFormValues(values));
      this.currentFilter = this.quickFilterStore.getFilters();
      this.isFiltering = !isEqual(this.currentFilter, DEFAULT_FILTERS);
    });
    this.quickFiltersForm.updateValueAndValidity();
  }

  private mapFormValues({ period, applications, plans }: LogFiltersForm) {
    return {
      period,
      plans: this.plansFromValues(plans),
      applications: this.applicationsFromValues(applications),
    };
  }

  private plansFromValues(ids: string[]): MultiFilter {
    return ids?.length > 0
      ? ids.map((id) => {
          const plan = this.plans.find((p) => p.id === id);
          return { label: plan.name, value: plan.id };
        })
      : DEFAULT_FILTERS.plans;
  }

  private applicationsFromValues(ids: string[]): MultiFilter {
    return ids?.length > 0 ? ids.map((id) => this.applicationsCache.find((app) => app.value === id)) : DEFAULT_FILTERS.applications;
  }
}
