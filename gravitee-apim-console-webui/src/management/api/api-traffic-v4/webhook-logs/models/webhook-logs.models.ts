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

import { ConnectionLog, Pagination } from '../../../../../entities/management-api-v2';
import { MultiFilter, SimpleFilter } from '../../runtime-logs/models';

/**
 * Webhook-specific additional metrics from backend
 */
export interface WebhookAdditionalMetrics {
  'string_webhook_req-method': string;
  string_webhook_url: string;
  'keyword_webhook_app-id': string;
  'keyword_webhook_sub-id': string;
  'int_webhook_retry-count': number;
  'json_webhook_retry-timeline': string;
  'string_webhook_last-error'?: string | null;
  'long_webhook_req-timestamp': number;
  'json_webhook_req-headers'?: string | null;
  'string_webhook_req-body'?: string | null;
  'long_webhook_resp-time'?: number;
  'int_webhook_resp-status': number;
  'int_webhook_resp-body-size'?: number;
  'json_webhook_resp-headers'?: string | null;
  'string_webhook_resp-body'?: string | null;
  bool_webhook_dlq: boolean;
}

/**
 * Webhook log extends ConnectionLog with webhook-specific fields
 */
export interface WebhookLog extends Omit<ConnectionLog, 'additionalMetrics'> {
  callbackUrl: string;
  duration: string;
  additionalMetrics?: WebhookAdditionalMetrics;
}

export interface WebhookLogsResponse {
  pagination: Pagination;
  data: WebhookLog[];
}

export interface WebhookFilters {
  searchTerm?: string;
  status?: string[];
  application?: string[];
  timeframe?: string;
  callbackUrls?: string[];
}

export interface WebhookMoreFiltersForm {
  period?: { label: string; value: string };
  from?: Moment | null;
  to?: Moment | null;
  callbackUrls?: string[];
}

export interface WebhookLogsQuickFilters {
  searchTerm?: string;
  statuses?: number[];
  applications?: MultiFilter;
  period?: SimpleFilter;
  from?: number;
  to?: number;
  callbackUrls?: string[];
}

export type WebhookLogsQuickFiltersInitialValues = WebhookLogsQuickFilters;

export const DEFAULT_WEBHOOK_LOGS_FILTERS: WebhookLogsQuickFilters = {};
