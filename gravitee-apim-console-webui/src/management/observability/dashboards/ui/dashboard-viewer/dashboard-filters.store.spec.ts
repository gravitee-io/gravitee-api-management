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
import { FilterCondition } from '@gravitee/gravitee-dashboard';

import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';

import { DashboardFiltersStore } from './dashboard-filters.store';
import { FilterLabelResolver } from './filter-label.resolver';

describe('DashboardFiltersStore', () => {
  let store: DashboardFiltersStore;
  let queryParams$: Subject<Record<string, string>>;
  let routerNavigateSpy: jest.Mock;

  const cond = (field: string, label: string): FilterCondition => ({
    field,
    label,
    operator: 'EQ',
    values: ['a'],
  });

  beforeEach(() => {
    queryParams$ = new Subject();
    routerNavigateSpy = jest.fn().mockResolvedValue(true);

    TestBed.configureTestingModule({
      providers: [
        DashboardFiltersStore,
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: queryParams$.asObservable(),
            snapshot: { queryParams: {} },
          },
        },
        {
          provide: Router,
          useValue: { navigate: routerNavigateSpy },
        },
      ],
    });

    store = TestBed.inject(DashboardFiltersStore);
  });

  it('should add conditions and sync to router', () => {
    store.add(cond('API', 'API'));
    expect(store.conditions()).toEqual([cond('API', 'API')]);
    expect(routerNavigateSpy).toHaveBeenCalled();
  });

  it('should edit by index', () => {
    store.add(cond('API', 'API'));
    const updated = { ...cond('API', 'API'), operator: 'IN', values: ['x', 'y'] };
    store.edit(0, updated);
    expect(store.conditions()[0]).toEqual(updated);
  });

  it('should ignore edit out of range', () => {
    store.add(cond('API', 'API'));
    store.edit(99, cond('X', 'X'));
    expect(store.conditions().length).toBe(1);
  });

  it('should remove by index', () => {
    store.add(cond('A', 'A'));
    store.add(cond('B', 'B'));
    store.remove(0);
    expect(store.conditions().map(c => c.field)).toEqual(['B']);
  });

  it('should clear', () => {
    store.add(cond('A', 'A'));
    store.clear();
    expect(store.conditions()).toEqual([]);
  });

  it('should increment refreshToken', () => {
    expect(store.refreshToken()).toBe(0);
    store.refresh();
    expect(store.refreshToken()).toBe(1);
  });

  it('should expose timeRange as ISO strings', () => {
    const tr = store.timeRange();
    expect(tr.from).toMatch(/^\d{4}-\d{2}-\d{2}T/);
    expect(tr.to).toMatch(/^\d{4}-\d{2}-\d{2}T/);
  });

  it('should expose requestFilters mapped from conditions', () => {
    store.add(cond('API', 'API'));
    const rf = store.requestFilters();
    expect(rf.length).toBe(1);
    expect(rf[0].name).toBe('API');
    expect(rf[0].operator).toBe('EQ');
  });

  it('should default period to 5m', () => {
    expect(store.periodControl.value.period).toBe('5m');
  });

  it('should merge values when adding same field with EQ operator', () => {
    store.add({ field: 'API', label: 'API', operator: 'EQ', values: ['a'], valueLabels: ['Api A'] });
    store.add({ field: 'API', label: 'API', operator: 'EQ', values: ['b'], valueLabels: ['Api B'] });
    expect(store.conditions().length).toBe(1);
    expect(store.conditions()[0].operator).toBe('IN');
    expect(store.conditions()[0].values).toEqual(['a', 'b']);
    expect(store.conditions()[0].valueLabels).toEqual(['Api A', 'Api B']);
  });

  it('should merge values when adding EQ into existing IN', () => {
    store.add({ field: 'API', label: 'API', operator: 'IN', values: ['a', 'b'], valueLabels: ['A', 'B'] });
    store.add({ field: 'API', label: 'API', operator: 'EQ', values: ['c'], valueLabels: ['C'] });
    expect(store.conditions().length).toBe(1);
    expect(store.conditions()[0].operator).toBe('IN');
    expect(store.conditions()[0].values).toEqual(['a', 'b', 'c']);
    expect(store.conditions()[0].valueLabels).toEqual(['A', 'B', 'C']);
  });

  it('should ignore exact duplicate values', () => {
    store.add({ field: 'API', label: 'API', operator: 'EQ', values: ['a'] });
    const callsBefore = routerNavigateSpy.mock.calls.length;
    store.add({ field: 'API', label: 'API', operator: 'EQ', values: ['a'] });
    expect(store.conditions().length).toBe(1);
    expect(store.conditions()[0].values).toEqual(['a']);
    // syncToRouter is still called but conditions did not change
    expect(routerNavigateSpy.mock.calls.length).toBe(callsBefore + 1);
  });

  it('should merge NEQ / NOT_IN family separately', () => {
    store.add({ field: 'API', label: 'API', operator: 'NEQ', values: ['x'] });
    store.add({ field: 'API', label: 'API', operator: 'NEQ', values: ['y'] });
    expect(store.conditions().length).toBe(1);
    expect(store.conditions()[0].operator).toBe('NOT_IN');
    expect(store.conditions()[0].values).toEqual(['x', 'y']);
  });

  it('should not merge incompatible operators on same field', () => {
    store.add({ field: 'STATUS', label: 'Status', operator: 'GTE', values: ['400'] });
    store.add({ field: 'STATUS', label: 'Status', operator: 'LTE', values: ['499'] });
    expect(store.conditions().length).toBe(2);
  });

  it('should not merge EQ and NEQ on same field', () => {
    store.add({ field: 'API', label: 'API', operator: 'EQ', values: ['a'] });
    store.add({ field: 'API', label: 'API', operator: 'NEQ', values: ['b'] });
    expect(store.conditions().length).toBe(2);
  });

  it('should not merge different fields', () => {
    store.add({ field: 'API', label: 'API', operator: 'EQ', values: ['a'] });
    store.add({ field: 'APPLICATION', label: 'Application', operator: 'EQ', values: ['b'] });
    expect(store.conditions().length).toBe(2);
  });

  it('should hydrate from route snapshot with unified q containing filters and time_range', () => {
    const queryParamsWithFilters$ = new Subject<Record<string, string>>();
    const snapshot = {
      queryParams: {
        q: JSON.stringify({
          filter: [{ field: 'API', operator: 'EQ', value: 'id-1' }],
          time_range: { type: 'relative', period: '1h' },
        }),
        v: '1',
      },
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        DashboardFiltersStore,
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: queryParamsWithFilters$.asObservable(),
            snapshot,
          },
        },
        {
          provide: Router,
          useValue: { navigate: jest.fn().mockResolvedValue(true) },
        },
      ],
    });

    const hydratedStore = TestBed.inject(DashboardFiltersStore);
    expect(hydratedStore.periodControl.value.period).toBe('1h');
    expect(hydratedStore.conditions().length).toBe(1);
    expect(hydratedStore.conditions()[0].field).toBe('API');
  });

  it('should propagate hydrated period to timeRange and interval signals (no stale 5m)', () => {
    // Regression test: prior to the explicit `_periodValue` signal, hydrating
    // via `setValue(..., { emitEvent: false })` left widgets stuck on the
    // default 5m window even though the URL/dropdown said e.g. '1w'.
    const snapshot = {
      queryParams: {
        q: JSON.stringify({ time_range: { type: 'relative', period: '1w' } }),
        v: '1',
      },
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        DashboardFiltersStore,
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: new Subject<Record<string, string>>().asObservable(),
            snapshot,
          },
        },
        {
          provide: Router,
          useValue: { navigate: jest.fn().mockResolvedValue(true) },
        },
      ],
    });

    const hydratedStore = TestBed.inject(DashboardFiltersStore);
    const { from, to } = hydratedStore.timeRangeEpoch();
    const ONE_WEEK_MS = 1000 * 60 * 60 * 24 * 7;
    const range = to - from;
    // tolerate some scheduling jitter when the test runs
    expect(range).toBeGreaterThanOrEqual(ONE_WEEK_MS - 1000);
    expect(range).toBeLessThanOrEqual(ONE_WEEK_MS + 1000);
    // interval should be derived from the 1w period, not the 5m default
    const FIVE_MIN_INTERVAL = (1000 * 60 * 5) / 30;
    expect(hydratedStore.interval()).toBeGreaterThan(FIVE_MIN_INTERVAL);
  });

  it('should hydrate custom timerange from q', () => {
    const queryParamsWithFilters$ = new Subject<Record<string, string>>();
    const snapshot = {
      queryParams: {
        q: JSON.stringify({
          time_range: { type: 'absolute', from: 1000000, to: 2000000 },
        }),
        v: '1',
      },
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        DashboardFiltersStore,
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: queryParamsWithFilters$.asObservable(),
            snapshot,
          },
        },
        {
          provide: Router,
          useValue: { navigate: jest.fn().mockResolvedValue(true) },
        },
      ],
    });

    const hydratedStore = TestBed.inject(DashboardFiltersStore);
    expect(hydratedStore.periodControl.value.period).toBe('custom');
    expect(hydratedStore.periodControl.value.from!.valueOf()).toBe(1000000);
    expect(hydratedStore.periodControl.value.to!.valueOf()).toBe(2000000);
  });

  it('should show a loading value label until URL-hydrated ID labels are resolved', () => {
    const labelResolverMock = {
      resolveLabels: jest.fn().mockReturnValue(new Subject<FilterCondition[]>()),
    };
    const snapshot = {
      queryParams: {
        q: JSON.stringify({ filter: [{ field: 'API', operator: 'EQ', value: 'id-1' }] }),
        v: '1',
      },
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        DashboardFiltersStore,
        { provide: FilterLabelResolver, useValue: labelResolverMock },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: new Subject<Record<string, string>>().asObservable(),
            snapshot,
          },
        },
        {
          provide: Router,
          useValue: { navigate: jest.fn().mockResolvedValue(true) },
        },
      ],
    });

    const hydratedStore = TestBed.inject(DashboardFiltersStore);
    expect(hydratedStore.conditions()[0].valueLabels).toEqual(['Loading...']);
  });

  it('should merge resolved labels by field and value without overwriting user changes', () => {
    const resolvedLabels$ = new Subject<FilterCondition[]>();
    const labelResolverMock = {
      resolveLabels: jest.fn().mockReturnValue(resolvedLabels$),
    };
    const snapshot = {
      queryParams: {
        q: JSON.stringify({ filter: [{ field: 'API', operator: 'EQ', value: 'id-1' }] }),
        v: '1',
      },
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        DashboardFiltersStore,
        { provide: FilterLabelResolver, useValue: labelResolverMock },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: new Subject<Record<string, string>>().asObservable(),
            snapshot,
          },
        },
        {
          provide: Router,
          useValue: { navigate: jest.fn().mockResolvedValue(true) },
        },
      ],
    });

    const hydratedStore = TestBed.inject(DashboardFiltersStore);
    hydratedStore.add({ field: 'API', label: 'API', operator: 'EQ', values: ['id-2'], valueLabels: ['API Two'] });

    resolvedLabels$.next([{ field: 'API', label: 'API', operator: 'EQ', values: ['id-1'], valueLabels: ['Resolved API'] }]);

    expect(hydratedStore.conditions()).toEqual([
      { field: 'API', label: 'API', operator: 'IN', values: ['id-1', 'id-2'], valueLabels: ['Resolved API', 'API Two'] },
    ]);
  });
});
