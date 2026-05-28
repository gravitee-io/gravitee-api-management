/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';

import { FilterCondition } from '@gravitee/gravitee-dashboard';

import { DashboardFiltersStore } from './dashboard-filters.store';

function createCondition(overrides: Partial<FilterCondition> = {}): FilterCondition {
  return {
    field: 'API',
    label: 'API',
    operator: 'EQ',
    values: ['api-1'],
    ...overrides,
  };
}

describe('DashboardFiltersStore', () => {
  let store: DashboardFiltersStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DashboardFiltersStore,
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParams: {} },
            queryParams: { pipe: () => ({ subscribe: () => {} }) },
          },
        },
      ],
    });

    store = TestBed.inject(DashboardFiltersStore);
  });

  it('should_start_with_empty_conditions', () => {
    expect(store.conditions()).toEqual([]);
  });

  it('should_add_condition', () => {
    const condition = createCondition();
    store.add(condition);
    expect(store.conditions()).toEqual([condition]);
  });

  it('should_merge_conditions_with_same_field_and_operator', () => {
    store.add(createCondition({ values: ['api-1'], valueLabels: ['API 1'] }));
    store.add(createCondition({ values: ['api-2'], valueLabels: ['API 2'] }));
    expect(store.conditions()).toHaveLength(1);
    expect(store.conditions()[0].values).toEqual(['api-1', 'api-2']);
    expect(store.conditions()[0].operator).toBe('IN');
  });

  it('should_not_merge_conditions_with_different_fields', () => {
    store.add(createCondition({ field: 'API', values: ['api-1'] }));
    store.add(createCondition({ field: 'APPLICATION', values: ['app-1'] }));
    expect(store.conditions()).toHaveLength(2);
  });

  it('should_remove_condition_by_index', () => {
    store.add(createCondition({ field: 'API', values: ['api-1'] }));
    store.add(createCondition({ field: 'APPLICATION', values: ['app-1'] }));
    store.remove(0);
    expect(store.conditions()).toHaveLength(1);
    expect(store.conditions()[0].field).toBe('APPLICATION');
  });

  it('should_clear_all_conditions', () => {
    store.add(createCondition({ field: 'API', values: ['api-1'] }));
    store.add(createCondition({ field: 'APPLICATION', values: ['app-1'] }));
    store.clear();
    expect(store.conditions()).toEqual([]);
  });

  it('should_edit_condition_at_index', () => {
    store.add(createCondition({ values: ['api-1'] }));
    const updated = createCondition({ values: ['api-2'], valueLabels: ['Updated API'] });
    store.edit(0, updated);
    expect(store.conditions()[0].values).toEqual(['api-2']);
  });

  it('should_compute_request_filters_from_conditions', () => {
    store.add(createCondition({ field: 'API', operator: 'EQ', values: ['api-1'] }));
    const filters = store.requestFilters();
    expect(filters).toHaveLength(1);
    expect(filters[0].name).toBe('API');
    expect(filters[0].value).toBe('api-1');
  });

  it('should_not_remove_when_index_is_out_of_bounds', () => {
    store.add(createCondition());
    store.remove(5);
    expect(store.conditions()).toHaveLength(1);
  });

  it('should_not_edit_when_index_is_out_of_bounds', () => {
    store.add(createCondition({ values: ['api-1'] }));
    store.edit(5, createCondition({ values: ['api-2'] }));
    expect(store.conditions()[0].values).toEqual(['api-1']);
  });
});
