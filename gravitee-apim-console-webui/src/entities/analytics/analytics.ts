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

export interface AnalyticsBucket {
  name: string;
  data: number[];
}

export interface AnalyticsTimestamp {
  from: number;
  to: number;
  interval: number;
}

// Status:
export interface AnalyticsResponseStatusValue {
  buckets: AnalyticsBucket[];
  field: string;
  name: string;
}

export interface AnalyticsResponseStatus {
  timestamp: AnalyticsTimestamp;
  values: AnalyticsResponseStatusValue[];
}

export interface AnalyticsV4ResponseStatus {
  timeRange: AnalyticsTimestamp;
  data: Record<string, number[]>;
}

// AverageResponseTimes:
export interface AnalyticsAverageResponseTimesValue {
  buckets: AnalyticsBucket[];
  field: string;
  name: string;
}

export interface AnalyticsAverageResponseTimes {
  timestamp: AnalyticsTimestamp;
  values: AnalyticsAverageResponseTimesValue[];
}

export interface AnalyticsV4ResponseTimes {
  timeRange: AnalyticsTimestamp;
  data: number[];
}

export interface TopApplication {
  id: string;
  name: string;
  count: number;
}

export enum AnalyticsDefinitionVersion {
  V2 = 'V2',
  V4 = 'V4',
}

export interface TopApplicationsByRequestsCountRes {
  data: TopApplication[];
}

export interface AnalyticsTopApis {
  id: string;
  name: string;
  count: number;
  definitionVersion: AnalyticsDefinitionVersion;
}

export interface AnalyticsTopFailedApi {
  id: string;
  name: string;
  definitionVersion: AnalyticsDefinitionVersion;
  failedCalls: number;
  failedCallsRatio: number;
}

export interface AnalyticsTopFailedApisRes {
  data: AnalyticsTopFailedApi[];
}
