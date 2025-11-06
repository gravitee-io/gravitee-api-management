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

import { Moment } from 'moment';

import { DEFAULT_PERIOD } from '../../models/traffic-filters.models';

export type SimpleFilter = FilterVM;
export type MultiFilter = FilterVM[];

export type FilterVM = {
  label: string;
  value: string;
};

export type LogFilters = {
  from?: number;
  to?: number;
  period?: SimpleFilter;
  entrypoints?: string[];
  applications?: MultiFilter;
  plans?: MultiFilter;
  methods?: string[];
  statuses?: Set<number>;
};

export type LogFiltersForm = { period: SimpleFilter; entrypoints: string[]; plans: string[]; methods: string[] };

export type MoreFiltersForm = { period: SimpleFilter; from: Moment; to: Moment; statuses: Set<number>; applications?: MultiFilter };

export type LogFiltersInitialValues = {
  applications?: MultiFilter;
  plans?: MultiFilter;
  from: Moment;
  to: Moment;
  entrypoints: string[];
  methods: string[];
  statuses: Set<number>;
};

export const DEFAULT_FILTERS: LogFilters = {
  period: DEFAULT_PERIOD,
  entrypoints: undefined,
  applications: undefined,
  plans: undefined,
  from: undefined,
  to: undefined,
  methods: undefined,
};
