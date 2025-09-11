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

export enum AggregationTypes {
  MAX = 'MAX',
  MIN = 'MIN',
  AVG = 'AVG',
  FIELD = 'FIELD',
  VALUE = 'VALUE',
  DELTA = 'DELTA',
}

export enum AggregationFields {
  APPLICATION_ID = 'application-id',
  STATUS = 'status',
  GATEWAY_RESPONSE_TIME_MS = 'gateway-response-time-ms',
  ENDPOINT_RESPONSE_TIME_MS = 'endpoint-response-time-ms',
  DOWNSTREAM_ACTIVE_CONNECTIONS = 'downstream-active-connections',
  UPSTREAM_ACTIVE_CONNECTIONS = 'upstream-active-connections',
  DOWNSTREAM_PUBLISH_MESSAGES_TOTAL = 'downstream-publish-messages-total',
  UPSTREAM_PUBLISH_MESSAGES_TOTAL = 'upstream-publish-messages-total',
  UPSTREAM_SUBSCRIBE_MESSAGES_TOTAL = 'upstream-subscribe-messages-total',
  DOWNSTREAM_SUBSCRIBE_MESSAGES_TOTAL = 'downstream-subscribe-messages-total',
}

export interface AnalyticsHistogramAggregation {
  type: AggregationTypes;
  field: AggregationFields;
  label?: string;
}

export interface AggregationHistogramTimestamp {
  from: number;
  to: number;
  interval: number;
}

export interface Bucket {
  name: string;
  data: number[];
}

export interface AggregationHistogramValue {
  buckets: Bucket[];
  field: string;
  name: string;
  metadata?: AggregationHistogramValueMetadata;
}

export interface AggregationHistogramValueMetadata {
  [key: string]: {
    name?: string;
  };
}

export interface HistogramAnalyticsResponse {
  analyticsType: 'HISTOGRAM';
  timestamp: AggregationHistogramTimestamp;
  values: AggregationHistogramValue[];
}
