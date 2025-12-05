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
 * Safely parses a numeric string, returning undefined if the value is not a valid number.
 */
function parseNumberOrUndefined(raw?: string): number | undefined {
  const num = Number(raw);
  return Number.isNaN(num) ? undefined : num;
}

/**
 * Extracts and transforms additionalMetrics from the backend format
 * to the typed WebhookAdditionalMetrics format.
 * Since we filter for documents with additional-metrics (requiresAdditional=true),
 * we're guaranteed that additionalMetrics exists when this function is called.
 */
export function extractWebhookAdditionalMetrics(additionalMetrics: { [key: string]: string }): Partial<WebhookAdditionalMetrics> {
  const metrics: Partial<WebhookAdditionalMetrics> = {};

  // Fields that are always present
  metrics['string_webhook_req-method'] = additionalMetrics['string_webhook_req-method'];
  metrics['string_webhook_url'] = additionalMetrics['string_webhook_url'];
  metrics['keyword_webhook_app-id'] = additionalMetrics['keyword_webhook_app-id'];
  metrics['keyword_webhook_sub-id'] = additionalMetrics['keyword_webhook_sub-id'];
  metrics['json_webhook_retry-timeline'] = additionalMetrics['json_webhook_retry-timeline'];
  metrics['long_webhook_req-timestamp'] = parseNumberOrZero(additionalMetrics['long_webhook_req-timestamp']);
  metrics['int_webhook_resp-status'] = parseNumberOrZero(additionalMetrics['int_webhook_resp-status']);
  metrics['int_webhook_retry-count'] = parseNumberOrZero(additionalMetrics['int_webhook_retry-count']);
  metrics['bool_webhook_dlq'] = additionalMetrics['bool_webhook_dlq'] === 'true';

  // Conditional fields - only assign if present
  if (additionalMetrics['string_webhook_last-error'] !== undefined) {
    metrics['string_webhook_last-error'] = additionalMetrics['string_webhook_last-error'] || null;
  }

  if (additionalMetrics['json_webhook_req-headers'] !== undefined) {
    metrics['json_webhook_req-headers'] = additionalMetrics['json_webhook_req-headers'] || null;
  }

  if (additionalMetrics['string_webhook_req-body'] !== undefined) {
    metrics['string_webhook_req-body'] = additionalMetrics['string_webhook_req-body'] || null;
  }

  if (additionalMetrics['long_webhook_resp-time'] !== undefined) {
    const value = parseNumberOrUndefined(additionalMetrics['long_webhook_resp-time']);
    if (value !== undefined) {
      metrics['long_webhook_resp-time'] = value;
    }
  }

  if (additionalMetrics['int_webhook_resp-body-size'] !== undefined) {
    const value = parseNumberOrUndefined(additionalMetrics['int_webhook_resp-body-size']);
    if (value !== undefined) {
      metrics['int_webhook_resp-body-size'] = value;
    }
  }

  if (additionalMetrics['json_webhook_resp-headers'] !== undefined) {
    metrics['json_webhook_resp-headers'] = additionalMetrics['json_webhook_resp-headers'] || null;
  }

  if (additionalMetrics['string_webhook_resp-body'] !== undefined) {
    metrics['string_webhook_resp-body'] = additionalMetrics['string_webhook_resp-body'] || null;
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
