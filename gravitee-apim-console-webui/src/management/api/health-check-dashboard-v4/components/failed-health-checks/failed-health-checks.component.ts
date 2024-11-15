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

import { Component, DestroyRef, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { AsyncPipe, DecimalPipe, TitleCasePipe } from '@angular/common';
import { catchError, distinctUntilChanged, tap } from 'rxjs/operators';
import { BehaviorSubject, combineLatestWith, EMPTY, Observable, switchMap } from 'rxjs';
import { isEqual } from 'lodash';

import { GioChartLineModule } from '../../../../../shared/components/gio-chart-line/gio-chart-line.module';
import { ApiHealthV2Service } from '../../../../../services-ngx/api-health-v2.service';
import { GioTableWrapperModule } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioTableWrapperFilters } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { HealthCheckLogsResponse } from '../../../../../entities/management-api-v2/api/v4/healthCheck';

@Component({
  imports: [
    MatCardModule,
    GioLoaderModule,
    GioChartLineModule,
    AsyncPipe,
    GioTableWrapperModule,
    MatSort,
    MatTableModule,
    DecimalPipe,
    TitleCasePipe,
  ],
  selector: 'failed-health-checks',
  standalone: true,
  styleUrl: './failed-health-checks.component.scss',
  templateUrl: './failed-health-checks.component.html',
})
export class FailedHealthChecksComponent implements OnInit {
  isLoading = signal(true);

  defaultFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: { index: 1, size: 10 },
  };

  protected filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.defaultFilters);

  private apiId = this.activatedRoute.snapshot.params.apiId;
  private showSuccessLogs = false;

  protected displayedColumns: string[] = ['timestamp', 'endpoint', 'gateway'];

  protected logs$: Observable<HealthCheckLogsResponse>;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    public readonly apiHealthV2Service: ApiHealthV2Service,
    public readonly snackBarService: SnackBarService,
    public readonly destroyRef: DestroyRef,
  ) {}

  ngOnInit() {
    this.logs$ = this.apiHealthV2Service.activeFilter().pipe(
      tap(() => {
        this.isLoading.set(true);
      }),
      combineLatestWith(this.filters$),
      distinctUntilChanged(isEqual),
      switchMap(([timeRange, tableFilter]) =>
        this.apiHealthV2Service.getApiHealthCheckLogs(this.apiId, {
          from: timeRange.from,
          to: timeRange.to,
          page: tableFilter.pagination.index,
          perPage: tableFilter.pagination.size,
          success: this.showSuccessLogs,
        }),
      ),
      tap(() => {
        this.isLoading.set(false);
      }),
      catchError(({ error }) => {
        this.snackBarService.error(error.message);
        return EMPTY;
      }),
    );
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters$.next({ ...this.filters$.value, ...filters });
  }
}
