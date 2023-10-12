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

import { DEFAULT_PERIOD, LogFilters, LogFiltersForm, LogFiltersInitialValues, PERIODS } from './models';
import { CacheEntry } from './components';

@Component({
  selector: 'api-runtime-logs-quick-filters',
  template: require('./api-runtime-logs-quick-filters.component.html'),
  styles: [require('./api-runtime-logs-quick-filters.component.scss')],
})
export class ApiRuntimeLogsQuickFiltersComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() initialValues: LogFiltersInitialValues;
  @Output() quickFilterSelection = new EventEmitter<LogFilters>();
  defaultFilter: LogFilters = {
    period: DEFAULT_PERIOD,
    applications: undefined,
  };
  readonly periods = PERIODS;
  isFiltering = false;
  quickFiltersForm: FormGroup;
  currentFilter: LogFilters;
  applicationsCache: CacheEntry[];

  ngOnInit(): void {
    this.currentFilter = {
      ...this.defaultFilter,
      applications: this.initialValues.applications ?? this.defaultFilter?.applications,
    };

    this.quickFiltersForm = new FormGroup({
      period: new FormControl(DEFAULT_PERIOD),
      applications: new FormControl(
        this.initialValues.applications?.map((application) => application.value) ?? this.defaultFilter?.applications,
      ),
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

  private mapFormValues({ period, applications }: LogFiltersForm) {
    return {
      period,
      applications:
        applications?.length > 0
          ? this.applicationsCache?.filter((app) => applications.includes(app.value))
          : this.defaultFilter.applications,
    };
  }
}
