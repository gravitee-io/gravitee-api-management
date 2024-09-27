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
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatChip, MatChipRow } from '@angular/material/chips';
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
import { Application } from '../../../../../entities/application/application';
import { LogsResponse, LogsResponseMetadataApi, LogsResponseMetadataTotalData } from '../../../../../entities/log/log';
import { CapitalizeFirstPipe } from '../../../../../pipe/capitalize-first.pipe';
import { ApplicationLogService, HttpMethodVM } from '../../../../../services/application-log.service';
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
  methods?: HttpMethodVM[];
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
  standalone: true,
  imports: [
    AsyncPipe,
    LoaderComponent,
    CapitalizeFirstPipe,
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
    MatIconButton,
    MatButton,
    MatFormField,
    MatSelect,
    MatOption,
    MatLabel,
    MatChipRow,
    MatChip,
    RouterLink,
  ],
  templateUrl: './application-log-table.component.html',
  styleUrl: './application-log-table.component.scss',
})
export class ApplicationLogTableComponent implements OnInit {
  @Input()
  application!: Application;

  logs$: Observable<LogVM[]> = of([]);
  applicationApis$: Observable<ApiVM[]> = of([]);

  pagination: Signal<{ hasPreviousPage: boolean; hasNextPage: boolean; currentPage: number; totalPages: number }> = computed(() => {
    const totalPages = Math.ceil(this.totalLogs() / 10);

    return {
      hasPreviousPage: this.currentLogsPage() > 1,
      hasNextPage: this.currentLogsPage() < totalPages,
      currentPage: this.currentLogsPage(),
      totalPages,
    };
  });

  filters: WritableSignal<FiltersVM> = signal({});
  noFiltersApplied: Signal<boolean> = computed(() =>
    Object.entries(this.filters())
      .filter(keyValueArray => keyValueArray[0] !== 'period')
      .every(keyValueArray => !keyValueArray[1]),
  );
  filtersPristine: Signal<boolean> = computed(() => isEqual(this.filters(), this.filtersInitialValue));

  httpMethods: HttpMethodVM[] = inject(ApplicationTabLogsService).httpMethods;
  responseTimes: ResponseTimeVM[] = inject(ApplicationTabLogsService).responseTimes;
  periods: PeriodVM[] = inject(ApplicationTabLogsService).periods;
  httpStatuses: HttpStatusVM[] = inject(ApplicationTabLogsService).httpStatuses;

  displayedColumns: string[] = ['api', 'timestamp', 'httpMethod', 'responseStatus', 'action'];

  private currentLogsPage: WritableSignal<number> = signal(1);
  private totalLogs: WritableSignal<number> = signal(0);
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
        const apis: string[] = this.mapQueryParamToStringArray(queryParams['apis']);

        const methodsQueryParams: string[] = this.mapQueryParamToStringArray(queryParams['methods']);
        const methods = ApplicationLogService.METHODS.filter(method => methodsQueryParams.includes(method.value));

        const responseTimes: string[] = this.mapQueryParamToStringArray(queryParams['responseTimes']);

        const period: string = queryParams['period'] ?? '1d';

        const from: number | undefined = queryParams['from'] ? +queryParams['from'] : undefined;
        const to: number | undefined = queryParams['to'] ? +queryParams['to'] : undefined;

        const requestId: string | undefined = queryParams['requestId'];
        const transactionId: string | undefined = queryParams['transactionId'];

        const httpStatuses: string[] = this.mapQueryParamToStringArray(queryParams['httpStatuses']);

        const messageText: string | undefined = queryParams['messageText'];
        const path: string | undefined = queryParams['path'];

        return { page, apis, methods, responseTimes, period, from, to, requestId, transactionId, httpStatuses, messageText, path };
      }),
      tap(values => this.initializeFiltersAndPagination(values)),
      switchMap(values => {
        const from = this.computeStartDateTimeFromQueryParams(values.from, values.to, values.period);
        return this.applicationLogService.list(this.application.id, { ...values, from }).pipe(
          catchError(err => {
            console.error(err);
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
      .list({ applicationId: this.application.id, size: -1, statuses: ['ACCEPTED', 'PAUSED'] })
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

  goToPreviousPage() {
    if (this.currentLogsPage() > 0) {
      this.navigate({ page: this.currentLogsPage() - 1 });
    }
  }

  goToNextPage() {
    if (this.currentLogsPage() < this.pagination().totalPages) {
      this.navigate({ page: this.currentLogsPage() + 1 });
    }
  }

  goToPage(page: number) {
    if (page > 0 && page <= this.pagination().totalPages) {
      this.navigate({ page });
    }
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
    const methods: string[] = this.filters().methods?.map(method => method.value) ?? [];
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
    apis: string[];
    methods: HttpMethodVM[];
    responseTimes: string[];
    period?: string;
    from?: number;
    to?: number;
    requestId?: string;
    transactionId?: string;
    httpStatuses: string[];
    messageText?: string;
    path?: string;
  }): void {
    this.currentLogsPage.set(params.page);
    this.selectedApis.set(params.apis);

    const responseTimes = this.responseTimes.filter(rt => params.responseTimes.includes(rt.value));

    const period = this.periods.find(p => p.value === params.period);

    const httpStatuses = this.httpStatuses.filter(hs => params.httpStatuses.includes(hs.value));

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
}
