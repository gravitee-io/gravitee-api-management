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
import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { editor } from 'monaco-editor';
import { GioBannerModule, GioClipboardModule, GioMonacoEditorModule } from '@gravitee/ui-particles-angular';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { EnvLog } from '../../models/env-log.model';
import { EnvLogsDetailsRowComponent } from '../env-logs-details-row/env-logs-details-row.component';
import { EnvironmentLogsService } from '../../../../../services-ngx/environment-logs.service';
import { ApiLogsV2Service } from '../../../../../services-ngx/api-logs-v2.service';
import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';

@Component({
  selector: 'env-logs-details',
  templateUrl: './env-logs-details.component.html',
  styleUrl: './env-logs-details.component.scss',
  imports: [
    DatePipe,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatButtonModule,
    GioBannerModule,
    GioClipboardModule,
    GioMonacoEditorModule,
    EnvLogsDetailsRowComponent,
  ],
  providers: [DatePipe],
  standalone: true,
})
export class EnvLogsDetailsComponent {
  // 1. Injections
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly environmentLogsService = inject(EnvironmentLogsService);
  private readonly apiLogsV2Service = inject(ApiLogsV2Service);
  private readonly apiAnalyticsV2Service = inject(ApiAnalyticsV2Service);
  private readonly datePipe = inject(DatePipe);

  // 2. State — derived from route params via toSignal
  private readonly logId = this.activatedRoute.snapshot.params['logId'] as string | undefined;
  private readonly apiId = this.activatedRoute.snapshot.queryParams['apiId'] as string | undefined;

  private readonly log$ =
    this.logId && this.apiId
      ? this.environmentLogsService.searchLogs({ requestId: this.logId }).pipe(
          switchMap(searchResponse => {
            const overviewLog = searchResponse.data[0];
            if (!overviewLog) {
              return of(undefined);
            }

            return forkJoin({
              overview: of(overviewLog),
              detail: this.apiLogsV2Service.searchConnectionLogDetail(overviewLog.apiId, overviewLog.id).pipe(catchError(() => of(null))),
              metrics: this.apiAnalyticsV2Service.getApiMetricsDetail(overviewLog.apiId, overviewLog.id).pipe(catchError(() => of(null))),
            }).pipe(
              map(({ overview, detail, metrics }) => {
                const envLog: EnvLog = {
                  id: overview.id,
                  apiId: overview.apiId,
                  timestamp: this.datePipe.transform(overview.timestamp, 'medium') ?? overview.timestamp,
                  api: overview.apiId,
                  application: overview.application?.name ?? overview.application?.id ?? '—',
                  method: overview.method ?? '—',
                  path: overview.uri ?? '—',
                  status: overview.status,
                  responseTime: overview.gatewayResponseTime != null ? `${overview.gatewayResponseTime} ms` : '—',
                  gateway: overview.gateway,
                  plan: overview.plan?.name ? { name: overview.plan.name } : undefined,
                  requestEnded: overview.requestEnded,
                  errorKey: overview.errorKey,
                  transactionId: overview.transactionId,
                  requestId: detail?.requestId,
                  clientIdentifier: detail?.clientIdentifier,
                  warnings: overview.warnings?.map(w => ({ key: w.key ?? '' })),
                  entrypointRequest: detail?.entrypointRequest,
                  endpointRequest: detail?.endpointRequest,
                  entrypointResponse: detail?.entrypointResponse,
                  endpointResponse: detail?.endpointResponse,
                  host: metrics?.host,
                  remoteAddress: metrics?.remoteAddress,
                  gatewayResponseTime: metrics?.gatewayResponseTime != null ? `${metrics.gatewayResponseTime} ms` : undefined,
                  endpointResponseTime: metrics?.endpointResponseTime != null ? `${metrics.endpointResponseTime} ms` : undefined,
                  gatewayLatency: metrics?.gatewayLatency != null ? `${metrics.gatewayLatency} ms` : undefined,
                  responseContentLength: metrics?.responseContentLength != null ? `${metrics.responseContentLength}` : undefined,
                  endpoint: metrics?.endpoint ?? overview.endpoint,
                };
                return envLog;
              }),
            );
          }),
        )
      : of(undefined);

  // 3. Computed signals derived from the log signal
  log = toSignal(this.log$, { initialValue: undefined });

  requestHeaders = computed(() => this.formatHeaders(this.log()?.entrypointRequest?.headers));
  requestBody = computed(() => this.log()?.entrypointRequest?.body ?? '');
  gatewayRequestHeaders = computed(() => this.formatHeaders(this.log()?.endpointRequest?.headers));
  gatewayRequestBody = computed(() => this.log()?.endpointRequest?.body ?? '');
  responseHeaders = computed(() => this.formatHeaders(this.log()?.entrypointResponse?.headers));
  responseBody = computed(() => this.log()?.entrypointResponse?.body ?? '');
  gatewayResponseHeaders = computed(() => this.formatHeaders(this.log()?.endpointResponse?.headers));
  gatewayResponseBody = computed(() => this.log()?.endpointResponse?.body ?? '');

  readonly monacoEditorOptions: editor.IStandaloneEditorConstructionOptions = {
    renderLineHighlight: 'none',
    hideCursorInOverviewRuler: true,
    overviewRulerBorder: false,
    occurrencesHighlight: 'off',
    selectionHighlight: false,
    readOnly: true,
    scrollbar: {
      vertical: 'hidden',
      horizontal: 'hidden',
      useShadows: false,
    },
  };

  // 4. Private methods
  private formatHeaders(headers?: Record<string, string[]>): { key: string; value: string }[] {
    if (!headers) {
      return [];
    }

    return Object.entries(headers)
      .map(([key, values]) => ({
        key,
        value: values.join(', '),
      }))
      .sort((a, b) => a.key.localeCompare(b.key));
  }
}
