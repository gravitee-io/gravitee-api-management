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
import { ActivatedRoute, Router } from '@angular/router';
import { isEmpty, isEqual } from 'lodash';
import { catchError, distinctUntilChanged, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { MoreFiltersDialogComponent, MoreFiltersDialogData } from './components/more-filters-dialog/more-filters-dialog.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { Application } from '../../../../entities/application/application';
import { LogsResponseMetadataApi, LogsResponseMetadataTotalData } from '../../../../entities/log/log';
import { CapitalizeFirstPipe } from '../../../../pipe/capitalize-first.pipe';
import { ApplicationLogService, HttpMethodVM } from '../../../../services/application-log.service';
import { SubscriptionService } from '../../../../services/subscription.service';

interface LogVM {
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

interface ResponseTimeVM {
  value: string;
  min: number;
  max?: number;
}

interface PeriodVM {
  milliseconds: number;
  period: number;
  unit: 'MINUTE' | 'HOUR' | 'DAY';
  value: string;
}

interface FiltersVM {
  apis?: ApiVM[];
  methods?: HttpMethodVM[];
  responseTimes?: ResponseTimeVM[];
  period?: PeriodVM;
  to?: number;
  from?: number;
}

@Component({
  selector: 'app-application-tab-logs',
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
  ],
  templateUrl: './application-tab-logs.component.html',
  styleUrl: './application-tab-logs.component.scss',
})
export class ApplicationTabLogsComponent implements OnInit {
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
  filtersIsEmpty: Signal<boolean> = computed(() => isEmpty(this.filters()));
  noFiltersApplied: Signal<boolean> = computed(() => isEqual(this.filters(), { period: this.filters().period }));
  filtersPristine: Signal<boolean> = computed(() => isEqual(this.filters(), this.filtersInitialValue));

  httpMethods: HttpMethodVM[] = ApplicationLogService.METHODS;
  responseTimes: ResponseTimeVM[] = [
    {
      value: '0 TO 100',
      min: 0,
      max: 100,
    },
    {
      value: '100 TO 200',
      min: 100,
      max: 200,
    },
    {
      value: '200 TO 300',
      min: 200,
      max: 300,
    },
    {
      value: '300 TO 400',
      min: 300,
      max: 400,
    },
    {
      value: '400 TO 500',
      min: 400,
      max: 500,
    },
    {
      value: '500 TO 1000',
      min: 500,
      max: 1000,
    },
    {
      value: '1000 TO 2000',
      min: 1000,
      max: 2000,
    },
    {
      value: '2000 TO 5000',
      min: 2000,
      max: 5000,
    },
    {
      value: '5000 TO *',
      min: 5000,
    },
  ];

  periods: PeriodVM[] = [
    {
      milliseconds: this.toMilliseconds(0, 0, 5),
      period: 5,
      unit: 'MINUTE',
      value: '5m',
    },
    {
      milliseconds: this.toMilliseconds(0, 0, 30),
      period: 30,
      unit: 'MINUTE',
      value: '30m',
    },
    {
      milliseconds: this.toMilliseconds(0, 1, 0),
      period: 1,
      unit: 'HOUR',
      value: '1h',
    },
    {
      milliseconds: this.toMilliseconds(0, 3, 0),
      period: 3,
      unit: 'HOUR',
      value: '3h',
    },
    {
      milliseconds: this.toMilliseconds(0, 6, 0),
      period: 6,
      unit: 'HOUR',
      value: '6h',
    },
    {
      milliseconds: this.toMilliseconds(0, 12, 0),
      period: 12,
      unit: 'HOUR',
      value: '12h',
    },
    {
      milliseconds: this.toMilliseconds(1, 0, 0),
      period: 1,
      unit: 'DAY',
      value: '1d',
    },
    {
      milliseconds: this.toMilliseconds(3, 0, 0),
      period: 3,
      unit: 'DAY',
      value: '3d',
    },
    {
      milliseconds: this.toMilliseconds(7, 0, 0),
      period: 7,
      unit: 'DAY',
      value: '7d',
    },
    {
      milliseconds: this.toMilliseconds(14, 0, 0),
      period: 14,
      unit: 'DAY',
      value: '14d',
    },
    {
      milliseconds: this.toMilliseconds(30, 0, 0),
      period: 30,
      unit: 'DAY',
      value: '30d',
    },
    {
      milliseconds: this.toMilliseconds(90, 0, 0),
      period: 90,
      unit: 'DAY',
      value: '90d',
    },
  ];

  displayedColumns: string[] = ['api', 'timestamp', 'httpMethod', 'responseStatus'];

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

        return { page, apis, methods, responseTimes, period, from, to };
      }),
      tap(values => this.initializeFiltersAndPagination(values)),
      switchMap(values => {
        const from = this.computeStartDateTimeFromQueryParams(values.from, values.to, values.period);
        return this.applicationLogService.list(this.application.id, { ...values, from });
      }),
      tap(({ metadata }) => {
        this.totalLogs.set((metadata['data'] as LogsResponseMetadataTotalData).total);
      }),
      map(response =>
        response.data.map(log => ({
          apiName: (response.metadata[log.api] as LogsResponseMetadataApi).name,
          apiVersion: (response.metadata[log.api] as LogsResponseMetadataApi).version,
          method: log.method,
          status: log.status,
          timestamp: log.timestamp,
        })),
      ),
      catchError(err => {
        console.error(err);
        return of([]);
      }),
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
        },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: dialogFilters => {
          this.filters.update(filters => ({ ...filters, from: dialogFilters?.startDate, to: dialogFilters?.endDate }));
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
  }): void {
    this.currentLogsPage.set(params.page);
    this.selectedApis.set(params.apis);

    const responseTimes = this.responseTimes.filter(rt => params.responseTimes.includes(rt.value));

    const period = this.periods.find(p => p.value === params.period);

    this.filters.update(filters => ({
      ...filters,
      ...(params.methods.length ? { methods: params.methods } : {}),
      ...(responseTimes.length ? { responseTimes } : {}),
      ...(period ? { period } : {}),
      ...(params.from ? { from: params.from } : {}),
      ...(params.to ? { to: params.to } : {}),
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

  private toMilliseconds(days: number, hours: number, minutes: number): number {
    return (days * 24 * 60 * 60 + hours * 60 * 60 + minutes * 60) * 1000;
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
