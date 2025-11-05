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

/**
 * Webhook-specific additional metrics from backend
 */
export interface WebhookAdditionalMetrics {
  long_webhook_responseTime: number;
  int_webhook_status: number;
  string_webhook_retry_timeline: string;
  int_webhook_retry_count: number;
  int_webhook_request_method: string;
  string_webhook_request_body: string | null;
  long_webhook_payload_size: number;
  string_webhook_last_error: string | null;
  string_webhook_request_headers: string;
  string_webhook_response_body: string | null;
  string_webhook_response_headers: string | null;
  string_webhook_url: string;
  keyword_webhook_application_id: string;
  keyword_webhook_subscription_id: string;
  long_webhook_request_timestamp: number;
  boolean_webhook_dl_queue: boolean;
}

/**
 * Webhook log extends ConnectionLog with webhook-specific fields
 */
export interface WebhookLog extends ConnectionLog {
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
