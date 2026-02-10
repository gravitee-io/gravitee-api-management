/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, Input, OnInit, Signal, signal, WritableSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatChip } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatOption, MatSelect, MatSelectChange } from '@angular/material/select';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable,
} from '@angular/material/table';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { isEqual } from 'lodash';
import { catchError, distinctUntilChanged, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { LoaderComponent } from '../../../../../components/loader/loader.component';
import { PaginationComponent } from '../../../../../components/pagination/pagination.component';
import { Application } from '../../../../../entities/application/application';
import { LogsResponse, LogsResponseMetadataApi, LogsResponseMetadataTotalData } from '../../../../../entities/log';
import { ApplicationLogService, ResponseTimeRange } from '../../../../../services/application-log.service';
import { SubscriptionService } from '../../../../../services/subscription.service';
import { ApplicationTabLogsService, HttpStatusVM, PeriodVM, ResponseTimeVM } from '../application-tab-logs.service';
import { MoreFiltersDialogComponent, MoreFiltersDialogData } from '../more-filters-dialog/more-filters-dialog.component';

interface LogVM {
  id: string;
  apiName: string;
  apiVersion: string;
  timestamp: number;
  method: string;
  status: number;
}

interface ApiVM {
  id: string;
  name: string;
  version: string;
}

interface FiltersVM {
  apis?: ApiVM[];
  methods?: string[];
  responseTimes?: ResponseTimeVM[];
  period?: PeriodVM;
  to?: number;
  from?: number;
  requestId?: string;
  transactionId?: string;
  httpStatuses?: HttpStatusVM[];
  messageText?: string;
  path?: string;
}

@Component({
  selector: 'app-application-log-table',
  imports: [
    AsyncPipe,
    LoaderComponent,
    MatCell,
    MatCellDef,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderRow,
    MatHeaderRowDef,
    MatIcon,
    MatRow,
    MatRowDef,
    MatTable,
    MatHeaderCellDef,
    DatePipe,
    MatButton,
    MatFormField,
    MatSelect,
    MatOption,
    MatLabel,
    MatChip,
    RouterLink,
    PaginationComponent,
  ],
  templateUrl: './application-log-table.component.html',
  styleUrl: './application-log-table.component.scss',
})
export class ApplicationLogTableComponent implements OnInit {
  @Input()
  application!: Application;

  logs$: Observable<LogVM[]> = of([]);
  applicationApis$: Observable<ApiVM[]> = of([]);

  filters: WritableSignal<FiltersVM> = signal({});
  noFiltersApplied: Signal<boolean> = computed(() =>
    Object.entries(this.filters())
      .filter(keyValueArray => keyValueArray[0] !== 'period')
      .every(keyValueArray => !keyValueArray[1]),
  );
  filtersPristine: Signal<boolean> = computed(() => isEqual(this.filters(), this.filtersInitialValue));

  httpMethods: string[] = inject(ApplicationTabLogsService).httpMethods;
  responseTimes: ResponseTimeVM[] = inject(ApplicationTabLogsService).responseTimes;
  periods: PeriodVM[] = inject(ApplicationTabLogsService).periods;
  httpStatuses: HttpStatusVM[] = inject(ApplicationTabLogsService).httpStatuses;

  displayedColumns: string[] = ['api', 'timestamp', 'httpMethod', 'responseStatus', 'action'];

  currentLogsPage: WritableSignal<number> = signal(1);
  totalLogs: WritableSignal<number> = signal(0);

  private selectedApis: WritableSignal<string[]> = signal([]);
  private filtersInitialValue: FiltersVM = {};

  private destroyRef = inject(DestroyRef);

  constructor(
    private applicationLogService: ApplicationLogService,
    private subscriptionService: SubscriptionService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private matDialog: MatDialog,
  ) {}

  ngOnInit() {
    this.logs$ = this.activatedRoute.queryParams.pipe(
      distinctUntilChanged(),
      map(queryParams => {
        const page: number = queryParams['page'] ? +queryParams['page'] : 1;
        const apiIds: string[] = this.mapQueryParamToStringArray(queryParams['apis']);

        const methods: string[] = this.mapQueryParamToStringArray(queryParams['methods']);

        const responseTimes: string[] = this.mapQueryParamToStringArray(queryParams['responseTimes']);

        const period: string = queryParams['period'] ?? '1d';

        const from: number | undefined = queryParams['from'] ? +queryParams['from'] : undefined;
        const to: number | undefined = queryParams['to'] ? +queryParams['to'] : undefined;

        const requestId: string | undefined = queryParams['requestId'];
        const transactionId: string | undefined = queryParams['transactionId'];

        const statuses: string[] = this.mapQueryParamToStringArray(queryParams['httpStatuses']);

        const messageText: string | undefined = queryParams['messageText'];
        const path: string | undefined = queryParams['path'];

        return { page, apiIds, methods, responseTimes, period, from, to, requestId, transactionId, statuses, messageText, path };
      }),
      tap(values => this.initializeFiltersAndPagination(values)),
      switchMap(values => {
        const from = this.computeStartDateTimeFromQueryParams(values.from, values.to, values.period);
        const responseTimeRanges: ResponseTimeRange[] = this.transformResponseTimes(values.responseTimes);

        return this.applicationLogService.search(this.application.id, values.page, 10, { ...values, from, responseTimeRanges }).pipe(
          catchError(_ => {
            return of({
              data: [],
              metadata: {
                data: {
                  total: 0,
                },
              },
            } as LogsResponse);
          }),
        );
      }),
      tap(response => {
        this.totalLogs.set((response.metadata['data'] as LogsResponseMetadataTotalData).total);
      }),
      map(response =>
        response.data.map(log => ({
          id: log.id,
          apiName: (response.metadata[log.api] as LogsResponseMetadataApi).name,
          apiVersion: (response.metadata[log.api] as LogsResponseMetadataApi).version,
          method: log.method,
          status: log.status,
          timestamp: log.timestamp,
        })),
      ),
    );

    this.applicationApis$ = this.subscriptionService
      .list({ applicationIds: [this.application.id], size: -1, statuses: ['ACCEPTED', 'PAUSED'] })
      .pipe(
        map(response => {
          const apiIds = [...new Set(response.data.map(s => s.api))];
          return apiIds.map(apiId => ({
            id: apiId,
            name: response.metadata[apiId]?.name ?? '',
            version: response.metadata[apiId]?.apiVersion ?? '',
          }));
        }),
        tap(apiFilters => {
          if (this.selectedApis().length) {
            this.filters.update(filters => ({ ...filters, apis: apiFilters.filter(apiVm => this.selectedApis().includes(apiVm.id)) }));
            this.filtersInitialValue.apis = this.filters().apis;
          }
        }),
      );
  }

  goToPage(page: number) {
    this.navigate({ page });
  }

  resetFilters() {
    this.filters.update(filters => ({ period: filters.period }));
  }

  search() {
    this.navigate({ page: 1 });
  }

  selectApis($event: MatSelectChange) {
    this.filters.update(filters => ({ ...filters, apis: $event.value }));
  }

  selectHttpMethods($event: MatSelectChange) {
    this.filters.update(filters => ({ ...filters, methods: $event.value }));
  }

  selectResponseTimes($event: MatSelectChange) {
    this.filters.update(filters => ({ ...filters, responseTimes: $event.value }));
  }

  selectPeriod($event: MatSelectChange) {
    this.filters.update(filters => ({ ...filters, period: $event.value }));
  }

  openMoreFiltersDialog() {
    this.matDialog
      .open<MoreFiltersDialogComponent, MoreFiltersDialogData, MoreFiltersDialogData>(MoreFiltersDialogComponent, {
        data: {
          startDate: this.filters().from,
          endDate: this.filters().to,
          requestId: this.filters().requestId,
          transactionId: this.filters().transactionId,
          httpStatuses: this.filters().httpStatuses,
          messageText: this.filters().messageText,
          path: this.filters().path,
        },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: dialogFilters => {
          this.filters.update(filters => ({
            ...filters,
            ...dialogFilters,
            from: dialogFilters?.startDate,
            to: dialogFilters?.endDate,
            requestId: dialogFilters?.requestId,
            transactionId: dialogFilters?.transactionId,
            httpStatuses: dialogFilters?.httpStatuses,
            messageText: dialogFilters?.messageText,
            path: dialogFilters?.path,
          }));
        },
      });
  }

  private navigate(params: { page: number }) {
    const apis: string[] = this.filters().apis?.map(api => api.id) ?? [];
    const methods: string[] = this.filters().methods ?? [];
    const responseTimes: string[] = this.filters().responseTimes?.map(rt => rt.value) ?? [];
    const period: string = this.filters().period?.value ?? '';
    const from = this.filters().from;
    const to = this.filters().to;
    const requestId = this.filters().requestId;
    const transactionId = this.filters().transactionId;
    const httpStatuses: string[] = this.filters().httpStatuses?.map(rt => rt.value) ?? [];
    const messageText = this.filters().messageText;
    const path = this.filters().path;

    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        page: params.page,
        ...(apis.length ? { apis } : {}),
        ...(methods.length ? { methods } : {}),
        ...(responseTimes.length ? { responseTimes } : {}),
        ...(period ? { period } : {}),
        ...(from ? { from } : {}),
        ...(to ? { to } : {}),
        ...(requestId ? { requestId } : {}),
        ...(transactionId ? { transactionId } : {}),
        ...(httpStatuses.length ? { httpStatuses } : {}),
        ...(messageText ? { messageText } : {}),
        ...(path ? { path } : {}),
      },
    });
  }

  private initializeFiltersAndPagination(params: {
    page: number;
    apiIds: string[];
    methods: string[];
    responseTimes: string[];
    period?: string;
    from?: number;
    to?: number;
    requestId?: string;
    transactionId?: string;
    statuses: string[];
    messageText?: string;
    path?: string;
  }): void {
    this.currentLogsPage.set(params.page);
    this.selectedApis.set(params.apiIds);

    const responseTimes = this.responseTimes.filter(rt => params.responseTimes.includes(rt.value));

    const period = this.periods.find(p => p.value === params.period);

    const httpStatuses = this.httpStatuses.filter(hs => params.statuses.includes(hs.value));

    this.filters.update(filters => ({
      ...filters,
      ...(params.methods.length ? { methods: params.methods } : { methods: undefined }),
      ...(responseTimes.length ? { responseTimes } : { responseTimes: undefined }),
      period,
      from: params.from,
      to: params.to,
      requestId: params.requestId,
      transactionId: params.transactionId,
      ...(httpStatuses.length ? { httpStatuses } : { httpStatuses: undefined }),
      messageText: params.messageText,
      path: params.path,
    }));
    this.filtersInitialValue = this.filters();
  }

  private mapQueryParamToStringArray(queryParam: string | string[] | undefined): string[] {
    if (!queryParam) {
      return [];
    }
    if (Array.isArray(queryParam)) {
      return queryParam;
    }
    return [queryParam];
  }

  private computeStartDateTimeFromQueryParams(
    fromQueryParam: number | undefined,
    toQueryParam: number | undefined,
    periodQueryParam: string,
  ): number | undefined {
    if (fromQueryParam) {
      return fromQueryParam;
    }

    if (!periodQueryParam) {
      return undefined;
    }

    const periodDifference = this.periods.find(p => p.value === periodQueryParam)?.milliseconds ?? 0;

    const endDateTime = toQueryParam ?? Date.now();
    return endDateTime - periodDifference;
  }

  private transformResponseTimes(responseTimes: string[]): ResponseTimeRange[] {
    if (!responseTimes) {
      return [];
    }
    return this.responseTimes.filter(rt => responseTimes.includes(rt.value)).map(rt => ({ from: rt.min, to: rt.max }));
  }
}
