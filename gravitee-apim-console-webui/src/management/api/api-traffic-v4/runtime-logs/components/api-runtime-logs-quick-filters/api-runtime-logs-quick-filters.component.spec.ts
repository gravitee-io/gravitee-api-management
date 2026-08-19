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
import { KeyValue } from '@angular/common';
import moment from 'moment';

import { ApiRuntimeLogsQuickFiltersComponent } from './api-runtime-logs-quick-filters.component';

import { DEFAULT_PERIOD, LogFilters, LogFiltersInitialValues, NONE_PERIOD, PERIODS } from '../../models';
import { QuickFiltersStoreService } from '../../services';

describe('ApiRuntimeLogsQuickFiltersComponent', () => {
  const last30Minutes = PERIODS.find(period => period.value === '-30m');
  const from = moment(1_000);
  const to = moment(2_000);

  function createComponent() {
    const store = new QuickFiltersStoreService();
    const component = new ApiRuntimeLogsQuickFiltersComponent(store);
    component.plans = [];
    component.initialValues = {
      from: undefined,
      to: undefined,
      entrypoints: undefined,
      methods: undefined,
      mcpMethods: undefined,
      statuses: undefined,
    } as LogFiltersInitialValues;
    component.ngOnInit();
    return { component, store };
  }

  function chip(key: string): KeyValue<string, LogFilters> {
    return { key, value: null };
  }

  it('should restore Last 5 Minutes and clear dates when the period chip is removed', () => {
    const { component, store } = createComponent();
    component.applyMoreFilters({ period: last30Minutes, from, to, statuses: null });

    component.removeFilter(chip('period'));

    expect(store.getFilters()).toEqual(
      expect.objectContaining({
        period: DEFAULT_PERIOD,
        from: undefined,
        to: undefined,
      }),
    );
  });

  it('should restore Last 5 Minutes when the last custom date chip is removed', () => {
    const { component, store } = createComponent();
    component.applyMoreFilters({ period: NONE_PERIOD, from, to: null, statuses: null });

    component.removeFilter(chip('from'));

    expect(store.getFilters()).toEqual(
      expect.objectContaining({
        period: DEFAULT_PERIOD,
        from: undefined,
        to: undefined,
      }),
    );
  });

  it('should keep the remaining custom date when one bound chip is removed', () => {
    const { component, store } = createComponent();
    component.applyMoreFilters({ period: NONE_PERIOD, from, to, statuses: null });

    component.removeFilter(chip('from'));

    expect(store.getFilters()).toEqual(
      expect.objectContaining({
        period: NONE_PERIOD,
        from: undefined,
        to: to.valueOf(),
      }),
    );
  });
});
