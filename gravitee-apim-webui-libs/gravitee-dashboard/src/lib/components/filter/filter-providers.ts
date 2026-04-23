/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { InjectionToken, Provider, Type } from '@angular/core';
import { Observable } from 'rxjs';

import { FilterDefinition } from './filter.model';

export interface FilterValueItem {
  value: string;
  label: string;
  id?: string;
}

export interface FilterValuesResult {
  data: FilterValueItem[];
  hasNextPage: boolean;
  totalCount?: number;
}

export interface FilterValuesQuery {
  filterName: string;
  query?: string;
  from?: number;
  to?: number;
  page: number;
  perPage: number;
}

export interface FilterDefinitionProvider {
  getDefinitions(): Observable<FilterDefinition[]>;
}

export interface FilterValuesProvider {
  getValues(query: FilterValuesQuery): Observable<FilterValuesResult>;
}

export const FILTER_DEFINITION_PROVIDER = new InjectionToken<FilterDefinitionProvider>('FILTER_DEFINITION_PROVIDER');

export const FILTER_VALUES_PROVIDER = new InjectionToken<FilterValuesProvider>('FILTER_VALUES_PROVIDER');

export function provideFilterDefinitions(impl: Type<FilterDefinitionProvider>): Provider {
  return { provide: FILTER_DEFINITION_PROVIDER, useExisting: impl };
}

export function provideFilterValues(impl: Type<FilterValuesProvider>): Provider {
  return { provide: FILTER_VALUES_PROVIDER, useExisting: impl };
}
