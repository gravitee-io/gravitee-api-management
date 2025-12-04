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
import { isNumber, isObject } from 'angular';

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTableModule } from '@angular/material/table';
import { editor } from 'monaco-editor';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { GioBannerModule, GioClipboardModule, GioMonacoEditorModule } from '@gravitee/ui-particles-angular';

import { FormatDurationPipe } from '../../../../shared/pipes/format-duration.pipe';
import { ConnectionLogDiagnostic } from '../../../../entities/management-api-v2';
import { WebhookLog, WebhookAdditionalMetrics } from '../webhook-logs/models/webhook-logs.models';
import { mapConnectionLogToWebhookLog } from '../webhook-logs/utils/webhook-metrics.utils';
import { ApiLogsV2Service } from '../../../../services-ngx/api-logs-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

type OverviewItem = { label: string; value: string | number | boolean; variant?: 'success' | 'warning' | 'error' };

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

interface RetryTimelineItem {
  attempt?: number;
  timestamp: number | string;
  duration: number;
  status: number;
  reason?: string;
}

interface ParsedHeaders {
  [key: string]: string | string[];
}

interface ConnectionFailureDetails {
  errorKey?: string | null;
  componentType?: string | null;
  componentName?: string | null;
  message?: string | null;
  diagnostics: ConnectionLogDiagnostic[];
  lastError?: string | null;
}

