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
export interface BaseEsFilter {
  field: string;
}

export interface IsInFilter extends BaseEsFilter {
  type: 'isin';
  values: string[];
}

export type EsFilter = IsInFilter;

export function toQuery(filters: EsFilter[]): string | null {
  if (!filters.length) return null;
  return filters
    .map(filter => {
      switch (filter.type) {
        case 'isin':
          return `${filter.field}:(${filter.values.map(v => `"${v}"`).join(' OR ')})`;
        default:
          throw new Error(`Unknown filter type: ${filter.type}`);
      }
    })
    .join(' AND ');
}
