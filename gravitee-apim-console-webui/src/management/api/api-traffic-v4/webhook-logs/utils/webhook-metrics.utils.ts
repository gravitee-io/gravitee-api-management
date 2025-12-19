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

import { isNumber } from 'angular';

import { ConnectionLog } from '../../../../../entities/management-api-v2';
import { WebhookAdditionalMetrics, WebhookLog } from '../models/webhook-logs.models';

/**
 * Safely parses a numeric string, returning 0 if the value is not a valid number.
 */
function parseNumberOrZero(raw?: string): number {
  const num = Number(raw);
  return Number.isNaN(num) ? 0 : num;
}

/**
 * Extracts and transforms additionalMetrics from the backend format
 * to the typed WebhookAdditionalMetrics format.
 * Since we filter for documents with additional-metrics (requiresAdditional=true),
 * we're guaranteed that additionalMetrics exists when this function is called.
 * Only transforms fields that need type conversion (numbers, booleans).
 */
export function extractWebhookAdditionalMetrics(additionalMetrics: {
  [key: string]: string | boolean | number;
}): Partial<WebhookAdditionalMetrics> {
  const getString = (key: string): string | undefined => {
    const value = additionalMetrics[key];
    return typeof value === 'string' ? value : undefined;
  };

  const getStringOrNumber = (key: string): string | number | undefined => {
    const value = additionalMetrics[key];
    return typeof value === 'string' || isNumber(value) ? value : undefined;
  };

  const parseOptionalNumber = (key: string): number | undefined => {
    const value = getStringOrNumber(key);
    if (value === undefined || value === null) {
      return undefined;
    }
    const num = isNumber(value) ? value : Number(value);
    return Number.isNaN(num) ? undefined : num;
  };

  const metrics: Partial<WebhookAdditionalMetrics> = {
    'string_webhook_req-method': getString('string_webhook_req-method'),
    string_webhook_url: getString('string_webhook_url'),
    'keyword_webhook_app-id': getString('keyword_webhook_app-id'),
    'keyword_webhook_sub-id': getString('keyword_webhook_sub-id'),
    'json_webhook_retry-timeline': getString('json_webhook_retry-timeline'),
    'string_webhook_last-error': getString('string_webhook_last-error'),
    'json_webhook_req-headers': getString('json_webhook_req-headers'),
    'string_webhook_req-body': getString('string_webhook_req-body'),
    'json_webhook_resp-headers': getString('json_webhook_resp-headers'),
    'string_webhook_resp-body': getString('string_webhook_resp-body'),

    'long_webhook_req-timestamp': parseNumberOrZero(getStringOrNumber('long_webhook_req-timestamp')?.toString()),
    'int_webhook_resp-status': parseNumberOrZero(getStringOrNumber('int_webhook_resp-status')?.toString()),
    'int_webhook_retry-count': parseNumberOrZero(getStringOrNumber('int_webhook_retry-count')?.toString()),
    'long_webhook_resp-time': parseOptionalNumber('long_webhook_resp-time'),
    'int_webhook_resp-body-size': parseOptionalNumber('int_webhook_resp-body-size'),
  };

  // Boolean field - only set if present in the backend response
  const boolWebhookDlq = additionalMetrics['bool_webhook_dlq'];
  if (typeof boolWebhookDlq === 'boolean') {
    metrics.bool_webhook_dlq = boolWebhookDlq;
  }

  return metrics;
}

/**
 * Formats a duration in milliseconds to a human-readable string.
 */
export function formatDuration(ms: number): string {
  if (ms < 1000) {
    return `${ms} ms`;
  }
  const seconds = (ms / 1000).toFixed(1);
  return `${seconds} s`;
}

/**
 * Maps a ConnectionLog from the backend to a WebhookLog format.
 * This transformation extracts webhook-specific fields and formats them appropriately.
 */
export function mapConnectionLogToWebhookLog(connectionLog: ConnectionLog): WebhookLog {
  const additionalMetrics = extractWebhookAdditionalMetrics(connectionLog.additionalMetrics || {});
  const gatewayLatencyMs = connectionLog.gatewayResponseTime || 0;

  const webhookLog: WebhookLog = {
    apiId: connectionLog.apiId,
    requestId: connectionLog.requestId,
    timestamp: connectionLog.timestamp,
    method: connectionLog.method,
    status: additionalMetrics['int_webhook_resp-status'] ?? 0,
    application: connectionLog.application,
    plan: connectionLog.plan,
    requestEnded: connectionLog.requestEnded,
    gatewayResponseTime: gatewayLatencyMs,
    uri: connectionLog.uri,
    endpoint: connectionLog.endpoint,
    message: connectionLog.message,
    errorKey: connectionLog.errorKey,
    errorComponentName: connectionLog.errorComponentName,
    errorComponentType: connectionLog.errorComponentType,
    warnings: connectionLog.warnings,
    callbackUrl: additionalMetrics['string_webhook_url'] ?? '',
    duration: formatDuration(gatewayLatencyMs),
    additionalMetrics,
  };

  return webhookLog;
}
