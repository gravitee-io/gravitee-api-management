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
import {
  FilterDefinition,
  FilterDefinitionProvider,
  FilterValuesProvider,
  FilterValuesQuery,
  FilterValuesResult,
} from '@gravitee/gravitee-dashboard';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Constants } from '../../../entities/Constants';

interface FilterSpecsResponseApi {
  data?: FilterSpecApi[];
}

interface FilterSpecApi {
  name: string;
  label: string;
  type: string;
  operators: string[];
  enumValues?: string[];
  values?: string[];
  range?: { min: number; max: number };
  apiTypes?: string[];
  signals?: string[];
}

interface FilterValuesPaginationApi {
  page: number;
  /** Present on many Management API list responses; filter values may omit it. */
  pageCount?: number;
  perPage?: number;
  pageItemsCount?: number;
  totalCount?: number;
}

interface FilterValuesResponseApi {
  data?: FilterValueItemApi[];
  pagination?: FilterValuesPaginationApi;
}

interface FilterValueItemApi {
  value: string;
  id?: string;
}

/** Observability surface consuming the shared filter catalog — mirrors the backend FilterSignal enum. */
export type ObservabilityFilterSignal = 'ANALYTICS' | 'LOGS';

/**
 * Surface hosting the filter bar. When provided, definitions carrying signals are narrowed to that
 * surface so a page only offers filters its query engine supports (APIM-14828).
 */
export const OBSERVABILITY_FILTER_SIGNAL = new InjectionToken<ObservabilityFilterSignal>('OBSERVABILITY_FILTER_SIGNAL');

@Injectable()
export class ObservabilityFiltersApiService implements FilterDefinitionProvider, FilterValuesProvider {
  private readonly http = inject(HttpClient);
  private readonly constants = inject(Constants);
  private readonly surfaceSignal = inject(OBSERVABILITY_FILTER_SIGNAL, { optional: true });

  getDefinitions(): Observable<FilterDefinition[]> {
    const url = `${this.constants.env?.v2BaseURL}/observability/filters/definition`;
    return this.http
      .get<FilterSpecsResponseApi>(url)
      .pipe(map(res => (res.data ?? []).map(item => this.mapDefinition(item)).filter(def => this.isSupportedOnSurface(def))));
  }

  getValues(query: FilterValuesQuery): Observable<FilterValuesResult> {
    const url = `${this.constants.env?.v2BaseURL}/observability/filters/${encodeURIComponent(query.filterName)}/values`;
    let params = new HttpParams().set('page', String(query.page)).set('perPage', String(query.perPage));
    if (query.query) {
      params = params.set('query', query.query);
    }
    if (query.from != null) {
      params = params.set('from', String(query.from));
    }
    if (query.to != null) {
      params = params.set('to', String(query.to));
    }
    return this.http.get<FilterValuesResponseApi>(url, { params }).pipe(map(res => this.mapValuesResult(res, query.perPage)));
  }

  /** Definitions without signals (older backends) stay visible on every surface; an empty list means no surface supports it. */
  private isSupportedOnSurface(def: FilterDefinition): boolean {
    if (!this.surfaceSignal || def.signals == null) {
      return true;
    }
    return def.signals.includes(this.surfaceSignal);
  }

  private mapDefinition(item: FilterSpecApi): FilterDefinition {
    return {
      name: item.name,
      label: item.label,
      type: item.type,
      operators: item.operators ?? [],
      range: item.range,
      values: item.enumValues ?? item.values,
      apiTypes: item.apiTypes,
      signals: item.signals,
    };
  }

  private mapValuesResult(res: FilterValuesResponseApi, requestedPerPage: number): FilterValuesResult {
    const rows = res.data ?? [];
    const pagination = res.pagination;
    const perPage = pagination?.perPage ?? requestedPerPage;
    const hasNextPage = this.computeHasNextPage(rows, perPage, pagination);
    return {
      data: rows.map(row => ({
        value: row.id ?? row.value,
        label: row.value,
        id: row.id,
      })),
      hasNextPage,
      totalCount: pagination?.totalCount,
    };
  }

  /**
   * Filter values pagination from the API includes page, perPage, pageItemsCount, and totalCount;
   * pageCount is optional. When it is missing, infer the next page from totalCount.
   */
  private computeHasNextPage(rows: FilterValueItemApi[], perPage: number, pagination: FilterValuesPaginationApi | undefined): boolean {
    if (!pagination) {
      return rows.length === perPage;
    }
    const pageCount = pagination.pageCount;
    if (pageCount != null && pageCount > 0) {
      return pagination.page < pageCount;
    }
    const total = pagination.totalCount;
    if (total != null && total > 0) {
      return pagination.page * perPage < total;
    }
    return rows.length === perPage;
  }
}
