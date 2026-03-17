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

export type EnvLogsInitialValues = {
  period?: { label: string; value: string };
  apiIds?: string[];
  applicationIds?: string[];
  methods?: string[];
  statuses?: Set<number>;
  entrypoints?: string[];
  plans?: string[];
  transactionId?: string;
  requestId?: string;
  uri?: string;
  responseTime?: number;
  from?: Moment;
  to?: Moment;
  errorKeys?: string[];
};
