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
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { EMPTY } from 'rxjs';
import { catchError, debounceTime, switchMap, tap } from 'rxjs/operators';
import moment from 'moment';

import type { EnvLog } from './models/env-log.model';

import { EnvLogsInitialValues } from './models/env-logs-initial-values.model';
import {
  EnvLogsFilterBarComponent,
  EnvLogsFilterValues,
  ENV_LOGS_PERIODS,
} from './components/env-logs-filter-bar/env-logs-filter-bar.component';
import { EnvLogsTableComponent } from './components/env-logs-table/env-logs-table.component';

import { EnvironmentLogsService, EnvironmentApiLog, SearchLogsParam, LogApiType } from '../../../services-ngx/environment-logs.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTableWrapperPagination } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Pagination } from '../../../entities/management-api-v2';
import { GioHeaderComponent } from '../../../shared/components/gio-header/gio-header.component';

const EMPTY_FIELD = '—';
const TAB_QUERY_PARAM = 'tab';
const MESSAGES_TAB_VALUE = 'messages';
const DEFAULT_PER_PAGE = 10;

const API_TYPE_LABELS: Record<LogApiType, string> = {
  HTTP_PROXY: 'HTTP',
  LLM_PROXY: 'LLM',
  MCP_PROXY: 'MCP',
};

export const enum EnvLogsTab {
  NON_MESSAGE_APIS = 0,
  MESSAGES = 1,
}

@Component({
  selector: 'env-logs',
  templateUrl: './env-logs.component.html',
  styleUrl: './env-logs.component.scss',
  imports: [EnvLogsTableComponent, EnvLogsFilterBarComponent, MatCardModule, MatTabsModule, GioBannerModule, GioHeaderComponent],
  providers: [DatePipe],
  standalone: true,
})
export class EnvLogsComponent {
  private readonly environmentLogsService = inject(EnvironmentLogsService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly datePipe = inject(DatePipe);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);

  pagination = signal<Pagination>({ page: 1, perPage: DEFAULT_PER_PAGE, totalCount: 0 });
  filters = signal<EnvLogsFilterValues | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  activeTab = signal<EnvLogsTab>(this.initialTabIndex());

  /** Placeholder: message logs are empty until APIM-13200 wires the backend */
  messageLogs = signal<EnvLog[]>([]);
  messagePagination = signal<Pagination>({ page: 1, perPage: DEFAULT_PER_PAGE, totalCount: 0 });

  messagePaginationWithTotal = computed(() => ({
    ...this.messagePagination(),
    // TODO(APIM-13200): Replace with actual totalCount from message logs response
    totalCount: 0,
  }));

  initialValues = signal<EnvLogsInitialValues | undefined>(this.parseQueryParams());

  private readonly searchParams = computed(() => ({
    pagination: this.pagination(),
    filters: this.filters(),
  }));

  private readonly logsResult$ = toObservable(this.searchParams).pipe(
    debounceTime(0),
    switchMap(params => this.fetchLogs(params)),
  );

  private readonly logsResult = toSignal(this.logsResult$);

  logs = computed(() => {
    const result = this.logsResult();
    return result?.data.map(log => this.mapToEnvLog(log)) ?? [];
  });

  paginationWithTotal = computed(() => ({
    ...this.pagination(),
    totalCount: this.logsResult()?.pagination.totalCount ?? 0,
  }));

  onRefresh() {
    this.pagination.update(prev => ({ ...prev, page: 1 }));
  }

  onFiltersChanged(filters: EnvLogsFilterValues) {
    this.filters.set(filters);
    this.pagination.update(prev => (prev.page === 1 ? prev : { ...prev, page: 1 }));
    this.syncQueryParams();
  }

  onPaginationUpdated(event: GioTableWrapperPagination) {
    this.pagination.update(prev => ({ ...prev, page: event.index, perPage: event.size }));
    this.syncQueryParams();
  }

  onTabChanged(index: number) {
    this.activeTab.set(index);
    this.pagination.update(prev => (prev.page === 1 ? prev : { ...prev, page: 1 }));
    this.messagePagination.update(prev => (prev.page === 1 ? prev : { ...prev, page: 1 }));
    this.syncQueryParams();
  }

  onMessagePaginationUpdated(event: GioTableWrapperPagination) {
    this.messagePagination.update(prev => ({ ...prev, page: event.index, perPage: event.size }));
    this.syncQueryParams();
  }

