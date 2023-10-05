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

import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { isEqual } from 'lodash';
import { Subject } from 'rxjs';
import { KeyValue } from '@angular/common';

export type PeriodFilter = {
  label: string;
  value: string;
};

export type LogFilter = {
  period?: PeriodFilter;
};

@Component({
  selector: 'api-runtime-logs-quick-filters',
  template: require('./api-runtime-logs-quick-filters.component.html'),
  styles: [require('./api-runtime-logs-quick-filters.component.scss')],
})
export class ApiRuntimeLogsQuickFiltersComponent implements OnInit, OnDestroy {
  private readonly defaultPeriod: PeriodFilter = { label: 'None', value: '0' };
  private defaultFilter: LogFilter = {
    period: this.defaultPeriod,
  };

  public readonly periods: PeriodFilter[] = [
    this.defaultPeriod,
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

  public isFiltering = false;

  public quickFiltersForm = new FormGroup({
    period: new FormControl(this.defaultPeriod),
  });

  public currentFilter: LogFilter = {};

  @Output()
  quickFilterSelection = new EventEmitter<LogFilter>();

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public ngOnInit(): void {
    this.quickFiltersForm.valueChanges.pipe(distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$)).subscribe(({ period }) => {
      this.currentFilter.period = period;
      this.isFiltering = !isEqual(this.currentFilter, this.defaultFilter);
      this.quickFilterSelection.emit(this.currentFilter);
    });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  removeFilter(removedFilter: KeyValue<string, LogFilter>) {
    let defaultValue;
    switch (removedFilter.key) {
      case 'period':
        defaultValue = this.defaultPeriod;
    }
    this.quickFiltersForm.get(removedFilter.key).setValue(defaultValue);
  }
}
