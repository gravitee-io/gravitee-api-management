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

import { Component, inject, OnInit } from '@angular/core';
import { map, shareReplay } from 'rxjs/operators';
import { ReplaySubject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import {
  WebhookLogsListComponent,
  WebhookLogsQuickFiltersComponent,
  WebhookLogDetailsDrawerComponent,
  WebhookSettingsDialogComponent,
} from './components';
import { ApiNavigationModule } from '../../api-navigation/api-navigation.module';
import { ReportingDisabledBannerComponent } from '../components/reporting-disabled-banner';

import { ApiV4 } from '../../../../entities/management-api-v2';
import { GioTableWrapperPagination } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { WebhookLog, WebhookLogsResponse, WebhookFilters } from './models';
import { LogFiltersInitialValues } from '../runtime-logs/models';

@Component({
  selector: 'webhook-logs',
  templateUrl: './webhook-logs.component.html',
  styleUrls: ['./webhook-logs.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    GioBannerModule,
    WebhookLogsListComponent,
    WebhookLogsQuickFiltersComponent,
    WebhookLogDetailsDrawerComponent,
    ApiNavigationModule,
    ReportingDisabledBannerComponent,
  ],
})
export class WebhookLogsComponent implements OnInit {
  private activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private apiService = inject(ApiV2Service);
  private dialog = inject(MatDialog);
  private api$ = this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(shareReplay(1));

  isReportingDisabled$ = this.api$.pipe(
    map((api: ApiV4) => !api.analytics.enabled || (!api.analytics.logging?.mode?.endpoint && !api.analytics.logging?.mode?.entrypoint)),
  );
  webhookLogsSubject$ = new ReplaySubject<WebhookLogsResponse>(1);
  webhookLogsData: WebhookLogsResponse | null = null;
  private allLogs: WebhookLog[] = [];
  private filteredLogs: WebhookLog[] = [];
  applicationsOptions: { id: string; name: string }[] = [];
  callbackUrlOptions: string[] = [];
  quickFiltersInitialValues: LogFiltersInitialValues | null = null;
  loading = true;
  selectedLogForDetails: WebhookLog | null = null;
  showDetailsDrawer = false;

  ngOnInit(): void {
    // TODO: Replace with real backend API call

    const webhookData: WebhookLogsResponse = {
      pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 5, totalCount: 5 },
      data: [
        {
          apiId: this.activatedRoute.snapshot.params.apiId,
          requestId: 'req-1',
          timestamp: '2025-06-15T12:00:00.000Z',
          method: 'POST' as any,
          status: 200,
          application: { id: 'app-1', name: 'Acme Warehouse Service', type: 'SIMPLE' as any, apiKeyMode: 'UNSPECIFIED' as any },
          plan: { id: 'plan-1', name: 'Webhook Plan', mode: 'PUSH' as any },
          requestEnded: true,
          gatewayResponseTime: 2800,
          uri: 'https://warehouse.acme.com/webhooks/fulfillment',
          endpoint: 'https://warehouse.acme.com/webhooks/fulfillment',
          callbackUrl: 'https://warehouse.acme.com/webhooks/fulfillment',
          duration: '2.8 s',
          additionalMetrics: {
            long_webhook_responseTime: 2800,
            int_webhook_status: 200,
            string_webhook_retry_timeline: '[]',
            int_webhook_retry_count: 0,
            int_webhook_request_method: 'POST',
            string_webhook_request_body: JSON.stringify(
              {
                id: 'evt_01J8M3TK9SP9WZ8B9K8Q5N9SX6',
                type: 'order.fulfillment.failed',
                created_at: '2025-06-16T10:35:02Z',
                data: {
                  order_id: 'ORD-103221',
                  reason: 'warehouse_timeout',
                  attempted_at: '2025-06-16T10:34:59Z',
                  items: [
                    { sku: 'SKU-RED-42', qty: 1 },
                    { sku: 'SKU-BLK-11', qty: 2 },
                  ],
                },
                meta: {
                  source_ip: '52.31.14.88',
                  region: 'eu-west-1',
                  version: '2025-05-21',
                },
              },
              null,
              2,
            ),
            long_webhook_payload_size: 1024,
            string_webhook_last_error: null,
            string_webhook_request_headers: JSON.stringify(
              {
                'Content-Type': 'application/json',
                Authorization: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
                'X-Request-ID': 'req-abc-123-def-456',
                'X-Correlation-ID': 'corr-789-xyz-012',
                'User-Agent': 'Gravitee.io-Webhook/4.0',
                'X-Webhook-Signature': 'sha256=5d41402abc4b2a76b9719d911017c592',
                Accept: 'application/json',
              },
              null,
              2,
            ),
            string_webhook_response_body: JSON.stringify(
              {
                success: true,
                orderId: 'ORD-103221',
                processed_at: '2025-06-16T10:35:05Z',
                warehouse: {
                  id: 'WH-001',
                  location: 'Dublin',
                  status: 'acknowledged',
                },
                next_actions: ['notify_customer', 'update_inventory', 'log_incident'],
              },
              null,
              2,
            ),
            string_webhook_response_headers: JSON.stringify(
              {
                'Content-Type': 'application/json',
                'X-Response-ID': 'resp-xyz-789',
                'X-Process-Time': '2745ms',
                'Cache-Control': 'no-cache',
                Date: 'Mon, 16 Jun 2025 10:35:05 GMT',
                Server: 'nginx/1.21.0',
              },
              null,
              2,
            ),
            string_webhook_url: 'https://warehouse.acme.com/webhooks/fulfillment',
            keyword_webhook_application_id: 'app-1',
            keyword_webhook_subscription_id: 'sub-1',
            long_webhook_request_timestamp: new Date('2025-06-15T12:00:00.000Z').getTime(),
            boolean_webhook_dl_queue: false,
          },
        },
        {
          apiId: this.activatedRoute.snapshot.params.apiId,
          requestId: 'req-2',
          timestamp: '2025-06-16T13:15:00.000Z',
          method: 'POST' as any,
          status: 500,
          application: { id: 'app-2', name: 'Acme Finance Service', type: 'SIMPLE' as any, apiKeyMode: 'UNSPECIFIED' as any },
          plan: { id: 'plan-1', name: 'Webhook Plan', mode: 'PUSH' as any },
          requestEnded: true,
          gatewayResponseTime: 980,
          uri: 'https://finance.acme.com/webhooks/payment-status',
          endpoint: 'https://finance.acme.com/webhooks/payment-status',
          callbackUrl: 'https://finance.acme.com/webhooks/payment-status',
          duration: '980 ms',
          additionalMetrics: {
            long_webhook_responseTime: 980,
            int_webhook_status: 500,
            string_webhook_retry_timeline:
              '[{"attempt":1,"timestamp":1718542500000,"status":500},{"attempt":2,"timestamp":1718542505000,"status":500}]',
            int_webhook_retry_count: 2,
            int_webhook_request_method: 'POST',
            string_webhook_request_body: '{"paymentId":"PAY-67890","amount":150.00}',
            long_webhook_payload_size: 512,
            string_webhook_last_error: 'Internal Server Error: Payment service unavailable',
            string_webhook_request_headers: '{"Content-Type":"application/json","X-Request-ID":"abc-123"}',
            string_webhook_response_body: '{"error":"Internal Server Error"}',
            string_webhook_response_headers: '{"Content-Type":"application/json","X-Error-Code":"PAYMENT_SVC_DOWN"}',
            string_webhook_url: 'https://finance.acme.com/webhooks/payment-status',
            keyword_webhook_application_id: 'app-2',
            keyword_webhook_subscription_id: 'sub-2',
            long_webhook_request_timestamp: new Date('2025-06-16T13:15:00.000Z').getTime(),
            boolean_webhook_dl_queue: true,
          },
        },
        {
          apiId: this.activatedRoute.snapshot.params.apiId,
          requestId: 'req-3',
          timestamp: '2025-06-17T14:30:00.000Z',
          method: 'POST' as any,
          status: 200,
          application: { id: 'app-3', name: 'Acme Inventory Control', type: 'SIMPLE' as any, apiKeyMode: 'UNSPECIFIED' as any },
          plan: { id: 'plan-1', name: 'Webhook Plan', mode: 'PUSH' as any },
          requestEnded: true,
          gatewayResponseTime: 155,
          uri: 'https://inventory.acme.com/webhooks/stock-updates',
          endpoint: 'https://inventory.acme.com/webhooks/stock-updates',
          callbackUrl: 'https://inventory.acme.com/webhooks/stock-updates',
          duration: '155 ms',
          additionalMetrics: {
            long_webhook_responseTime: 155,
            int_webhook_status: 200,
            string_webhook_retry_timeline: '[]',
            int_webhook_retry_count: 0,
            int_webhook_request_method: 'POST',
            string_webhook_request_body: '{"sku":"INV-999","stock":42}',
            long_webhook_payload_size: 256,
            string_webhook_last_error: null,
            string_webhook_request_headers: '{"Content-Type":"application/json"}',
            string_webhook_response_body: '{"acknowledged":true}',
            string_webhook_response_headers: '{"Content-Type":"application/json"}',
            string_webhook_url: 'https://inventory.acme.com/webhooks/stock-updates',
            keyword_webhook_application_id: 'app-3',
            keyword_webhook_subscription_id: 'sub-3',
            long_webhook_request_timestamp: new Date('2025-06-17T14:30:00.000Z').getTime(),
            boolean_webhook_dl_queue: false,
          },
        },
        {
          apiId: this.activatedRoute.snapshot.params.apiId,
          requestId: 'req-4',
          timestamp: '2025-06-18T15:45:00.000Z',
          method: 'POST' as any,
          status: 500,
          application: { id: 'app-4', name: 'Acme CRM', type: 'SIMPLE' as any, apiKeyMode: 'UNSPECIFIED' as any },
          plan: { id: 'plan-1', name: 'Webhook Plan', mode: 'PUSH' as any },
          requestEnded: true,
          gatewayResponseTime: 1600,
          uri: 'https://crm.acme.com/webhooks/customer-notify',
          endpoint: 'https://crm.acme.com/webhooks/customer-notify',
          callbackUrl: 'https://crm.acme.com/webhooks/customer-notify',
          duration: '1.6 s',
          additionalMetrics: {
            long_webhook_responseTime: 1600,
            int_webhook_status: 500,
            string_webhook_retry_timeline: '[{"attempt":1,"timestamp":1718637900000,"status":500}]',
            int_webhook_retry_count: 1,
            int_webhook_request_method: 'POST',
            string_webhook_request_body: '{"customerId":"CUST-456","notification":"Order shipped"}',
            long_webhook_payload_size: 768,
            string_webhook_last_error: 'Connection timeout after 1500ms',
            string_webhook_request_headers: '{"Content-Type":"application/json","X-Correlation-ID":"crm-789"}',
            string_webhook_response_body: null,
            string_webhook_response_headers: null,
            string_webhook_url: 'https://crm.acme.com/webhooks/customer-notify',
            keyword_webhook_application_id: 'app-4',
            keyword_webhook_subscription_id: 'sub-4',
            long_webhook_request_timestamp: new Date('2025-06-18T15:45:00.000Z').getTime(),
            boolean_webhook_dl_queue: true,
          },
        },
        {
          apiId: this.activatedRoute.snapshot.params.apiId,
          requestId: 'req-5',
          timestamp: '2025-06-19T17:00:00.000Z',
          method: 'POST' as any,
          status: 500,
          application: { id: 'app-5', name: 'Acme Monitoring', type: 'SIMPLE' as any, apiKeyMode: 'UNSPECIFIED' as any },
          plan: { id: 'plan-1', name: 'Webhook Plan', mode: 'PUSH' as any },
          requestEnded: true,
          gatewayResponseTime: 340,
          uri: 'https://monitoring.acme.com/webhooks/store-environment',
          endpoint: 'https://monitoring.acme.com/webhooks/store-environment',
          callbackUrl: 'https://monitoring.acme.com/webhooks/store-environment',
          duration: '340 ms',
          additionalMetrics: {
            long_webhook_responseTime: 340,
            int_webhook_status: 500,
            string_webhook_retry_timeline:
              '[{"attempt":1,"timestamp":1718815200000,"status":503},{"attempt":2,"timestamp":1718815205000,"status":500}]',
            int_webhook_retry_count: 2,
            int_webhook_request_method: 'POST',
            string_webhook_request_body: '{"sensorId":"TEMP-01","reading":22.5}',
            long_webhook_payload_size: 384,
            string_webhook_last_error: 'Service Unavailable: Monitoring backend overloaded',
            string_webhook_request_headers: '{"Content-Type":"application/json","X-Sensor-ID":"TEMP-01"}',
            string_webhook_response_body: '{"error":"Service Unavailable"}',
            string_webhook_response_headers: '{"Content-Type":"application/json","Retry-After":"60"}',
            string_webhook_url: 'https://monitoring.acme.com/webhooks/store-environment',
            keyword_webhook_application_id: 'app-5',
            keyword_webhook_subscription_id: 'sub-5',
            long_webhook_request_timestamp: new Date('2025-06-19T17:00:00.000Z').getTime(),
            boolean_webhook_dl_queue: true,
          },
        },
      ],
    };

    // Build initial values from query params to mimic runtime logs behavior
    const qp = this.activatedRoute.snapshot.queryParams;
    const initialStatuses: string[] = qp?.statuses ? qp.statuses.split(',') : [];
    const initialApplications: string[] = qp?.applicationIds ? qp.applicationIds.split(',') : [];
    const initialTimeframe: string | undefined = qp?.timeframe ?? '0';
    const initialSearch: string = qp?.search ?? '';

    this.quickFiltersInitialValues = {
      applications: initialApplications,
      statuses: new Set(initialStatuses.map((s) => Number(s))),
      searchTerm: initialSearch,
      timeframe: initialTimeframe,
    } as unknown as LogFiltersInitialValues;

    this.webhookLogsData = webhookData;
    this.allLogs = webhookData.data;
    this.filteredLogs = [...this.allLogs];
    this.buildFilterOptions();
    this.pushPage(1, webhookData.pagination.perPage);

    this.loading = false;
  }

  /**
   * TODO: Replace with backend API call
   * Currently does client-side pagination on demo data
   */
  paginationUpdated(event: GioTableWrapperPagination): void {
    this.pushPage(event.index, event.size);
    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: { page: event.index, perPage: event.size },
      queryParamsHandling: 'merge',
    });
  }

  refresh(): void {
    // TODO: Implement refresh logic - call backend API to reload webhook logs
    console.log('Refresh clicked');
  }

  /**
   * Client-side filtering for demo data.
   * TODO: Replace with backend API call that sends filter params to server
   */
  onFiltersChanged(filters: WebhookFilters): void {
    const term = filters.searchTerm?.trim().toLowerCase() || '';
    const statuses = filters.status || [];
    const applications = filters.application || [];
    const callbackUrls = filters.callbackUrls || [];
    const timeframe = filters.timeframe;

    this.filteredLogs = this.allLogs.filter((log) => {
      const matchesSearch = !term || this.logMatchesSearchTerm(log, term);
      const matchesStatus = !statuses.length || statuses.includes(String(log.status));
      const matchesApp = !applications.length || applications.includes(log.application?.id);
      const matchesCallback = !callbackUrls.length || callbackUrls.includes(log.callbackUrl || log.uri);
      const matchesTimeframe = this.matchesTimeframe(log.timestamp, timeframe);

      return matchesSearch && matchesStatus && matchesApp && matchesCallback && matchesTimeframe;
    });

    this.webhookLogsData.pagination.totalCount = this.filteredLogs.length;
    this.pushPage(1, this.webhookLogsData.pagination.perPage);

    this.updateUrlParams(term, statuses, applications, callbackUrls, timeframe);
  }

  /**
   * TODO: Remove when backend filtering is implemented
   */
  private logMatchesSearchTerm(log: WebhookLog, term: string): boolean {
    const searchableFields = [log.callbackUrl, log.uri, log.requestId, log.application?.name].filter(Boolean) as string[];

    return searchableFields.some((field) => field.toLowerCase().includes(term));
  }

  private updateUrlParams(search: string, statuses: string[], applications: string[], callbackUrls: string[], timeframe?: string): void {
    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        search: search || null,
        statuses: statuses.length ? statuses.join(',') : null,
        applicationIds: applications.length ? applications.join(',') : null,
        callbackUrls: callbackUrls.length ? callbackUrls.join(',') : null,
        timeframe: timeframe || null,
        page: 1,
      },
      queryParamsHandling: 'merge',
    });
  }

  /**
   * TODO: Remove when backend provides filter options directly
   */
  private buildFilterOptions(): void {
    const appsMap = new Map<string, string>();
    const urlsSet = new Set<string>();

    this.allLogs.forEach((log) => {
      if (log.application?.id && log.application?.name) {
        appsMap.set(log.application.id, log.application.name);
      }
      const url = log.callbackUrl || log.uri;
      if (url) {
        urlsSet.add(url);
      }
    });

    this.applicationsOptions = Array.from(appsMap.entries()).map(([id, name]) => ({ id, name }));
    this.callbackUrlOptions = Array.from(urlsSet);
  }

  /**
   * TODO: Remove this when backend API with server-side pagination is implemented
   */
  private pushPage(page: number, perPage: number) {
    const start = (page - 1) * perPage;
    const end = start + perPage;
    const pageData = this.filteredLogs.slice(start, end);
    const total = this.filteredLogs.length;

    const pagination = {
      page,
      perPage,
      pageItemsCount: pageData.length,
      pageCount: Math.max(1, Math.ceil(total / perPage || 1)),
      totalCount: total,
    };

    this.webhookLogsSubject$.next({ pagination, data: pageData });
  }

  /**
   * TODO: Remove this when backend API filtering is implemented
   */
  private matchesTimeframe(timestampIso: string, timeframe?: string): boolean {
    if (!timeframe || timeframe === '0') {
      return true;
    }
    const durationMs = this.parseTimeframeToMs(timeframe);
    if (!durationMs) {
      return true;
    }
    const logTs = new Date(timestampIso).getTime();
    const referenceNow = this.allLogs.reduce((max, l) => Math.max(max, new Date(l.timestamp).getTime()), 0);
    return logTs >= referenceNow - durationMs && logTs <= referenceNow;
  }

  /**
   * TODO: Remove this when backend API filtering is implemented
   */
  private parseTimeframeToMs(tf: string): number | null {
    const m = tf.match(/^-(\d+)([mhd])$/);
    if (!m) {
      return null;
    }
    const value = Number(m[1]);
    const unit = m[2];
    const minute = 60 * 1000;
    const hour = 60 * minute;
    const day = 24 * hour;
    switch (unit) {
      case 'm':
        return value * minute;
      case 'h':
        return value * hour;
      case 'd':
        return value * day;
      default:
        return null;
    }
  }

  openSettingsDialog(): void {
    const dialogRef = this.dialog.open(WebhookSettingsDialogComponent, {
      width: '750px',
      data: this.activatedRoute.snapshot.params.apiId,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result?.saved) {
        console.log('Settings saved successfully');
        // TODO: Implement post-save actions (e.g., show success message, reload data if needed)
      }
    });
  }

  onLogDetailsClicked(log: WebhookLog): void {
    this.selectedLogForDetails = log;
    this.showDetailsDrawer = true;
  }

  closeDetailsDrawer(): void {
    this.showDetailsDrawer = false;
    this.selectedLogForDetails = null;
  }
}
