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
import { Component, computed, Input, OnInit, Signal, signal, WritableSignal } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatChip, MatChipRow } from '@angular/material/chips';
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
import { isEmpty } from 'lodash';
import { catchError, distinctUntilChanged, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

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

interface FiltersVM {
  apis?: ApiVM[];
  methods?: HttpMethodVM[];
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
  filtersPristine: boolean = true;

  httpMethods: HttpMethodVM[] = ApplicationLogService.METHODS;

  displayedColumns: string[] = ['api', 'timestamp', 'httpMethod', 'responseStatus'];

  private currentLogsPage: WritableSignal<number> = signal(1);
  private totalLogs: WritableSignal<number> = signal(0);
  private selectedApis: WritableSignal<string[]> = signal([]);

  constructor(
    private applicationLogService: ApplicationLogService,
    private subscriptionService: SubscriptionService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.logs$ = this.activatedRoute.queryParams.pipe(
      distinctUntilChanged(),
      map(queryParams => {
        const page: number = queryParams['page'] ? +queryParams['page'] : 1;
        const apis: string[] = queryParams['apis'] ?? [];
        const methodsQueryParams: string[] = queryParams['methods'] ?? [];
        const methods = ApplicationLogService.METHODS.filter(method => methodsQueryParams.includes(method.value));
        return { page, apis, methods };
      }),
      tap(values => this.initializeFiltersAndPagination(values)),
      switchMap(values => this.applicationLogService.list(this.application.id, values)),
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
            name: response.metadata[apiId].name ?? '',
            version: response.metadata[apiId].apiVersion ?? '',
          }));
        }),
        tap(apiFilters => {
          if (this.selectedApis().length) {
            this.filters.update(filters => ({ ...filters, apis: apiFilters.filter(apiVm => this.selectedApis().includes(apiVm.id)) }));
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
    this.filters.set({});
    this.navigate({ page: 1 });
  }

  search() {
    this.navigate({ page: 1 });
  }

  selectApis($event: MatSelectChange) {
    this.filtersPristine = false;
    this.filters.update(filters => ({ ...filters, apis: $event.value }));
  }

  selectHttpMethods($event: MatSelectChange) {
    this.filtersPristine = false;
    this.filters.update(filters => ({ ...filters, methods: $event.value }));
  }

  private navigate(params: { page: number }) {
    this.filtersPristine = true;

    const apis: string[] = this.filters().apis?.map(api => api.id) ?? [];
    const methods: string[] = this.filters().methods?.map(method => method.value) ?? [];

    this.router.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        page: params.page,
        ...(apis.length ? { apis } : {}),
        ...(methods.length ? { methods } : {}),
      },
    });
  }

  private initializeFiltersAndPagination(params: { page: number; apis: string[]; methods: HttpMethodVM[] }): void {
    this.filtersPristine = true;
    this.currentLogsPage.set(params.page);
    this.selectedApis.set(params.apis);
    this.filters.update(filters => ({ ...filters, ...(params.methods.length ? { methods: params.methods } : {}) }));
  }
}