const LAST_ERROR_KEY: keyof WebhookAdditionalMetrics = 'string_webhook_last-error';
const RETRY_TIMELINE_KEY: keyof WebhookAdditionalMetrics = 'json_webhook_retry-timeline';
const REQ_HEADERS_KEY: keyof WebhookAdditionalMetrics = 'json_webhook_req-headers';
const RESP_HEADERS_KEY: keyof WebhookAdditionalMetrics = 'json_webhook_resp-headers';
const REQ_BODY_KEY: keyof WebhookAdditionalMetrics = 'string_webhook_req-body';
const RESP_BODY_KEY: keyof WebhookAdditionalMetrics = 'string_webhook_resp-body';
const RESP_BODY_SIZE_KEY: keyof WebhookAdditionalMetrics = 'int_webhook_resp-body-size';

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
export class WebhookLogsDetailsComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly apiLogsService = inject(ApiLogsV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly requestId = this.activatedRoute.snapshot.params.requestId;
  readonly apiId = this.activatedRoute.snapshot.params.apiId;
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

  ngOnInit(): void {
    if (!this.requestId || !this.apiId) {
      this.snackBarService.error('Missing request ID or API ID');
      return;
    }

    const logFromState = this.getWebhookLogFromNavigationState();
    if (logFromState && logFromState.requestId === this.requestId) {
      this.applyLog(logFromState);
      return;
    }

    this.loadWebhookLog();
  }

  private getWebhookLogFromNavigationState(): WebhookLog | null {
    const navigation = this.router.getCurrentNavigation();
    const fromNavigation = navigation?.extras?.state?.['webhookLog'];
    const fromHistory = history.state?.['webhookLog'];
    const candidate = fromNavigation ?? fromHistory;

    if (candidate && isObject(candidate) && 'requestId' in candidate) {
      return candidate as WebhookLog;
    }

    return null;
  }

  private loadWebhookLog(): void {
    this.apiLogsService
      .searchApiMessageLogs(this.apiId, {
        requestId: this.requestId,
        connectorId: 'webhook',
        connectorType: 'entrypoint',
        operation: 'subscribe',
        page: 1,
        perPage: 1,
      })
      .pipe(
        catchError(() => {
          this.snackBarService.error('Failed to load webhook log details');
          return of({
            data: [],
            pagination: { page: 1, perPage: 1, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
          });
        }),
      )
      .subscribe((response) => {
        if (response.data && response.data.length > 0) {
          const webhookLog = mapConnectionLogToWebhookLog(response.data[0]);
          this.applyLog(webhookLog);
        } else {
          this.snackBarService.error('Webhook log not found');
          this.applyLog(null);
        }
        this.cdr.markForCheck();
      });
  }

  formatAttemptTimestamp(timestamp: string): string {
    return this.formatDateTime(timestamp);
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

    if (log.status === 0) {
      this.connectionFailure = {
        errorKey: log.errorKey ?? null,
        componentType: log.errorComponentType ?? null,
        componentName: log.errorComponentName ?? null,
        message: log.message ?? null,
        diagnostics: log.warnings ?? [],
        lastError: typeof log.additionalMetrics?.[LAST_ERROR_KEY] === 'string' ? log.additionalMetrics[LAST_ERROR_KEY] : null,
      };
    } else {
      this.connectionFailure = null;
    }

    this.deliveryAttemptsDataSource = this.buildDeliveryAttempts(log);
    const attemptsCount = this.deliveryAttemptsDataSource.length || (log.additionalMetrics?.['int_webhook_retry-count'] ?? 1);

    this.overviewRequest = [
      { label: 'Date', value: this.formatDateTime(log.timestamp) },
      { label: 'Method', value: log.method },
      { label: 'Attempts', value: attemptsCount },
      { label: 'Callback URL', value: log.callbackUrl ?? log.uri },
    ];

    this.overviewResponse = [
      { label: 'Status', value: log.status },
      {
        label: 'Total duration',
        value: log.duration || `${(log.gatewayResponseTime / 1000).toFixed(1)} s`,
      },
      { label: 'Payload size', value: this.formatPayloadSize(log) },
      { label: 'Last error', value: log.additionalMetrics?.[LAST_ERROR_KEY] ?? '—' },
    ];

    this.requestHeaders = this.parseHeaders(
      typeof log.additionalMetrics?.[REQ_HEADERS_KEY] === 'string' ? log.additionalMetrics?.[REQ_HEADERS_KEY] : null,
    );

    this.responseHeaders = this.parseHeaders(
      typeof log.additionalMetrics?.[RESP_HEADERS_KEY] === 'string' ? log.additionalMetrics?.[RESP_HEADERS_KEY] : null,
    );
    this.requestBody = typeof log.additionalMetrics?.[REQ_BODY_KEY] === 'string' ? log.additionalMetrics[REQ_BODY_KEY] : '';

    this.responseBody = typeof log.additionalMetrics?.[RESP_BODY_KEY] === 'string' ? log.additionalMetrics[RESP_BODY_KEY] : '';

    this.cdr.markForCheck();
  }

  private buildDeliveryAttempts(log: WebhookLog): DeliveryAttempt[] {
    const rawTimeline = log.additionalMetrics?.[RETRY_TIMELINE_KEY];

    if (typeof rawTimeline !== 'string' || rawTimeline.trim() === '' || rawTimeline === '[]') {
      return [this.createSingleDeliveryAttempt(log)];
    }

    let parsed: RetryTimelineItem[];

    try {
      parsed = JSON.parse(rawTimeline) as RetryTimelineItem[];
    } catch {
      return [this.createSingleDeliveryAttempt(log)];
    }

    if (!Array.isArray(parsed) || parsed.length === 0) {
      return [this.createSingleDeliveryAttempt(log)];
    }

    const lastErrorRaw = log.additionalMetrics?.[LAST_ERROR_KEY];
    const lastError = typeof lastErrorRaw === 'string' ? lastErrorRaw : undefined;

    return parsed.map((item, index): DeliveryAttempt => {
      let reason: string | undefined;

      if (typeof item.reason === 'string') {
        reason = item.reason;
      } else if (lastError) {
        reason = lastError;
      } else {
        reason = 'Initial delivery attempt';
      }

      return {
        attempt: item.attempt ?? index + 1,
        timestamp: this.normalizeAttemptTimestamp(item.timestamp, log.timestamp),
        duration: item.duration ?? log.gatewayResponseTime,
        status: item.status ?? log.status,
        reason,
      };
    });
  }

  private normalizeAttemptTimestamp(raw: string | number | undefined, fallback: string): string {
    if (typeof raw === 'string') {
      return raw;
    }

    if (isNumber(raw)) {
      const date = new Date(raw);
      return Number.isNaN(date.getTime()) ? fallback : date.toISOString();
    }

    return fallback;
  }

  private createSingleDeliveryAttempt(log: WebhookLog): DeliveryAttempt {
    const rawLastError = log.additionalMetrics?.[LAST_ERROR_KEY];
    const reason = typeof rawLastError === 'string' ? rawLastError : 'Initial delivery attempt';

    return {
      attempt: 1,
      timestamp: log.timestamp,
      duration: log.gatewayResponseTime,
      status: log.status,
      reason,
    };
  }

  private parseHeaders(raw?: string | null): HeaderItem[] {
    if (!raw) {
      return [];
    }
    try {
      const parsed: ParsedHeaders = JSON.parse(raw);
      return Object.entries(parsed).map(([key, value]) => ({
        key,
        value: Array.isArray(value) ? value.join(', ') : value,
      }));
    } catch {
      return [];
    }
  }

  private formatPayloadSize(log: WebhookLog): string {
    const size = log.additionalMetrics?.[RESP_BODY_SIZE_KEY];
    if (size === null || size === undefined) {
      return '—';
    }
    if (isNumber(size) && size >= 1024) {
      return `${(size / 1024).toFixed(1)} KB`;
    }
    return `${size} B`;
  }

  private formatDateTime(value: string | number): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return isNumber(value) ? String(value) : value;
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
