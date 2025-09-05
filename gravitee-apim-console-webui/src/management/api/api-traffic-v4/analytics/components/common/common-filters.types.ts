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

export interface CommonFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  plans: string[] | null;
}

export interface CommonFilterForm {
  timeframe: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>;
  plans: FormControl<string[] | null>;
}

export interface FilterChip {
  key: string;
  value: string;
  display: string;
}

// Native-specific filters (future extensions)
export interface NativeSpecificFilters {
  // Future: hosts?: string[] | null;
}

// Proxy-specific filters
export interface ProxySpecificFilters {
  httpStatuses: string[] | null;
  applications: string[] | null;
}

// Proxy-specific form controls
export interface ProxySpecificForm {
  httpStatuses: FormControl<string[] | null>;
  applications: FormControl<string[] | null>;
}

// Complete filter types for each component
export type NativeFilters = CommonFilters & NativeSpecificFilters;
export type ProxyFilters = CommonFilters & ProxySpecificFilters;

// Complete form types for each component
export type NativeFilterForm = CommonFilterForm; // No specific controls yet
export type ProxyFilterForm = CommonFilterForm & ProxySpecificForm;
