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

export const ENV_LOGS_DEFAULT_PERIOD = { label: 'None', value: '0' };

export interface EnvLogsMoreFiltersForm {
  from?: Moment | null;
  to?: Moment | null;
  statuses: Set<number>;
  entrypoints?: string[] | null;
  methods?: string[] | null;
  plans?: string[] | null;
  mcpMethod?: string | null;
  transactionId?: string | null;
  requestId?: string | null;
  uri?: string | null;
  responseTime?: number | null;
}

export const createDefaultMoreFilters = (): EnvLogsMoreFiltersForm => ({
  from: null,
  to: null,
  statuses: new Set(),
  entrypoints: null,
  methods: null,
  plans: null,
  mcpMethod: null,
  transactionId: null,
  requestId: null,
  uri: null,
  responseTime: null,
});

export const DEFAULT_MORE_FILTERS: EnvLogsMoreFiltersForm = createDefaultMoreFilters();
