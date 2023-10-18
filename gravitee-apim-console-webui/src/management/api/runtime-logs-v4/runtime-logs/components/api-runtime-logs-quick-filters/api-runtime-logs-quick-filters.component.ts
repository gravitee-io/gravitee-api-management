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

import { DEFAULT_PERIOD, LogFilters, LogFiltersForm, LogFiltersInitialValues, MultiFilter, PERIODS } from './models';
import { CacheEntry } from './components';

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
  @Output() quickFilterSelection = new EventEmitter<LogFilters>();

  readonly periods = PERIODS;
  defaultFilter: LogFilters = { period: DEFAULT_PERIOD, applications: undefined, plans: undefined };
  isFiltering = false;
  quickFiltersForm: FormGroup;
  currentFilter: LogFilters;
  applicationsCache: CacheEntry[];

  ngOnInit(): void {
    this.currentFilter = {
      ...this.defaultFilter,
      applications: this.initialValues.applications ?? this.defaultFilter?.applications,
      plans: this.initialValues.plans ?? this.defaultFilter?.plans,
    };

    this.quickFiltersForm = new FormGroup({
      period: new FormControl(DEFAULT_PERIOD),
      applications: new FormControl(this.initialValues.applications?.map((application) => application.value) ?? null),
      plans: new FormControl(this.initialValues.plans?.map((plan) => plan.value) ?? this.defaultFilter?.plans),
    });

    this.onValuesChanges();

    this.isFiltering = !isEqual(this.currentFilter, this.defaultFilter);
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  removeFilter(removedFilter: KeyValue<string, LogFilters>) {
    const defaultValue = this.defaultFilter[removedFilter.key];
    this.quickFiltersForm.get(removedFilter.key).patchValue(defaultValue);
    this.currentFilter[removedFilter.key] = defaultValue;
    this.isFiltering = !isEqual(this.currentFilter, this.defaultFilter);
  }

  private onValuesChanges() {
    this.quickFiltersForm.valueChanges.pipe(distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$)).subscribe((values) => {
      this.currentFilter = this.mapFormValues(values);
      this.isFiltering = !isEqual(this.currentFilter, this.defaultFilter);
      this.quickFilterSelection.emit(this.currentFilter);
    });
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
      : this.defaultFilter.plans;
  }

  private applicationsFromValues(ids: string[]): MultiFilter {
    return ids?.length > 0 ? ids.map((id) => this.applicationsCache.find((app) => app.value === id)) : this.defaultFilter.applications;
  }
}
