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

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTableModule } from '@angular/material/table';
import { editor } from 'monaco-editor';
import { GioBannerModule, GioClipboardModule, GioMonacoEditorModule, MonacoEditorLanguageConfig } from '@gravitee/ui-particles-angular';

import { FormatDurationPipe } from '../../../../shared/pipes/format-duration.pipe';
import { ConnectionLogDiagnostic } from '../../../../entities/management-api-v2';
import { WebhookLog } from '../webhook-logs/models/webhook-logs.models';
import { getWebhookLogMockByRequestId, WEBHOOK_SAMPLE_LOG } from '../webhook-logs/mocks/webhook-logs.mock';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

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
interface ConnectionFailureDetails {
  errorKey?: string | null;
  componentType?: string | null;
  componentName?: string | null;
  message?: string | null;
  diagnostics: ConnectionLogDiagnostic[];
  lastError?: string | null;
}

@Component({
  selector: 'webhook-logs-details',
  templateUrl: './webhook-logs-details.component.html',
  styleUrls: ['./webhook-logs-details.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatExpansionModule,
    MatTableModule,
    GioClipboardModule,
    GioMonacoEditorModule,
    FormatDurationPipe,
    GioTableWrapperModule,
    GioBannerModule,
  ],
})
export class WebhookLogsDetailsComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly requestId = this.activatedRoute.snapshot.params.requestId;
  readonly isNumber = isNumber;

  overviewRequest: OverviewItem[] = [];
  overviewResponse: OverviewItem[] = [];
  deliveryAttemptsDataSource: DeliveryAttempt[] = [];
  readonly displayedColumns: string[] = ['attempt', 'timestamp', 'duration', 'status'];
  requestHeaders: HeaderItem[] = [];
  responseHeaders: HeaderItem[] = [];
  requestBody = '';
  responseBody = '';
  selectedLog: WebhookLog | null = null;
  connectionFailure: ConnectionFailureDetails | null = null;
  languageConfig: MonacoEditorLanguageConfig = { language: 'json', schemas: [] };
  readonly monacoEditorOptions: editor.IStandaloneEditorConstructionOptions = {
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
    // TODO: Backend Integration Required
    // - Replace mock data lookup with actual backend API call to fetch log by requestId
    // - Create/use a service method: getWebhookLogByRequestId(requestId: string, apiId: string)
    // - Handle loading state while fetching log data
    // - Handle error states (log not found, network errors, API errors)
    // - Show appropriate error message or empty state if log cannot be found
    // - Remove dependency on getWebhookLogMockByRequestId and WEBHOOK_SAMPLE_LOG from webhook-logs.mock.ts
    // - Consider using ngOnInit() with async data loading pattern instead of constructor

    // Look up the log by requestId from mock data
    const log = this.findLogByRequestId(this.requestId);
    this.applyLog(log);
  }

  private findLogByRequestId(requestId: string | undefined): WebhookLog | null {
    if (!requestId) {
      return WEBHOOK_SAMPLE_LOG;
    }

    return getWebhookLogMockByRequestId(requestId) ?? WEBHOOK_SAMPLE_LOG;
  }

  formatAttemptTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) {
      return timestamp;
    }
    return date.toLocaleString();
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
      this.resetViewState();
      return;
    }

    this.deliveryAttemptsDataSource = this.buildDeliveryAttempts(log);
    const attemptsCount = this.deliveryAttemptsDataSource.length || (log.additionalMetrics?.['int_webhook_retry-count'] ?? 1);

    this.overviewRequest = [
      { label: 'Date', value: this.formatDate(log.timestamp) },
      { label: 'Method', value: log.method },
      { label: 'Attempts', value: attemptsCount },
      { label: 'Callback URL', value: log.callbackUrl ?? log.uri },
    ];

    this.overviewResponse = [
      { label: 'Status', value: log.status },
      { label: 'Total duration', value: log.duration || `${(log.gatewayResponseTime / 1000).toFixed(1)} s` },
      { label: 'Payload size', value: this.formatPayloadSize(log) },
      { label: 'Last error', value: log.additionalMetrics?.['string_webhook_last-error'] ?? '—' },
    ];

    this.requestHeaders = this.parseHeaders(log.additionalMetrics?.['json_webhook_req-headers']);
    this.responseHeaders = this.parseHeaders(log.additionalMetrics?.['json_webhook_resp-headers']);
    this.requestBody = log.additionalMetrics?.['string_webhook_req-body'] ?? '';
    this.responseBody = log.additionalMetrics?.['string_webhook_resp-body'] ?? '';
    this.connectionFailure = this.buildConnectionFailure(log);
  }

  private buildDeliveryAttempts(log: WebhookLog): DeliveryAttempt[] {
    const rawTimeline = log.additionalMetrics?.['json_webhook_retry-timeline'];
    if (!rawTimeline || rawTimeline === '[]') {
      return [this.createSingleDeliveryAttempt(log)];
    }

    try {
      const parsed = JSON.parse(rawTimeline) as Array<{
        attempt?: number;
        timestamp: number | string;
        duration: number;
        status: number;
        reason?: string;
      }>;

      if (!parsed || parsed.length === 0) {
        return [this.createSingleDeliveryAttempt(log)];
      }

      return parsed.map((item, index) => ({
        attempt: item.attempt ?? index + 1,
        timestamp: isNumber(item.timestamp) ? new Date(item.timestamp).toISOString() : (item.timestamp ?? log.timestamp),
        duration: item.duration ?? log.gatewayResponseTime,
        status: item.status ?? log.status,
        reason: item.reason ?? log.additionalMetrics?.['string_webhook_last-error'] ?? 'Initial delivery attempt',
      }));
    } catch {
      return [this.createSingleDeliveryAttempt(log)];
    }
  }

  private createSingleDeliveryAttempt(log: WebhookLog): DeliveryAttempt {
    return {
      attempt: 1,
      timestamp: log.timestamp,
      duration: log.gatewayResponseTime,
      status: log.status,
      reason: log.additionalMetrics?.['string_webhook_last-error'] ?? 'Initial delivery attempt',
    };
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

  private buildConnectionFailure(log: WebhookLog): ConnectionFailureDetails | null {
    if (log.status !== 0) {
      return null;
    }

    return {
      errorKey: log.errorKey ?? null,
      componentType: log.errorComponentType ?? null,
      componentName: log.errorComponentName ?? null,
      message: log.message ?? null,
      diagnostics: log.warnings ?? [],
      lastError: log.additionalMetrics?.['string_webhook_last-error'] ?? null,
    };
  }

  private formatPayloadSize(log: WebhookLog): string {
    const size = log.additionalMetrics?.['int_webhook_resp-body-size'];
    if (size === null || size === undefined) {
      return '—';
    }
    if (size >= 1024) {
      return `${(size / 1024).toFixed(1)} KB`;
    }
    return `${size} B`;
  }

  private formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString();
  }

  private resetViewState(): void {
    this.overviewRequest = [];
    this.overviewResponse = [];
    this.deliveryAttemptsDataSource = [];
    this.requestHeaders = [];
    this.responseHeaders = [];
    this.requestBody = '';
    this.responseBody = '';
    this.connectionFailure = null;
  }
}
