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
import angular from 'angular';

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { editor } from 'monaco-editor';

import { WebhookLog } from '../webhook-logs/models';

type OverviewItem = { label: string; value: string | number; variant?: 'success' | 'warning' | 'error' };
interface HeaderItem {
  key: string;
  value: string;
}
interface DeliveryAttempt {
  timestamp: string;
  attempt: number;
  status: number;
  duration: number;
  reason?: string;
}

@Component({
  selector: 'webhook-logs-details',
  templateUrl: './webhook-logs-details.component.html',
  styleUrls: ['./webhook-logs-details.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class WebhookLogsDetailsComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly requestId = this.activatedRoute.snapshot.params.requestId;

  overviewRequest: OverviewItem[] = [];
  overviewResponse: OverviewItem[] = [];
  deliveryAttempts: DeliveryAttempt[] = [];
  requestHeaders: HeaderItem[] = [];
  responseHeaders: HeaderItem[] = [];
  requestBody = '';
  responseBody = '';
  selectedLog: WebhookLog | null = null;
  deliveryAttemptsExpanded = true;
  monacoEditorOptions: editor.IStandaloneEditorConstructionOptions = {
    renderLineHighlight: 'none',
    hideCursorInOverviewRuler: true,
    overviewRulerBorder: false,
    scrollbar: {
      vertical: 'hidden',
      horizontal: 'hidden',
      useShadows: false,
    },
  };

  constructor() {
    // Temporary sample data to make the layout visible until real data is wired.
    this.applyLog(this.buildSampleLog());
  }

  formatAttemptTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) {
      return timestamp;
    }
    return date.toLocaleString();
  }

  formatAttemptDuration(duration: number): string {
    return duration >= 1000 ? `${(duration / 1000).toFixed(1)} s` : `${duration} ms`;
  }

  loadMoreAttempts(): void {
    if (!this.deliveryAttempts.length) {
      return;
    }
    const last = this.deliveryAttempts[this.deliveryAttempts.length - 1];
    const nextIndex = last.attempt + 1;
    const nextTimestamp = new Date(new Date(last.timestamp).getTime() + 60_000).toISOString();
    this.deliveryAttempts = [
      ...this.deliveryAttempts,
      { attempt: nextIndex, timestamp: nextTimestamp, duration: 640, status: 200, reason: 'Delivery confirmed' },
    ];
  }

  copyToClipboard(content: string): void {
    if (!content) {
      return;
    }
    navigator?.clipboard?.writeText(content).catch(() => undefined);
  }

  openLogSettings(): void {
    this.router.navigate(['../'], {
      relativeTo: this.activatedRoute,
      queryParams: { openSettings: 'true' },
    });
  }

  private applyLog(log: WebhookLog | null): void {
    this.selectedLog = log;

    if (!log) {
      this.overviewRequest = [];
      this.overviewResponse = [];
      this.deliveryAttempts = [];
      this.requestHeaders = [];
      this.responseHeaders = [];
      this.requestBody = '';
      this.responseBody = '';
      return;
    }

    this.deliveryAttempts = this.buildDeliveryAttempts(log);
    const attemptsCount = this.deliveryAttempts.length || (log.additionalMetrics?.['int_webhook_retry-count'] ?? 1);

    this.overviewRequest = [
      { label: 'Date', value: this.formatDate(log.timestamp) },
      { label: 'Method', value: log.method },
      { label: 'Attempts', value: attemptsCount },
      { label: 'Callback URL', value: log.callbackUrl ?? log.uri },
    ];

    this.overviewResponse = [
      { label: 'Status', value: log.status, variant: this.toVariant(log.status) },
      { label: 'Total duration', value: this.formatDuration(log) },
      { label: 'Payload size', value: this.formatPayloadSize(log) },
      { label: 'Last error', value: log.additionalMetrics?.['string_webhook_last-error'] ?? '—' },
    ];

    this.requestHeaders = this.parseHeaders(log.additionalMetrics?.['string_webhook_request-headers']);
    this.responseHeaders = this.parseHeaders(log.additionalMetrics?.['string_webhook_response-headers']);
    this.requestBody = log.additionalMetrics?.['string_webhook_request-body'] ?? '';
    this.responseBody = log.additionalMetrics?.['string_webhook_response-body'] ?? '';
  }

  private buildDeliveryAttempts(log: WebhookLog): DeliveryAttempt[] {
    const rawTimeline = log.additionalMetrics?.['string_webhook_retry-timeline'];
    if (!rawTimeline) {
      return [
        {
          attempt: 1,
          timestamp: log.timestamp,
          duration: log.gatewayResponseTime,
          status: log.status,
          reason: log.additionalMetrics?.['string_webhook_last-error'] ?? 'Initial delivery attempt',
        },
      ];
    }

    try {
      const parsed = JSON.parse(rawTimeline) as Array<{
        attempt?: number;
        timestamp: number | string;
        duration: number;
        status: number;
        reason?: string;
      }>;
      return parsed.map((item, index) => ({
        attempt: item.attempt ?? index + 1,
        timestamp: angular.isNumber(item.timestamp) ? new Date(item.timestamp).toISOString() : item.timestamp,
        duration: item.duration,
        status: item.status,
        reason: item.reason,
      }));
    } catch {
      return [];
    }
  }

  private parseHeaders(raw?: string | null): HeaderItem[] {
    if (!raw) {
      return [];
    }
    try {
      const parsed = JSON.parse(raw) as Record<string, string | string[]>;
      return Object.entries(parsed).map(([key, value]) => ({
        key,
        value: Array.isArray(value) ? value.join(', ') : value,
      }));
    } catch {
      return [];
    }
  }

  private toVariant(status: number): 'success' | 'warning' | 'error' {
    if (status >= 500) {
      return 'error';
    }
    if (status >= 400) {
      return 'warning';
    }
    return 'success';
  }

  private formatDuration(log: WebhookLog): string {
    if (log.duration) {
      return log.duration;
    }
    return `${(log.gatewayResponseTime / 1000).toFixed(1)} s`;
  }

  private formatPayloadSize(log: WebhookLog): string {
    const size = log.additionalMetrics?.['int_webhook_response-payload-size'];
    if (size == null) {
      return '—';
    }
    if (size >= 1024) {
      return `${(size / 1024).toFixed(1)} KB`;
    }
    return `${size} B`;
  }

  statusVariant(status?: number | null): 'success' | 'warning' | 'error' | null {
    if (status == null) {
      return null;
    }
    return this.toVariant(status);
  }

  private formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString();
  }

  private buildInitials(name?: string | null): string {
    if (!name) {
      return 'WH';
    }
    const parts = name.trim().split(/\s+/);
    if (!parts.length) {
      return 'WH';
    }
    return parts
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }

  private buildSampleLog(): WebhookLog {
    return {
      apiId: 'api-sample',
      requestId: this.requestId ?? 'req-sample',
      timestamp: '2025-06-15T12:00:00.000Z',
      method: 'POST' as any,
      status: 200,
      application: { id: 'app-1', name: 'Postman Monitoring', type: 'SIMPLE' as any, apiKeyMode: 'UNSPECIFIED' },
      plan: { id: 'plan-1', name: 'Webhook Plan', mode: 'PUSH' as any },
      requestEnded: true,
      gatewayResponseTime: 2800,
      uri: 'https://warehouse.acme.com/webhooks/fulfillment',
      endpoint: 'https://warehouse.acme.com/webhooks/fulfillment',
      callbackUrl: 'https://warehouse.acme.com/webhooks/fulfillment',
      duration: '2.8 s',
      additionalMetrics: {
        'string_webhook_request-method': 'POST',
        string_webhook_url: 'https://warehouse.acme.com/webhooks/fulfillment',
        'keyword_webhook_application-id': 'app-1',
        'keyword_webhook_subscription-id': 'sub-1',
        'int_webhook_retry-count': 5,
        'string_webhook_retry-timeline': JSON.stringify([
          {
            attempt: 5,
            timestamp: new Date('2025-06-15T12:00:00Z').getTime(),
            duration: 2800,
            status: 200,
            reason: 'Delivered successfully',
          },
          {
            attempt: 4,
            timestamp: new Date('2025-06-15T11:59:00Z').getTime(),
            duration: 155,
            status: 500,
            reason: 'Warehouse callback timed out',
          },
          {
            attempt: 3,
            timestamp: new Date('2025-06-15T11:58:00Z').getTime(),
            duration: 155,
            status: 500,
            reason: 'Warehouse callback timed out',
          },
          {
            attempt: 2,
            timestamp: new Date('2025-06-15T11:57:00Z').getTime(),
            duration: 1600,
            status: 500,
            reason: 'Warehouse callback timed out',
          },
          {
            attempt: 1,
            timestamp: new Date('2025-06-15T11:56:00Z').getTime(),
            duration: 340,
            status: 500,
            reason: 'Warehouse callback timed out',
          },
        ]),
        'string_webhook_last-error': 'Warehouse callback timed out',
        'long_webhook_request-timestamp': new Date('2025-06-15T11:56:00Z').getTime(),
        'string_webhook_request-headers': JSON.stringify({
          Host: 'api-gateway.team.gravitee.dev',
          'User-Agent': 'Gravitee.io 4.5 SNAPSHOT',
          'X-Forwarded-For': '127.0.0.1',
          'X-Forwarded-Proto': 'https',
          'X-Request-ID': 'cb4e476c-236c-4f06-846c-3c2a356f0c84',
        }),
        'string_webhook_request-body': `{
  "id": "evt_01J8M3TKQ5PM2W8X69JKS95X56",
  "type": "order.fulfillment.failed",
  "created_at": "2025-06-16T10:35:02Z",
  "data": {
    "order_id": "ORD-183221",
    "reason": "warehouse_timeout",
    "attempted_at": "2025-06-16T10:35:01Z",
    "items": [
      { "sku": "SKU-RED-42", "qty": 1 },
      { "sku": "SKU-BLK-11", "qty": 2 }
    ],
    "meta": {
      "region": "eu-west-1",
      "version": "2025-05-21"
    }
  }
}`,
        'long_webhook_response-time': 2800,
        'int_webhook_response-status': 200,
        'int_webhook_response-payload-size': 42800,
        'string_webhook_response-headers': JSON.stringify({
          Status: '200',
          'Content-Length': '428',
          'Content-Type': 'application/json; charset=utf-8',
          Date: 'Thu, 15 Jun 2025 12:00:05 GMT',
          'X-Correlation-Id': 'cb4e476c-236c-4f06-846c-3c2a356f0c84',
        }),
        'string_webhook_response-body': `{
  "status": "acknowledged"
}`,
        'boolean_webhook_dl-queue': false,
      },
    };
  }
}