  private syncQueryParams() {
    const filters = this.filters();
    const pagination = this.activeTab() === EnvLogsTab.MESSAGES ? this.messagePagination() : this.pagination();
    const queryParams: Record<string, string | null> = {
      tab: this.activeTab() === EnvLogsTab.MESSAGES ? MESSAGES_TAB_VALUE : null,
      page: pagination.page > 1 ? String(pagination.page) : null,
      perPage: pagination.perPage === DEFAULT_PER_PAGE ? null : String(pagination.perPage),
      period: filters?.period && filters.period !== '0' ? filters.period : null,
      apiIds: filters?.apiIds?.length ? filters.apiIds.join(',') : null,
      applicationIds: filters?.applicationIds?.length ? filters.applicationIds.join(',') : null,
      methods: filters?.more?.methods?.length ? filters.more.methods.join(',') : null,
      statuses: filters?.more?.statuses?.size ? [...filters.more.statuses].join(',') : null,
      entrypoints: filters?.more?.entrypoints?.length ? filters.more.entrypoints.join(',') : null,
      planIds: filters?.more?.plans?.length ? filters.more.plans.join(',') : null,
      transactionId: filters?.more?.transactionId ?? null,
      requestId: filters?.more?.requestId ?? null,
      uri: filters?.more?.uri ?? null,
      responseTime: filters?.more?.responseTime != null && filters.more.responseTime > 0 ? String(filters.more.responseTime) : null,
      from: filters?.more?.from?.toISOString() ?? null,
      to: filters?.more?.to?.toISOString() ?? null,
      errorKeys: filters?.more?.errorKeys?.length ? filters.more.errorKeys.join(',') : null,
    };

    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams,
      replaceUrl: true,
    });
  }

  private initialTabIndex(): EnvLogsTab {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    return queryParams[TAB_QUERY_PARAM] === MESSAGES_TAB_VALUE ? EnvLogsTab.MESSAGES : EnvLogsTab.NON_MESSAGE_APIS;
  }

  private parseQueryParams(): EnvLogsInitialValues | undefined {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    this.restorePagination(queryParams);

    const nonFilterKeys = new Set(['apiId', 'page', 'perPage', TAB_QUERY_PARAM]);
    const hasAnyFilter = Object.keys(queryParams).some(k => !nonFilterKeys.has(k));
    if (!hasAnyFilter) return undefined;

    const period = queryParams['period'] ? ENV_LOGS_PERIODS.find(p => p.value === queryParams['period']) : undefined;
    const splitOrUndefined = (key: string): string[] | undefined => queryParams[key]?.split(',').filter(Boolean) || undefined;

    return {
      period,
      apiIds: splitOrUndefined('apiIds'),
      applicationIds: splitOrUndefined('applicationIds'),
      methods: splitOrUndefined('methods'),
      statuses: queryParams['statuses'] ? new Set(queryParams['statuses'].split(',').map(Number)) : undefined,
      entrypoints: splitOrUndefined('entrypoints'),
      plans: splitOrUndefined('planIds'),
      transactionId: queryParams['transactionId'] || undefined,
      requestId: queryParams['requestId'] || undefined,
      uri: queryParams['uri'] || undefined,
      responseTime: queryParams['responseTime'] ? Number(queryParams['responseTime']) : undefined,
      from: queryParams['from'] ? moment(queryParams['from']) : undefined,
      to: queryParams['to'] ? moment(queryParams['to']) : undefined,
      errorKeys: splitOrUndefined('errorKeys'),
    };
  }

  private restorePagination(queryParams: Record<string, string>) {
    const page = Number(queryParams['page']);
    if (page > 1) {
      this.pagination.update(prev => ({ ...prev, page }));
    }
    const perPage = Number(queryParams['perPage']);
    if (perPage && perPage !== DEFAULT_PER_PAGE) {
      this.pagination.update(prev => ({ ...prev, perPage }));
    }
  }

  private fetchLogs(params: { pagination: Pagination; filters: EnvLogsFilterValues | null }) {
    this.loading.set(true);
    this.error.set(null);

    return this.environmentLogsService.searchLogs(this.buildSearchParam(params)).pipe(
      tap(() => this.loading.set(false)),
      catchError(err => {
        this.loading.set(false);
        const message =
          err instanceof HttpErrorResponse
            ? `Request failed: ${err.status} ${err.statusText}`
            : 'An unexpected error occurred while loading logs.';
        this.error.set(message);
        this.snackBarService.error(message);
        return EMPTY;
      }),
    );
  }

  private buildSearchParam(params: { pagination: Pagination; filters: EnvLogsFilterValues | null }): SearchLogsParam {
    const { page, perPage } = params.pagination;
    const filters = params.filters;
    return {
      page,
      perPage,
      period: filters?.period,
      from: filters?.more?.from?.toISOString(),
      to: filters?.more?.to?.toISOString(),
      apiIds: filters?.apiIds?.length ? filters.apiIds : undefined,
      applicationIds: filters?.applicationIds?.length ? filters.applicationIds : undefined,
      planIds: filters?.more?.plans?.length ? filters.more.plans : undefined,
      methods: filters?.more?.methods?.length ? filters.more.methods : undefined,
      statuses: filters?.more?.statuses?.size ? [...filters.more.statuses] : undefined,
      entrypoints: filters?.more?.entrypoints?.length ? filters.more.entrypoints : undefined,
      transactionId: filters?.more?.transactionId ?? undefined,
      requestId: filters?.more?.requestId ?? undefined,
      uri: filters?.more?.uri ?? undefined,
      responseTime: filters?.more?.responseTime ?? undefined,
      errorKeys: filters?.more?.errorKeys?.length ? filters.more.errorKeys : undefined,
    };
  }

  private mapToEnvLog(log: EnvironmentApiLog): EnvLog {
    return {
      id: log.id,
      timestamp: this.datePipe.transform(log.timestamp, 'medium') ?? log.timestamp,
      api: log.apiName ?? log.apiId,
      apiId: log.apiId,
      apiType: log.apiType ? (API_TYPE_LABELS[log.apiType] ?? log.apiType) : undefined,
      apiProductName: log.apiProductName ?? undefined,
      application: log.application?.name ?? log.application?.id ?? EMPTY_FIELD,
      method: log.method ?? EMPTY_FIELD,
      path: log.uri ?? EMPTY_FIELD,
      status: log.status,
      responseTime: log.gatewayResponseTime == null ? EMPTY_FIELD : `${log.gatewayResponseTime} ms`,
      gateway: log.gateway ?? undefined,
      plan: log.plan?.name ? { name: log.plan.name } : undefined,
      requestEnded: log.requestEnded,
      errorKey: log.errorKey,
      warnings: log.warnings?.map(w => ({ key: w.key ?? '' })),
    };
  }
}
