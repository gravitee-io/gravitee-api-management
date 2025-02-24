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
import { TopApis } from '../../shared/components/top-apis-widget/top-apis-widget.component';

export class AnalyticsMetadata {
  name: string;
  order: string;
  version?: string;
  unknown?: boolean;
}

export class AnalyticsGroupByResponse {
  values: { [key: string]: number };
  metadata: { [key: string]: AnalyticsMetadata };
}

export class AnalyticsStatsResponse {
  count: number;
  min: number;
  max: number;
  avg: number;
  sum: number;
  rps: number;
  rpm: number;
  rph: number;
}

export class AnalyticsCountResponse {
  count: number;
}

export interface AnalyticsTopApisResponse {
  data: TopApis[];
}

export interface AnalyticsV4StatsResponse {
  requestsPerSecond: number;
  requestsTotal: number;
  responseMinTime: number;
  responseMaxTime: number;
  responseAvgTime: number;
}
