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

import { FormControl } from '@angular/forms';
import { Moment } from 'moment';

export const FILTER_KEYS = {
  TIMEFRAME: 'timeframe',
  PLANS: 'plans',
  HTTP_STATUSES: 'httpStatuses',
  APPLICATIONS: 'applications',
} as const;

export const FILTER_FIELDS = {
  NATIVE: [FILTER_KEYS.PLANS] as string[],
  PROXY: [FILTER_KEYS.HTTP_STATUSES, FILTER_KEYS.PLANS, FILTER_KEYS.APPLICATIONS] as string[],
} as const;

export interface CommonFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  [FILTER_KEYS.PLANS]: string[] | null;
}

export interface CommonFilterForm {
  [FILTER_KEYS.TIMEFRAME]: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>;
  [FILTER_KEYS.PLANS]: FormControl<string[] | null>;
}

export interface FilterChip {
  key: string;
  value: string;
  display: string;
}

export interface NativeSpecificFilters {
  // Future: hosts?: string[] | null;
}

export interface ProxySpecificFilters {
  [FILTER_KEYS.HTTP_STATUSES]: string[] | null;
  [FILTER_KEYS.APPLICATIONS]: string[] | null;
}

export interface ProxySpecificForm {
  [FILTER_KEYS.HTTP_STATUSES]: FormControl<string[] | null>;
  [FILTER_KEYS.APPLICATIONS]: FormControl<string[] | null>;
}

export type NativeFilters = CommonFilters;
export type ProxyFilters = CommonFilters & ProxySpecificFilters;

export type NativeFilterForm = CommonFilterForm;
export type ProxyFilterForm = CommonFilterForm & ProxySpecificForm;
