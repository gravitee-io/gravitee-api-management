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
import { toQuery, EsFilter } from './esQuery';

describe('toQuery', () => {
  it('should return null for empty filters', () => {
    expect(toQuery([])).toBeNull();
  });

  it('should build query for single IsInFilter', () => {
    const filters: EsFilter[] = [{ type: 'isin', field: 'status', values: ['200', '404'] }];
    expect(toQuery(filters)).toBe('status:("200" OR "404")');
  });

  it('should build query for multiple IsInFilters', () => {
    const filters: EsFilter[] = [
      { type: 'isin', field: 'status', values: ['200', '404'] },
      { type: 'isin', field: 'host', values: ['example.com', 'test.com'] },
    ];
    expect(toQuery(filters)).toBe('status:("200" OR "404") AND host:("example.com" OR "test.com")');
  });

  it('should throw error for unknown filter type', () => {
    const filters: any[] = [{ type: 'unknown', field: 'foo', values: ['bar'] }];
    expect(() => toQuery(filters)).toThrow('Unknown filter type: unknown');
  });
});
