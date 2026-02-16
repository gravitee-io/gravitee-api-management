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
import { map, shareReplay, switchMap, take, catchError, finalize } from 'rxjs/operators';
import { ReplaySubject, of, Observable } from 'rxjs';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { GioBannerModule, GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';
import moment from 'moment';

import DurationConstructor = moment.unitOfTime.DurationConstructor;

import {
  WebhookLog,
  WebhookLogsQuickFilters,
  WebhookLogsQuickFiltersInitialValues,
  WebhookLogsResponse,
} from './models/webhook-logs.models';
import { mapConnectionLogToWebhookLog } from './utils/webhook-metrics.utils';
import { WebhookLogsListComponent } from './components/webhook-logs-list/webhook-logs-list.component';
import { WebhookSettingsDialogComponent } from './components/webhook-settings-dialog/webhook-settings-dialog.component';
import { WebhookLogsQuickFiltersComponent } from './components/webhook-logs-quick-filters/webhook-logs-quick-filters.component';

import { ConnectionLog, Pagination, Api, ApiV4, Entrypoint } from '../../../../entities/management-api-v2';
import { ApiNavigationModule } from '../../api-navigation/api-navigation.module';
import { ReportingDisabledBannerComponent } from '../components/reporting-disabled-banner/reporting-disabled-banner.component';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiLogsV2Service } from '../../../../services-ngx/api-logs-v2.service';
import { ApiSubscriptionV2Service } from '../../../../services-ngx/api-subscription-v2.service';
import { ApplicationService } from '../../../../services-ngx/application.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { DEFAULT_PERIOD, MultiFilter, PERIODS, SimpleFilter } from '../runtime-logs/models';
import { GioTableWrapperPagination } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

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
    ApiNavigationModule,
    ReportingDisabledBannerComponent,
  ],
})
export class WebhookLogsComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly apiService = inject(ApiV2Service);
  private readonly apiLogsService = inject(ApiLogsV2Service);
  private readonly apiSubscriptionService = inject(ApiSubscriptionV2Service);
  private readonly applicationService = inject(ApplicationService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly api$ = this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(shareReplay(1));

  isReportingDisabled$ = this.api$.pipe(
    map(api => {
      if (!this.isApiV4(api)) {
        return false;
      }
      const webhookEntrypoint = this.findWebhookEntrypoint(api);
      if (!webhookEntrypoint) {
        return false;
      }
      return !webhookEntrypoint.configuration?.logging?.enabled;
    }),
  );

  hasDlqConfigured$ = this.api$.pipe(
    map(api => {
      const webhookEntrypoint = this.isApiV4(api) ? this.findWebhookEntrypoint(api) : undefined;
      return !!webhookEntrypoint?.dlq?.endpoint;
    }),
  );

  webhookLogsSubject$ = new ReplaySubject<WebhookLogsResponse>(1);
  webhookLogsData: WebhookLogsResponse | undefined = undefined;
  private allLogs: WebhookLog[] = [];

  quickFiltersInitialValues: WebhookLogsQuickFiltersInitialValues = {};
  callbackUrlOptions: string[] = [];
  private readonly periods = PERIODS;
  currentFilters: WebhookLogsQuickFilters = {};
  loading = false;

  ngOnInit(): void {
    const qp = this.activatedRoute.snapshot.queryParams;

    const initialStatuses = qp?.statuses
      ? qp.statuses
          .split(',')
          .map((status: string) => Number(status))
          .filter(value => !Number.isNaN(value))
      : [];

    const initialApplicationsIds: string[] = qp?.applicationIds ? qp.applicationIds.split(',').filter(Boolean) : [];
    const initialPeriod = this.buildInitialPeriod(qp?.period);
    const initialFrom = this.parseTimestamp(qp?.from);
    const initialTo = this.parseTimestamp(qp?.to);
    const initialCallbackUrls: string[] = qp?.callbackUrls ? qp.callbackUrls.split(',').filter(Boolean) : [];

    const initialApplications = this.buildInitialApplications(initialApplicationsIds);

    // Build raw filters from query params
    const initialFilters: WebhookLogsQuickFilters = {
      statuses: initialStatuses.length ? initialStatuses : undefined,
      applications: initialApplications?.length ? initialApplications : undefined,
      period: initialPeriod.value !== DEFAULT_PERIOD.value ? initialPeriod : undefined,
      from: initialFrom,
      to: initialTo,
      callbackUrls: initialCallbackUrls.length ? initialCallbackUrls : undefined,
    };

    // Normalize and store as currentFilters
    this.currentFilters = this.normalizeFilters(initialFilters);

    // Use the same values for the quick filters UI
    this.quickFiltersInitialValues = initialFilters;

    const page = qp?.page ? Number(qp.page) : 1;
    const perPage = qp?.perPage ? Number(qp.perPage) : 10;
    this.loadWebhookLogs(this.currentFilters, page, perPage);
    this.loadCallbackUrlOptions();
    this.openSettingsDialogIfRequested();
  }

  private loadWebhookLogs(filters?: WebhookLogsQuickFilters, page: number = 1, perPage: number = 10): void {
    this.loading = true;
    const apiId = this.activatedRoute.snapshot.params.apiId;

    const normalizedFilters = this.normalizeFilters(filters ?? {});
    const { statuses, callbackUrls, applicationIds, from, to } = this.buildSearchParams(normalizedFilters);

    this.apiLogsService
      .searchApiMessageLogs(apiId, {
        connectorId: 'webhook',
        connectorType: 'entrypoint',
        operation: 'subscribe',
        page,
        perPage,
        statuses,
        callbackUrls,
        applicationIds,
        from,
        to,
        requiresAdditional: true,
      })
      .pipe(
        map(response => this.mapToWebhookLogsResponse(response)),
        switchMap(webhookLogsResponse => this.addApplicationNameToLogs(webhookLogsResponse)),
        catchError(() => {
          this.snackBarService.error('Failed to load webhook logs');
          return of({
            data: [],
            pagination: { page, perPage, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
          });
        }),
        finalize(() => {
          this.loading = false;
        }),
      )
      .subscribe(webhookLogsResponse => {
        this.webhookLogsData = webhookLogsResponse;
        this.allLogs = webhookLogsResponse.data;
        this.buildCallbackUrlOptions();
        this.webhookLogsSubject$.next(webhookLogsResponse);
        this.updateUrlParams(filters ?? this.currentFilters, page, perPage);
      });
  }

  private buildSearchParams(filters: WebhookLogsQuickFilters): {
    statuses?: number[];
    callbackUrls?: string[];
    applicationIds?: string[];
    from?: number;
    to?: number;
  } {
    const statuses = filters.statuses && filters.statuses.length > 0 ? filters.statuses : undefined;
    const callbackUrls = filters.callbackUrls && filters.callbackUrls.length > 0 ? filters.callbackUrls : undefined;
    const applicationIds = filters.applications && filters.applications.length > 0 ? filters.applications.map(app => app.value) : undefined;

    const customRange = this.buildCustomDateRange(filters);
    const periodRange = filters.period ? this.preparePeriodRange(filters.period) : undefined;
    const range = customRange ?? periodRange;

    return {
      statuses,
      callbackUrls,
      applicationIds,
      from: range?.from,
      to: range?.to,
    };
  }

  private mapToWebhookLogsResponse(response: { data: ConnectionLog[]; pagination: Pagination }): WebhookLogsResponse {
    const webhookLogs: WebhookLog[] = response.data.map(log => mapConnectionLogToWebhookLog(log));

    return {
      data: webhookLogs,
      pagination: response.pagination,
    };
  }

  private readonly APP_ID_METRIC_KEY = 'keyword_webhook_app-id';

  private addApplicationNameToLogs(webhookLogsResponse: WebhookLogsResponse): Observable<WebhookLogsResponse> {
    const webhookLogs = webhookLogsResponse.data ?? [];

    if (webhookLogs.length === 0) {
      return of(webhookLogsResponse);
    }

    const applicationIdsToEnrich = new Set<string>();

    for (const log of webhookLogs) {
      if (log.application?.id && !log.application.name) {
        applicationIdsToEnrich.add(log.application.id);
        continue;
      }

      if (!log.application?.id) {
        const appIdFromMetrics = log.additionalMetrics[this.APP_ID_METRIC_KEY];
        if (appIdFromMetrics && typeof appIdFromMetrics === 'string' && appIdFromMetrics.trim().length > 0) {
          applicationIdsToEnrich.add(appIdFromMetrics);
        }
      }
    }

    if (applicationIdsToEnrich.size === 0) {
      return of(webhookLogsResponse);
    }

    const appIdsArray = Array.from(applicationIdsToEnrich);

    return this.applicationService.findByIds(appIdsArray, 1, appIdsArray.length).pipe(
      map(applicationsResponse => {
        const apps = applicationsResponse?.data ?? [];

        const applicationMap = new Map<string, { id: string; name: string }>();
        for (const app of apps) {
          if (app?.id && app?.name) {
            applicationMap.set(app.id, { id: app.id, name: app.name });
          }
        }

        if (applicationMap.size === 0) {
          return webhookLogsResponse;
        }

        const enrichedLogs: WebhookLog[] = webhookLogs.map(log => {
          const appId = this.resolveApplicationId(log);
          if (!appId) {
            return log;
          }

          const appData = applicationMap.get(appId);
          if (!appData) {
            return log;
          }

          if (log.application && !log.application.name) {
            return {
              ...log,
              application: {
                ...log.application,
                name: appData.name,
              },
            };
          }

          if (!log.application) {
            return {
              ...log,
              application: {
                id: appData.id,
                name: appData.name,
                apiKeyMode: 'UNSPECIFIED' as const,
              },
            };
          }

          return log;
        });

        return {
          ...webhookLogsResponse,
          data: enrichedLogs,
        };
      }),
      catchError(() => {
        return of(webhookLogsResponse);
      }),
    );
  }

  private resolveApplicationId(log: WebhookLog): string | undefined {
    if (log.application?.id) {
      return log.application.id;
    }
    const metricId = log.additionalMetrics[this.APP_ID_METRIC_KEY];
    return typeof metricId === 'string' && metricId.trim().length > 0 ? metricId : undefined;
  }

  paginationUpdated(event: GioTableWrapperPagination): void {
    const page = event.index;
    const perPage = event.size;
    this.loadWebhookLogs(this.currentFilters, page, perPage);
  }

  refresh(): void {
    const qp = this.activatedRoute.snapshot.queryParams;
    const page = qp?.page ? Number(qp.page) : 1;
    const perPage = qp?.perPage ? Number(qp.perPage) : 10;
    this.loadWebhookLogs(this.currentFilters, page, perPage);
  }

  onFiltersChanged(filters: WebhookLogsQuickFilters): void {
    this.applyFilters(filters);
  }

  private applyFilters(filters: WebhookLogsQuickFilters = {}): void {
    const normalizedFilters = this.normalizeFilters(filters);
    this.currentFilters = normalizedFilters;

    // Reset to page 1 when filters change
    this.loadWebhookLogs(normalizedFilters, 1, 10);
  }

  private buildCallbackUrlOptions(): void {
    const urlsFromLogs = new Set<string>();
    this.allLogs.forEach(log => {
      if (log.callbackUrl) {
        urlsFromLogs.add(log.callbackUrl);
      }
    });

    const allUrls = new Set<string>([...this.callbackUrlOptions, ...urlsFromLogs]);
    this.callbackUrlOptions = Array.from(allUrls).sort((a, b) => a.localeCompare(b));
  }

  private loadCallbackUrlOptions(): void {
    const apiId = this.activatedRoute.snapshot.params.apiId;

    this.apiSubscriptionService
      .list(apiId, '1', '1000', ['ACCEPTED', 'PENDING', 'PAUSED'])
      .pipe(
        map(response => {
          const urls = new Set<string>();

          if (response.data) {
            response.data.forEach(subscription => {
              if (
                subscription.consumerConfiguration?.entrypointId === 'webhook' &&
                subscription.consumerConfiguration?.entrypointConfiguration?.callbackUrl
              ) {
                const callbackUrl = subscription.consumerConfiguration.entrypointConfiguration.callbackUrl;
                if (callbackUrl) {
                  urls.add(callbackUrl);
                }
              }
            });
          }

          return Array.from(urls).sort((a, b) => a.localeCompare(b));
        }),
        catchError(() => {
          // If subscription fetch fails, fallback to empty options
          return of<string[]>([]);
        }),
      )
      .subscribe(urls => {
        this.callbackUrlOptions = urls;
      });
  }

  private normalizeFilters(filters: WebhookLogsQuickFilters = {}): WebhookLogsQuickFilters {
    const statuses = filters?.statuses?.filter(status => !Number.isNaN(status));
    const uniqueStatuses = statuses?.length ? Array.from(new Set(statuses)) : undefined;

    const applications = filters?.applications?.length
      ? filters.applications.filter(
          (application, index, array) => application && array.findIndex(app => app.value === application.value) === index,
        )
      : undefined;

    const callbackUrls = filters?.callbackUrls?.map(url => url?.trim()).filter(Boolean);
    const uniqueCallbackUrls = callbackUrls?.length ? Array.from(new Set(callbackUrls)) : undefined;

    const normalizedFrom = isNumber(filters?.from) && !Number.isNaN(filters.from) ? filters.from : undefined;
    const normalizedTo = isNumber(filters?.to) && !Number.isNaN(filters.to) ? filters.to : undefined;

    const normalized: WebhookLogsQuickFilters = {
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
    const match = this.periods.find(available => available.value === period.value);
    return match && match.value !== DEFAULT_PERIOD.value ? match : undefined;
  }

  private preparePeriodRange(period: SimpleFilter): { from: number; to: number } | undefined {
    if (!period || period.value === DEFAULT_PERIOD.value) {
      return undefined;
    }

    const now = moment();
    const operation = period.value.charAt(0);
    const timeUnit: DurationConstructor = period.value.charAt(period.value.length - 1) as DurationConstructor;
    const duration = Number(period.value.substring(1, period.value.length - 1));

    const from = operation === '-' ? now.clone().subtract(duration, timeUnit) : now.clone().add(duration, timeUnit);

    return { from: from.valueOf(), to: now.valueOf() };
  }

  private updateUrlParams(filters: WebhookLogsQuickFilters, page: number, perPage: number): void {
    const nextParams = {
      statuses: filters.statuses?.length ? filters.statuses.join(',') : null,
      applicationIds: filters.applications?.length ? filters.applications.map(app => app.value).join(',') : null,
      period: filters.period?.value ?? null,
      callbackUrls: filters.callbackUrls?.length ? filters.callbackUrls.join(',') : null,
      from: filters.from ?? null,
      to: filters.to ?? null,
      page: page.toString(),
      perPage: perPage.toString(),
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
      statuses: string | null;
      applicationIds: string | null;
      period: string | null;
      callbackUrls: string | null;
      from: number | null;
      to: number | null;
      page: string;
      perPage: string;
    },
  ): boolean {
    const currentStatuses = current?.statuses ?? null;
    const currentPage = current?.page ?? null;
    const currentPerPage = current?.perPage ?? null;
    const currentApplications = current?.applicationIds ?? null;
    const currentPeriod = current?.period ?? null;
    const currentCallbackUrls = current?.callbackUrls ?? null;
    const currentFrom = current?.from ? Number(current.from) : null;
    const currentTo = current?.to ? Number(current.to) : null;

    return (
      currentStatuses === next.statuses &&
      currentApplications === next.applicationIds &&
      currentPeriod === next.period &&
      currentCallbackUrls === next.callbackUrls &&
      (currentFrom ?? null) === next.from &&
      (currentTo ?? null) === next.to &&
      (currentPage ?? '1') === next.page &&
      (currentPerPage ?? '10') === next.perPage
    );
  }

  private buildInitialPeriod(periodValue?: string): SimpleFilter {
    if (!periodValue) {
      return DEFAULT_PERIOD;
    }
    return this.periods.find(period => period.value === periodValue) ?? DEFAULT_PERIOD;
  }

  /**
   * Builds initial applications filter from IDs.
   * Note: This is called before logs are loaded, so `this.allLogs` is empty.
   * Application labels will fall back to IDs if names aren't available yet.
   * Labels may be updated later when logs are loaded and enriched with application names.
   */
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

    const applications = ids.map(id => ({ value: id, label: applicationMap.get(id) ?? id }));
    return applications.length ? applications : undefined;
  }

  openSettingsDialog(): void {
    this.api$
      .pipe(
        take(1),
        switchMap(api => {
          if (!this.isApiV4(api)) {
            this.snackBarService.error('Webhook logs settings are only available for v4 APIs.');
            return of(undefined);
          }

          const dialogRef = this.dialog.open(WebhookSettingsDialogComponent, {
            width: GIO_DIALOG_WIDTH.MEDIUM,
            data: { api },
          });

          return dialogRef.afterClosed();
        }),
      )
      .subscribe(result => {
        if (result?.saved) {
          const qp = this.activatedRoute.snapshot.queryParams;
          const page = qp?.page ? Number(qp.page) : 1;
          const perPage = qp?.perPage ? Number(qp.perPage) : 10;
          this.loadWebhookLogs(this.currentFilters, page, perPage);
        }
      });
  }

  onLogDetailsClicked(log: WebhookLog): void {
    if (!log?.requestId) {
      return;
    }

    this.router.navigate(['./', log.requestId], {
      relativeTo: this.activatedRoute,
      state: { webhookLog: log },
    });
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

  private isApiV4(api: Api): api is ApiV4 {
    return api?.definitionVersion === 'V4';
  }

  /**
   * Finds the webhook entrypoint from the API's listeners.
   * The webhook entrypoint configuration contains logging settings at:
   * entrypoint.configuration.logging.{enabled, request.{headers, payload}, response.{headers, payload}}
   */
  private findWebhookEntrypoint(api: ApiV4): Entrypoint | undefined {
    return api.listeners?.flatMap(listener => listener.entrypoints ?? []).find(entrypoint => entrypoint.type === 'webhook') ?? undefined;
  }
}
