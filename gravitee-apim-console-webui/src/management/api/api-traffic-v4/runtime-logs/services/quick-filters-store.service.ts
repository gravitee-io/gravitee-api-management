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
import { BehaviorSubject } from 'rxjs';
import { Injectable } from '@angular/core';
import moment from 'moment';

import DurationConstructor = moment.unitOfTime.DurationConstructor;

import { DEFAULT_FILTERS, LogFilters, SimpleFilter } from '../models';

@Injectable()
export class QuickFiltersStoreService {
  private _filters$: BehaviorSubject<LogFilters> = new BehaviorSubject<LogFilters>(DEFAULT_FILTERS);

  public filters$() {
    return this._filters$.asObservable();
  }

  public getFilters() {
    return this._filters$.getValue();
  }

  public next(values: LogFilters) {
    this._filters$.next(values);
  }

  public toLogFilterQueryParam(logFilters: LogFilters, page: number, perPage: number) {
    const { from, to } = this.preparePeriodFilter(logFilters.period);
    return {
      page,
      perPage,
      from: logFilters.from ? logFilters.from : from,
      to: logFilters.to ? logFilters.to : to,
      entrypointIds: logFilters.entrypoints?.length > 0 ? logFilters.entrypoints.join(',') : null,
      applicationIds: logFilters.applications?.length > 0 ? logFilters.applications?.map(app => app.value).join(',') : null,
      planIds: logFilters.plans?.length > 0 ? logFilters.plans?.map(plan => plan.value).join(',') : null,
      methods: logFilters.methods?.length > 0 ? logFilters.methods?.join(',') : null,
      mcpMethods: logFilters.mcpMethods?.length > 0 ? logFilters.mcpMethods?.join(',') : null,
      statuses: logFilters.statuses?.size > 0 ? Array.from(logFilters.statuses)?.join(',') : null,
    };
  }

  private preparePeriodFilter(period: SimpleFilter): { from: number; to: number } {
    if (period.value === '0') {
      return { from: null, to: null };
    }
    const now = moment();
    const operation = period.value.charAt(0);
    const timeUnit: DurationConstructor = period.value.charAt(period.value.length - 1) as DurationConstructor;
    const duration = Number(period.value.substring(1, period.value.length - 1));
    let from;
    if (operation === '-') {
      from = now.clone().subtract(duration, timeUnit);
    } else {
      from = now.clone().add(duration, timeUnit);
    }
    return { from: from.valueOf(), to: now.valueOf() };
  }
}
