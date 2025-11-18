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
import { WEBHOOK_LOGS_MOCK_RESPONSE as webhookData } from './mocks/webhook-logs.mock';
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
  callbackUrlOptions: string[] = [];
  private readonly periods = PERIODS;
  currentFilters: WebhookLogsQuickFilters = {};
  loading = true;

  ngOnInit(): void {
    // TODO: Backend Integration Required
    // - Replace mock data (WEBHOOK_LOGS_MOCK_RESPONSE) with actual backend API call
    // - Create/use a service method to fetch webhook logs with filters (searchTerm, statuses, applications, period, from, to, callbackUrls)
    // - Implement server-side pagination (currently client-side pagination using filteredLogs.slice())
    // - Update applyFilters() to call backend API instead of filtering client-side
    // - Update buildStatusOptions() to fetch available statuses from backend or API response metadata
    // - Update buildCallbackUrlOptions() to fetch available callback URLs from backend or API response metadata
    // - Handle loading states during API calls
    // - Handle error states (network errors, API errors)
    // - Update refresh() method to refetch data from backend
    // - Remove dependency on webhook-logs.mock.ts once backend is integrated

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
    const initialFrom = this.parseTimestamp(qp?.from);
    const initialTo = this.parseTimestamp(qp?.to);
    const initialCallbackUrls: string[] = qp?.callbackUrls ? qp.callbackUrls.split(',').filter(Boolean) : [];

    this.webhookLogsData = webhookData;
    this.allLogs = webhookData.data;
    this.buildStatusOptions(initialStatuses);
    this.buildCallbackUrlOptions(initialCallbackUrls);
    const initialApplications = this.buildInitialApplications(initialApplicationsIds);

    this.quickFiltersInitialValues = {
      searchTerm: initialSearch || undefined,
      statuses: initialStatuses.length ? initialStatuses : undefined,
      applications: initialApplications?.length ? initialApplications : undefined,
      period: initialPeriod?.value !== DEFAULT_PERIOD.value ? initialPeriod : undefined,
      from: initialFrom ?? undefined,
      to: initialTo ?? undefined,
      callbackUrls: initialCallbackUrls.length ? initialCallbackUrls : undefined,
    };

    this.applyFilters(this.quickFiltersInitialValues);
    this.loading = false;

    this.openSettingsDialogIfRequested();
  }

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
    const customRange = this.buildCustomDateRange(normalizedFilters);
    const periodRange = normalizedFilters.period ? this.preparePeriodRange(normalizedFilters.period) : undefined;
    const range = customRange ?? periodRange;
    const callbackUrls = normalizedFilters.callbackUrls;

    this.filteredLogs = this.allLogs.filter((log) => {
      const matchesSearch = !searchTerm || this.logMatchesSearchTerm(log, searchTerm);
      const matchesStatus = !statuses?.length || statuses.includes(log.status);
      const matchesApplication = !applications?.length || applications.includes(log.application?.id);
      const matchesPeriod = !range || this.logMatchesPeriod(log, range);
      const matchesCallback = !callbackUrls?.length || callbackUrls.includes(log.callbackUrl);
      return matchesSearch && matchesStatus && matchesApplication && matchesPeriod && matchesCallback;
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

  private buildCallbackUrlOptions(initialUrls: string[] = []): void {
    const urls = new Set<string>(initialUrls);
    this.allLogs.forEach((log) => {
      if (log.callbackUrl) {
        urls.add(log.callbackUrl);
      }
    });
    this.callbackUrlOptions = Array.from(urls).sort((a, b) => a.localeCompare(b));
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

    const callbackUrls = filters?.callbackUrls?.map((url) => url?.trim()).filter(Boolean);
    const uniqueCallbackUrls = callbackUrls?.length ? Array.from(new Set(callbackUrls)) : undefined;
    const normalizedFrom = isNumber(filters?.from) && !Number.isNaN(filters.from) ? filters.from : undefined;
    const normalizedTo = isNumber(filters?.to) && !Number.isNaN(filters.to) ? filters.to : undefined;

    const normalized: WebhookLogsQuickFilters = {
      searchTerm: searchTerm?.length ? searchTerm : undefined,
      statuses: uniqueStatuses,
      applications,
      from: normalizedFrom,
      to: normalizedTo,
      callbackUrls: uniqueCallbackUrls,
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
      callbackUrls: filters.callbackUrls?.length ? filters.callbackUrls.join(',') : null,
      from: filters.from ?? null,
      to: filters.to ?? null,
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
    next: {
      search: string | null;
      statuses: string | null;
      applicationIds: string | null;
      period: string | null;
      callbackUrls: string | null;
      from: number | null;
      to: number | null;
      page: number;
    },
  ): boolean {
    const currentSearch = current?.search ?? null;
    const currentStatuses = current?.statuses ?? null;
    const currentPage = current?.page ? Number(current.page) : null;
    const currentApplications = current?.applicationIds ?? null;
    const currentPeriod = current?.period ?? null;
    const currentCallbackUrls = current?.callbackUrls ?? null;
    const currentFrom = current?.from ? Number(current.from) : null;
    const currentTo = current?.to ? Number(current.to) : null;

    return (
      currentSearch === next.search &&
      currentStatuses === next.statuses &&
      currentApplications === next.applicationIds &&
      currentPeriod === next.period &&
      currentCallbackUrls === next.callbackUrls &&
      (currentFrom ?? null) === next.from &&
      (currentTo ?? null) === next.to &&
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
        // TODO: Backend Integration Required
        // - Reload webhook logs data after settings are saved (if analytics settings affect log availability)
        // - Consider showing a success message or notification
        // - Check if API redeployment is needed and show appropriate banner
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

  private buildCustomDateRange(filters: WebhookLogsQuickFilters): { from: number; to: number } | undefined {
    if (filters?.from == null || filters?.to == null) {
      return undefined;
    }
    const from = Math.min(filters.from, filters.to);
    const to = Math.max(filters.from, filters.to);
    return { from, to };
  }

  private parseTimestamp(value?: string | null): number | undefined {
    if (!value) {
      return undefined;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? undefined : parsed;
  }
}
