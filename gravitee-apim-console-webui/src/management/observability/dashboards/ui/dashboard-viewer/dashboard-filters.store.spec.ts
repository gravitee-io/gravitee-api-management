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
import { DashboardFiltersStore } from './dashboard-filters.store';

describe('DashboardFiltersStore', () => {
  const cond = (field: string, label: string) => ({
    field,
    label,
    operator: 'EQ' as const,
    values: ['a'],
  });

  it('should add conditions', () => {
    const store = new DashboardFiltersStore();
    store.add(cond('API', 'API'));
    expect(store.conditions()).toEqual([cond('API', 'API')]);
    store.add(cond('PLAN', 'Plan'));
    expect(store.conditions().length).toBe(2);
  });

  it('should edit by index', () => {
    const store = new DashboardFiltersStore();
    store.add(cond('API', 'API'));
    const updated = { ...cond('API', 'API'), operator: 'IN' as const, values: ['x', 'y'] };
    store.edit(0, updated);
    expect(store.conditions()[0]).toEqual(updated);
  });

  it('should ignore edit out of range', () => {
    const store = new DashboardFiltersStore();
    store.add(cond('API', 'API'));
    store.edit(99, cond('X', 'X'));
    expect(store.conditions().length).toBe(1);
    expect(store.conditions()[0].field).toBe('API');
  });

  it('should remove by index', () => {
    const store = new DashboardFiltersStore();
    store.add(cond('A', 'A'));
    store.add(cond('B', 'B'));
    store.remove(0);
    expect(store.conditions().map(c => c.field)).toEqual(['B']);
  });

  it('should ignore remove out of range', () => {
    const store = new DashboardFiltersStore();
    store.add(cond('A', 'A'));
    store.remove(5);
    expect(store.conditions().length).toBe(1);
  });

  it('should clear', () => {
    const store = new DashboardFiltersStore();
    store.add(cond('A', 'A'));
    store.clear();
    expect(store.conditions()).toEqual([]);
  });
});
