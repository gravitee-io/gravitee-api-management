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

import { Component, DestroyRef, Input, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';
import { switchMap, tap } from 'rxjs/operators';
import { Observable, zip } from 'rxjs';
import { MatSort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { AsyncPipe, DecimalPipe, TitleCasePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GioChartLineModule } from '../../../../../shared/components/gio-chart-line/gio-chart-line.module';
import { ApiHealthV2Service } from '../../../../../services-ngx/api-health-v2.service';
import { ApiAvailability, ApiAverageResponseTime, FieldParameter } from '../../../../../entities/management-api-v2/api/v4/healthCheck';
import { GioTableWrapperModule } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioTableWrapperFilters } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

interface TableData {
  name: string;
  availability: number;
  responseTime: number;
}

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
  selector: 'availability-per-field',
  standalone: true,
  styleUrl: './availability-per-field.component.scss',
  templateUrl: './availability-per-field.component.html',
})
export class AvailabilityPerFieldComponent implements OnInit {
  @Input()
  field: FieldParameter;

  currentFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: { index: 1, size: 5 },
  };

  private apiId = this.activatedRoute.snapshot.params.apiId;
  public isLoading = true;

  private responseTimeData$: Observable<ApiAverageResponseTime>;
  private availabilityData$: Observable<ApiAvailability>;

  protected displayedColumns: string[] = ['name', 'availability', 'response-time'];
  protected dataSource: TableData[];
  protected filteredDataSource: TableData[];
  protected totalLength: number;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    public readonly apiHealthV2Service: ApiHealthV2Service,
    public readonly snackBarService: SnackBarService,
    public readonly destroyRef: DestroyRef,
  ) {}

  ngOnInit() {
    this.availabilityData$ = this.apiHealthV2Service.activeFilter().pipe(
      takeUntilDestroyed(this.destroyRef),
      tap(() => (this.isLoading = true)),
      switchMap((timeRangeParam) =>
        this.apiHealthV2Service.getApiAvailability(this.apiId, timeRangeParam.from, timeRangeParam.to, this.field),
      ),
    );
    this.responseTimeData$ = this.apiHealthV2Service.activeFilter().pipe(
      takeUntilDestroyed(this.destroyRef),
      tap(() => (this.isLoading = true)),
      switchMap((timeRangeParam) =>
        this.apiHealthV2Service.getApiAverageResponseTime(this.apiId, timeRangeParam.from, timeRangeParam.to, this.field),
      ),
    );

    zip(this.availabilityData$, this.responseTimeData$, (availabilityData: ApiAvailability, responseTimeData: ApiAverageResponseTime) =>
      this.mergeGroups(responseTimeData, availabilityData),
    ).subscribe({
      next: (mergedData) => {
        this.isLoading = false;
        this.dataSource = mergedData;
        this.filteredDataSource = this.dataSource.slice(0, 5);
        this.totalLength = this.dataSource.length;
      },
      error: (e) => {
        this.isLoading = false;
        this.snackBarService.error(e.error?.message ?? 'An error occurred while loading list.');
      },
    });
  }

  mergeGroups = (responseTimes: ApiAverageResponseTime, availability: ApiAvailability): TableData[] => {
    const tableData: TableData[] = [];
    Object.keys(responseTimes.group).forEach((key) => {
      if (key in availability.group) {
        tableData.push({
          name: key,
          availability: availability.group[key],
          responseTime: responseTimes.group[key],
        });
      }
    });

    return tableData;
  };

  onFiltersChanged(filters: GioTableWrapperFilters) {
    const filtered = gioTableFilterCollection(this.dataSource, filters);
    this.filteredDataSource = filtered.filteredCollection;
    this.totalLength = filtered.unpaginatedLength;
  }
}
