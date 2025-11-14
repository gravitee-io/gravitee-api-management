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
import { ActivatedRoute, Params, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import moment, { unitOfTime } from 'moment';

import { WebhookLog, WebhookLogsQuickFilters, WebhookLogsQuickFiltersInitialValues, WebhookLogsResponse } from './models';
import { WebhookLogsListComponent } from './components/webhook-logs-list';
import { WebhookSettingsDialogComponent } from './components/webhook-settings-dialog';
import { WebhookLogsQuickFiltersComponent } from './components/webhook-logs-quick-filters';

import { ApiNavigationModule } from '../../api-navigation/api-navigation.module';
import { ReportingDisabledBannerComponent } from '../components/reporting-disabled-banner';
import { ApiV4 } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { DEFAULT_PERIOD, MultiFilter, PERIODS, SimpleFilter } from '../runtime-logs/models';

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
    WebhookLogsQuickFiltersComponent,
    WebhookLogsListComponent,
    WebhookSettingsDialogComponent,
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
    map((api) => {
      const apiV4 = api as ApiV4;
      const loggingMode = apiV4.analytics?.logging?.mode;
      return !apiV4.analytics?.enabled || (!loggingMode?.endpoint && !loggingMode?.entrypoint);
    }),
  );
  webhookLogsSubject$ = new ReplaySubject<WebhookLogsResponse>(1);
  webhookLogsData: WebhookLogsResponse | null = null;
  private allLogs: WebhookLog[] = [];
  private filteredLogs: WebhookLog[] = [];
  quickFiltersInitialValues: WebhookLogsQuickFiltersInitialValues = {};
  statusOptions: number[] = [];
  private readonly periods = PERIODS;
  currentFilters: WebhookLogsQuickFilters = {};
  loading = true;

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
            'string_webhook_request-method': 'POST',
            string_webhook_url: 'https://warehouse.acme.com/webhooks/fulfillment',
            'keyword_webhook_application-id': 'app-1',
            'keyword_webhook_subscription-id': 'sub-1',
            'int_webhook_retry-count': 0,
            'string_webhook_retry-timeline': '[]',
            'string_webhook_last-error': null,
            'long_webhook_request-timestamp': new Date('2025-06-15T12:00:00.000Z').getTime(),
            'string_webhook_request-headers': JSON.stringify(
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
            'string_webhook_request-body': JSON.stringify(
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
                  region: 'eu-west-1',
                  version: '2025-05-21',
                },
              },
              null,
              2,
            ),
            'long_webhook_response-time': 2800,
            'int_webhook_response-status': 200,
            'int_webhook_response-payload-size': 1024,
            'string_webhook_response-headers': JSON.stringify(
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
            'string_webhook_response-body': JSON.stringify(
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
            'boolean_webhook_dl-queue': false,
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
            'string_webhook_request-method': 'POST',
            string_webhook_url: 'https://finance.acme.com/webhooks/payment-status',
            'keyword_webhook_application-id': 'app-2',
            'keyword_webhook_subscription-id': 'sub-2',
            'int_webhook_retry-count': 2,
            'string_webhook_retry-timeline':
              '[{"attempt":1,"timestamp":1718542500000,"status":500},{"attempt":2,"timestamp":1718542505000,"status":500}]',
            'string_webhook_last-error': 'Internal Server Error: Payment service unavailable',
            'long_webhook_request-timestamp': new Date('2025-06-16T13:15:00.000Z').getTime(),
            'string_webhook_request-headers': '{"Content-Type":"application/json","X-Request-ID":"abc-123"}',
            'string_webhook_request-body': '{"paymentId":"PAY-67890","amount":150.00}',
            'long_webhook_response-time': 980,
            'int_webhook_response-status': 500,
            'int_webhook_response-payload-size': 512,
            'string_webhook_response-headers': '{"Content-Type":"application/json","X-Error-Code":"PAYMENT_SVC_DOWN"}',
            'string_webhook_response-body': '{"error":"Internal Server Error"}',
            'boolean_webhook_dl-queue': true,
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
            'string_webhook_request-method': 'POST',
            string_webhook_url: 'https://inventory.acme.com/webhooks/stock-updates',
            'keyword_webhook_application-id': 'app-3',
            'keyword_webhook_subscription-id': 'sub-3',
            'int_webhook_retry-count': 0,
            'string_webhook_retry-timeline': '[]',
            'string_webhook_last-error': null,
            'long_webhook_request-timestamp': new Date('2025-06-17T14:30:00.000Z').getTime(),
            'string_webhook_request-headers': '{"Content-Type":"application/json"}',
            'string_webhook_request-body': '{"sku":"INV-999","stock":42}',
            'long_webhook_response-time': 155,
            'int_webhook_response-status': 200,
            'int_webhook_response-payload-size': 256,
            'string_webhook_response-headers': '{"Content-Type":"application/json"}',
            'string_webhook_response-body': '{"acknowledged":true}',
            'boolean_webhook_dl-queue': false,
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
            'string_webhook_request-method': 'POST',
            string_webhook_url: 'https://crm.acme.com/webhooks/customer-notify',
            'keyword_webhook_application-id': 'app-4',
            'keyword_webhook_subscription-id': 'sub-4',
            'int_webhook_retry-count': 1,
            'string_webhook_retry-timeline': '[{"attempt":1,"timestamp":1718637900000,"status":500}]',
            'string_webhook_last-error': 'Connection timeout after 1500ms',
            'long_webhook_request-timestamp': new Date('2025-06-18T15:45:00.000Z').getTime(),
            'string_webhook_request-headers': '{"Content-Type":"application/json","X-Correlation-ID":"crm-789"}',
            'string_webhook_request-body': '{"customerId":"CUST-456","notification":"Order shipped"}',
            'long_webhook_response-time': 1600,
            'int_webhook_response-status': 500,
            'int_webhook_response-payload-size': 768,
            'string_webhook_response-headers': null,
            'string_webhook_response-body': null,
            'boolean_webhook_dl-queue': true,
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
            'string_webhook_request-method': 'POST',
            string_webhook_url: 'https://monitoring.acme.com/webhooks/store-environment',
            'keyword_webhook_application-id': 'app-5',
            'keyword_webhook_subscription-id': 'sub-5',
            'int_webhook_retry-count': 2,
            'string_webhook_retry-timeline':
              '[{"attempt":1,"timestamp":1718815200000,"status":503},{"attempt":2,"timestamp":1718815205000,"status":500}]',
            'string_webhook_last-error': 'Service Unavailable: Monitoring backend overloaded',
            'long_webhook_request-timestamp': new Date('2025-06-19T17:00:00.000Z').getTime(),
            'string_webhook_request-headers': '{"Content-Type":"application/json","X-Sensor-ID":"TEMP-01"}',
            'string_webhook_request-body': '{"sensorId":"TEMP-01","reading":22.5}',
            'long_webhook_response-time': 340,
            'int_webhook_response-status': 500,
            'int_webhook_response-payload-size': 384,
            'string_webhook_response-headers': '{"Content-Type":"application/json","Retry-After":"60"}',
            'string_webhook_response-body': '{"error":"Service Unavailable"}',
            'boolean_webhook_dl-queue': true,
          },
        },
      ],
    };

    // Build initial values from query params to mimic runtime logs behavior
    const qp = this.activatedRoute.snapshot.queryParams;
    const initialStatuses = qp?.statuses
      ? qp.statuses
          .split(',')
          .map((status: string) => Number(status))
          .filter((value) => !Number.isNaN(value))
      : [];
    const initialSearch: string = qp?.search ?? '';
    const initialApplicationsIds: string[] = qp?.applicationIds ? qp.applicationIds.split(',').filter(Boolean) : [];
    const initialPeriod = this.buildInitialPeriod(qp?.period);

    this.webhookLogsData = webhookData;
    this.allLogs = webhookData.data;
    this.buildStatusOptions(initialStatuses);
    const initialApplications = this.buildInitialApplications(initialApplicationsIds);

    this.quickFiltersInitialValues = {
      searchTerm: initialSearch || undefined,
      statuses: initialStatuses.length ? initialStatuses : undefined,
      applications: initialApplications?.length ? initialApplications : undefined,
      period: initialPeriod?.value !== DEFAULT_PERIOD.value ? initialPeriod : undefined,
    };

    this.applyFilters(this.quickFiltersInitialValues);
    this.loading = false;

    this.openSettingsDialogIfRequested();
  }

  /**
   * TODO: Replace with backend API call
   * Currently does client-side pagination on demo data
   */
  // paginationUpdated(event: GioTableWrapperPagination): void {
  //   this.pushPage(event.index, event.size);
  //   this.router.navigate(['.'], {
  //     relativeTo: this.activatedRoute,
  //     queryParams: { page: event.index, perPage: event.size },
  //     queryParamsHandling: 'merge',
  //   });
  // }

  refresh(): void {
    this.applyFilters(this.currentFilters);
  }

  onFiltersChanged(filters: WebhookLogsQuickFilters): void {
    this.applyFilters(filters);
  }

  private applyFilters(filters: WebhookLogsQuickFilters = {}): void {
    if (!this.webhookLogsData) {
      return;
    }

    const normalizedFilters = this.normalizeFilters(filters);
    this.currentFilters = normalizedFilters;

    const searchTerm = normalizedFilters.searchTerm?.toLowerCase();
    const statuses = normalizedFilters.statuses;
    const applications = normalizedFilters.applications?.map((app) => app.value);
    const periodRange = normalizedFilters.period ? this.preparePeriodRange(normalizedFilters.period) : undefined;

    this.filteredLogs = this.allLogs.filter((log) => {
      const matchesSearch = !searchTerm || this.logMatchesSearchTerm(log, searchTerm);
      const matchesStatus = !statuses?.length || statuses.includes(log.status);
      const matchesApplication = !applications?.length || applications.includes(log.application?.id);
      const matchesPeriod = !periodRange || this.logMatchesPeriod(log, periodRange);
      return matchesSearch && matchesStatus && matchesApplication && matchesPeriod;
    });

    this.webhookLogsData = {
      ...this.webhookLogsData,
      pagination: {
        ...this.webhookLogsData.pagination,
        totalCount: this.filteredLogs.length,
      },
    };

    const perPage = this.webhookLogsData.pagination.perPage ?? (this.filteredLogs.length || 10);
    this.pushPage(1, perPage);
    this.updateUrlParams(normalizedFilters);
  }

  private buildStatusOptions(initialStatuses: number[] = []): void {
    const statusesSet = new Set<number>(initialStatuses);
    this.allLogs.forEach((log) => statusesSet.add(log.status));
    this.statusOptions = Array.from(statusesSet).sort((a, b) => a - b);
  }

  private normalizeFilters(filters: WebhookLogsQuickFilters = {}): WebhookLogsQuickFilters {
    const searchTerm = filters?.searchTerm?.trim();
    const statuses = filters?.statuses?.filter((status) => !Number.isNaN(status));
    const uniqueStatuses = statuses?.length ? Array.from(new Set(statuses)) : undefined;

    const applications = filters?.applications?.length
      ? filters.applications.filter(
          (application, index, array) => application && array.findIndex((app) => app.value === application.value) === index,
        )
      : undefined;

    const normalized: WebhookLogsQuickFilters = {
      searchTerm: searchTerm?.length ? searchTerm : undefined,
      statuses: uniqueStatuses,
      applications,
    };
    const normalizedPeriod = this.normalizePeriod(filters?.period);
    if (normalizedPeriod) {
      normalized.period = normalizedPeriod;
    }
    return normalized;
  }

  private normalizePeriod(period?: SimpleFilter): SimpleFilter | undefined {
    if (!period) {
      return undefined;
    }
    const match = this.periods.find((available) => available.value === period.value);
    return match && match.value !== DEFAULT_PERIOD.value ? match : undefined;
  }

  private preparePeriodRange(period: SimpleFilter): { from: number; to: number } | undefined {
    if (!period || period.value === DEFAULT_PERIOD.value) {
      return undefined;
    }
    const value = period.value;
    const operation = value.charAt(0);
    const unit = value.charAt(value.length - 1) as unitOfTime.DurationConstructor;
    const duration = Number(value.substring(1, value.length - 1));
    if (Number.isNaN(duration) || !['-', '+'].includes(operation)) {
      return undefined;
    }
    const now = moment();
    const fromMoment = operation === '-' ? now.clone().subtract(duration, unit) : now.clone().add(duration, unit);
    return { from: fromMoment.valueOf(), to: now.valueOf() };
  }

  private logMatchesPeriod(log: WebhookLog, range: { from: number; to: number }): boolean {
    if (!log?.timestamp) {
      return false;
    }
    const timestamp = new Date(log.timestamp).getTime();
    if (Number.isNaN(timestamp)) {
      return false;
    }
    return timestamp >= range.from && timestamp <= range.to;
  }

  private logMatchesSearchTerm(log: WebhookLog, term: string): boolean {
    const searchableFields = [log.callbackUrl, log.uri, log.requestId, log.application?.name].filter(Boolean) as string[];
    return searchableFields.some((field) => field.toLowerCase().includes(term));
  }

  private updateUrlParams(filters: WebhookLogsQuickFilters): void {
    const nextParams = {
      search: filters.searchTerm ?? null,
      statuses: filters.statuses?.length ? filters.statuses.join(',') : null,
      applicationIds: filters.applications?.length ? filters.applications.map((app) => app.value).join(',') : null,
      period: filters.period?.value ?? null,
      page: 1,
    };

    if (this.areQueryParamsEqual(this.activatedRoute.snapshot.queryParams, nextParams)) {
      return;
    }

    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: nextParams,
      queryParamsHandling: 'merge',
    });
  }

  private areQueryParamsEqual(
    current: Params,
    next: { search: string | null; statuses: string | null; applicationIds: string | null; period: string | null; page: number },
  ): boolean {
    const currentSearch = current?.search ?? null;
    const currentStatuses = current?.statuses ?? null;
    const currentPage = current?.page ? Number(current.page) : null;
    const currentApplications = current?.applicationIds ?? null;
    const currentPeriod = current?.period ?? null;

    return (
      currentSearch === next.search &&
      currentStatuses === next.statuses &&
      currentApplications === next.applicationIds &&
      currentPeriod === next.period &&
      (currentPage ?? 1) === next.page
    );
  }

  private pushPage(page: number, perPage: number): void {
    const safePerPage = perPage > 0 ? perPage : this.filteredLogs.length || 1;
    const start = (page - 1) * safePerPage;
    const end = start + safePerPage;
    const pageData = this.filteredLogs.slice(start, end);
    const total = this.filteredLogs.length;

    const pagination = {
      page,
      perPage: safePerPage,
      pageItemsCount: pageData.length,
      pageCount: Math.max(1, Math.ceil(total / safePerPage || 1)),
      totalCount: total,
    };

    this.webhookLogsSubject$.next({ pagination, data: pageData });
  }

  private buildInitialPeriod(periodValue?: string): SimpleFilter {
    if (!periodValue) {
      return DEFAULT_PERIOD;
    }
    return this.periods.find((period) => period.value === periodValue) ?? DEFAULT_PERIOD;
  }

  private buildInitialApplications(ids: string[]): MultiFilter | undefined {
    if (!ids?.length) {
      return undefined;
    }
    const applicationMap = this.allLogs.reduce((map, log) => {
      if (log.application?.id && log.application?.name) {
        map.set(log.application.id, log.application.name);
      }
      return map;
    }, new Map<string, string>());

    const applications = ids.map((id) => ({ value: id, label: applicationMap.get(id) ?? id }));
    return applications.length ? applications : undefined;
  }

  openSettingsDialog(): void {
    const dialogRef = this.dialog.open(WebhookSettingsDialogComponent, {
      width: '750px',
      data: this.activatedRoute.snapshot.params.apiId,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result?.saved) {
        // TODO: Implement post-save actions (e.g., show success message, reload data if needed)
      }
    });
  }

  onLogDetailsClicked(log: WebhookLog): void {
    if (!log?.requestId) {
      return;
    }

    this.router.navigate(['./', log.requestId], { relativeTo: this.activatedRoute });
  }

  openSettings() {
    this.openSettingsDialog();
  }

  private openSettingsDialogIfRequested(): void {
    if (this.activatedRoute.snapshot.queryParamMap.get('openSettings') === 'true') {
      this.openSettingsDialog();
      this.router.navigate([], {
        relativeTo: this.activatedRoute,
        queryParams: { openSettings: null },
        queryParamsHandling: 'merge',
      });
    }
  }
}
