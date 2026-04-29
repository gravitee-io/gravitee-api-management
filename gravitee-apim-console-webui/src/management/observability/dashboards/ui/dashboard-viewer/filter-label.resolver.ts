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
import { FilterCondition, FilterDefinition, FILTER_DEFINITION_PROVIDER, ID_BASED_FILTER_NAMES } from '@gravitee/gravitee-dashboard';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map, shareReplay } from 'rxjs/operators';

import { Constants } from '../../../../../entities/Constants';

interface ResolveRequestEntry {
  filterName: string;
  ids: string[];
}

interface ResolveResponseEntry {
  filterName: string;
  labels: Record<string, string>;
}

interface ResolveResponse {
  entries: ResolveResponseEntry[];
}

const ID_BASED_FILTER_NAME_SET = new Set(ID_BASED_FILTER_NAMES);

@Injectable()
export class FilterLabelResolver {
  private readonly http = inject(HttpClient);
  private readonly constants = inject(Constants);
  private readonly definitionProvider = inject(FILTER_DEFINITION_PROVIDER);
  private readonly definitions$ = this.definitionProvider.getDefinitions().pipe(shareReplay({ bufferSize: 1, refCount: false }));

  resolveLabels(conditions: FilterCondition[]): Observable<FilterCondition[]> {
    if (conditions.length === 0) return of([]);

    return forkJoin({
      definitions: this.definitions$,
      resolvedLabels: this.resolveIdBasedLabels(conditions),
    }).pipe(
      map(({ definitions, resolvedLabels }) => {
        const defMap = new Map<string, FilterDefinition>();
        definitions.forEach(d => defMap.set(d.name, d));

        return conditions.map(c => {
          const def = defMap.get(c.field);
          const label = def?.label ?? c.field;

          const resolvedForField = resolvedLabels.get(c.field);
          const valueLabels = resolvedForField ? c.values.map(v => resolvedForField[v] ?? v) : undefined;

          return { ...c, label, valueLabels };
        });
      }),
      catchError(() => of(conditions)),
    );
  }

  private resolveIdBasedLabels(conditions: FilterCondition[]): Observable<Map<string, Record<string, string>>> {
    const idsByFilterName = new Map<string, Set<string>>();
    for (const c of conditions) {
      if (ID_BASED_FILTER_NAME_SET.has(c.field)) {
        const ids = idsByFilterName.get(c.field) ?? new Set<string>();
        c.values.forEach(value => ids.add(value));
        idsByFilterName.set(c.field, ids);
      }
    }

    const entries: ResolveRequestEntry[] = Array.from(idsByFilterName.entries()).map(([filterName, ids]) => ({
      filterName,
      ids: Array.from(ids),
    }));

    if (entries.length === 0) return of(new Map());

    const url = `${this.constants.env?.v2BaseURL}/observability/filters/resolve`;
    return this.http.post<ResolveResponse>(url, { entries }).pipe(
      map(res => {
        const result = new Map<string, Record<string, string>>();
        for (const entry of res.entries ?? []) {
          result.set(entry.filterName, entry.labels ?? {});
        }
        return result;
      }),
      catchError(() => of(new Map())),
    );
  }
}
